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

package boofcv.alg.geo.f;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.QueueMatrix;
import georegression.geometry.GeometryMath_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Standardized checks for computing fundamental matrices
 *
 * @author Peter Abeles
 */
public abstract class CommonFundamentalChecks extends EpipolarTestSimulation {

	double zeroTol = 1e-8;

	FastQueue<DenseMatrix64F> solutions = new QueueMatrix(3, 3);

	public abstract void computeFundamental(List<AssociatedPair> pairs , FastQueue<DenseMatrix64F> solutions );


	public void checkEpipolarMatrix(int N, boolean isFundamental) {
		init(100, isFundamental);

		// run several trials with different inputs of size N
		for (int trial = 0; trial < 20; trial++) {
			List<AssociatedPair> pairs = randomPairs(N);

			computeFundamental(pairs,solutions);

			// At least one solution should have been found
			assertTrue(solutions.size() > 0);

			int totalMatchedAll = 0;
			int totalPassedMatrix = 0;

			for (DenseMatrix64F F : solutions.toList()) {
				// normalize F to ensure a consistent scale
				CommonOps.scale(1.0/CommonOps.elementMaxAbs(F),F);

				// sanity check, F is not zero
				if (NormOps.normF(F) <= 0.1)
					continue;

				// the determinant should be zero
				if (Math.abs(CommonOps.det(F)) > zeroTol)
					continue;

				totalPassedMatrix++;

				// see if this hypothesis matched all the points
				boolean matchedAll = true;
				for (AssociatedPair p : this.pairs) {
					double val = GeometryMath_F64.innerProd(p.p2, F, p.p1);
					if (val > zeroTol) {
						matchedAll = false;
						break;
					}
				}
				if (matchedAll) {
					totalMatchedAll++;
				}
			}

			// At least one of them should be consistent with the original set of points
			assertTrue(totalMatchedAll > 0);
			assertTrue(totalPassedMatrix > 0);
		}
	}
}
