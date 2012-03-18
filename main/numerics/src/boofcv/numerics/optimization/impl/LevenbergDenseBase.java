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

import boofcv.numerics.optimization.functions.CoupledJacobian;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * @author Peter Abeles
 */
public abstract class LevenbergDenseBase extends LevenbergBase {

	// jacobian at x
	protected DenseMatrix64F jacobianVals = new DenseMatrix64F(1,1);

	// Jacobian inner product. Used to approximate Hessian
	// B=J'*J
	protected DenseMatrix64F B = new DenseMatrix64F(1,1);
	// diagonal elements of JtJ
	protected DenseMatrix64F Bdiag = new DenseMatrix64F(1,1);

	// Least-squares Function being optimized
	protected CoupledJacobian function;

	public LevenbergDenseBase(double initialDampParam, double absoluteErrorTol, double relativeErrorTol) {
		super(initialDampParam, absoluteErrorTol, relativeErrorTol);
	}

	@Override
	protected void setFunctionParameters(double[] param) {
		function.setInput(param);
	}

	@Override
	protected void computeResiduals(double[] output) {
		function.computeFunctions(output);
	}

	@Override
	protected double getMinimumDampening() {
		return CommonOps.elementMax(Bdiag);
	}

	/**
	 * Specifies function being optimized.
	 *
	 * @param function Computes residuals and Jacobian.
	 */
	public void setFunction( CoupledJacobian function ) {
		internalInitialize(function.getN(),function.getM());
		this.function = function;

		jacobianVals.reshape(M,N,false);

		B.reshape(N, N, false);
		Bdiag.reshape(N,1,false);
	}
}
