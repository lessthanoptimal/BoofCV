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

package boofcv.numerics.optimization.wrap;

import boofcv.numerics.optimization.functions.CoupledDerivative;
import boofcv.numerics.optimization.functions.FunctionStoS;

/**
 * @author Peter Abeles
 */
public class WrapCoupledDerivative implements CoupledDerivative {

	double input;
	FunctionStoS function;
	FunctionStoS derivative;

	public WrapCoupledDerivative(FunctionStoS function, FunctionStoS derivative) {
		this.function = function;
		this.derivative = derivative;
	}

	@Override
	public void setInput(double x) {
		input = x;
	}

	@Override
	public double computeFunction() {
		return function.process(input);
	}

	@Override
	public double computeDerivative() {
		return derivative.process(input);
	}
}
