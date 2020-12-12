/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.factory.geo.ConfigFundamental;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.ops.DConvertMatrixStruct;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Given a {@link LookUpSimilarImages graph of images} with similar appearance, create a graph in which
 * images with a geometric relationship are connected to each other. Determine if that relationship has 3D geometry
 * or is composed of a homography.
 *
 * If there is a geometric relationship or not is determined by the number of inliers. The minimum number is specified
 * by {@link #minimumInliers}. A threshold is used for classifying an edge as 3D or not {@link #ratio3D} len(F)/len(H)
 * a value of 1 just requires equality, greater than one means there must be more features from F (fundamental) than
 * H (homography). See [1] for more details on this test.
 *
 * <p>[1] Pollefeys, Marc, et al. "Visual modeling with a hand-held camera." International Journal of Computer
 * Vision 59.3 (2004): 207-232.</p>
 *
 * @author Peter Abeles
 */
public class GeneratePairwiseImageGraph implements VerbosePrint {
	public final @Getter PairwiseImageGraph graph = new PairwiseImageGraph();
	private List<String> imageIds;

	// concensus matching algorithms
	ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;
	ModelMatcher<Homography2D_F64, AssociatedPair> ransacH;

	/**
	 * The minimum number of inliers for an edge to be accepted
	 */
	public @Getter @Setter int minimumInliers = 30;
	/**
	 * If number of matches from fundamental divided by homography is more than this then it is considered a 3D scene
	 */
	public @Getter @Setter double ratio3D = 1.5;

	private PrintStream verbose;

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

		ransac3D = FactoryMultiViewRobust.fundamentalRansac(configF, configRansacF);
		ransacH = FactoryMultiViewRobust.homographyRansac(null, configRansacH);
	}

	/**
	 * Connects images by testing features for a geometric relationship. Retrieve the results using
	 * {@link #getGraph()}
	 *
	 * @param db Images with feature associations
	 */
	public void process( LookUpSimilarImages db ) {
		this.imageIds = db.getImageIDs();
		this.graph.reset();

		List<String> similar = new ArrayList<>();
		DogArray<Point2D_F64> srcFeats = new DogArray<>(Point2D_F64::new);
		DogArray<Point2D_F64> dstFeats = new DogArray<>(Point2D_F64::new);
		DogArray<AssociatedIndex> matches = new DogArray<>(AssociatedIndex::new);
		DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);

		// map to quickly look up the ID of a view
		Map<String, Integer> imageToIndex = new HashMap<>();

		// Create a node in the graph for each image
		for (int idxTgt = 0; idxTgt < imageIds.size(); idxTgt++) {
			imageToIndex.put(imageIds.get(idxTgt), idxTgt);
			graph.createNode(imageIds.get(idxTgt));
		}

		if (verbose != null) verbose.println("total images = " + imageIds.size());

		// For each image examine all related images for a true geometric relationship
		// if one exists then add an edge to the graph describing their relationship
		for (int idxTgt = 0; idxTgt < imageIds.size(); idxTgt++) {
			String src = imageIds.get(idxTgt);

			db.findSimilar(src, similar);
			db.lookupPixelFeats(src, srcFeats);

			if (verbose != null) verbose.println("ID=" + src + " similar=" + similar.size() + "  obs=" + srcFeats.size);

			graph.nodes.get(idxTgt).totalObservations = srcFeats.size;

			for (int idxSimilar = 0; idxSimilar < similar.size(); idxSimilar++) {
				String dst = similar.get(idxSimilar);

				// make sure it isn't considering the same motion twice
				int dstIdx = imageToIndex.get(dst);
				if (dstIdx <= idxTgt)
					continue;

				// get information on the features and association
				db.lookupPixelFeats(dst, dstFeats);
				db.lookupMatches(src, dst, matches);

				pairs.reset();
				for (int i = 0; i < matches.size; i++) {
					AssociatedIndex m = matches.get(i);
					pairs.grow().setTo(srcFeats.get(m.src), dstFeats.get(m.dst));
				}

				createEdge(src, dst, pairs, matches);
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
	protected void createEdge( String src, String dst,
							   DogArray<AssociatedPair> pairs, DogArray<AssociatedIndex> matches ) {
		// Fitting Essential/Fundamental works when the scene is not planar and not pure rotation
		int countF = 0;
		if (ransac3D.process(pairs.toList())) {
			countF = ransac3D.getMatchSet().size();
		}

		// Fitting homography will work when all or part of the scene is planar or motion is pure rotation
		int countH = 0;
		if (ransacH.process(pairs.toList())) {
			countH = ransacH.getMatchSet().size();
		}

		if( verbose != null ) verbose.println("   dst='"+dst+"' ransac F="+countF+" H="+countH+" pairs.size="+pairs.size());

		// fail if not enough features are remaining after RANSAC
		if (Math.max(countF, countH) < minimumInliers)
			return;

		// The idea here is that if the number features for F is greater than H then it's a 3D scene.
		// If they are similar then it might be a plane
		boolean is3D = countF > countH * ratio3D;

		PairwiseImageGraph.Motion edge = graph.edges.grow();
		edge.is3D = is3D;
		edge.countF = countF;
		edge.countH = countH;
		edge.index = graph.edges.size - 1;
		edge.src = graph.lookupNode(src);
		edge.dst = graph.lookupNode(dst);
		edge.src.connections.add(edge);
		edge.dst.connections.add(edge);

		if (is3D) {
			saveInlierMatches(ransac3D, matches, edge);
			edge.F.set(ransac3D.getModelParameters());
		} else {
			saveInlierMatches(ransacH, matches, edge);
			Homography2D_F64 H = ransacH.getModelParameters();
			DConvertMatrixStruct.convert(H, edge.F);
		}
	}

	/**
	 * Puts the inliers from RANSAC into the edge's list of associated features
	 *
	 * @param ransac RANSAC
	 * @param matches List of matches from feature association
	 * @param edge The edge that the inliers are to be saved to
	 */
	private void saveInlierMatches( ModelMatcher<?, ?> ransac,
									DogArray<AssociatedIndex> matches, PairwiseImageGraph.Motion edge ) {

		int N = ransac.getMatchSet().size();
		edge.inliers.reset();
		edge.inliers.resize(N);
		for (int i = 0; i < N; i++) {
			int idx = ransac.getInputIndex(i);
			edge.inliers.get(i).set(matches.get(idx));
		}
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
