/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.struct.FastQueue;
import boofcv.struct.feature.TupleDesc_F64;
import pja.storage.GrowQueue_F64;
import pja.storage.GrowQueue_I32;


/**
 * Different variants of greedy association for objects described by a {@link TupleDesc_F64}.  An
 * object is associated with whichever object has the best fit score.  Each variant is different
 * in how it scores this fit.
 *
 * @author Peter Abeles
 */
public class AssociateGreedy<T> {

	// computes association score
	private ScoreAssociation<T> score;
	// worst allowed fit score to associate
	private double maxFitError;
	// stores the quality of fit score
	private GrowQueue_F64 fitQuality = new GrowQueue_F64(100);
	// stores indexes of associated
	private GrowQueue_I32 pairs = new GrowQueue_I32(100);
	// various
	private GrowQueue_F64 workBuffer = new GrowQueue_F64(100);
	// if true backwardsValidation is done
	private boolean backwardsValidation;

	public AssociateGreedy(ScoreAssociation<T> score,
								double maxFitError,
								boolean backwardsValidation) {
		this.score = score;
		this.maxFitError = maxFitError;
		this.backwardsValidation = backwardsValidation;
	}

	public void associate( FastQueue<T> src ,
						   FastQueue<T> dst )
	{
		fitQuality.reset();
		pairs.reset();
		workBuffer.reset();

		for( int i = 0; i < src.size; i++ ) {
			T a = src.data[i];
			double bestScore = maxFitError;
			int bestIndex = -1;

			for( int j = 0; j < dst.size; j++ ) {
				T b = dst.data[j];

				double fit = score.score(a,b);
				workBuffer.push(fit);

				if( fit < bestScore ) {
					bestIndex = j;
					bestScore = fit;
				}
			}
			pairs.push(bestIndex);
			fitQuality.push(bestScore);
		}

		if( backwardsValidation ) {
			for( int i = 0; i < src.size; i++ ) {
				int match = pairs.data[i];
				if( match == -1 )
					continue;

				double scoreToBeat = workBuffer.data[i*dst.size+match];

				for( int j = 0; j < src.size; j++ , match += dst.size ) {
					if( workBuffer.data[match] < scoreToBeat ) {
						pairs.data[i] = -1;
						fitQuality.data[i] = Double.MAX_VALUE;
						break;
					}
				}
			}
		}
	}

	public int[] getPairs() {
		return pairs.data;
	}

	public double[] getFitQuality() {
		return fitQuality.data;
	}
}
