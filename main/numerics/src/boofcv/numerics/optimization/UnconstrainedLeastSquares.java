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
 * TODO Update
 *
 * describe jacobian format
 *
 * @author Peter Abeles
 */
public interface UnconstrainedLeastSquares extends IterativeOptimization {

	/**
	 * Specifies the function being optimized. A numerical Jacobian will be computed
	 * if null is passed in.   TODO Update
	 *
	 * @param function Function being optimized.
	 * @param jacobian
	 */
	public void setFunction( FunctionNtoM function , FunctionNtoMxN jacobian );

	/**
	 * Specify the initial set of parameters from which to start from. Call after
	 * {@link #setFunction} has been called.      TODO Update
	 *
	 * @param initial Initial parameters or guess.
	 */
	public void initialize( double initial[] );

	/**
	 * After each iteration this function can be called to get the current best
	 * set of parameters.         TODO Update
	 */
	public double[] getParameters();
}
