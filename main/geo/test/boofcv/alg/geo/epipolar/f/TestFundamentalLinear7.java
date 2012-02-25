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

package boofcv.alg.geo.epipolar.f;

import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFundamentalLinear7 extends CommonFundamentalChecks{

	@Test
	public void perfectFundamental() {
		checkEpipolarMatrix(7,true,new FundamentalLinear7(true));
	}

	@Test
	public void perfectEssential() {
		checkEpipolarMatrix(7,false,new FundamentalLinear7(false));
	}

	@Test
	public void enforceZeroDeterminant() {
		for( int i = 0; i < 20; i++ ) {
			SimpleMatrix F1 = SimpleMatrix.random(3, 3, 0.1, 2, rand);
			SimpleMatrix F2 = SimpleMatrix.random(3, 3, 0.1, 2, rand);

			double alpha = FundamentalLinear7.enforceZeroDeterminant(F1.getMatrix(),F2.getMatrix(),new double[4]);

			SimpleMatrix F = F1.scale(alpha).plus(F2.scale(1 - alpha));

//			System.out.println("det = "+F.determinant()+"  F1 = "+F1.determinant());

			assertEquals(0, F.determinant(), 1e-8);
		}
	}

	@Test
	public void computeCoefficients() {
		SimpleMatrix F1 = SimpleMatrix.random(3, 3, 0.1, 2, rand);
		SimpleMatrix F2 = SimpleMatrix.random(3, 3, 0.1, 2, rand);

		double coefs[] = new double[4];

		FundamentalLinear7.computeCoefficients(F1.getMatrix(), F2.getMatrix(), coefs);

		double alpha = 0.4;

		double expected = F1.scale(alpha).plus(F2.scale(1 - alpha)).determinant();
		double found = coefs[0] + alpha*coefs[1] + alpha*alpha*coefs[2] + alpha*alpha*alpha*coefs[3];

		assertEquals(expected,found,1e-8);
	}
}
