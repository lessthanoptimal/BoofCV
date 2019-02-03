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
import boofcv.struct.feature.AssociatedIndex;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.*;

/**
 * Goal: find clusters of views with 3D connections that share a large number of common features.
 *
 * @author Peter Abeles
 */
public class DoStuffFromPairwiseGraph {

	List<SeedInfo> scoresNodes = new ArrayList<>();
	FastQueue<ScoreIndex> scoresMotions = new FastQueue<>(ScoreIndex.class,true);

	ProjectiveInitializeAllCommon initProjective = new ProjectiveInitializeAllCommon();
	ProjectiveExpandStructure expandProjective = new ProjectiveExpandStructure();

	public void process( LookupSimilarImages db , PairwiseImageGraph2 graph ) {
		// Score nodes for their ability to be seeds
		Map<Integer, SeedInfo> mapScores = scoreNodesAsSeeds(graph);
		List<SeedInfo> seeds = selectSeeds(graph, mapScores);

		for (int i = 0; i < seeds.size(); i++) {
			// Find the common features
			GrowQueue_I32 common = findCommonTracks(seeds.get(i));

			// initialize projective scene using common tracks
			initProjective.process(db,seeds.get(i).seed,common,null); // TODO get just the motions used in scoring

			// TODO Grow the projective view to include all connected views that meet minimum conditions

			// TODO Compute metric scenes from projective
		}

		for (int i = 0; i < seeds.size(); i++) {
			// TODO add new views to the closest cluster

			// do not attach new views to a view once it is a member of two clusters
		}

		// once there are no more views which can be added to a cluster
		// 1) prune views which are not attached
		// 2) merge adjacent clusters together
	}

	/**
	 * Finds the indexes of tracks which are common to all views
	 * @param target The seed view
	 * @return indexes of common tracks
	 */
	private GrowQueue_I32 findCommonTracks( SeedInfo target ) {
		// if true then it is visible in all tracks
		boolean visibleAll[] = new boolean[target.seed.totalFeatures];
		Arrays.fill(visibleAll,true);
		// used to keep track of which features are visible in the current motion
		boolean visibleMotion[] = new boolean[target.seed.totalFeatures];

		// Only look at features in the motions that were used to compute the score
		for (int idxMotion = 0; idxMotion < target.motions.size; idxMotion++) {
			PairwiseImageGraph2.Motion m = target.seed.connections.get(target.motions.get(idxMotion));
			boolean seedIsSrc = m.src==target.seed;
			Arrays.fill(visibleMotion,false);
			for (int i = 0; i < m.associated.size; i++) {
				AssociatedIndex a = m.associated.get(i);
				visibleMotion[seedIsSrc?a.src:a.dst] = true;
			}
			for (int i = 0; i < target.seed.totalFeatures; i++) {
				visibleAll[i] &= visibleMotion[i];
			}
		}
		GrowQueue_I32 common = new GrowQueue_I32(target.seed.totalFeatures/10+1);
		for (int i = 0; i < target.seed.totalFeatures; i++) {
			if( visibleAll[i] ) {
				common.add(i);
			}
		}
		return common;
	}


	private Map<Integer, SeedInfo> scoreNodesAsSeeds(PairwiseImageGraph2 graph) {
		scoresNodes.clear();
		Map<Integer,SeedInfo> mapScores = new HashMap<>();
		for (int idxView = 0; idxView < graph.nodes.size; idxView++) {
			View v = graph.nodes.get(idxView);
			SeedInfo info = score(v);
			scoresNodes.add(info);
			mapScores.put(idxView,info);
		}
		return mapScores;
	}

	private List<SeedInfo> selectSeeds(PairwiseImageGraph2 graph, Map<Integer, SeedInfo> mapScores) {
		// Greedily assign nodes as seeds while making their neighbors as not seeds
		int maxSeeds = Math.max(1,graph.nodes.size/5);
		List<SeedInfo> seeds = new ArrayList<>();
		Collections.sort(scoresNodes);

		// ignore nodes with too low of a score
		double minScore = scoresNodes.get(scoresNodes.size()-1).score*0.2;

		// grab nodes with the highest scores first
		for (int i = scoresNodes.size()-1; i >= 0 && seeds.size()<maxSeeds; i--) {
			SeedInfo s = scoresNodes.get(i);
			if( s.score <= minScore )
				continue;

			seeds.add(s);

			// zero the score of children so that they can't be a seed
			for (int j = 0; j < s.seed.connections.size; j++) {
				mapScores.get(s.seed.connections.get(j).index).score = 0;
			}
		}
		return seeds;
	}

	/**
	 * Score a view for how well it could be a seed based on the the 3 best 3D motions associated with it
	 */
	private SeedInfo score( View target ) {
		SeedInfo output = new SeedInfo();
		output.seed = target;
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

		for (int i = Math.min(2, scoresMotions.size); i >= 0; i--) {
			output.motions.add(scoresMotions.get(i).index);
			output.score += scoresMotions.get(i).score;
		}

		return output;
	}

	/**
	 * Scores the motion for its ability to capture 3D structure
	 */
	public static double score( PairwiseImageGraph2.Motion m ) {
		// countF and countF will be <= totalFeatures

		// Prefer a scene more features from a fundamental matrix than a homography.
		// This can be sign that the scene has a rich 3D structure and is poorly represented by
		// a plane or rotational motion
		double score = Math.min(5,m.countF/(double)(m.countH+1));
		// Also prefer more features from the original image to be matched
		score *= m.countF;

		return score;
	}

	private static class SeedInfo implements Comparable<SeedInfo> {
		View seed;
		double score;
		GrowQueue_I32 motions = new GrowQueue_I32();

		@Override
		public int compareTo(SeedInfo o) {
			return Double.compare(score, o.score);
		}
	}
}
