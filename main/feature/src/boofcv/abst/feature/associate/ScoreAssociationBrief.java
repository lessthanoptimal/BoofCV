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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.HammingTable16;
import boofcv.alg.feature.describe.brief.BriefFeature;

/**
 * Score association between two BRIEF features.  Scoring is done using the Hamming distance.
 * Hamming distance is the number of bits in the descriptor which do not have the same value.
 *
 * @author Peter Abeles
 */
public class ScoreAssociationBrief implements ScoreAssociation<BriefFeature>{

//	HammingTable8 table = new HammingTable8();
	HammingTable16 table = new HammingTable16();

	@Override
	public double score(BriefFeature a, BriefFeature b) {

		int score = 0;

		for( int i = 0; i < a.data.length; i++ ) {
			int dataA = a.data[i];
			int dataB = b.data[i];

//			score += table.score[ (dataA & 0xFF) << 8 | (dataB & 0xFF) ];
//			score += table.score[ (dataA>>8 & 0xFF) << 8 | (dataB>>8 & 0xFF) ];
//			score += table.score[ (dataA>>16 & 0xFF) << 8 | (dataB>>16 & 0xFF) ];
//			score += table.score[ (dataA>>24 & 0xFF) << 8 | (dataB>>24 & 0xFF) ];

			score += table.lookup( (short)dataA , (short)dataB );
			score += table.lookup( (short)(dataA >> 16) , (short)(dataB >> 16) );

//			score += table.lookup( (byte)dataA , (byte)dataB );
//			score += table.lookup( (byte)(dataA >> 8)  , (byte)(dataB >> 8) );
//			score += table.lookup( (byte)(dataA >> 16) , (byte)(dataB >> 16) );
//			score += table.lookup( (byte)(dataA >> 24) , (byte)(dataB >> 24) );
		}

		return score;
	}

	@Override
	public boolean isZeroMinimum() {
		return true;
	}
}
