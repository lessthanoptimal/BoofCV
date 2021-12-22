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

package boofcv.alg.geo.selfcalib;

import boofcv.misc.BoofMiscOps;
import boofcv.misc.ConfigConverge;
import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.derivative.NumericalJacobianForward_DDRM;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * <p>
 * Non-linear refinement of Dual Quadratic using algebraic error. Different assumptions about the intrinsics
 * parameters can be made, and the number of cameras can be less than the number of views.
 * This should be called after a linear estimate has been generated and should be followed up by bundle adjustment.
 * The optimization function is parameterized using intrinsic parameters of each camera directly along with
 * the plane at infinity.
 * </p>
 *
 * <p>Optional Assumptions: known aspect ratio, known principle point, single camera.</p>
 *
 * <p>
 * w<sup>*</sup><sub>i</sub> = K<sub>i</sub> *K<sup>T</sup><sub>i</sub> <br>
 * w<sup>*</sup><sub>i</sub> = P<sub>i</sub>Q<sup>*</sup><sub>&infin;</sub>P<sup>T</sup><sub>i</sub>
 * </p>
 * where K<sub>i</sub>  is the 3x3 camera calibration matrix for view i. Q is a 4x4 symmetric matrix and is the
 * absolute dual quadratic. P<sub>i</sub> is a projective transform from view i+1 to i.
 *
 * <p>
 * A[i] = P<sub>i</sub>Q<sup>*</sup><sub>&infin;</sub>P<sup>T</sup><sub>i</sub> <br>
 * residual[i] = w<sup>*</sup><sub>i</sub>/||w<sup>*</sup><sub>i</sub>|| - A[i]/||A[i]||
 * </p>
 * Residuals are computed for each projective. The F-norm of the residuals are what is minimized.
 * w and A are normalized to ensure that they have the same scale.
 *
 * NOTE: It would be possible to add zero-skew but this is much more simplistic without it and you rarely need
 * to relax the assumptions.
 *
 * <ol>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ol>
 *
 * @author Peter Abeles
 * @see SelfCalibrationLinearDualQuadratic
 */
public class RefineDualQuadraticAlgebraicError implements VerbosePrint {

	/** Optimization algorithm */
	public @Getter @Setter
	UnconstrainedLeastSquares<DMatrixRMaj> minimizer = FactoryOptimization.levenbergMarquardt(null, false);

	/** If true then the images are assumed to have a known aspect ratio */
	public @Getter @Setter boolean knownAspect = false;

	/** If true then the image center is assumed to be known */
	public @Getter @Setter boolean knownPrinciplePoint = false;

	/** Convergence criteria */
	public @Getter final ConfigConverge converge = new ConfigConverge(1e-12, 1e-8, 20);

	/** Resulting plane at infinity that was found */
	public @Getter final Point3D_F64 planeAtInfinity = new Point3D_F64();

	/** Found/updated camera parameters */
	public @Getter final DogArray<CameraState> cameras = new DogArray<>(CameraState::new, CameraState::reset);
	// cameras is also internal work space when optimizing

	// Cameras that contain the initial state/known state
	DogArray<CameraState> priorCameras = new DogArray<>(CameraState::new, CameraState::reset);

	// Initial estimate of plane at infinity
	Point3D_F64 priorPlaneAtInfinity = new Point3D_F64();

	// Mapping from view to camera
	DogArray_I32 viewToCamera = new DogArray_I32();

	DogArray<DMatrixRMaj> projectiveCameras = new DogArray<>(() -> new DMatrixRMaj(3, 4), DMatrixRMaj::zero);

	// Optimization error function
	ResidualFunction function = new ResidualFunction();

	// Storage for initial parameters
	DogArray_F64 parameters = new DogArray_F64();

	// storage for image of the absolute conic
	DMatrixRMaj w = new DMatrixRMaj(3, 3);
	// storage for the plane at infinity
	DMatrixRMaj p = new DMatrixRMaj(3, 1);
	// storage for p'*w
	DMatrixRMaj pw = new DMatrixRMaj(3, 1);

	@Nullable PrintStream verbose;

	/**
	 * Must call this function before all others. Specifies the number of cameras and views. Preallocates memory.
	 *
	 * @param numCameras Number of cameras.
	 * @param numViews Number of views.
	 */
	public void initialize( int numCameras, int numViews ) {
		BoofMiscOps.checkTrue(numCameras >= 1 && numCameras <= numViews);
		BoofMiscOps.checkTrue(numViews >= 2);

		priorCameras.resetResize(numCameras);
		cameras.resetResize(numCameras);
		projectiveCameras.resetResize(numViews);

		// fill with -1 so that it will fail hard if not initialized
		viewToCamera.resetResize(numViews, -1);
	}

	/**
	 * Specifies the initial parameters or known parameters for a camera
	 *
	 * @param cameraIndex index of the camera
	 * @param fx focal length x-axis
	 * @param cx image center. x-axis
	 * @param cy image center. y-axis
	 * @param aspectRatio aspect ratio
	 */
	public void setCamera( int cameraIndex, double fx, double cx, double cy, double aspectRatio ) {
		priorCameras.get(cameraIndex).setTo(fx, aspectRatio, cx, cy);
	}

	/**
	 * Specifies which view was generated by which camera
	 */
	public void setViewToCamera( int viewIndex, int cameraIndex ) {
		viewToCamera.set(viewIndex, cameraIndex);
	}

	/**
	 * The observed camera matrix
	 *
	 * @param P Camera matrix. 3x4
	 */
	public void setProjective( int cameraIndex, DMatrixRMaj P ) {
		BoofMiscOps.checkEq(3, P.numRows);
		BoofMiscOps.checkEq(4, P.numCols);
		projectiveCameras.get(cameraIndex).setTo(P);
	}

	/**
	 * Specifies the initial state for the plane at infinity
	 */
	public void setPlaneAtInfinity( double x, double y, double z ) {
		this.priorPlaneAtInfinity.setTo(x, y, z);
	}

	/**
	 * Refines the initial parameters to minimize algebraic error.
	 *
	 * @return true if there are no catastrophic errors
	 */
	public boolean refine() {
		BoofMiscOps.checkTrue(viewToCamera.get(0) != -1, "You must specify view to camera");

		// Copy the cameras with prior information into the work cameras
		for (int cameraIdx = 0; cameraIdx < priorCameras.size; cameraIdx++) {
			cameras.get(cameraIdx).setTo(priorCameras.get(cameraIdx));
		}

		parameters.resize(function.getNumOfInputsN());
		encodeParameters(priorPlaneAtInfinity, priorCameras, parameters.data);

		// Configure the minimization
		minimizer.setFunction(function, new NumericalJacobianForward_DDRM(new ResidualFunction()));
		minimizer.initialize(parameters.data, converge.ftol, converge.gtol);

		double errorBefore = minimizer.getFunctionValue();

		// Iterate until a final condition has been met
		int iterations;
		for (iterations = 0; iterations < converge.maxIterations; iterations++) {
			if (minimizer.iterate())
				break;
		}

		if (verbose != null)
			verbose.printf("before=%.2e after=%.2e iterations=%d converged=%s\n",
					errorBefore, minimizer.getFunctionValue(), iterations, minimizer.isConverged());

		// Extract the output
		decodeParameters(minimizer.getParameters(), cameras, planeAtInfinity);

		// Crude sanity check. Things went poorly if there is a non-positive focal length
		for (int i = 0; i < cameras.size; i++) {
			CameraState c = cameras.get(i);
			if (c.fx <= 0 || c.aspectRatio <= 0)
				return false;
		}

		return true;
	}

	protected void encodeParameters( Point3D_F64 planeAtInfinity, DogArray<CameraState> cameras, double[] parameters ) {
		parameters[0] = planeAtInfinity.x;
		parameters[1] = planeAtInfinity.y;
		parameters[2] = planeAtInfinity.z;

		int paramIndex = 3;
		for (int cameraIdx = 0; cameraIdx < cameras.size; cameraIdx++) {
			CameraState work = cameras.get(cameraIdx);
			parameters[paramIndex++] = work.fx;
			if (!knownAspect)
				parameters[paramIndex++] = work.aspectRatio;
			if (!knownPrinciplePoint) {
				parameters[paramIndex++] = work.cx;
				parameters[paramIndex++] = work.cy;
			}
		}
	}

	protected void decodeParameters( double[] parameters, DogArray<CameraState> cameras, Point3D_F64 planeAtInfinity ) {
		// Decode the cameras. plane at infinity is in the first 3 elements
		planeAtInfinity.setTo(parameters[0], parameters[1], parameters[2]);

		int paramIndex = 3;
		for (int cameraIdx = 0; cameraIdx < cameras.size; cameraIdx++) {
			CameraState work = cameras.get(cameraIdx);
			work.fx = parameters[paramIndex++];
			if (!knownAspect)
				work.aspectRatio = parameters[paramIndex++];
			if (!knownPrinciplePoint) {
				work.cx = parameters[paramIndex++];
				work.cy = parameters[paramIndex++];
			}
		}
	}

	/**
	 * Computes K*K
	 */
	protected void encodeKK( CameraState camera, DMatrixRMaj kk ) {
		final double fx = camera.fx;
		final double fy = camera.aspectRatio*fx;
		final double cx = camera.cx;
		final double cy = camera.cy;

		// assign by index. Matrix is in row major order. 3x3
		kk.data[0] = fx*fx + cx*cx;     // (0,0)
		kk.data[1] = cx*cy;
		kk.data[2] = cx;
		kk.data[3] = kk.data[1];        // (1,0)
		kk.data[4] = fy*fy + cy*cy;
		kk.data[5] = cy;
		kk.data[6] = kk.data[2];        // (2,0)
		kk.data[7] = kk.data[5];
		kk.data[8] = 1;
	}

	/**
	 * Encodes the ADQ
	 */
	protected void encodeQ( CameraState camera, double infx, double infy, double infz, DMatrixRMaj Q ) {
		// plane at infinity
		p.data[0] = infx;
		p.data[1] = infy;
		p.data[2] = infz;

		encodeKK(camera, w);

		CommonOps_DDRM.insert(w, Q, 0, 0);

		CommonOps_DDRM.multTransA(p, w, pw);
		double dot = CommonOps_DDRM.dot(pw, p);

		for (int i = 0; i < 3; i++) {
			Q.unsafe_set(i, 3, -pw.get(i));
			Q.unsafe_set(3, i, -pw.get(i));
		}
		Q.unsafe_set(3, 3, dot);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/**
	 * Error function for  sum_i ||K[i]*K'[i] - P[i]*Q*P[i]||. Implementation takes advantage of structure
	 * forced by parameterizing using intrinsic parameters and plane at infinity directory.
	 */
	private class ResidualFunction implements FunctionNtoM {
		// Storage for absolute dual quadratic
		DMatrixRMaj Q = new DMatrixRMaj(4, 4);
		// Storage for K*K where K is the intrinsic calibration matrix
		DMatrixRMaj KK = new DMatrixRMaj(3, 3);

		// Storage for intermediate results
		DMatrixRMaj PQ = new DMatrixRMaj(3, 4);
		DMatrixRMaj PQP = new DMatrixRMaj(3, 3);

		@Override public void process( double[] input, double[] output ) {
			decodeParameters(input, cameras, planeAtInfinity);

			// Compute the Q matrix,
			encodeQ(cameras.get(viewToCamera.get(0)), planeAtInfinity.x, planeAtInfinity.y, planeAtInfinity.z, Q);

			int indexOutput = 0;
			for (int projectiveIdx = 0; projectiveIdx < projectiveCameras.size; projectiveIdx++) {
				int cameraIndex = viewToCamera.get(projectiveIdx);

				encodeKK(cameras.get(cameraIndex), KK);

				DMatrixRMaj P = projectiveCameras.get(projectiveIdx);
				CommonOps_DDRM.mult(P, Q, PQ);
				CommonOps_DDRM.multTransB(PQ, P, PQP);

				// Resolve scale ambiguity by normalizing the matrices
				CommonOps_DDRM.divide(PQP, NormOps_DDRM.normPInf(PQP));
				CommonOps_DDRM.divide(KK, NormOps_DDRM.normPInf(KK));

				// Compute residuals. off diagonal elements are multiply by two since the matrix is symmetric
				output[indexOutput++] = KK.data[0] - PQP.data[0];       // (0,0)
				output[indexOutput++] = 2.0*(KK.data[1] - PQP.data[1]); // (0,1)
				output[indexOutput++] = 2.0*(KK.data[2] - PQP.data[2]); // (0,2)
				output[indexOutput++] = KK.data[4] - PQP.data[4];       // (1,1)
				output[indexOutput++] = 2.0*(KK.data[5] - PQP.data[5]); // (1,2)
				output[indexOutput++] = KK.data[8] - PQP.data[8];       // (2,2)
			}
		}

		@Override public int getNumOfInputsN() {
			int paramsPerCamera = 1;
			if (!knownAspect)
				paramsPerCamera += 1;
			if (!knownPrinciplePoint)
				paramsPerCamera += 2;

			// plane at infinity has 3-dof
			return 3 + paramsPerCamera*priorCameras.size;
		}

		@Override public int getNumOfOutputsM() {
			return 6*projectiveCameras.size;
		}
	}

	public static class CameraState {
		/** Focal length along x-axis in pixels */
		public double fx;

		/** Image aspect ratio. Initial estimate or assumed value */
		public double aspectRatio;

		/** Image principle point (pixels). Either initial estimate or assumed value */
		public double cx, cy;

		public void reset() {
			fx = 0.0;
			aspectRatio = 1.0;
			cx = cy = 0.0;
		}

		public void setTo( double fx, double aspectRatio, double cx, double cy ) {
			this.fx = fx;
			this.aspectRatio = aspectRatio;
			this.cx = cx;
			this.cy = cy;
		}

		public void setTo( CameraState src ) {
			this.fx = src.fx;
			this.aspectRatio = src.aspectRatio;
			this.cx = src.cx;
			this.cy = src.cy;
		}
	}
}
