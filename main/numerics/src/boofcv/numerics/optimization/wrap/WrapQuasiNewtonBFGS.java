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

import boofcv.numerics.optimization.LineSearch;
import boofcv.numerics.optimization.OptimizationException;
import boofcv.numerics.optimization.UnconstrainedMinimization;
import boofcv.numerics.optimization.functions.FunctionNtoN;
import boofcv.numerics.optimization.functions.FunctionNtoS;
import boofcv.numerics.optimization.functions.GradientLineFunction;
import boofcv.numerics.optimization.impl.LineSearchMore94;
import boofcv.numerics.optimization.impl.QuasiNewtonBFGS;

/**
 * @author Peter Abeles
 */
public class WrapQuasiNewtonBFGS implements UnconstrainedMinimization {

	// line search parmeters
	private static final double gtol = 0.9;
	private static final double ftol = 1e-3;
	private static final double xtol = 0.1;

	QuasiNewtonBFGS alg;
	double relativeErrorTol;
	double absoluteErrorTol;
	double minFunctionValue;

	public WrapQuasiNewtonBFGS(double relativeErrorTol,
							   double absoluteErrorTol,
							   double minFunctionValue)
	{
		this.relativeErrorTol = relativeErrorTol;
		this.absoluteErrorTol = absoluteErrorTol;
		this.minFunctionValue = minFunctionValue;
	}

	@Override
	public void setFunction(FunctionNtoS function, FunctionNtoN gradient) {
		LineSearch lineSearch = new LineSearchMore94(ftol,gtol,xtol);

		GradientLineFunction gradLine;

		if( gradient == null ) {
			gradLine = new CachedNumericalGradientLineFunction(function);
		} else {
			gradLine = new CachedGradientLineFunction(function,gradient);
		}

		alg = new QuasiNewtonBFGS(gradLine,lineSearch,minFunctionValue,gtol,relativeErrorTol,absoluteErrorTol);
	}

	@Override
	public void initialize(double[] initial) {
		alg.initialize(initial);
	}

	@Override
	public double[] getParameters() {
		return alg.getParameters();
	}

	@Override
	public boolean iterate() throws OptimizationException {
		return alg.iterate();
	}

	@Override
	public boolean isConverged() {
		return alg.isConverged();
	}

	@Override
	public String getWarning() {
		return alg.getWarning();
	}
}
