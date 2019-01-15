/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.struct.ScoreIndex;
import org.ddogleg.struct.FastQueue;

import java.util.*;

/**
 * Goal: find clusters of views with 3D connections that share a large number of common features.
 *
 * @author Peter Abeles
 */
public class DoStuffFromPairwiseGraph {

	FastQueue<ScoreIndex> scoresNodes = new FastQueue<>(ScoreIndex.class,true);
	FastQueue<ScoreIndex> scoresMotions = new FastQueue<>(ScoreIndex.class,true);

	InitializeProjectiveStructure initProjective = new InitializeProjectiveStructure();

	public void process( PairwiseImageGraph2 graph ) {
		// Score nodes for their ability to be seeds
		Map<Integer, ScoreIndex> mapScores = scoreNodesAsSeeds(graph);
		List<View> seeds = selectSeeds(graph, mapScores);

		for (int i = 0; i < seeds.size(); i++) {
			// TODO create feature tracks from all common features

			// TODO initialize projective scene

			// TODO Compute metric scenes from projective
		}

		// TODO score each view based on how well it will probably estimate the 3D structure

		for (int i = 0; i < seeds.size(); i++) {
			// TODO add new views to the closest cluster

			// do not attach new views to a view once it is a member of two clusters
		}

		// once there are no more views which can be added to a cluster
		// 1) prune views which are not attached
		// 2) merge adjacent clusters together
	}

	private void findCommonTracks( View seed ) {

	}


	private Map<Integer, ScoreIndex> scoreNodesAsSeeds(PairwiseImageGraph2 graph) {
		scoresNodes.reset();
		Map<Integer,ScoreIndex> mapScores = new HashMap<>();
		for (int idxView = 0; idxView < graph.nodes.size; idxView++) {
			View v = graph.nodes.get(idxView);
			mapScores.put(idxView,scoresNodes.grow().set(score(v),idxView));
		}
		return mapScores;
	}

	private List<View> selectSeeds(PairwiseImageGraph2 graph, Map<Integer, ScoreIndex> mapScores) {
		// Greedily assign nodes as seeds while making their neighbors as not seeds
		int maxSeeds = Math.max(1,graph.nodes.size/5);
		List<View> seeds = new ArrayList<>();
		Collections.sort(scoresNodes.toList());
		double minScore = scoresNodes.getTail(0).score*0.2;

		for (int i = scoresNodes.size-1; i >= 0 && seeds.size()<maxSeeds; i--) {
			ScoreIndex s = scoresNodes.get(i);
			if( s.score <= minScore )
				continue;

			View v = graph.nodes.get(s.index);
			seeds.add(v);

			// zero the score of children so that they can't be a seed
			for (int j = 0; j < v.connections.size; j++) {
				mapScores.get(v.connections.get(j).index).score = 0;
			}
		}
		return seeds;
	}

	private double score( View target ) {
		scoresMotions.reset();

		// score all edges
		for (int i = 0; i < target.connections.size; i++) {
			PairwiseImageGraph2.Motion m = target.connections.get(i);
			if( !m.is3D )
				continue;

			scoresMotions.grow().set(score(m),i);
		}

		// only score the 3 best. This is to avoid biasing it for
		Collections.sort(scoresMotions.toList());

		double total = 0;
		for (int i = Math.min(2, scoresMotions.size); i >= 0; i--) {
			total += scoresMotions.get(i).score;
		}

		return total;
	}

	/**
	 * Scores the motion for its ability to capture 3D structure
	 */
	private double score( PairwiseImageGraph2.Motion m ) {
		// countF and countF will be <= totalFeatures

		// Prefer a scene more features from a fundamental matrix than a homography.
		// This can be sign that the scene has a rich 3D structure and is poorly represented by
		// a plane or rotational motion
		double score = Math.min(5,m.countF/(double)m.countH);
		// Also prefer more features from the original image to be matched
		score *= m.countF;

		return score;
	}
}
