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

import org.ejml.UtilEjml;

/**
 * Computes a numerical approximation of the Jacobian using a forward difference. The gradient
 * calculation is designed to be scale invariant, but different "differenceScale" parameters
 * might produce better results.  Based upon the discussion in minpack's documentation.
 *
 * @author Peter Abeles
 */
public class NumericalJacobian<Observation,State> implements OptimizationDerivative<State> {

	// base model parameters
	double[] model;

	// each function's output with original model parameters
	double[] outputOrig;
	// modified function output after adjusting the model
	double[] outputModified;

	// scaling of the difference parameter
	double differenceScale;

	// function whose derivative is being numerically estimated
	OptimizationFunction<State> function;

	public NumericalJacobian(OptimizationFunction<State> function )
	{
		this(function, Math.sqrt(UtilEjml.EPS));
	}

	public NumericalJacobian(OptimizationFunction<State> function,
							 double differenceScale ) {
		this.function = function;
		model = new double[ function.getModelSize() ];
		this.differenceScale = differenceScale;

		outputOrig = new double[ function.getNumberOfFunctions() ];
		outputModified = new double[ function.getNumberOfFunctions() ];
	}

	@Override
	public void setModel(double[] model) {
		System.arraycopy(model, 0, this.model, 0, model.length);
	}

	@Override
	public boolean computeDerivative( State state, double[][] gradient) {
		function.setModel(model);
		function.estimate(state,outputOrig);

		for( int i = 0; i < model.length; i++ ) {
			double x = model[i];
			double h = x != 0 ? differenceScale*Math.abs(x) : differenceScale;

			// takes in account round off error
			double temp = x+h;
			h = temp-x;
			
			model[i] = temp;
			function.setModel(model);
			function.estimate(state,outputModified);

			for( int j = 0; j < function.getNumberOfFunctions(); j++ ) {
				gradient[j][i] = (outputModified[j] - outputOrig[j])/h;
			}

			model[i] = x;
		}

		return true;
	}
}
