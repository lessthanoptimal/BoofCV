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

import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;

/**
 * <p>
 * Non-linear least squares problems have a special structure which can be taken advantage of for optimization.
 * The least squares problem is defined below:<br>
 * F(x) = 0.5*sum( i=1:m , f<sub>i</sub>(x)^2 )<br>
 * where f_i(x) is a function from &real;<sup>N</sup> to &real;. m > n
 * </p>
 *
 * <p>
 * FORMATS:<br>
 * Input functions are specified using {@link FunctionNtoM} for the set of M functions, and {@link FunctionNtoMxN}
 * for the Jacobian.  The function's output is a vector of length M, where element i correspond to function i's output.
 * The Jacobian is an array containing the partial derivatives of each function.  Element J(i,j) corresponds
 * to the partial of function i and parameter j.   The array is stored in a row major format.  The partial
 * for F(i,j) would be stored at index = i*N+j in the data array.
 * </p>
 *
 * @author Peter Abeles
 */
public interface UnconstrainedLeastSquares extends IterativeOptimization {

	/**
	 * Specifies a set of functions and their Jacobian.  See class description for documentation
	 * on output data format.
	 *
	 * @param function Computes the output of M functions which take in M inputs.
	 * @param jacobian Computes the Jacobian.  If null a numerical Jacobian will be used.
	 */
	public void setFunction( FunctionNtoM function , FunctionNtoMxN jacobian );

	/**
	 * Specify the initial set of parameters from which to start from. Call after
	 * {@link #setFunction} has been called.
	 *
	 * @param initial Initial parameters or guess.
	 */
	public void initialize( double initial[] );

	/**
	 * After each iteration this function can be called to get the current best
	 * set of parameters.
	 */
	public double[] getParameters();
}
