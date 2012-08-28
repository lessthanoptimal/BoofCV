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

package boofcv.numerics.solver;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestSturmSequence {

	@Test
	public void countRealRoots() {
		Polynomial poly = Polynomial.wrap(-1,0,3,1);

		SturmSequence alg = new SturmSequence(12);

		alg.setPolynomial(poly);

		assertEquals(1,alg.countRealRoots(-3,-2));
		assertEquals(0,alg.countRealRoots(2,3));
		assertEquals(3,alg.countRealRoots(-3,3));
		assertEquals(3,alg.countRealRoots(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY));

		// more difficult case where special code is required for infinite bounds
		poly = Polynomial.wrap(-1.322309e+02 , 3.713984e+02 , -5.007874e+02 , 3.744386e+02 ,-1.714667e+02  ,
				4.865014e+01 ,-1.059870e+01  ,  1.642273e+00 ,-2.304341e-01,2.112391e-03,-2.273737e-13);

		alg.setPolynomial(poly);

		assertEquals(8,alg.countRealRoots(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY));
	}


	@Test
	public void checkSequence() {
		Polynomial poly = Polynomial.wrap(-1,0,3,1,5,-3);

		SturmSequence alg = new SturmSequence(10);

		alg.setPolynomial(poly);

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
	 * Compute the sturm sequence using a straight forward method
	 */
	private List<Double> computeSturm( Polynomial poly , double x ) {
		Polynomial d = new Polynomial(poly.size);

		PolynomialOps.derivative(poly,d);

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
