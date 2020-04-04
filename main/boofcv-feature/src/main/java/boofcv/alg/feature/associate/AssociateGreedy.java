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
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastAccess;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>
 * Brute force greedy association for objects described by a {@link TupleDesc_F64}.  An
 * object is associated with whichever object has the best fit score and every possible combination
 * is examined.  If there are a large number of features this can be quite slow.
 * </p>
 *
 * <p>
 * Optionally, backwards validation can be used to reduce the number of false associations.
 * Backwards validation works by checking to see if two objects are mutually the best association
 * for each other.  First an association is found from src to dst, then the best fit in dst is
 * associated with feature in src.
 * </p>
 *
 * @param <D> Feature description type.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"Duplicates"})
public class AssociateGreedy<D> extends AssociateGreedyBase<D> {
	/**
	 * Configure association
	 *
	 * @param score Computes the association score.
	 * @param backwardsValidation If true then backwards validation is performed.
	 */
	public AssociateGreedy(ScoreAssociation<D> score,
						   boolean backwardsValidation) {
		super(score,backwardsValidation);
	}

	/**
	 * Associates the two sets objects against each other by minimizing fit score.
	 *
	 * @param src Source list.
	 * @param dst Destination list.
	 */
	@Override
	public void associate(FastAccess<D> src , FastAccess<D> dst )
	{
		fitQuality.reset();
		pairs.reset();
		workBuffer.reset();

		pairs.resize(src.size);
		fitQuality.resize(src.size);
		workBuffer.resize(src.size*dst.size);

		final double ratioTest = this.ratioTest;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.size, i -> {
		for( int i = 0; i < src.size; i++ ) {
			D a = src.data[i];
			double bestScore = maxFitError;
			double secondBest = bestScore;
			int bestIndex = -1;

			final int workIdx = i*dst.size;
			for( int j = 0; j < dst.size; j++ ) {
				D b = dst.data[j];

				double fit = score.score(a,b);
				workBuffer.set(workIdx+j,fit);

				if( fit <= bestScore ) {
					bestIndex = j;
					secondBest = bestScore;
					bestScore = fit;
				}
			}

			if( ratioTest < 1.0 && bestIndex != -1 && bestScore != 0.0 ) {
				// the second best could lie after the best was seen
				for (int j = bestIndex+1; j < dst.size; j++) {
					double fit = workBuffer.get(workIdx+j);
					if( fit < secondBest ) {
						secondBest = fit;
					}
				}
				pairs.set(i,secondBest*ratioTest >= bestScore ? bestIndex : -1);
			} else {
				pairs.set(i,bestIndex);
			}

			fitQuality.set(i,bestScore);
		}
		//CONCURRENT_ABOVE });

		if( backwardsValidation ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.size, i -> {
			for( int i = 0; i < src.size; i++ ) {
				int match = pairs.data[i];
				if( match == -1 )
					//CONCURRENT_BELOW return;
					continue;

				double scoreToBeat = workBuffer.data[i*dst.size+match];

				for( int j = 0; j < src.size; j++ , match += dst.size ) {
					if( workBuffer.data[match] <= scoreToBeat && j != i) {
						pairs.data[i] = -1;
						fitQuality.data[i] = Double.MAX_VALUE;
						break;
					}
				}
			}
			//CONCURRENT_ABOVE });
		}
	}
}
