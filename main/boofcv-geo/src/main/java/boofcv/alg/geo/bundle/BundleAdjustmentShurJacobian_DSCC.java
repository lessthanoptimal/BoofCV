/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.alg.geo.RodriguesRotationJacobian;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.optimization.functions.SchurJacobian;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.ops.ConvertDMatrixStruct;

/**
 * Computes the Jacobian for {@link boofcv.abst.geo.bundle.BundleAdjustmentShur_DSCC} using sparse matrices
 * in EJML. Parameterization is done using the format in {@link CodecBundleAdjustmentSceneStructure}.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentShurJacobian_DSCC implements SchurJacobian<DMatrixSparseCSC>
{
	private BundleAdjustmentSceneStructure structure;
	private BundleAdjustmentObservations observations;

	// number of views with parameters that are going to be adjusted
	private int numViewsUnknown;

	// total number of parameters being optimized
	private int numParameters;

	// used to compute the Jacobian from Rodrigues coordinates
	private RodriguesRotationJacobian rodJacobian = new RodriguesRotationJacobian();
	private Se3_F64 worldToView = new Se3_F64();

	// local variable which stores the predicted location of the feature in the camera frame
	private Rodrigues_F64 rodrigues = new Rodrigues_F64();
	// feature location in world coordinates
	private Point3D_F64 worldPt = new Point3D_F64();
	// feature location in camera coordinates
	private Point3D_F64 cameraPt = new Point3D_F64();

	// index in parameters of the first point
	private int indexFirstView;
	private int indexLastView;
	// view to parameter index
	private int viewParameterIndexes[];
	// first index in input/parameters vector for each camera
	private int cameraParameterIndexes[];

	// Jacobian matrix index of x and y partial
	private int jacRowX,jacRowY;

	// reference to output Jacobian matrix
	private DMatrixSparseTriplet tripletPoint = new DMatrixSparseTriplet();
	private DMatrixSparseTriplet tripletView = new DMatrixSparseTriplet();

	// Storage for gradients
	private double pointGradX[] = new double[3];
	private double pointGradY[] = new double[3];
	private double calibGradX[] = null;
	private double calibGradY[] = null;

	public void configure( BundleAdjustmentSceneStructure structure , BundleAdjustmentObservations observations ) {
		this.structure = structure;
		this.observations = observations;

		numViewsUnknown = structure.getUnknownViewCount();
		int numCameraParameters = structure.getUnknownCameraParameterCount();

		indexFirstView = structure.points.length*3;
		indexLastView = indexFirstView + numViewsUnknown*6;
		numParameters = indexLastView + numCameraParameters;

		viewParameterIndexes = new int[structure.views.length];
		int index = 0;
		for (int i = 0; i < structure.views.length; i++) {
			viewParameterIndexes[i] = index;
			if( !structure.views[i].known ) {
				index += 6;
			}
		}

		// Create a lookup table for each camera. Camera ID to location in parameter vector
		cameraParameterIndexes = new int[structure.cameras.length];
		index = 0;
		int largestCameraSize = 0;
		for (int i = 0; i < structure.cameras.length; i++) {
			if( !structure.cameras[i].known ) {
				cameraParameterIndexes[i] = index;
				int count = structure.cameras[i].model.getParameterCount();
				largestCameraSize = Math.max(largestCameraSize,count);
				index += count;
			}
		}

		calibGradX = new double[largestCameraSize];
		calibGradY = new double[largestCameraSize];
	}

	@Override
	public int getNumOfInputsN() {
		return numParameters;
	}

	@Override
	public int getNumOfOutputsM() {
		return observations.getObservationCount()*2;
	}

	@Override
	public void process( double[] input, DMatrixSparseCSC left, DMatrixSparseCSC right) {
		int numRows = getNumOfOutputsM();
		int numPointParam = structure.points.length*3;
		int numViewParam = numParameters-numPointParam; // view + camera

		tripletPoint.reshape(numRows,numPointParam);
		tripletView.reshape(numRows,numViewParam);

		int observationIndex = 0;
		// first decode the transformation
		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			BundleAdjustmentSceneStructure.View view = structure.views[viewIndex];
			BundleAdjustmentSceneStructure.Camera camera = structure.cameras[view.camera];

			if( !view.known ) {
				int paramIndex = viewParameterIndexes[viewIndex]+indexFirstView;
				double rodX = input[paramIndex];
				double rodY = input[paramIndex+1];
				double rodZ = input[paramIndex+2];

				worldToView.T.x = input[paramIndex+3];
				worldToView.T.y = input[paramIndex+4];
				worldToView.T.z = input[paramIndex+5];

				rodrigues.setParamVector(rodX,rodY,rodZ);
				rodJacobian.process(rodX,rodY,rodZ);

				ConvertRotation3D_F64.rodriguesToMatrix(rodrigues,worldToView.R);
			} else {
				worldToView.set(view.worldToView);
			}
			int cameraParamStartIndex = cameraParameterIndexes[view.camera];
			if( !camera.known ) {
				camera.model.setParameters(input,indexLastView+cameraParamStartIndex);
			}

			BundleAdjustmentObservations.View obsView = observations.views[viewIndex];

			for (int i = 0; i < obsView.size(); i++) {
				int featureIndex = obsView.feature.get(i);
				int columnOfPointInJac = featureIndex*3;

				worldPt.x = input[columnOfPointInJac];
				worldPt.y = input[columnOfPointInJac+1];
				worldPt.z = input[columnOfPointInJac+2];

				SePointOps_F64.transform(worldToView,worldPt,cameraPt);

				jacRowX = observationIndex*2;
				jacRowY = jacRowX+1;

				//============ Partial of camera parameters
				if( !camera.known ) {
					int N = camera.model.getParameterCount();
					camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z, pointGradX, pointGradY, calibGradX, calibGradY);

					int location = indexLastView-indexFirstView+cameraParamStartIndex;
					for (int j = 0; j < N; j++) {
						tripletView.addItemCheck(jacRowX,location+j,calibGradX[j]);
						tripletView.addItemCheck(jacRowY,location+j,calibGradY[j]);
					}
				} else {
					camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z, pointGradX, pointGradY);
				}
				//============ Partial of worldPt
				// partial of (R*X + T) with respect to X is a 3 by 3 matrix
				// This turns out to be just R
				// grad F(G(X)) = 2 x 3 matrix which is then multiplied by R
				addToJacobian(tripletPoint,columnOfPointInJac,pointGradX,pointGradY,worldToView.R);

				if( !view.known ) {
					int col = viewParameterIndexes[viewIndex];

					//============== Partial of view rotation parameters
					addToJacobian(tripletView, col+0, pointGradX, pointGradY, rodJacobian.Rx,worldPt);
					addToJacobian(tripletView, col+1, pointGradX, pointGradY, rodJacobian.Ry,worldPt);
					addToJacobian(tripletView, col+2, pointGradX, pointGradY, rodJacobian.Rz,worldPt);

					//============== Partial of view translation parameters
					tripletView.addItemCheck(jacRowX,col+3, pointGradX[0]); tripletView.addItem(jacRowY,col+3, pointGradY[0]);
					tripletView.addItemCheck(jacRowX,col+4, pointGradX[1]); tripletView.addItem(jacRowY,col+4, pointGradY[1]);
					tripletView.addItemCheck(jacRowX,col+5, pointGradX[2]); tripletView.addItem(jacRowY,col+5, pointGradY[2]);
				}

				observationIndex++;
			}
		}

		ConvertDMatrixStruct.convert(tripletPoint,left);
		ConvertDMatrixStruct.convert(tripletView,right);

//		left.print();
//		right.print();
//		System.out.println("Asdads");
	}

	/**
	 * J[rows,col:(col+3)] =  [a;b]*R
	 */
	private void addToJacobian(DMatrixSparseTriplet tripplet, int col , double a[], double b[], DMatrixRMaj R ) {
		tripplet.addItem(jacRowX,col+0,a[0]*R.data[0] + a[1]*R.data[3] + a[2]*R.data[6]);
		tripplet.addItem(jacRowX,col+1,a[0]*R.data[1] + a[1]*R.data[4] + a[2]*R.data[7]);
		tripplet.addItem(jacRowX,col+2,a[0]*R.data[2] + a[1]*R.data[5] + a[2]*R.data[8]);

		tripplet.addItem(jacRowY,col+0,b[0]*R.data[0] + b[1]*R.data[3] + b[2]*R.data[6]);
		tripplet.addItem(jacRowY,col+1,b[0]*R.data[1] + b[1]*R.data[4] + b[2]*R.data[7]);
		tripplet.addItem(jacRowY,col+2,b[0]*R.data[2] + b[1]*R.data[5] + b[2]*R.data[8]);
	}

	private void addToJacobian(DMatrixSparseTriplet tripplet, int col , double a[], double b[], DMatrixRMaj R , Point3D_F64 X  ) {

		double x = R.data[0]*X.x + R.data[1]*X.y + R.data[2]*X.z;
		double y = R.data[3]*X.x + R.data[4]*X.y + R.data[5]*X.z;
		double z = R.data[6]*X.x + R.data[7]*X.y + R.data[8]*X.z;

		tripplet.addItem(jacRowX,col,a[0]*x + a[1]*y + a[2]*z);
		tripplet.addItem(jacRowY,col,b[0]*x + b[1]*y + b[2]*z);
	}
}
