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

package boofcv.alg.distort.kanbra;

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoN;
import org.ddogleg.optimization.functions.FunctionNtoS;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

//CUSTOM ignore KannalaBrandtUtils_F64

/**
 * @author Peter Abeles
 */
class TestKannalaBrandtUtils_F64 extends BoofStandardJUnit {
	/**
	 * Compare to numerical derivative. This is linear so it should be very accurate
	 */
	@Test void polynomialDerivative() {
		/**/double[] coefs = new /**/double[]{0.1, -0.05, 0.01, 0.05};

		FunctionNtoS function = new FunctionNtoS() {
			@Override public int getNumOfInputsN() {return 1;}

			@Override public /**/double process( /**/double[] input ) {
				return KannalaBrandtUtils_F64.polynomial(coefs, input[0]);
			}
		};

		FunctionNtoN gradient = new FunctionNtoN() {
			@Override public int getN() {return 1;}

			@Override public void process( /**/double[] input, /**/double[] output ) {
				output[0] = KannalaBrandtUtils_F64.polynomialDerivative(coefs, input[0]);
			}
		};

		assertTrue(DerivativeChecker.gradient(function, gradient, new /**/double[]{0.1},
				UtilEjml.TEST_F64, Math.sqrt(UtilEjml.EPS)));
	}

	/**
	 * Compare to numerical derivative.
	 */
	@Test void polytrigDerivative() {
		/**/double[] coefs = new /**/double[]{0.1, -0.05, 0.01, 0.05};

		FunctionNtoS function = new FunctionNtoS() {
			@Override public int getNumOfInputsN() {return 1;}

			@Override public /**/double process( /**/double[] input ) {
				double c = Math.cos(input[0]);
				double s = Math.sin(input[0]);
				return KannalaBrandtUtils_F64.polytrig(coefs, c, s);
			}
		};

		FunctionNtoN gradient = new FunctionNtoN() {
			@Override public int getN() {return 1;}

			@Override public void process( /**/double[] input, /**/double[] output ) {
				double c = Math.cos(input[0]);
				double s = Math.sin(input[0]);
				output[0] = KannalaBrandtUtils_F64.polytrigDerivative(coefs, c, s);
			}
		};

		// Relaes the tolerance a little bit since this is a fairly non-linear function
		assertTrue(DerivativeChecker.gradient(function, gradient, new /**/double[]{0.1},
				UtilEjml.TEST_F64_SQ, Math.sqrt(UtilEjml.EPS)));
	}
}
