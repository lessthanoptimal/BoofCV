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

package boofcv.numerics.optimization.impl;

import org.ejml.data.DenseMatrix64F;

/**
 * <p>
 * Computes the next step to take in the trust region approach and the expected reduction.  This interface has been
 * designed to facilitate efficient computation.
 * </p>
 * 
 * <p>
 * The predicted reduction is defined as L(0) - L(h)<br>
 * where L(h) is a linear model:<br>
 * L(h) = 0.5||f(x) + J(x)*h||<sup>2</sup><br>
 * which can be reduced to:<br>
 * L(0) - L(h) = -f<sup>T</sup>(x)*J(x)*h  - 0.5*h<sup>T</sup>J(x)<sup>T</sup>*J(x)*h
 * </p>
 * 
 * @author Peter Abeles
 */
public interface TrustRegionStep {

	/**
	 * Initialize internal data structures.  Only needs to be called once.
	 * 
	 * @param numParam Number of parameters being optimizes.  This is the length of 'x'
	 * @param numFunctions Number of functions. Number of outputs to f(x)
	 */
	public void init( int numParam , int numFunctions );

	/**
	 * Specifies the state of the system being optimized.  Call before {@link #computeStep}.
	 * 
	 * @param x Sample point being considered.
	 * @param residuals Function output: f(x)
	 * @param J Jacobian: J(x)
	 * @param gradient Gradient: J<sup>T</sup>(x)*f(x)
	 * @param fx Residual at x: 0.5*f<sup>T</sup>(x)*f(x)
	 */
	public void setInputs( DenseMatrix64F x , DenseMatrix64F residuals , DenseMatrix64F J ,
						   DenseMatrix64F gradient , double fx );

	/**
	 * Computes the next step to take for a given trust region.  Must invoke {@link #setInputs} before, but it only
	 * needs to be called once.
	 * 
	 * @param regionRadius Size of the trust region.
	 * @param step Output, the computed step.
	 */
	public void computeStep( double regionRadius , DenseMatrix64F step );

	/**
	 * Returns the predicted reduction for the step.  A linear model is used to predict the reduction.  See
	 * class description for
	 * 
	 * @return The predicted reduction.
	 */
	public double predictedReduction();

	/**
	 * Was a step equal to the regionRadius taken?
	 * 
	 * @return true if maximum step and false if less than the maximum step
	 */
	public boolean isMaxStep();
}
