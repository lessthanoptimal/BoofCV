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

package boofcv.numerics.optimization;

import boofcv.numerics.optimization.functions.FunctionNtoN;
import boofcv.numerics.optimization.functions.FunctionNtoS;

/**
 * <p>
 * Optimization algorithm which seeks to minimize F(X) &isin; &real; and X &isin; &real;<sup>N</sup>
 * </p>
 *
 * <p>
 * Two convergence thresholds are specified, f-test and g-test.  The f-test is a relative convergence
 * test based on the function's value and is designed to test to see when it is near the optimal
 * solution.  G-test is an absolute test based on the gradient's norm,
 * </p>
 *
 * <p>
 * F-test:    ftol &le; 1 - f(x+p)/f(x)<br>
 * G-test:    gtol &le; ||g(x)||<sub>inf</sub><br>
 * An absolute f-test can be done by checking the value of {@link #getFunctionValue} in each iteration.
 * </p>
 *
 * @author Peter Abeles
 */
public interface UnconstrainedMinimization extends IterativeOptimization {

	/**
	 * Specifies the function being optimized. A numerical Jacobian will be computed
	 * if null is passed in.
	 *
	 * @param function Function being optimized.
	 * @param gradient Partial derivative for each input in the function. If null a numerical
	 *                 gradient will be computed.
	 * @param minFunctionValue Minimum possible value that 'function' can have.  E.g. for least squares problems
	 *                         this value should be set to zero.
	 */
	public void setFunction( FunctionNtoS function , FunctionNtoN gradient , double minFunctionValue );

	/**
	 * Specify the initial set of parameters from which to start from. Call after
	 * {@link #setFunction} has been called.
	 * 
	 * @param initial Initial parameters or guess.
	 * @param ftol Relative convergence test based on function value. 0 disables test.  0 &le; ftol < 1
	 * @param gtol Absolute convergence test based on gradient. 0 disables test.  0 &le; gtol
	 */
	public void initialize( double initial[] , double ftol , double gtol );

	/**
	 * After each iteration this function can be called to get the current best
	 * set of parameters.
	 */
	public double[] getParameters();

	/**
	 * Returns the value of the objective function being evaluated at the current
	 * parameters value.  If not supported then an exception is thrown.
	 *
	 * @return Objective function's value.
	 */
	public double getFunctionValue();

}
