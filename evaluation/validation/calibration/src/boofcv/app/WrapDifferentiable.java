/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.app;

import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;

/**
 * @author Peter Abeles
 */
public class WrapDifferentiable implements DifferentiableMultivariateVectorFunction {

	FunctionNtoM function;
	FunctionNtoMxN jacobian;

	double output[];

	public WrapDifferentiable(FunctionNtoM function, FunctionNtoMxN jacobian) {
		this.function = function;
		this.jacobian = jacobian;

		output = new double[function.getM()];
	}

	@Override
	public MultivariateMatrixFunction jacobian() {
		return new WrapMultivariateMatrixFunction(jacobian);
	}

	@Override
	public double[] value(double[] point) throws IllegalArgumentException {
		function.process(point,output);
		return output;
	}
}
