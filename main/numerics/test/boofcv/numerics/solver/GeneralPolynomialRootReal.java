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

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GeneralPolynomialRootReal {

	Random rand = new Random(234);


	@Test
	public void rootsSmallReal() {
		for( int numCoef = 2; numCoef < 6; numCoef++ ) {
			Polynomial poly = new Polynomial(numCoef);

			for( int trial = 0; trial < 20; trial++ ) {
				for( int i = 0; i < numCoef; i++ ) {
					poly.c[i] = 10*(rand.nextDouble()-0.5);
				}

				List<Double> roots = computeRealRoots(poly);

				// the root of the polynomial should be zero.  higher degree polynomials have
				// more stability problems
				for( double d : roots ) {
					assertEquals(0,poly.evaluate(d),1e-8);
				}

				int expectedRoots = PolynomialOps.countRealRoots(poly);
				assertTrue(roots.size()==expectedRoots);
			}
		}
	}


	@Test
	public void rootsLargeReal() {
		for( int numCoef = 10; numCoef < 15; numCoef++ ) {
			Polynomial poly = new Polynomial(numCoef);

			for( int trial = 0; trial < 20; trial++ ) {
				for( int i = 0; i < numCoef; i++ ) {
					poly.c[i] = 2000*(rand.nextDouble()-0.5);
				}

				List<Double> roots = computeRealRoots(poly);

				// the root of the polynomial should be zero.  higher degree polynomials have
				// more stability problems
				for( double d : roots ) {
					assertEquals(0,poly.evaluate(d),1e-8);
				}

				int expectedRoots = PolynomialOps.countRealRoots(poly);
				assertTrue(roots.size()==expectedRoots);
			}
		}
	}

	public abstract List<Double> computeRealRoots( Polynomial poly );
}
