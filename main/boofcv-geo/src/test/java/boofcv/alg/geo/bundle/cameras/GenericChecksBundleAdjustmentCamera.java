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

package boofcv.alg.geo.bundle.cameras;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericChecksBundleAdjustmentCamera extends BoofStandardJUnit {
	BundleAdjustmentCamera model;

	// set of parameters to test
	double[][] parameters;

	double tol = 1e-4;

	public boolean print = false;

	protected double[][] test_X = new double[][]{{0.2, 0.6, 2}, {-0.2, 0.6, 2}, {0.2, -0.6, 2}, {0.2, -0.6, -2}, {1.2, -1.3, 2}};

	protected GenericChecksBundleAdjustmentCamera( BundleAdjustmentCamera model ) {
		this.model = model;
	}

	protected GenericChecksBundleAdjustmentCamera( BundleAdjustmentCamera model, double tol ) {
		this.model = model;
		this.tol = tol;
	}

	public GenericChecksBundleAdjustmentCamera setParameters( double[][] parameters ) {
		this.parameters = parameters;
		return this;
	}

	public GenericChecksBundleAdjustmentCamera setPrint( boolean value ) {
		this.print = value;
		return this;
	}

	public void checkAll() {
		jacobians();
		compare_input_jacobians();
	}

	/**
	 * Check the jacobians numerically
	 */
	@Test void jacobians() {

		// pick a point which would be in front of the camera
		for (double[] X : test_X) {
			for (double[] p : parameters) {
				if (print) {
					System.out.println("param[] " + Arrays.toString(p));
					System.out.println("Point");
					DerivativeChecker.jacobianPrintR(new FunctionOfPoint(p), new JacobianOfPoint(p), X, tol);
					System.out.println("Param");
					DerivativeChecker.jacobianPrintR(new FunctionOfParameters(X), new JacobianOfParameters(X), p, tol);
				}

				assertTrue(DerivativeChecker.jacobianR(new FunctionOfPoint(p), new JacobianOfPoint(p), X, tol));
				assertTrue(DerivativeChecker.jacobianR(new FunctionOfParameters(X), new JacobianOfParameters(X), p, tol));
			}
		}
	}

	/**
	 * Make sure the jacobian of the input is the same no matter how it's computed
	 */
	@Test void compare_input_jacobians() {
		for (double[] X : test_X) {
			double[] found0 = new double[3];
			double[] found1 = new double[3];
			double[] found2 = new double[3];
			double[] found3 = new double[3];

			int N = model.getIntrinsicCount();
			for (double[] p : parameters) {
				model.setIntrinsic(p, 0);
				model.jacobian(X[0], X[1], X[2], found0, found1, true, new double[N], new double[N]);
				model.jacobian(X[0], X[1], X[2], found2, found3, false, null, null);

				for (int i = 0; i < 3; i++) {
					assertEquals(found0[i], found2[i], UtilEjml.TEST_F64);
					assertEquals(found1[i], found3[i], UtilEjml.TEST_F64);
				}
			}
		}
	}

	private class FunctionOfPoint implements FunctionNtoM {

		Point2D_F64 p = new Point2D_F64();

		public FunctionOfPoint( double[] parameters ) {
			model.setIntrinsic(parameters, 0);
		}

		@Override
		public void process( double[] input, double[] output ) {
			model.project(input[0], input[1], input[2], p);
			output[0] = p.x;
			output[1] = p.y;
		}

		@Override
		public int getNumOfInputsN() {
			return 3;
		}

		@Override
		public int getNumOfOutputsM() {
			return 2;
		}
	}

	private class JacobianOfPoint implements FunctionNtoMxN<DMatrixRMaj> {

		double[] gradX;
		double[] gradY;

		public JacobianOfPoint( double[] parameters ) {
			model.setIntrinsic(parameters, 0);
			gradX = new double[3];
			gradY = new double[3];
		}

		@Override
		public void process( double[] input, DMatrixRMaj output ) {
			model.jacobian(input[0], input[1], input[2], gradX, gradY, false, null, null);
			for (int i = 0; i < 3; i++) {
				output.data[i] = gradX[i];
				output.data[3 + i] = gradY[i];
			}
		}

		@Override
		public int getNumOfInputsN() {
			return 3;
		}

		@Override
		public int getNumOfOutputsM() {
			return 2;
		}

		@Override
		public DMatrixRMaj declareMatrixMxN() {
			return new DMatrixRMaj(getNumOfOutputsM(), getNumOfInputsN());
		}
	}

	private class FunctionOfParameters implements FunctionNtoM {
		Point2D_F64 p = new Point2D_F64();
		double[] X;

		public FunctionOfParameters( double[] X ) {
			this.X = X;
		}

		@Override
		public void process( double[] input, double[] output ) {
			model.setIntrinsic(input, 0);
			model.project(X[0], X[1], X[2], p);
			output[0] = p.x;
			output[1] = p.y;
		}

		@Override
		public int getNumOfInputsN() {
			return model.getIntrinsicCount();
		}

		@Override
		public int getNumOfOutputsM() {
			return 2;
		}
	}

	private class JacobianOfParameters implements FunctionNtoMxN<DMatrixRMaj> {
		double[] X;
		double[] gradX;
		double[] gradY;

		public JacobianOfParameters( double[] X ) {
			this.X = X;
			gradX = new double[model.getIntrinsicCount()];
			gradY = new double[model.getIntrinsicCount()];
		}

		@Override
		public void process( double[] input, DMatrixRMaj output ) {
			model.setIntrinsic(input, 0);
			model.jacobian(X[0], X[1], X[2], new double[3], new double[3], true, gradX, gradY);
			int N = gradX.length;
			for (int i = 0; i < N; i++) {
				output.data[i] = gradX[i];
				output.data[N + i] = gradY[i];
			}
		}

		@Override
		public int getNumOfInputsN() {
			return model.getIntrinsicCount();
		}

		@Override
		public int getNumOfOutputsM() {
			return 2;
		}

		@Override
		public DMatrixRMaj declareMatrixMxN() {
			return new DMatrixRMaj(getNumOfOutputsM(), getNumOfInputsN());
		}
	}
}
