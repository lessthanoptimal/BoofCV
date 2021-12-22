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

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.optimization.derivative.NumericalJacobianForward_DDRM;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.optimization.functions.FunctionNtoMxN;
import org.ejml.data.DMatrixRMaj;

/**
 * Computes numerical jacobian from {@link BundleAdjustmentCamera}. The specific numerical Jacobian
 * algorithm is configurable by overriding {@link #createNumericalAlgorithm(FunctionNtoM)}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BundleCameraNumericJacobian {

	BundleAdjustmentCamera model; // camera model
	private final double[] X = new double[3]; // storage for camera point as an array
	double[] intrinsic = new double[0]; // storage for intrinsic parameters
	int numIntrinsic; // length of intrinsic parameters. Array could be a different length

	// numerical Jacobian algorithms
	private FunctionNtoMxN<DMatrixRMaj> numericalPoint;
	private FunctionNtoMxN<DMatrixRMaj> numericalIntrinsic;

	// functions for computing numeric jacobian
	private final FunctionOfPoint funcPoint = new FunctionOfPoint();
	private final FunctionOfIntrinsic funcIntrinsic = new FunctionOfIntrinsic();

	// storage for computed jacobian
	private final DMatrixRMaj jacobian = new DMatrixRMaj(1, 1);

	// workspace
	private final Point2D_F64 p = new Point2D_F64();

	/**
	 * Specifies the camera model. The current state of its intrinsic parameters
	 */
	public void setModel( BundleAdjustmentCamera model ) {
		this.model = model;
		numIntrinsic = model.getIntrinsicCount();
		if (numIntrinsic > intrinsic.length) {
			intrinsic = new double[numIntrinsic];
		}
		model.getIntrinsic(intrinsic, 0);

		numericalPoint = createNumericalAlgorithm(funcPoint);
		numericalIntrinsic = createNumericalAlgorithm(funcIntrinsic);
	}

	protected FunctionNtoMxN<DMatrixRMaj> createNumericalAlgorithm( FunctionNtoM function ) {
		return new NumericalJacobianForward_DDRM(function);
	}

	/**
	 * Computes Jacobian for Point
	 *
	 * @param camX 3D point in camera reference frame
	 * @param camY 3D point in camera reference frame
	 * @param camZ 3D point in camera reference frame
	 * @param pointX (Output) Partial of projected x' relative to input camera point.<code>[@x'/@camX, @ x' / @ camY, @ x' / @ camZ]</code> length 3
	 * @param pointY (Output) Partial of projected y' relative to input camera point.<code>[@y'/@camX, @ y' / @ camY, @ y' / @ camZ]</code> length 3
	 */
	public void jacobianPoint( double camX, double camY, double camZ,
							   double[] pointX, double[] pointY ) {
		funcPoint.setParameters(intrinsic);
		X[0] = camX; X[1] = camY; X[2] = camZ;
		jacobian.reshape(2, 3);
		numericalPoint.process(X, jacobian);

		for (int i = 0; i < 3; i++) {
			pointX[i] = jacobian.data[i];
			pointY[i] = jacobian.data[i + 3];
		}
	}

	/**
	 * Computes Jacobian for intrinsic parameters
	 *
	 * @param camX 3D point in camera reference frame
	 * @param camY 3D point in camera reference frame
	 * @param camZ 3D point in camera reference frame
	 * @param calibX (Output) Partial of projected x' relative to calibration parameters. length N
	 * @param calibY (Output) Partial of projected y' relative to calibration parameters. length N
	 */
	public void jacobianIntrinsics( double camX, double camY, double camZ,
									double[] calibX, double[] calibY ) {
		funcIntrinsic.X.setTo(camX, camY, camZ);

		jacobian.reshape(2, numIntrinsic);
		numericalIntrinsic.process(intrinsic, jacobian);

		for (int i = 0; i < numIntrinsic; i++) {
			calibX[i] = jacobian.data[i];
			calibY[i] = jacobian.data[i + numIntrinsic];
		}

		// make sure its intrinsic parameters have not been modified
		model.setIntrinsic(intrinsic, 0);
	}

	/**
	 * Wrapper to convert the projection function into a format the numerical jacobian understands
	 */
	private class FunctionOfPoint implements FunctionNtoM {

		public void setParameters( double[] parameters ) {
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

	/**
	 * Wrapper to convert the intrinsic function into a format the numerical jacobian understands
	 */
	private class FunctionOfIntrinsic implements FunctionNtoM {
		Point3D_F64 X = new Point3D_F64(); // the point in camera reference frame

		@Override
		public void process( double[] input, double[] output ) {
			model.setIntrinsic(input, 0);
			model.project(X.x, X.y, X.z, p);
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
}
