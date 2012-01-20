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
 *
 * f'(0) < 0
 * alpha >= 0
 *
 *
 * @author Peter Abeles
 */
public interface LineSearch {

	/**
	 * Sets the function being optimized.
	 *
	 * @param function Function being optimized.
	 * @param derivative Function's derivative.
	 */
	public void setFunction( FunctionStoS function , FunctionStoS derivative );

	/**
	 * Initializes and resets the line search.
	 *
	 * @param funcAtZero Value of f(0)
	 * @param derivAtZero Derivative of at f(0)
	 * @param funcAtInit Value of f at initial value of step: f(step)
	 * @param stepInit Initial step size
	 * @return Approximate value of alpha which minimizes the function
	 */
	public void init( final double funcAtZero, final double derivAtZero,
					  final double funcAtInit, final double stepInit );


	/**
	 * Updates the line search. Check the return value to see if it needs to be iterated more.
	 * After it returns true {@link #isConverged} should be called to see if it converged to a solution
	 * or stopped for some other reason.
	 *
	 * @return If true that means that it has converged or that no more progress can be made.
	 */
	public boolean iterate() throws OptimizationException;

	/**
	 * Indicates if iteration stopped due to convergence or not.
	 *
	 * @return True if iteration stopped because it converged.
	 */
	public boolean isConverged();
	
	/**
	 * Returns the current approximate solution for the line search
	 *
	 * @return current solution
	 */
	public double getStep();

	/**
	 * Provides feed back if something went wrong, but still produced a solution.
	 * If there is no message then null is returned.  The meaning and type of messages
	 * are implementation specific.
	 *
	 * @return Additional info on the computed solution.
	 */
	public String getWarning();
}
