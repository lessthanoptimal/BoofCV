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

import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.alg.feature.associate.ScoreAssociation;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWrapAssociateGreedy {

	/**
	 * Perform basic tests to make sure GeneralAssociation is correctly
	 * being implemented.
	 */
	@Test
	public void basicTests() {
		// test the cases where the number of matches is more than and less than the maximum
		performBasicTest(50, 100);
		performBasicTest(150, 100);
	}

	private void performBasicTest(int numFeatures, int maxAssoc) {
		AssociateGreedy<Double> greedy = new AssociateGreedy<Double>(new DoubleScore(),5,false);
		WrapAssociateGreedy<Double> alg = new WrapAssociateGreedy<Double>(greedy,maxAssoc);

		FastQueue<Double> listSrc = new FastQueue<Double>(numFeatures,Double.class,false);
		FastQueue<Double> listDst = new FastQueue<Double>(numFeatures,Double.class,false);

		for( int i = 0; i < numFeatures; i++ ) {
			listSrc.add((double)i);
			listDst.add((double)i+0.1+i*0.00001);

		}

		alg.associate(listSrc,listDst);

		FastQueue<AssociatedIndex> matches = alg.getMatches();

		// check to see that max assoc is being obeyed.
		assertEquals(matches.size(),Math.min(maxAssoc,numFeatures));

		// see if everything is assigned as expected
		for( int i = 0; i < matches.size(); i++ ) {
			AssociatedIndex a = matches.get(i);
			assertEquals(a.src,a.dst);
			assertTrue(a.src==i);
			assertTrue(a.fitScore != 0 );
		}
	}

	private class DoubleScore implements ScoreAssociation<Double> {

		@Override
		public double score(Double a, Double b) {
			return Math.abs(a-b);
		}

		@Override
		public boolean isZeroMinimum() {
			return true;
		}
	}
}
