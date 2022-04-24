/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.slam;

import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.alg.structure.PairwiseImageGraph;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch Simultaneous Location and Mapping (SLAM) system which assumed a known multi camera system is viewing the world.
 * A feature based approach is used were first salient image features are found then matched to each other using
 * descriptors.
 *
 * @author Peter Abeles
 */
public class BatchSlamMultiCameras {

	int countConsideredConnections = 3;

	// Checks to see if two views where captured at the same time by the multi-camera system
	CheckSynchronized checkSynchronized;

	GeneratePairwiseGraphFromMultiCameraSystem generatePairwise;

	DogArray<SeedInfo> seeds = new DogArray<>(SeedInfo::new, SeedInfo::reset);

	public void process( MultiCameraSystem sensors, LookUpSimilarImages similarImages ) {
		// Learn how much geometric information is available between views
		generatePairwise.process(sensors, similarImages);

		// Decide which views are preferred as seeds
		scoreViewsAsSeeds();

		// Select seeds and perform reconstructions
		while (true) {
			// TODO select view with the best seed score

			// TODO pick a seed, grow the reconstruction graph until there are no more views it can add
		}

		// TODO Batch refine all scenes
	}

	/**
	 * Score each view as a potential seed.
	 */
	void scoreViewsAsSeeds() {
		FastAccess<PairwiseImageGraph.View> views = generatePairwise.getPairwise().nodes;
		seeds.resetResize(views.size);
		for (int viewIdx = 0; viewIdx < views.size; viewIdx++) {
			PairwiseImageGraph.View v = views.get(viewIdx);
			SeedInfo s = seeds.get(viewIdx);
			s.viewIndex = viewIdx;
			scoreViewAsSeed(v, s);
		}
	}

	/**
	 * Scores a view as a seed based on the scores of the best N connected motion. Motions are selected which are
	 * discintive from other already selected motions
	 */
	void scoreViewAsSeed( PairwiseImageGraph.View target, SeedInfo info ) {
		// Select the motion with the best score
		double bestScore = 0;
		int bestIndex = -1;
		for (int i = 0; i < target.connections.size; i++) {
			PairwiseImageGraph.Motion m = target.connections.get(i);

			if (!m.is3D || m.score3D <= bestScore)
				continue;

			bestScore = m.score3D;
			bestIndex = i;
		}
		// Nothing was selected, this is a horrible seed
		if (bestIndex == -1)
			return;

		addNeighbor(info, target, target.connections.get(bestIndex), bestScore);

		// The remaining neighbors are selected by finding the neighbor which the best score which is the minimum
		// of the score to the target and any of the already connected. This avoids adds two very similar view
		// that happen to have a high score to the target
		for (int considerIdx = 1; considerIdx < countConsideredConnections; considerIdx++) {
			bestScore = 0;
			bestIndex = -1;
			for (int connIdx = 0; connIdx < target.connections.size; connIdx++) {
				PairwiseImageGraph.Motion m = target.connections.get(connIdx);
				PairwiseImageGraph.View mview = m.other(target);

				// Only consider motions which have not been selected and could have a better score
				if (!m.is3D || m.score3D <= bestScore || info.neighbors.contains(m))
					continue;

				double score = m.score3D;
				for (int nghIdx = 0; nghIdx < info.neighbors.size(); nghIdx++) {
					PairwiseImageGraph.View nview = info.neighbors.get(nghIdx).other(target);
					@Nullable PairwiseImageGraph.Motion m2n = mview.findMotion(nview);
					if (m2n == null)
						continue;
					if (m2n.is3D && m2n.score3D < score)
						score = m2n.score3D;
				}

				if (score <= bestScore)
					continue;

				bestScore = score;
				bestIndex = connIdx;
			}

			if (bestIndex == -1)
				return;

			addNeighbor(info, target, target.connections.get(bestIndex), bestScore);
		}
	}

	public void addNeighbor( SeedInfo info, PairwiseImageGraph.View target, PairwiseImageGraph.Motion m, double score ) {
		info.knownScale |= isExtrinsicsKnown(target, m.other(target));
		info.score += score;
		info.neighbors.add(m);
	}

	/** Returns true if the two views have a known baseline / extrinsics between them */
	public boolean isExtrinsicsKnown( PairwiseImageGraph.View va, PairwiseImageGraph.View vb ) {
		return checkSynchronized.isSynchronized(va.id, vb.id);
	}

	public static class SeedInfo implements Comparable<SeedInfo> {
		int viewIndex;
		double score;
		// This seed will have a known scale if one of the motions associated with it comes from a known stereo pair
		boolean knownScale;
		// List of motions which connect to a neighboring view used to compute the seed's score
		List<PairwiseImageGraph.Motion> neighbors = new ArrayList<>();

		public void reset() {
			viewIndex = -1;
			score = 0;
			knownScale = false;
			neighbors.clear();
		}

		/**
		 * Prefer a seed with a known scale and higher score
		 */
		@Override public int compareTo( BatchSlamMultiCameras.SeedInfo o ) {
			if (knownScale == o.knownScale) {
				return Double.compare(o.score, score);
			} else if (knownScale) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
