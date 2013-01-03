/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.HammingTable16;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_B;

/**
 * Score association between two BRIEF features.  Scoring is done using the Hamming distance.
 * Hamming distance is the number of bits in the descriptor which do not have the same value.
 *
 * @author Peter Abeles
 */
public class ScoreAssociateHamming_B implements ScoreAssociation<TupleDesc_B>{

	HammingTable16 table = new HammingTable16();

	@Override
	public double score(TupleDesc_B a, TupleDesc_B b) {

		int score = 0;

		for( int i = 0; i < a.data.length; i++ ) {
			int dataA = a.data[i];
			int dataB = b.data[i];

			score += table.lookup( (short)dataA , (short)dataB );
			score += table.lookup( (short)(dataA >> 16) , (short)(dataB >> 16) );
		}

		return score;
	}

	@Override
	public MatchScoreType getScoreType() {
		return MatchScoreType.NORM_ERROR;
	}
}
