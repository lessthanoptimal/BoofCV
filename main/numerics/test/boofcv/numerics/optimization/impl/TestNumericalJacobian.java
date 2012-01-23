/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization.impl;

import boofcv.numerics.optimization.OptimizationDerivative;
import boofcv.numerics.optimization.OptimizationFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNumericalJacobian {

	/**
	 * Compare a numerical gradient to an analytically computed gradient from a non-linear system.
	 */
	@Test
	public void compareToKnown() {
		OptimizationFunction<double[]> func = new TestLevenbergMarquardt.NonlinearResidual();
		OptimizationDerivative<double[]> derivTrue = new TestLevenbergMarquardt.NonlinearGradient();

		OptimizationDerivative<double[]> deriv = new NumericalJacobian<double[],double[]>(func);

		double[]model = new double[]{1,2};
		double[]state = new double[]{1.7};
		double[][]gradient = new double[1][2];
		double[][]expected = new double[1][2];

		deriv.setModel(model);
		deriv.computeDerivative(state,gradient);

		derivTrue.setModel(model);
		derivTrue.computeDerivative(state,expected);

		assertEquals(expected[0][0],gradient[0][0],1e-5);
		assertEquals(expected[0][1],gradient[0][1],1e-5);
	}
}
