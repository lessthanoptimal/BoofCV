/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle.jacobians;

import org.ddogleg.optimization.derivative.NumericalJacobianForward_DDRM;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DMatrixRMaj;

/**
 * Implements a numerical Jacobian for the SO3
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class JacobianSo3Numerical implements JacobianSo3 {

	private FunctionNtoMxN<DMatrixRMaj> numericalJac; // function used to compute numerical jacobian
	private double[] paramInternal; // copy of parameters
	private DMatrixRMaj[] jacR; // storage for the partials by parameter
	private DMatrixRMaj jacobian; // storage for the numerical Jacobian
	private int N; // number of parameters
	private final FunctionOfPoint function = new FunctionOfPoint(); // used to compute numerical jacobian

	private final DMatrixRMaj R = new DMatrixRMaj(3, 3);

	protected JacobianSo3Numerical() {
		init();
	}

	/**
	 * Initializes data structures. Separate function to make it easier to extend the class
	 */
	public void init() {
		N = getParameterLength();
		jacR = new DMatrixRMaj[N];
		for (int i = 0; i < N; i++) {
			jacR[i] = new DMatrixRMaj(3, 3);
		}
		jacobian = new DMatrixRMaj(N, 9);
		paramInternal = new double[N];
		numericalJac = createNumericalAlgorithm(function);
	}

	/**
	 * Creates numerical Jacobian algorithm. Override to change algorithm/settings
	 */
	protected FunctionNtoMxN<DMatrixRMaj> createNumericalAlgorithm( FunctionNtoM function ) {
		return new NumericalJacobianForward_DDRM(function);
	}

	@Override
	public void setParameters( double[] parameters, int offset ) {
		computeRotationMatrix(parameters, offset, R);
		// copy it because numerical jacobian requires the index to start at 0
		System.arraycopy(parameters, offset, paramInternal, 0, N);

		numericalJac.process(paramInternal, jacobian);
		for (int i = 0, idx = 0; i < 9; i++) {
			for (int j = 0; j < N; j++, idx++) {
				jacR[j].data[i] = jacobian.data[idx];
			}
		}
	}

	/**
	 * Computes rotation matrix given the parameters
	 */
	public abstract void computeRotationMatrix( double[] parameters, int offset, DMatrixRMaj R );

	@Override
	public DMatrixRMaj getPartial( int param ) {
		return jacR[param];
	}

	@Override
	public DMatrixRMaj getRotationMatrix() {
		return R;
	}

	private class FunctionOfPoint implements FunctionNtoM {

		@Override
		public void process( double[] input, double[] output ) {
			computeRotationMatrix(input, 0, R);
			System.arraycopy(R.data, 0, output, 0, 9);
		}

		@Override
		public int getNumOfInputsN() {
			return N;
		}

		@Override
		public int getNumOfOutputsM() {
			return 9;
		}
	}
}
