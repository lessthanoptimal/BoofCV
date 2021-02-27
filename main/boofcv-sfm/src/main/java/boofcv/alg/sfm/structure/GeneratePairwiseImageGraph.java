/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Given a {@link LookUpSimilarImages graph of images} with similar appearance, create a graph in which
 * images with a geometric relationship are connected to each other. Determine if that relationship has 3D geometry
 * or is composed of a homography.
 *
 * If there is a geometric relationship or not is determined using the passed in {@link EpipolarScore3D}.
 *
 * @author Peter Abeles
 */
public class GeneratePairwiseImageGraph implements VerbosePrint {
	public final @Getter PairwiseImageGraph graph = new PairwiseImageGraph();
	private List<String> imageIds;

	/** Used to score if the two views have a 3D relationship or not */
	public final @Getter EpipolarScore3D epipolarScore;

	private PrintStream verbose;

	/**
	 * Specifies consensus matching algorithms
	 */
	public GeneratePairwiseImageGraph( EpipolarScore3D epipolarScore ) {
		this.epipolarScore = epipolarScore;
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

		DMatrixRMaj fundamental = new DMatrixRMaj(3, 3);
		DogArray_I32 inlierIdx = new DogArray_I32();

		if (!epipolarScore.process(pairs.toList(), fundamental, inlierIdx)) {
			// Don't create an edge here
			return;
		}

		PairwiseImageGraph.Motion edge = graph.edges.grow();
		edge.is3D = epipolarScore.is3D();
		edge.score3D = epipolarScore.getScore();
		edge.index = graph.edges.size - 1;
		edge.src = graph.lookupNode(src);
		edge.dst = graph.lookupNode(dst);
		edge.src.connections.add(edge);
		edge.dst.connections.add(edge);

		// Allocate memory and copy inliers
		edge.inliers.resize(inlierIdx.size);
		for (int i = 0; i < inlierIdx.size; i++) {
			edge.inliers.get(i).setTo(matches.get(inlierIdx.get(i)));
		}
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
		this.epipolarScore.setVerbose(out, configuration);
	}
}
