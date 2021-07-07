/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standard tests for implementations of AssociateDescription
 *
 * @author Peter Abeles
 */
public abstract class StandardAssociateDescriptionChecks<Desc> extends BoofStandardJUnit {

	// if true then the error used is squared
	boolean distanceIsSquared = true;
	FastArray<Desc> listSrc;
	FastArray<Desc> listDst;

	protected StandardAssociateDescriptionChecks( Class<Desc> descType ) {
		listSrc = new FastArray<>(descType);
		listDst = new FastArray<>(descType);
	}

	public void allTests() {
		checkScoreType();
		basicTests();
		checkDefaultThreshold();
		checkSetThreshold();
		uniqueSource();
		uniqueDestination();
		checkUnassociatedLists();
		checkUnassociatedLists_emptySrc();
		checkUnassociatedLists_emptyDst();
	}

	/**
	 * Match error must be less than the specified euclidean error
	 */
	public abstract AssociateDescription<Desc> createAssociate();

	protected void init() {
		listSrc.reset();
		listDst.reset();
	}

	@Test
	void checkScoreType() {
		AssociateDescription<Desc> alg = createAssociate();

		assertSame(MatchScoreType.NORM_ERROR, alg.getScoreType(), "Test are designed for norm error");
	}

	/**
	 * Basic tests where there should be unique association in both direction
	 */
	@Test
	void basicTests() {
		// test the cases where the number of matches is more than and less than the maximum
		performBasicTest(20);
		performBasicTest(40);
	}

	private void performBasicTest(int numFeatures ) {
		init();

		AssociateDescription<Desc> alg = createAssociate();
		alg.setMaxScoreThreshold(0.01);

		for( int i = 0; i < numFeatures; i++ ) {
			listSrc.add(c(i+1) );
			listDst.add(c(i + 1 + 0.001));
		}

		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		FastAccess<AssociatedIndex> matches = alg.getMatches();

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
	@Test void checkDefaultThreshold() {
		init();

		listSrc.add( c(1) );
		listDst.add( c(100) );

		AssociateDescription<Desc> alg = createAssociate();
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();
		assertEquals(1,alg.getMatches().size);
	}

	/**
	 * Checks to see if changing the threshold increases or reduces the number of associations
	 */
	@Test void checkSetThreshold() {
		init();

		listSrc.add( c(1) );
		listDst.add( c(1+0.1) );

		AssociateDescription<Desc> alg = createAssociate();
		alg.setSource(listSrc);
		alg.setDestination(listDst);

		// no matches should be found since the error is too large
		alg.setMaxScoreThreshold(0.01);
		alg.associate();
		assertEquals(0,alg.getMatches().size);

		// Test edge case for threshold. If it is exactly the distance away then should be included
		alg.setMaxScoreThreshold(1.1-1);
		alg.associate();
		assertEquals(1,alg.getMatches().size);

		// Threshold is greater than the assoc error
		alg.setMaxScoreThreshold(0.2);
		alg.associate();
		assertEquals(1,alg.getMatches().size);

		// Test no threshold case
		alg.setMaxScoreThreshold(Double.MAX_VALUE);
		alg.associate();
		assertEquals(1,alg.getMatches().size);
	}

	@Test
	void checkUnassociatedLists() {
		init();

		AssociateDescription<Desc> alg = createAssociate();

		listSrc.add( c(1) );
		listSrc.add( c(2) );
		listSrc.add( c(3) );
		listDst.add( c(1+0.1) );
		listDst.add( c(2+0.05) );
		listDst.add( c(3+0.05) );
		listDst.add( c(20) );  // can't be paired with anything

		// set threshold so that one pair won't be considered
		alg.setMaxScoreThreshold(distanceIsSquared?0.07*0.07:0.07);
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		assertEquals(2,alg.getMatches().size);
		assertEquals(1,alg.getUnassociatedSource().size);
		assertEquals(2,alg.getUnassociatedDestination().size);
	}

	@Test
	void checkUnassociatedLists_emptySrc() {
		init();

		AssociateDescription<Desc> alg = createAssociate();

		listDst.add( c(1+0.1) );
		listDst.add( c(2+0.05) );
		listDst.add( c(3+0.05) );
		listDst.add( c(20) );  // can't be paired with anything

		// set threshold so that one pair won't be considered
		alg.setMaxScoreThreshold(distanceIsSquared?0.07*0.07:0.07);
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		assertEquals(0,alg.getMatches().size);
		assertEquals(0,alg.getUnassociatedSource().size);
		assertEquals(4,alg.getUnassociatedDestination().size);

		DogArray_I32 unassociated = alg.getUnassociatedDestination();
		unassociated.sort();
		for( int i = 0; i < unassociated.size; i++ ) {
			assertEquals(i,unassociated.get(i));
		}
	}

	@Test
	void checkUnassociatedLists_emptyDst() {
		init();

		AssociateDescription<Desc> alg = createAssociate();

		listSrc.add( c(1) );
		listSrc.add( c(2) );
		listSrc.add( c(3) );

		// set threshold so that one pair won't be considered
		alg.setMaxScoreThreshold(distanceIsSquared?0.07*0.07:0.07);
		alg.setSource(listSrc);
		alg.setDestination(listDst);
		alg.associate();

		assertEquals(0,alg.getMatches().size);
		assertEquals(3,alg.getUnassociatedSource().size);
		assertEquals(0,alg.getUnassociatedDestination().size);

		DogArray_I32 unassociated = alg.getUnassociatedSource();
		unassociated.sort();
		for( int i = 0; i < unassociated.size; i++ ) {
			assertEquals(i,unassociated.get(i));
		}
	}

	@Test
	void uniqueSource() {
		init();

		listSrc.add(c(1));
		listDst.add( c(1) );
		listDst.add( c(1.001) );

		AssociateDescription<Desc> alg = createAssociate();
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
	void uniqueDestination() {
		init();

		listSrc.add( c(1) );
		listSrc.add( c(1.001) );
		listDst.add( c(1) );

		AssociateDescription<Desc> alg = createAssociate();
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

	private int numMatchesSrc( int index , FastAccess<AssociatedIndex> list ) {
		int ret = 0;
		for( AssociatedIndex l : list.toList() ) {
			if( l.src == index )
				ret++;
		}
		return ret;
	}

	private int numMatchesDst( int index , FastAccess<AssociatedIndex> list ) {
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
