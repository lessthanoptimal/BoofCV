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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.ScoreAssociation;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Greedily assigns two features to each other based on their scores while pruning features based on their
 * distance apart. it is a brute force algorithm since the distance between all possible pairs of source and
 * destination and checked to see if they are too far apart. Distance is computed using the provided distance
 * function. Examples of distance include Euclidean and Epipolar.
 *
 * @author Peter Abeles
 */
public class AssociateGreedyBruteForce2D<D> extends AssociateGreedyBase2D<D> {

	/**
	 * Specifies score mechanism
	 *
	 * @param scoreAssociation How features are scored.
	 */
	public AssociateGreedyBruteForce2D( ScoreAssociation<D> scoreAssociation,
										AssociateImageDistanceFunction distanceFunction ) {
		super(scoreAssociation, distanceFunction);
	}

	/**
	 * Performs association by computing a 2D score matrix. First the score matrix is computed while finding
	 * the best fit relative to the source. Then additional sanity checks are done to see if it's a valid match/
	 */
	@Override
	public void associate() {
		setupForAssociate(descSrc.size, descDst.size);

		final double ratioTest = this.ratioTest;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0,descSrc.size,distances, (distanceFunction,idx0,idx1) -> {
		int idx0 = 0, idx1 = descSrc.size;
		for (int idxSrc = idx0; idxSrc < idx1; idxSrc++) {
			distanceFunction.setSource(idxSrc, locationSrc.get(idxSrc));
			D a = descSrc.data[idxSrc];
			double bestScore = maxFitError;
			double secondBest = bestScore;
			int bestIndex = -1;

			final int workIdx = idxSrc*descDst.size;
			for (int idxDst = 0; idxDst < descDst.size; idxDst++) {
				D b = descDst.data[idxDst];

				// compute distance between the two features and don't even consider if too far apart
				double distance = distanceFunction.distance(idxDst, locationDst.get(idxDst));
				if (distance > maxDistanceUnits) {
					scoreMatrix.set(workIdx + idxDst, maxFitError);
					continue;
				}

				double fit = score.score(a, b);
				scoreMatrix.set(workIdx + idxDst, fit);

				if (fit <= bestScore) {
					bestIndex = idxDst;
					secondBest = bestScore;
					bestScore = fit;
				}
			}

			if (ratioTest < 1.0 && bestIndex != -1 && bestScore != 0.0) {
				// the second best could lie after the best was seen
				for (int j = bestIndex + 1; j < descDst.size; j++) {
					double fit = scoreMatrix.get(workIdx + j);
					if (fit < secondBest) {
						secondBest = fit;
					}
				}
				pairs.set(idxSrc, secondBest*ratioTest >= bestScore ? bestIndex : -1);
			} else {
				pairs.set(idxSrc, bestIndex);
			}

			fitQuality.set(idxSrc, bestScore);
		}
		//CONCURRENT_ABOVE }});

		if (backwardsValidation) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, descSrc.size, i -> {
			for (int i = 0; i < descSrc.size; i++) {
				forwardsBackwards(i, descSrc.size, descDst.size);
			}
			//CONCURRENT_ABOVE });
		}
	}
}
