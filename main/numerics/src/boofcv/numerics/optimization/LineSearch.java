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
 * <p>
 * Line search for nonlinear optimization.  Computes function values at different step lengths.
 * all step lengths must be greater than or equal to zero.  The derivative at step zero must be
 * less than zero.
 * </p>
 *
 * @author Peter Abeles
 */
public interface LineSearch extends IterativeOptimization {

	/**
	 * Sets the function being optimized.
	 *
	 * @param function Function being optimized.
	 * @param derivative Function's derivative.
	 */
	public void setFunction( FunctionStoS function , FunctionStoS derivative );

	/**
	 * Initializes and resets the line search.  In some implementations a reasonable
	 * minimum and maximum step bound is set here.
	 *
	 * @param funcAtZero Value of f(0)
	 * @param derivAtZero Derivative of at f(0)
	 * @param funcAtInit Value of f at initial value of step: f(step)
	 * @param stepInit Initial step size
	 * @param stepMin Minimum allowed step.
	 * @param stepMax Maximum allowed step.
	 * @return Approximate value of alpha which minimizes the function
	 */
	public void init( final double funcAtZero, final double derivAtZero,
					  final double funcAtInit, final double stepInit ,
					  double stepMin, double stepMax );

	/**
	 * Returns the current approximate solution for the line search
	 *
	 * @return current solution
	 */
	public double getStep();
}
