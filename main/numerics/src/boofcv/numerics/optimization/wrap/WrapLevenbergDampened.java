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

package boofcv.numerics.optimization.wrap;

import boofcv.numerics.optimization.OptimizationException;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import boofcv.numerics.optimization.impl.LevenbergDampened;
import boofcv.numerics.optimization.impl.NumericalJacobianForward;

/**
 * Wrapper around {@link boofcv.numerics.optimization.impl.LevenbergMarquardtDampened} for {@link boofcv.numerics.optimization.UnconstrainedLeastSquares}
 *
 * @author Peter Abeles
 */
public class WrapLevenbergDampened implements UnconstrainedLeastSquares {

	LevenbergDampened alg;

	public WrapLevenbergDampened(LevenbergDampened alg) {
		this.alg = alg;
	}

	@Override
	public void setFunction(FunctionNtoM function, FunctionNtoMxN jacobian) {

		if( jacobian == null )
			jacobian = new NumericalJacobianForward(function);

		alg.setFunction(new WrapCoupledJacobian(function,jacobian));
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
		return alg.getMessage();
	}
}
