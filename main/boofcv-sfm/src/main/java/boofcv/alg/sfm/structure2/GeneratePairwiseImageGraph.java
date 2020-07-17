/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.sfm.structure2;

import boofcv.factory.geo.ConfigFundamental;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.ops.ConvertDMatrixStruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Given a {@link LookupSimilarImages graph of images} with similar appearance, create a graph in which
 * images with a geometric relationship are connected to each other. Determine if that relationship has 3D geometry
 * or is composed of a homography.
 *
 * If there is a geometric relationship or not is determined by the number of inliers. The minimum number is specified
 * by {@link #minimumInliers}. A threshold is used for classifying an edge as 3D or not {@link #ratio3D} len(F)/len(H)
 * a value of 1 just requires equality, greater than one means there must be more features from F (fundamental) than
 * H (homography).
 *
 * @author Peter Abeles
 */
public class GeneratePairwiseImageGraph {
	public PairwiseImageGraph2 graph = new PairwiseImageGraph2();
	private List<String> imageIds;

	// concensus matching algorithms
	ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;
	ModelMatcher<Homography2D_F64,AssociatedPair> ransacH;

	/**
	 * The minimum number of inliers for an edge to be accepted
	 */
	public int minimumInliers = 30;
	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public double ratio3D = 1.5;

	/**
	 * Configures and declares concensum matching algorithms
	 */
	public GeneratePairwiseImageGraph() {
		ConfigRansac configRansacF = new ConfigRansac();
		configRansacF.iterations = 500;
		configRansacF.inlierThreshold = 1;

		// F computes epipolar error, which isn't as strict as reprojection error for H, so give H a larger error tol
		ConfigRansac configRansacH = new ConfigRansac();
		configRansacH.iterations = 500;
		configRansacH.inlierThreshold = 2.0;

		ConfigFundamental configF = new ConfigFundamental();
		configF.errorModel = ConfigFundamental.ErrorModel.GEOMETRIC;
		configF.numResolve = 1;

		ransac3D = FactoryMultiViewRobust.fundamentalRansac(configF,configRansacF);
		ransacH = FactoryMultiViewRobust.homographyRansac(null,configRansacH);
	}

	/**
	 * Connects images by testing features for a geometric relationship. Retrieve the results using
	 * {@link #getGraph()}
	 *
	 * @param db Images with feature associations
	 */
	public void process( LookupSimilarImages db ) {
		this.imageIds = db.getImageIDs();
		this.graph.reset();

		List<String> similar = new ArrayList<>();
		FastQueue<Point2D_F64> srcFeats = new FastQueue<>(Point2D_F64::new);
		FastQueue<Point2D_F64> dstFeats = new FastQueue<>(Point2D_F64::new);
		FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex::new);
		FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair::new);

		// map to quickly look up the ID of a view
		Map<String,Integer> imageToindex = new HashMap<>();

		// Create a node in the graph for each image
		for (int idxTgt = 0; idxTgt < imageIds.size(); idxTgt++) {
			imageToindex.put(imageIds.get(idxTgt),idxTgt);
			graph.createNode(imageIds.get(idxTgt));
		}

		// For each image examine all related images for a true geometric relationship
		// if one exists then add an edge to the graph describing their relationship
		for (int idxTgt = 0; idxTgt < imageIds.size(); idxTgt++) {
			String src = imageIds.get(idxTgt);

			db.findSimilar(src,similar);
			db.lookupPixelFeats(src,srcFeats);

			graph.nodes.get(idxTgt).totalObservations = srcFeats.size;

			for (int idxSimilar = 0; idxSimilar < similar.size(); idxSimilar++) {
				String dst = similar.get(idxSimilar);

				// make sure it isn't considering the same motion twice
				int dstIdx = imageToindex.get(dst);
				if( dstIdx <= idxTgt )
					continue;

				// get information on the features and association
				db.lookupPixelFeats(dst,dstFeats);
				db.lookupMatches(src,dst,matches);

				pairs.reset();
				for (int i = 0; i < matches.size; i++) {
					AssociatedIndex m = matches.get(i);
					pairs.grow().set(srcFeats.get(m.src),dstFeats.get(m.dst));
				}

				createEdge(src,dst,pairs,matches);
			}
		}
	}

	/**
	 * Connects two views together if they meet a minimal set of geometric requirements. Determines if there
	 * is strong evidence that there is 3D information present and not just a homography
	 *
	 * @param src ID of src image
	 * @param dst ID of dst image
	 * @param pairs Associated features pixels
	 * @param matches Associated features feature indexes
	 */
	protected void createEdge( String src , String dst ,
							   FastQueue<AssociatedPair> pairs , FastQueue<AssociatedIndex> matches ) {
		// Fitting Essential/Fundamental works when the scene is not planar and not pure rotation
		int countF = 0;
		if( ransac3D.process(pairs.toList()) ) {
			countF = ransac3D.getMatchSet().size();
		}

		// Fitting homography will work when all or part of the scene is planar or motion is pure rotation
		int countH = 0;
		if( ransacH.process(pairs.toList()) ) {
			countH = ransacH.getMatchSet().size();
		}

		// fail if not enough features are remaining after RANSAC
		if( Math.max(countF,countH) < minimumInliers )
			return;

		// The idea here is that if the number features for F is greater than H then it's a 3D scene.
		// If they are similar then it might be a plane
		boolean is3D = countF > countH*ratio3D;

		PairwiseImageGraph2.Motion edge = graph.edges.grow();
		edge.is3D = is3D;
		edge.countF = countF;
		edge.countH = countH;
		edge.index = graph.edges.size-1;
		edge.src = graph.lookupNode(src);
		edge.dst = graph.lookupNode(dst);
		edge.src.connections.add(edge);
		edge.dst.connections.add(edge);

		if( is3D ) {
			saveInlierMatches(ransac3D, matches,edge);
			edge.F.set(ransac3D.getModelParameters());
		} else {
			saveInlierMatches(ransacH, matches,edge);
			Homography2D_F64 H = ransacH.getModelParameters();
			ConvertDMatrixStruct.convert(H,edge.F);
		}
	}

	/**
	 * Puts the inliers from RANSAC into the edge's list of associated features
	 * @param ransac RANSAC
	 * @param matches List of matches from feature association
	 * @param edge The edge that the inliers are to be saved to
	 */
	private void saveInlierMatches(ModelMatcher<?, ?> ransac,
								   FastQueue<AssociatedIndex> matches, PairwiseImageGraph2.Motion edge) {

		int N = ransac.getMatchSet().size();
		edge.inliers.reset();
		for (int i = 0; i < N; i++) {
			int idx = ransac.getInputIndex(i);
			edge.inliers.grow().set(matches.get(idx));
		}
	}

	public PairwiseImageGraph2 getGraph() {
		return graph;
	}

	public int getMinimumInliers() {
		return minimumInliers;
	}

	public void setMinimumInliers(int minimumInliers) {
		this.minimumInliers = minimumInliers;
	}

	public double getRatio3D() {
		return ratio3D;
	}

	public void setRatio3D(double ratio3D) {
		this.ratio3D = ratio3D;
	}
}
