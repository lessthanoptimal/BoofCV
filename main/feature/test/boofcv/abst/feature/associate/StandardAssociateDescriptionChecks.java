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

import boofcv.struct.FastQueue;
import boofcv.struct.GrowingArrayInt;
import boofcv.struct.feature.AssociatedIndex;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Standard tests for implementations of AssociateDescription
 *
 * @author Peter Abeles
 */
public abstract class StandardAssociateDescriptionChecks<D> {

	/**
	 * Match error must be less than the specified euclidean error
	 */
	public abstract AssociateDescription<D> createAlg();

	/**
	 * Adds a feature pair to the list with the specified amount of error between the pair
	 */
	public abstract void addFeature( FastQueue<D> listSrc , FastQueue<D> listDst , double error );

	public abstract Class<D> getDescType();

	/**
	 * Perform basic tests to make sure AssociateDescription is correctly
	 * being implemented.
	 */
	@Test
	public void basicTests() {
		// test the cases where the number of matches is more than and less than the maximum
		performBasicTest(20, 30);
		performBasicTest(40, 30);
	}

	private void performBasicTest(int numFeatures, int maxAssoc) {

		double maxError = 0.001*(maxAssoc+0.5);

		AssociateDescription<D> alg = createAlg();
		alg.setThreshold(maxError);

		FastQueue<D> listSrc = new FastQueue<D>(numFeatures,getDescType(),false);
		FastQueue<D> listDst = new FastQueue<D>(numFeatures,getDescType(),false);

		for( int i = 0; i < numFeatures; i++ ) {
			addFeature(listSrc, listDst, (i + 1) * 0.001);
		}

		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		FastQueue<AssociatedIndex> matches = alg.getMatches();

		// check to see that max assoc is being obeyed.
		assertEquals(Math.min(maxAssoc,numFeatures),matches.size());

		// see if everything is assigned as expected
		for( int i = 0; i < matches.size(); i++ ) {
			int numMatches = 0;
			for( int j = 0; j < matches.size(); j++ ) {
				AssociatedIndex a = matches.get(j);

				if( i == a.src ) {
					assertEquals(a.src,a.dst);
					assertTrue(a.fitScore != 0 );
					numMatches++;
				}
			}
			assertEquals(1,numMatches);
		}

		// see if the expected number of features are in the unassociated list
		GrowingArrayInt unassoc = alg.getUnassociatedSource();
		assertEquals(Math.max(numFeatures-maxAssoc,0),unassoc.size);

		// make sure none of the unassociated are contained in the associated list
		for( int i = 0; i < unassoc.size; i++ ) {
			int index = unassoc.data[i];
			for( int j = 0; j < matches.size(); j++ ) {
				if( matches.get(j).src == index )
					fail("match found");
			}
		}
	}

	/**
	 * Checks to see if changing the threshold increases or reduces the number of associations
	 */
	@Test
	public void checkSetThreshold() {
		int numFeatures = 10;

		AssociateDescription<D> alg = createAlg();

		FastQueue<D> listSrc = new FastQueue<D>(numFeatures,getDescType(),false);
		FastQueue<D> listDst = new FastQueue<D>(numFeatures,getDescType(),false);

		addFeature(listSrc,listDst,0.1);

		alg.setSource(listSrc);
		alg.setDestination(listDst);

		// no matches should be found since the error is too large
		alg.setThreshold(0.01);
		alg.associate();
		assertEquals(0,alg.getMatches().size);

		// Since the threshold is equal to the error NO matches are found
		alg.setThreshold(0.1);
		alg.associate();
		assertEquals(0,alg.getMatches().size);

		// Threshold is greater than the assoc error
		alg.setThreshold(0.2);
		alg.associate();
		assertEquals(1,alg.getMatches().size);

		// Test no threshold case
		alg.setThreshold(Double.MAX_VALUE);
		alg.associate();
		assertEquals(1,alg.getMatches().size);
	}
}
