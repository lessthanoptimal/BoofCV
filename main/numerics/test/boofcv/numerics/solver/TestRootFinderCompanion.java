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

import org.ejml.data.Complex64F;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRootFinderCompanion {

	@Test
	public void basicTest() {
		double coefs[] = new double[]{4,3,2,1};

		PolynomialRootFinder alg = new RootFinderCompanion();

		assertTrue(alg.process(coefs));

		List<Complex64F> roots = alg.getRoots();

		int numReal = 0;
		for( Complex64F c : roots ) {
			if( c.isReal() ) {
				assertEquals(0,TestPolynomialSolver.cubic(4, 3, 2, 1,c.real),1e-8);
				numReal++;
			}
		}

		assertTrue(numReal>0);
	}
}
