/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a {@link LookupSimilarImages graph of images} with similar appearance, create a graph in which
 * images with
 *
 * @author Peter Abeles
 */
public class GeneratePairwiseImageGraph {
	PairwiseImageGraph2 graph = new PairwiseImageGraph2();
	List<String> imageIds;

	protected ModelMatcher<DMatrixRMaj, AssociatedPair> ransac3D;
	protected ModelMatcher<DMatrixRMaj, AssociatedPair> ransacH;

	int minimumInliers = 50;

	public void process( LookupSimilarImages similarImages ) {
		this.imageIds = similarImages.getImageIDs();
		this.graph.reset();

		List<String> similar = new ArrayList<>();
		FastQueue<Point2D_F64> srcFeats = new FastQueue<>(Point2D_F64.class,true);
		FastQueue<Point2D_F64> dstFeats = new FastQueue<>(Point2D_F64.class,true);
		FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class,true);
		FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair.class,true);

		for (int idxTgt = 0; idxTgt < imageIds.size(); idxTgt++) {
			graph.createNode(imageIds.get(idxTgt));
		}

		for (int idxTgt = 0; idxTgt < imageIds.size(); idxTgt++) {
			String src = imageIds.get(idxTgt);

			similarImages.findSimilar(src,similar);
			similarImages.lookupFeatures(src,srcFeats);

			for (int idxSimilar = 0; idxSimilar < similar.size(); idxSimilar++) {
				String dst = similar.get(idxSimilar);
				similarImages.lookupFeatures(dst,dstFeats);

				similarImages.lookupMatches(src,dst,matches);

				pairs.reset();
				for (int i = 0; i < matches.size; i++) {
					AssociatedIndex m = matches.get(i);
					pairs.grow().set(srcFeats.get(m.src),dstFeats.get(m.dst));
				}

				createEdge(src,dst,pairs,matches);
			}
		}
	}

	protected void createEdge( String src , String dst ,
							   FastQueue<AssociatedPair> pairs , FastQueue<AssociatedIndex> matches ) {
		if( !ransac3D.process(pairs.toList()) ) {
			return;
		}
		int count3D = ransac3D.getMatchSet().size();
		if( count3D < minimumInliers )
			return;

		int countH = 0;
		if( ransacH.process(pairs.toList()) ) {
			countH = ransacH.getMatchSet().size();
		}

		// TODO can SVD of F be used to see if it's pure rotation or not? It's fine if there's a translation

		boolean is3D = count3D > countH*1.2;

		PairwiseImageGraph2.Motion edge = graph.edges.grow();
		edge.is3D = is3D;
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
			edge.F.set(ransacH.getModelParameters());
		}
	}

	private void saveInlierMatches(ModelMatcher<DMatrixRMaj, AssociatedPair> ransac,
								   FastQueue<AssociatedIndex> matches, PairwiseImageGraph2.Motion edge) {

		int N = ransac.getMatchSet().size();
		edge.associated.reset();
		for (int i = 0; i < N; i++) {
			int idx = ransac.getInputIndex(i);
			edge.associated.grow().set(matches.get(idx));
		}
	}
}
