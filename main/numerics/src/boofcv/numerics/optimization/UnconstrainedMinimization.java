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
	 */
	public void setFunction( FunctionNtoS function , FunctionNtoN gradient );

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

	/**
	 * Returns the value of the objective function being evaluated at the current
	 * parameters value.  If not supported then an exception is thrown.
	 *
	 * @return Objective function's value.
	 */
	public double getFunctionValue();

}
