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

import boofcv.numerics.optimization.functions.CoupledJacobian;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;

/**
 * Wrapper around {@link FunctionNtoM} and {@link FunctionNtoMxN} for {@link CoupledJacobian}.
 * 
 * @author Peter Abeles
 */
public class WrapCoupledJacobian implements CoupledJacobian {
	
	FunctionNtoM func;
	FunctionNtoMxN jacobian;

	double[] x;
	
	public WrapCoupledJacobian(FunctionNtoM func, FunctionNtoMxN jacobian) {
		if( func.getM() != jacobian.getM() )
			throw new IllegalArgumentException("M not equal");

		if( func.getN() != jacobian.getN() )
			throw new IllegalArgumentException("N not equal");

		this.func = func;
		this.jacobian = jacobian;
	}

	@Override
	public int getN() {
		return func.getN();
	}

	@Override
	public int getM() {
		return func.getM();
	}

	@Override
	public void setInput(double[] x) {
		this.x = x;
	}

	@Override
	public void computeFunctions(double[] output) {
		func.process(x,output);
	}

	@Override
	public void computeJacobian(double[] jacobian) {
		this.jacobian.process(x,jacobian);
	}
}
