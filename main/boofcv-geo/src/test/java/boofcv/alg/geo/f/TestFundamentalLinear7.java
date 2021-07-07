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

package boofcv.alg.geo.f;

import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFundamentalLinear7 extends BoofStandardJUnit {

	@Test void perfectFundamental() {
		createCommonChecks(true).checkEpipolarMatrix(7, true);
	}

	@Test void perfectEssential() {
		createCommonChecks(false).checkEpipolarMatrix(7, false);
	}

	private CommonFundamentalChecks createCommonChecks( final boolean isFundamental ) {
		return new CommonFundamentalChecks() {
			FundamentalLinear7 alg = new FundamentalLinear7(isFundamental);

			@Override
			public void computeFundamental(List<AssociatedPair> pairs, DogArray<DMatrixRMaj> solutions) {
				assertTrue(alg.process(pairs,solutions));
			}
		};
	}

	@Test void computeCoefficients() {
		SimpleMatrix F1 = SimpleMatrix.random_DDRM(3, 3, 0.1, 2, rand);
		SimpleMatrix F2 = SimpleMatrix.random_DDRM(3, 3, 0.1, 2, rand);

		double coefs[] = new double[4];

		FundamentalLinear7.computeCoefficients(F1.getDDRM(), F2.getDDRM(), coefs);

		double alpha = 0.4;

		double expected = F1.scale(alpha).plus(F2.scale(1 - alpha)).determinant();
		double found = coefs[0] + alpha*coefs[1] + alpha*alpha*coefs[2] + alpha*alpha*alpha*coefs[3];

		assertEquals(expected,found,1e-8);
	}

}
