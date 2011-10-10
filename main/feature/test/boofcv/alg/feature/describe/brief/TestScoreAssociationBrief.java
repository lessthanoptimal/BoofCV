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

package boofcv.alg.feature.describe.brief;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociationBrief {

	Random rand = new Random(123);

	/**
	 * Generate random descriptions and see two hamming distance calculations return the same result.
	 */
	@Test
	public void testRandom() {
		ScoreAssociationBrief scorer = new ScoreAssociationBrief();

		BriefFeature a = new BriefFeature(512);
		BriefFeature b = new BriefFeature(512);

		for( int numTries = 0; numTries < 20; numTries++ ) {
			for( int i = 0; i < a.data.length; i++ ) {
				a.data[i] = rand.nextInt();
				b.data[i] = rand.nextInt();
			}

			assertEquals(hamming(a,b),scorer.score(a,b),1e-4);
		}
	}

	private int hamming( BriefFeature a, BriefFeature b) {
		int ret = 0;
		for( int i = 0; i < a.data.length; i++ ) {
			ret += hamming(a.data[i],b.data[i]);
		}
		return ret;
	}

	private int hamming( int a , int b ) {
		int distance = 0;

		// see which bits are different
		int val = a ^ b;

		// brute force hamming distance
		if( (val & 0x00000001) != 0)
			distance++;
		if( (val & 0x00000002) != 0)
			distance++;
		if( (val & 0x00000004) != 0)
			distance++;
		if( (val & 0x00000008) != 0)
			distance++;

		if( (val & 0x00000010) != 0)
			distance++;
		if( (val & 0x00000020) != 0)
			distance++;
		if( (val & 0x00000040) != 0)
			distance++;
		if( (val & 0x00000080) != 0)
			distance++;

		if( (val & 0x00000100) != 0)
			distance++;
		if( (val & 0x00000200) != 0)
			distance++;
		if( (val & 0x00000400) != 0)
			distance++;
		if( (val & 0x00000800) != 0)
			distance++;

		if( (val & 0x00001000) != 0)
			distance++;
		if( (val & 0x00002000) != 0)
			distance++;
		if( (val & 0x00004000) != 0)
			distance++;
		if( (val & 0x00008000) != 0)
			distance++;


		if( (val & 0x00010000) != 0)
			distance++;
		if( (val & 0x00020000) != 0)
			distance++;
		if( (val & 0x00040000) != 0)
			distance++;
		if( (val & 0x00080000) != 0)
			distance++;

		if( (val & 0x00100000) != 0)
			distance++;
		if( (val & 0x00200000) != 0)
			distance++;
		if( (val & 0x00400000) != 0)
			distance++;
		if( (val & 0x00800000) != 0)
			distance++;

		if( (val & 0x01000000) != 0)
			distance++;
		if( (val & 0x02000000) != 0)
			distance++;
		if( (val & 0x04000000) != 0)
			distance++;
		if( (val & 0x08000000) != 0)
			distance++;

		if( (val & 0x10000000) != 0)
			distance++;
		if( (val & 0x20000000) != 0)
			distance++;
		if( (val & 0x40000000) != 0)
			distance++;
		if( (val & 0x80000000) != 0)
			distance++;

		return distance;
	}
}
