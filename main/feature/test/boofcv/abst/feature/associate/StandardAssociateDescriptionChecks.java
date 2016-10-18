/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Standard tests for implementations of AssociateDescription
 *
 * @author Peter Abeles
 */
public abstract class StandardAssociateDescriptionChecks<Desc> {

	FastQueue<Desc> listSrc;
	FastQueue<Desc> listDst;

	protected StandardAssociateDescriptionChecks( Class<Desc> descType ) {
		listSrc = new FastQueue<>(descType, false);
		listDst = new FastQueue<>(descType, false);
	}

	public void allTests() {
		checkScoreType();
		basicTests();
		checkDefaultThreshold();
		checkSetThreshold();
		uniqueSource();
		uniqueDestination();
	}

	/**
	 * Match error must be less than the specified euclidean error
	 */
	public abstract AssociateDescription<Desc> createAlg();

	protected void init() {
		listSrc.reset();
		listDst.reset();
	}

	@Test
	public void checkScoreType() {
		AssociateDescription<Desc> alg = createAlg();

		assertTrue("Test are designed for norm error",MatchScoreType.NORM_ERROR == alg.getScoreType());
	}

	/**
	 * Basic tests where there should be unique association in both direction
	 */
	@Test
	public void basicTests() {
		// test the cases where the number of matches is more than and less than the maximum
		performBasicTest(20);
		performBasicTest(40);
	}

	private void performBasicTest(int numFeatures ) {
		init();

		AssociateDescription<Desc> alg = createAlg();
		alg.setThreshold(0.01);

		for( int i = 0; i < numFeatures; i++ ) {
			listSrc.add(c(i+1) );
			listDst.add(c(i + 1 + 0.001));
		}

		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		FastQueue<AssociatedIndex> matches = alg.getMatches();

		// Every features should be associated
		assertEquals(numFeatures,matches.size());

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

		// in this example there should be perfect unambiguous associations
		assertEquals(0,alg.getUnassociatedSource().size);
		assertEquals(0,alg.getUnassociatedDestination().size);
	}

	/**
	 * The default threshold should allow for all matches to work
	 */
	@Test
	public void checkDefaultThreshold() {
		init();

		listSrc.add( c(1) );
		listDst.add( c(100) );

		AssociateDescription<Desc> alg = createAlg();
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();
		assertEquals(1,alg.getMatches().size);
	}

	/**
	 * Checks to see if changing the threshold increases or reduces the number of associations
	 */
	@Test
	public void checkSetThreshold() {
		init();

		listSrc.add( c(1) );
		listDst.add( c(1+0.1) );

		AssociateDescription<Desc> alg = createAlg();
		alg.setSource(listSrc);
		alg.setDestination(listDst);

		// no matches should be found since the error is too large
		alg.setThreshold(0.01);
		alg.associate();
		assertEquals(0,alg.getMatches().size);

		// Test edge case for threshold.  If it is exactly the distance away then should be included
		alg.setThreshold(1.1-1);
		alg.associate();
		assertEquals(1,alg.getMatches().size);

		// Threshold is greater than the assoc error
		alg.setThreshold(0.2);
		alg.associate();
		assertEquals(1,alg.getMatches().size);

		// Test no threshold case
		alg.setThreshold(Double.MAX_VALUE);
		alg.associate();
		assertEquals(1,alg.getMatches().size);
	}

	@Test
	public void checkUnassociatedLists() {
		init();

		AssociateDescription<Desc> alg = createAlg();

		listSrc.add( c(1) );
		listSrc.add( c(2) );
		listSrc.add( c(3) );
		listDst.add( c(1+0.1) );
		listDst.add( c(2+0.05) );
		listDst.add( c(3+0.05) );
		listDst.add( c(20) );  // can't be paired with anything

		// set threshold so that one pair won't be considered
		alg.setThreshold(0.07);
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		assertEquals(2,alg.getMatches().size);
		assertEquals(1,alg.getUnassociatedSource().size);
		assertEquals(2,alg.getUnassociatedDestination().size);
	}

	@Test
	public void uniqueSource() {
		init();

		listSrc.add(c(1));
		listDst.add( c(1) );
		listDst.add( c(1.001) );

		AssociateDescription<Desc> alg = createAlg();
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		if( alg.uniqueSource() ) {
			assertEquals(1,numMatchesSrc(0,alg.getMatches()));
		} else {
			// both dst will match up the first src
			assertEquals(2,numMatchesSrc(0,alg.getMatches()));
		}
	}

	@Test
	public void uniqueDestination() {
		init();

		listSrc.add( c(1) );
		listSrc.add( c(1.001) );
		listDst.add( c(1) );

		AssociateDescription<Desc> alg = createAlg();
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		if( alg.uniqueDestination() ) {
			assertEquals(1,numMatchesDst(0, alg.getMatches()));
		} else {
			// both src will match up the first dst
			assertEquals(2,numMatchesDst(0, alg.getMatches()));
		}
	}

	private int numMatchesSrc( int index , FastQueue<AssociatedIndex> list ) {
		int ret = 0;
		for( AssociatedIndex l : list.toList() ) {
			if( l.src == index )
				ret++;
		}
		return ret;
	}

	private int numMatchesDst( int index , FastQueue<AssociatedIndex> list ) {
		int ret = 0;
		for( AssociatedIndex l : list.toList() ) {
			if( l.dst == index )
				ret++;
		}
		return ret;
	}

	/**
	 * Creates a description with the specified value
	 */
	protected abstract Desc c( double value );
}
