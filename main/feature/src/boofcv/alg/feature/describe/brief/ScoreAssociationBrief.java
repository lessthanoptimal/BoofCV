/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe.brief;

import boofcv.alg.feature.associate.ScoreAssociation;

/**
 * Score association between two BRIEF features.  Scoring is done using the Hamming distance.
 * Hamming distance is the number of bits in the descriptor which do not have the same value.
 *
 * @author Peter Abeles
 */
public class ScoreAssociationBrief implements ScoreAssociation<BriefFeature>{
	@Override
	public double score(BriefFeature a, BriefFeature b) {
		int score = 0;
		final int N = a.data.length;
		for( int i = 0; i < N; i++ ) {
			score += hamming(a.data[i],b.data[i]);
		}
		return score;
	}

	public static int hamming( int a , int b ) {
		int distance = 0;
		// see which bits are different
		int val = a ^ b;

		while( val != 0 ) {
			val &= val - 1;
			distance++;
		}
		return distance;
	}

	@Override
	public boolean isZeroMinimum() {
		return true;
	}
}
