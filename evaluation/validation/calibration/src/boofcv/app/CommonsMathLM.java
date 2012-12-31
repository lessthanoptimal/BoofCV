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

import org.ddogleg.optimization.OptimizationException;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ddogleg.optimization.impl.NumericalJacobianForward;
import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;

import java.util.Arrays;

/**
 * @author Peter Abeles
 */
public class CommonsMathLM implements UnconstrainedLeastSquares {

	LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer();

	DifferentiableMultivariateVectorFunction problem;

	double target[];
	double weights[];

	double solution[];

	@Override
	public void setFunction(FunctionNtoM function, FunctionNtoMxN jacobian) {

		if( jacobian == null )
			jacobian = new NumericalJacobianForward(function);

		problem = new WrapDifferentiable(function,jacobian);

		int numFuncs = function.getM();
		target = new double[numFuncs];
		weights = new double[numFuncs];
		Arrays.fill(weights,1);
	}

	@Override
	public void initialize(double[] initial, double ftol, double gtol) {


		solution = optimizer.optimize(500,
				problem,
				target,
				weights,
				initial).getPoint();
	}

	@Override
	public double[] getParameters() {
		return solution;
	}

	@Override
	public double getFunctionValue() {
		return 0;
	}

	@Override
	public boolean iterate() throws OptimizationException {
		return true;
	}

	@Override
	public boolean isUpdated() {
		return true;
	}

	@Override
	public boolean isConverged() {
		return true;
	}

	@Override
	public String getWarning() {
		return null;
	}
}
