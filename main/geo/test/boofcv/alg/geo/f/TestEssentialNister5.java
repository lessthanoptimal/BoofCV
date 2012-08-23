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

package boofcv.alg.geo.f;

import boofcv.alg.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEssentialNister5 extends CommonFundamentalChecks {

	@Test
	public void minimumNumber() {
		EssentialNister5 alg = new EssentialNister5();

		init(5, false);

		// compute essential
		assertTrue(alg.process(pairs));

		// validate by testing essential properties
		List<DenseMatrix64F> found = alg.getSolutions();

		for( DenseMatrix64F E : found ) {
			E.print();

			// sanity check, F is not zero
			assertTrue(NormOps.normF(E) > 0.1);

			// see if it follows the epipolar constraint
			for( AssociatedPair p : pairs ) {
				double val = GeometryMath_F64.innerProd(p.currLoc, E, p.keyLoc);
				assertEquals(0,val,1e-8);
			}
		}
	}
}
