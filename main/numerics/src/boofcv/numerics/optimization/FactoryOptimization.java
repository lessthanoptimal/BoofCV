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

import boofcv.numerics.optimization.wrap.WrapQuasiNewtonBFGS;

/**
 * Creates optimization algorithms using easy to use interfaces.  These implementations/interfaces
 * are designed to be easy to use and effective for most tasks.  If more control is needed then
 * create an implementation directly.
 *
 * @author Peter Abeles
 */
public class FactoryOptimization {

	/**
	 * Creates a solver for the unconstrained minimization problem.  Here a function has N parameters
	 * and a single output.  The goal is the minimize the output given the function and its derivative.
	 *
	 * @param relativeErrorTol Relative tolerance used to terminate the optimization. 0 <= x < 1
	 * @param absoluteErrorTol Absolute tolerance used to terminate the optimization. 0 <= x
	 * @param minFunctionValue The smallest possible value out of the function.  Sometimes used to bound
	 *                         the problem.
	 * @return UnconstrainedMinimization
	 */
	public static UnconstrainedMinimization unconstrained( double relativeErrorTol,
														   double absoluteErrorTol,
														   double minFunctionValue )
	{
		return new WrapQuasiNewtonBFGS(relativeErrorTol,absoluteErrorTol,minFunctionValue);
	}
}
