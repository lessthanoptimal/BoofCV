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

package boofcv.numerics.solver.impl;

import boofcv.numerics.solver.Polynomial;
import boofcv.numerics.solver.PolynomialOps;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSturmSequence {

	Random rand = new Random(234);

	@Test
	public void countRealRoots() {
		Polynomial poly = Polynomial.wrap(-1,0,3,1);

		assertEquals(1, countRealRoots(poly, -3, -2));
		assertEquals(0, countRealRoots(poly, 2, 3));
		assertEquals(3, countRealRoots(poly, -3, 3));
		assertEquals(3, countRealRoots(poly, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

		// see if it handles small polynomials correctly
		assertEquals(0, countRealRoots(Polynomial.wrap(2), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
		assertEquals(1, countRealRoots(Polynomial.wrap(2, 3), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
		assertEquals(0,countRealRoots(Polynomial.wrap(2,3),0,Double.POSITIVE_INFINITY));
		assertEquals(0,countRealRoots(Polynomial.wrap(2,3),Double.NEGATIVE_INFINITY,-10));
		assertEquals(1,countRealRoots(Polynomial.wrap(2,3),-2,-0.5));
		assertEquals(0, countRealRoots(Polynomial.wrap(2, 3, 4), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
		assertEquals(2, countRealRoots(Polynomial.wrap(2, -1, -4), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

		// more difficult case where special code is required for infinite bounds
		poly = Polynomial.wrap(-1.322309e+02 , 3.713984e+02 , -5.007874e+02 , 3.744386e+02 ,-1.714667e+02  ,
				4.865014e+01 ,-1.059870e+01  ,  1.642273e+00 ,-2.304341e-01,2.112391e-03,-2.273737e-13);

		assertEquals(2, countRealRoots(poly, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
	}

	public int countRealRoots( Polynomial p , double low , double high ) {
		SturmSequence alg = new SturmSequence(p.size);
		alg.initialize(p);
		return alg.countRealRoots(low,high);
	}

	/**
	 * Check sequence against a hand selected sequence
	 */
	@Test
	public void checkSequence() {
		Polynomial poly = Polynomial.wrap(-1,0,3,1,5,-3);

		SturmSequence alg = new SturmSequence(10);

		alg.initialize(poly);

		alg.computeFunctions(-3);

		List<Double> expected = computeSturm(poly,-3);

		assertEquals(expected.size(),alg.sequenceLength);
		for( int i = 0; i < expected.size(); i++ ) {
			assertEquals(expected.get(i),alg.f[i],1e-8);
		}
	}

	@Test
	public void checkCount() {
		SturmSequence alg = new SturmSequence(10);

		alg.f = new double[]{0,1,1,-1,0,-1,-1,0,1,1};
		alg.sequenceLength = alg.f.length;

		assertEquals(2,alg.countSignChanges());

		alg.f = new double[]{1,1,1,-1,0,-1,-1,0,1,1,0,-1,-1,1};
		alg.sequenceLength = alg.f.length;

		assertEquals(4,alg.countSignChanges());
	}

	/**
	 * Compare the computed sequence against randomly generated polynomials of different length
	 */
	@Test
	public void checkSequence_Random() {
		for( int i = 3; i < 20; i++ ) {
			Polynomial p = new Polynomial(i);

			for( int trial = 0; trial < 20; trial++ ) {
				for( int j = 0; j < p.size; j++ ) {
					p.c[j] = (rand.nextDouble()-0.5)*2;
				}

				double value = (rand.nextDouble()-0.5)*4;

				compareSequences( p, value);
				compareSequences( p, Double.POSITIVE_INFINITY);
				compareSequences( p, Double.NEGATIVE_INFINITY);
			}
		}
	}

	/**
	 * Examine a case which was found to cause problems
	 */
	@Test
	public void checkSpecificPoly01() {
		Polynomial poly = Polynomial.wrap(-41.118263303597175,-120.95384505825373,-417.8477600492497,-634.5308297409192,
				-347.7885168491812,6.771313016808563,79.70258790927392,31.68212813610444,5.0248961592587875,
				0.2879701466217739,0.0);

		// Compare computed sequence to the standard
		compareSequences( poly, -500);
		compareSequences( poly, Double.NEGATIVE_INFINITY);

		SturmSequence alg = new SturmSequence(poly.size);
		alg.initialize(poly);
		int N = alg.countRealRoots(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
		int M = alg.countRealRoots(-500,500);

		assertTrue( M <= N);
	}

	private void compareSequences( Polynomial p, double value) {
		List<Double> expected = computeSturm(p,value);
		SturmSequence alg = new SturmSequence(p.size);

		alg.initialize(p);
		alg.computeFunctions(value);

		assertEquals(expected.size(),alg.sequenceLength);

		for( int j = 0; j < expected.size(); j++ ) {
			if( Double.isInfinite(expected.get(j)) ) {
				assertTrue(expected.get(j) == alg.f[j]);
			} else {
				assertEquals(expected.get(j),alg.f[j],Math.abs(alg.f[j])*1e-6);
			}
		}
	}

	/**
	 * Compute the sturm sequence using a straight forward method
	 */
	private List<Double> computeSturm( Polynomial poly , double x ) {
		Polynomial d = new Polynomial(poly.size);

		PolynomialOps.derivative(poly, d);

		List<Double> found = new ArrayList<Double>();
		found.add( poly.evaluate(x));
		found.add( d.evaluate(x));

		Polynomial q = new Polynomial(poly.size);
		Polynomial r = new Polynomial(poly.size);
		Polynomial p1 = new Polynomial(poly.size);
		Polynomial p2 = new Polynomial(poly.size);

		p1.setTo(poly);
		p2.setTo(d);

		do {
			PolynomialOps.divide(p1,p2,q,r);

			for( int i = 0; i < r.size; i++ ) {
				r.c[i] = -r.c[i];
			}

			found.add(r.evaluate(x));

			p1.setTo(p2);
			p2.setTo(r);

		} while( r.computeDegree() > 0 );

		return found;
	}

}
