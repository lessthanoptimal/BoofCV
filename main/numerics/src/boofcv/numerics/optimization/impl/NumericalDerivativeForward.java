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

package boofcv.numerics.optimization.impl;

import boofcv.numerics.optimization.functions.FunctionStoS;
import org.ejml.UtilEjml;

/**
 * Finite difference numerical gradient calculation using forward equation. Forward
 * difference equation, f'(x) = f(x+h)-f(x)/h.  Scaling is taken in account by h based
 * upon the magnitude of the elements in variable x.
 *
 * @author Peter Abeles
 */
public class NumericalDerivativeForward implements FunctionStoS
{
	// function being differentiated
	private FunctionStoS function;

	// scaling of the difference parameter
	private double differenceScale;

	public NumericalDerivativeForward(FunctionStoS function, double differenceScale) {
		this.function = function;
		this.differenceScale = differenceScale;
	}

	public NumericalDerivativeForward(FunctionStoS function) {
		this(function,Math.sqrt(UtilEjml.EPS));
	}

	@Override
	public double process(double x) {
		double valueOrig = function.process(x);
		double h = x != 0 ? differenceScale*Math.abs(x) : differenceScale;

		// takes in account round off error
		double temp = x+h;
		h = temp-x;

		double perturbed = function.process(temp);
		return (perturbed - valueOrig)/h;
	}
}
