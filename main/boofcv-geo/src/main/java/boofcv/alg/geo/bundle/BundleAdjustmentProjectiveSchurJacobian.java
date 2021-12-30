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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentSchur;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.ReshapeMatrix;
import org.jetbrains.annotations.Nullable;

/**
 * Computes the Jacobian for {@link BundleAdjustmentSchur} for generic matrices.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class BundleAdjustmentProjectiveSchurJacobian<M extends DMatrix>
		implements BundleAdjustmentSchur.Jacobian<SceneStructureProjective, M> {
	private SceneStructureProjective structure;
	private SceneObservations observations;

	// work space for jacobian
	private final DMatrixRMaj worldToView = new DMatrixRMaj(3, 4);

	// number of views with parameters that are going to be adjusted
	private int numViewsUnknown;

	// total number of parameters being optimized
	private int numParameters;

	// length of a 3D point. 3 = regular, 4 = homogenous
	private int lengthPoint;

	// feature location in world coordinates
	private final Point4D_F64 worldPt = new Point4D_F64();

	// Observed pixel in homogenous coordinates. X'=P*X
	private final Point3D_F64 pixelH = new Point3D_F64();

	// index in parameters of the first point
	private int indexFirstView;
	private int indexLastView;
	// view to parameter index
	private int[] viewParameterIndexes;
	// first index in input/parameters vector for each camera. Right side
	private int[] cameraParameterIndexes;

	// Jacobian matrix index of x and y partial
	private int jacRowX, jacRowY;

	// Storage for gradients
	private final double[] worldGradX = new double[4];  // pixel homogeneous partial to world point homogeneous
	private final double[] worldGradY = new double[4];
	private final double[] worldGradZ = new double[4];
	private final double[] camGradX = new double[12];   // pixel homogeneous partial to camera matrix
	private final double[] camGradY = new double[12];
	private final double[] camGradZ = new double[12];
	private final double[] pixelhGradX = new double[3]; // 2D pixel partial to pixel homogeneous
	private final double[] pixelhGradY = new double[3];
	private double[] intrGradX;                         // 2D pixel partial to intrinsic camera parameters
	private double[] intrGradY;
	private final double[] chainRuleX = new double[12]; // Storage for partial computed using chain rule
	private final double[] chainRuleY = new double[12];

	@Override
	public void configure( SceneStructureProjective structure, SceneObservations observations ) {
		this.structure = structure;
		this.observations = observations;

		if (!structure.isHomogenous()) {
			worldPt.w = 1;
			lengthPoint = 3;
		} else {
			lengthPoint = 4;
		}

		numViewsUnknown = structure.getUnknownViewCount();
		int numCameraParameters = structure.getUnknownCameraParameterCount();

		indexFirstView = structure.points.size*lengthPoint;
		indexLastView = indexFirstView + numViewsUnknown*12;
		numParameters = indexLastView + numCameraParameters;

		viewParameterIndexes = new int[structure.views.size];
		for (int i = 0, index = 0; i < structure.views.size; i++) {
			viewParameterIndexes[i] = index;
			if (!structure.views.data[i].known) {
				index += 12;
			}
		}

		// Create a lookup table for each camera. Camera ID to location in parameter vector
		cameraParameterIndexes = new int[structure.cameras.size];
		int largestCameraSize = 0;
		for (int i = 0, index = 0; i < structure.cameras.size; i++) {
			if (!structure.cameras.get(i).known) {
				cameraParameterIndexes[i] = index;
				int count = structure.cameras.data[i].model.getIntrinsicCount();
				largestCameraSize = Math.max(largestCameraSize, count);
				index += count;
			}
		}

		intrGradX = new double[largestCameraSize];
		intrGradY = new double[largestCameraSize];
	}

	@Override
	public int getNumOfInputsN() {
		return numParameters;
	}

	@Override
	public int getNumOfOutputsM() {
		return observations.getObservationCount()*2;
	}

	/**
	 * Internal matrix type agnostic process function.
	 *
	 * @param input Input parameters describing the current state of the optimization
	 * @param leftPoint Storage for left Jacobian
	 * @param rightView Storage for right Jacobian
	 */
	public void processInternal( double[] input, DMatrix leftPoint, DMatrix rightView ) {
		int numRows = getNumOfOutputsM();
		int numPointParam = structure.points.size*lengthPoint;
		int numViewParam = numParameters - numPointParam; // view + camera

		((ReshapeMatrix)leftPoint).reshape(numRows, numPointParam);
		((ReshapeMatrix)rightView).reshape(numRows, numViewParam);
		leftPoint.zero();
		rightView.zero();

		int observationIndex = 0;
		// first decode the transformation
		for (int viewIndex = 0; viewIndex < structure.views.size; viewIndex++) {
			SceneStructureProjective.View view = structure.views.data[viewIndex];
			SceneStructureCommon.Camera camera = structure.cameras.data[view.camera];
			int cameraParamStartIndex = cameraParameterIndexes[view.camera];

			if (!view.known) {
				int paramIndex = viewParameterIndexes[viewIndex] + indexFirstView;
				for (int i = 0; i < 12; i++) {
					worldToView.data[i] = input[paramIndex++];
				}
			} else {
				worldToView.setTo(view.worldToView);
			}

			SceneObservations.View obsView = observations.views.data[viewIndex];

			for (int i = 0; i < obsView.size(); i++) {
				int featureIndex = obsView.point.get(i);
				int columnOfPointInJac = featureIndex*lengthPoint;

				worldPt.x = input[columnOfPointInJac];
				worldPt.y = input[columnOfPointInJac + 1];
				worldPt.z = input[columnOfPointInJac + 2];
				if (structure.isHomogenous()) {
					worldPt.w = input[columnOfPointInJac + 3];
				}

				// X' = P*X
				PerspectiveOps.renderPixel(worldToView, worldPt, pixelH);

				if (view.known) {
					if (structure.isHomogenous())
						partialCameraMatrixH(worldPt.x, worldPt.y, worldPt.z, worldPt.w,
								worldToView, worldGradX, worldGradY, worldGradZ, null, null, null);
					else
						partialCameraMatrix(worldPt.x, worldPt.y, worldPt.z,
								worldToView, worldGradX, worldGradY, worldGradZ, null, null, null);
				} else {
					if (structure.isHomogenous())
						partialCameraMatrixH(worldPt.x, worldPt.y, worldPt.z, worldPt.w,
								worldToView, worldGradX, worldGradY, worldGradZ, camGradX, camGradY, camGradZ);
					else
						partialCameraMatrix(worldPt.x, worldPt.y, worldPt.z,
								worldToView, worldGradX, worldGradY, worldGradZ, camGradX, camGradY, camGradZ);
				}

				jacRowX = observationIndex*2;
				jacRowY = jacRowX + 1;

				//============ Partial of camera parameters
				if (!camera.known) {
					int N = camera.model.getIntrinsicCount();
					camera.model.jacobian(pixelH.x, pixelH.y, pixelH.z,
							pixelhGradX, pixelhGradY, true, intrGradX, intrGradY);

					int location = indexLastView - indexFirstView + cameraParamStartIndex;

					// partial of residual (pixel) w.r.t. intrinsic camera parameters
					for (int j = 0; j < N; j++) {
						set(rightView, jacRowX, location + j, intrGradX[j]);
						set(rightView, jacRowY, location + j, intrGradY[j]);
					}
				} else {
					camera.model.jacobian(pixelH.x, pixelH.y, pixelH.z, pixelhGradX, pixelhGradY,
							false, null, null);
				}

				//============ Partial of worldPt
				// partial of residual (pixel) w.r.t. world point X
				for (int j = 0; j < lengthPoint; j++) {
					chainRuleX[j] = pixelhGradX[0]*worldGradX[j] + pixelhGradX[1]*worldGradY[j] + pixelhGradX[2]*worldGradZ[j];
					chainRuleY[j] = pixelhGradY[0]*worldGradX[j] + pixelhGradY[1]*worldGradY[j] + pixelhGradY[2]*worldGradZ[j];
				}
				addToJacobian(leftPoint, columnOfPointInJac, lengthPoint, chainRuleX, chainRuleY);

				if (!view.known) {
					// partial of residual (pixel) w.r.t. camera matrix P
					for (int j = 0; j < 12; j++) {
						chainRuleX[j] = pixelhGradX[0]*camGradX[j] + pixelhGradX[1]*camGradY[j] + pixelhGradX[2]*camGradZ[j];
						chainRuleY[j] = pixelhGradY[0]*camGradX[j] + pixelhGradY[1]*camGradY[j] + pixelhGradY[2]*camGradZ[j];
					}

					// partial of x' = (1/z)*P*X with respect to P is a 2 by 12 matrix
					int col = viewParameterIndexes[viewIndex];
					addToJacobian(rightView, col, 12, chainRuleX, chainRuleY);
				}

				observationIndex++;
			}
		}
	}

	static void partialCameraMatrix( double X, double Y, double Z,
									 DMatrixRMaj P,
									 double[] pointGradX, double[] pointGradY, double[] pointGradZ,
									 @Nullable double[] camGradX, @Nullable double[] camGradY, @Nullable double[] camGradZ ) {
		double P11 = P.data[0], P12 = P.data[1], P13 = P.data[2];
		double P21 = P.data[4], P22 = P.data[5], P23 = P.data[6];
		double P31 = P.data[8], P32 = P.data[9], P33 = P.data[10];

//		double xx = P11*X +P12*Y + P13*Z + P14;
//		double yy = P21*X +P22*Y + P23*Z + P24;
//		double zz = P31*X +P32*Y + P33*Z + P34;

		// @formatter:off
		pointGradX[0] = P11; pointGradX[1] = P12; pointGradX[2] = P13;
		pointGradY[0] = P21; pointGradY[1] = P22; pointGradY[2] = P23;
		pointGradZ[0] = P31; pointGradZ[1] = P32; pointGradZ[2] = P33;

		if (camGradX == null || camGradY == null || camGradZ == null)
			return;

		camGradX[0] = X; camGradX[1] = Y; camGradX[2 ] = Z; camGradX[3 ] = 1;
		camGradX[4] = 0; camGradX[5] = 0; camGradX[6 ] = 0; camGradX[7 ] = 0;
		camGradX[8] = 0; camGradX[9] = 0; camGradX[10] = 0; camGradX[11] = 0;

		camGradY[0] = 0; camGradY[1] = 0; camGradY[2 ] = 0; camGradY[3 ] = 0;
		camGradY[4] = X; camGradY[5] = Y; camGradY[6 ] = Z; camGradY[7 ] = 1;
		camGradY[8] = 0; camGradY[9] = 0; camGradY[10] = 0; camGradY[11] = 0;

		camGradZ[0] = 0; camGradZ[1] = 0; camGradZ[2 ] = 0; camGradZ[3 ] = 0;
		camGradZ[4] = 0; camGradZ[5] = 0; camGradZ[6 ] = 0; camGradZ[7 ] = 0;
		camGradZ[8] = X; camGradZ[9] = Y; camGradZ[10] = Z; camGradZ[11] = 1;
		// @formatter:on
	}

	static void partialCameraMatrixH( double X, double Y, double Z, double W,
									  DMatrixRMaj P,
									  double[] pointGradX, double[] pointGradY, double[] pointGradZ,
									  @Nullable double[] camGradX, @Nullable double[] camGradY, @Nullable double[] camGradZ ) {
		double P11 = P.data[0], P12 = P.data[1], P13 = P.data[2], P14 = P.data[3];
		double P21 = P.data[4], P22 = P.data[5], P23 = P.data[6], P24 = P.data[7];
		double P31 = P.data[8], P32 = P.data[9], P33 = P.data[10], P34 = P.data[11];

//		pixelH.x = P11*X + P12*Y + P13*Z + P14*W;
//		pixelH.y = P21*X + P22*Y + P23*Z + P24*W;
//		pixelH.z = P31*X + P32*Y + P33*Z + P34*W;

		// @formatter:off
		pointGradX[0] = P11; pointGradX[1] = P12; pointGradX[2] = P13; pointGradX[3] = P14;
		pointGradY[0] = P21; pointGradY[1] = P22; pointGradY[2] = P23; pointGradY[3] = P24;
		pointGradZ[0] = P31; pointGradZ[1] = P32; pointGradZ[2] = P33; pointGradZ[3] = P34;

		if (camGradX == null || camGradY == null || camGradZ == null)
			return;

		camGradX[0] = X; camGradX[1] = Y; camGradX[2 ] = Z; camGradX[3 ] = W;
		camGradX[4] = 0; camGradX[5] = 0; camGradX[6 ] = 0; camGradX[7 ] = 0;
		camGradX[8] = 0; camGradX[9] = 0; camGradX[10] = 0; camGradX[11] = 0;

		camGradY[0] = 0; camGradY[1] = 0; camGradY[2 ] = 0; camGradY[3 ] = 0;
		camGradY[4] = X; camGradY[5] = Y; camGradY[6 ] = Z; camGradY[7 ] = W;
		camGradY[8] = 0; camGradY[9] = 0; camGradY[10] = 0; camGradY[11] = 0;

		camGradZ[0] = 0; camGradZ[1] = 0; camGradZ[2 ] = 0; camGradZ[3 ] = 0;
		camGradZ[4] = 0; camGradZ[5] = 0; camGradZ[6 ] = 0; camGradZ[7 ] = 0;
		camGradZ[8] = X; camGradZ[9] = Y; camGradZ[10] = Z; camGradZ[11] = W;
		// @formatter:on
	}

	private void addToJacobian( DMatrix triplet, int col, int length, double[] a, double[] b ) {
		for (int i = 0; i < length; i++) {
			set(triplet, jacRowX, col + i, a[i]);
			set(triplet, jacRowY, col + i, b[i]);
		}
	}

	/**
	 * Abstract interface for settings the value of a matrix without knowing the type of matrix
	 */
	protected abstract void set( DMatrix matrix, int row, int col, double value );
}
