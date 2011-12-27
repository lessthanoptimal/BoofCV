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

package boofcv.numerics.optimization;

/**
 * Used to validate an algebraic Jacobian numerically.
 *
 * @author Peter Abeles
 */
public class JacobianChecker {

	public static <Observed,State> double[][]
	compareToNumerical(OptimizationFunction<State> function,
					   OptimizationDerivative<State> gradient,
					   State state, double... param)
	{

		OptimizationDerivative<State> numerical =
				new NumericalJacobian<Observed,State>(function);

		gradient.setModel(param);
		numerical.setModel(param);

		int numFuncs = function.getNumberOfFunctions();
		int numModel = function.getModelSize();

		double found[][] = new double[numFuncs][numModel];
		double expected[][] = new double[numFuncs][numModel];

		gradient.computeDerivative(state,found);
		numerical.computeDerivative(state,expected);

		for( int i = 0; i < numFuncs; i++ ) {
			for( int j = 0; j < numModel; j++ ) {
				found[i][j] = Math.abs(found[i][j] - expected[i][j]);
			}
		}

		return found;
	}

	public static boolean checkErrors( double[][] errors , double tolerance )
	{
		for( int i = 0; i < errors.length; i++ ) {
			for( int j = 0; j < errors[i].length; j++ ) {
				if( errors[i][j] > tolerance )
					return false;
			}
		}

		return true;
	}
}
