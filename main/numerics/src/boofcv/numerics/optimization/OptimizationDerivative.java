/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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
 * Interface for computing the gradient of a set of functions given a set of model parameters.
 * Before {@link #computeDerivative(Object, double[][])} is called, the model must be set using
 * {@link #setModel(double[])}.
 *
 * @author Peter Abeles
 */
public interface OptimizationDerivative<State>
{
	/**
	 * Specifies the current model parameters around which the gradient is computed.
	 *
	 * @param model Model parameters.
	 */
	public void setModel( double[] model );

	/**
	 * <p>
	 * Computes the gradient for each function with respect to model parameters.  The
	 * derivative is a 2D array.  The first axis is for each function and the second
	 * for each model parameter:<br>
	 * <br>
	 * derivative[i][j] = &partial; f<sub>i</sub> / &partial; p<sub>j</sub>
	 * </p>
	 *
	 * @param state State of the system being examined.
	 * @param gradient Gradient with respect to the current model parameters,
	 * @return true if successful or false if it failed.
	 */
	public boolean computeDerivative( State state, double[][] gradient );
}
