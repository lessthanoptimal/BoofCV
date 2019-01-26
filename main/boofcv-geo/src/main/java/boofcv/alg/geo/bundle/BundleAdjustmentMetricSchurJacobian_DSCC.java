/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustmentSchur_DSCC;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.jacobians.JacobianSo3Rodrigues;
import boofcv.alg.geo.bundle.jacobians.JacobianSo3_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.ConvertDMatrixStruct;

/**
 * Computes the Jacobian for {@link BundleAdjustmentSchur_DSCC} using sparse matrices
 * in EJML. Parameterization is done using the format in {@link CodecSceneStructureMetric}.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentMetricSchurJacobian_DSCC
		implements BundleAdjustmentSchur_DSCC.Jacobian<SceneStructureMetric>
{
	private SceneStructureMetric structure;
	private SceneObservations observations;

	// number of views with parameters that are going to be adjusted
	private int numViewsUnknown;
	private int numRigidUnknown;

	// total number of parameters being optimized
	private int numParameters;

	// length of a 3D point. 3 = regular, 4 = homogenous
	private int lengthPoint;

	// used to compute the Jacobian of a rotation matrix
	private JacobianSo3_F64 jacSO3 = new JacobianSo3Rodrigues();
	private Se3_F64 worldToView = new Se3_F64();

	// jacobians for rigid objects
	private JacobianSo3_F64[] jacRigidS03;

	// feature location in world coordinates
	private Point3D_F64 worldPt3 = new Point3D_F64();
	private Point4D_F64 worldPt4 = new Point4D_F64();
	// feature location in rigid body coordinates
	private Point3D_F64 rigidPt3 = new Point3D_F64();
	private Point4D_F64 rigidPt4 = new Point4D_F64();
	// feature location in camera coordinates
	private Point3D_F64 cameraPt = new Point3D_F64();

	// Number of parameters to describe SE3 (rotation + translation)
	private int lengthSE3;
	// first index for rigid body parameters
	private int indexFirstRigid;
	// index in parameters of the first point
	private int indexFirstView;
	private int indexLastView;
	// rigid to parameter index. Left side
	private int rigidParameterIndexes[];
	// view to parameter index. Right side
	private int viewParameterIndexes[];
	// first index in input/parameters vector for each camera. Right side
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

	// work space for R2*R1
	DMatrixRMaj RR = new DMatrixRMaj(3,3);

	@Override
	public void configure(SceneStructureMetric structure , SceneObservations observations ) {
		this.structure = structure;
		this.observations = observations;

		if( !structure.isHomogenous() ) {
			lengthPoint = 3;
		} else {
			lengthPoint = 4;
		}

		// 3 for translation + orientation parameterization
		lengthSE3 = 3+jacSO3.getParameterLength();

		//----- Pre-Compute location of parameters for different structures
		numRigidUnknown = structure.getUnknownRigidCount();
		numViewsUnknown = structure.getUnknownViewCount();
		int numCameraParameters = structure.getUnknownCameraParameterCount();

		indexFirstRigid = structure.points.length*lengthPoint;
		indexFirstView = indexFirstRigid + numRigidUnknown*lengthSE3;
		indexLastView = indexFirstView + numViewsUnknown*lengthSE3;
		numParameters = indexLastView + numCameraParameters;

		// pre-compute index of first parameter in each unknown rigid object. Left side
		jacRigidS03 = new JacobianSo3_F64[structure.rigids.length];
		rigidParameterIndexes = new int[ jacRigidS03.length ];
		for (int i = 0, index = 0; i < jacRigidS03.length; i++) {
			rigidParameterIndexes[i] = index;
			jacRigidS03[i] = new JacobianSo3Rodrigues();
			if(!structure.rigids[i].known ) {
				index += lengthSE3;
			}
		}

		// pre-compute index of first parameter for each unknown view. Right side
		viewParameterIndexes = new int[structure.views.length];
		for (int i = 0, index = 0; i < structure.views.length; i++) {
			viewParameterIndexes[i] = index;
			if( !structure.views[i].known ) {
				index += lengthSE3;
			}
		}

		// Create a lookup table for each camera. Camera ID to location in parameter vector
		cameraParameterIndexes = new int[structure.cameras.length];
		int largestCameraSize = 0;
		for (int i = 0, index = 0; i < structure.cameras.length; i++) {
			if( !structure.cameras[i].known ) {
				cameraParameterIndexes[i] = index;
				int count = structure.cameras[i].model.getIntrinsicCount();
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

	/**
	 * All parameters related to estimating the location of points goes on left hand side. This includes
	 * general 3D points and rigid body parameters. Everything else on the right hand side.
	 *
	 * @param input
	 * @param left
	 * @param right
	 */
	@Override
	public void process( double[] input, DMatrixSparseCSC left, DMatrixSparseCSC right) {
		int numRows = getNumOfOutputsM();
		// number of parameters on left. All points
		int numPointParam = structure.points.length*lengthPoint + numRigidUnknown*lengthSE3;
		// Number of paramters on right. views + camera
		int numViewParam = numParameters-numPointParam; // view + camera

		tripletPoint.reshape(numRows,numPointParam);
		tripletView.reshape(numRows,numViewParam);

		// parse parameters for rigid bodies. the translation + rotation is the same for all views
		for (int rigidIndex = 0; rigidIndex < structure.rigids.length; rigidIndex++) {
			if( !structure.rigids[rigidIndex].known ) {
				jacRigidS03[rigidIndex].setParameters(input,indexFirstRigid+rigidParameterIndexes[rigidIndex]);
			}
		}

		int observationIndex = 0;
		// first decode the transformation
		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			SceneStructureMetric.View view = structure.views[viewIndex];
			SceneStructureMetric.Camera camera = structure.cameras[view.camera];

			if( !view.known ) {
				int paramIndex = viewParameterIndexes[viewIndex]+indexFirstView;
				jacSO3.setParameters(input,paramIndex);
				paramIndex += jacSO3.getParameterLength();

				worldToView.T.x = input[paramIndex];
				worldToView.T.y = input[paramIndex+1];
				worldToView.T.z = input[paramIndex+2];

				worldToView.getR().set(jacSO3.getRotationMatrix());
			} else {
				worldToView.set(view.worldToView);
			}
			int cameraParamStartIndex = cameraParameterIndexes[view.camera];
			if( !camera.known ) {
				camera.model.setIntrinsic(input,indexLastView+cameraParamStartIndex);
			}

			observationIndex = computeGeneralPoints(input, observationIndex, viewIndex, view, camera, cameraParamStartIndex);
			if( observations.viewsRigid != null )
				observationIndex = computeRigidPoints(observationIndex, viewIndex, view, camera, cameraParamStartIndex);
		}

		ConvertDMatrixStruct.convert(tripletPoint,left);
		ConvertDMatrixStruct.convert(tripletView,right);
	}

	private int computeGeneralPoints(double[] input, int observationIndex, int viewIndex, SceneStructureMetric.View view, SceneStructureMetric.Camera camera, int cameraParamStartIndex) {
		SceneObservations.View obsView = observations.views[viewIndex];

		for (int i = 0; i < obsView.size(); i++) {
			int featureIndex = obsView.point.get(i);
			int columnOfPointInJac = featureIndex*lengthPoint;

			if( structure.isHomogenous() ) {
				worldPt4.x = input[columnOfPointInJac];
				worldPt4.y = input[columnOfPointInJac + 1];
				worldPt4.z = input[columnOfPointInJac + 2];
				worldPt4.w = input[columnOfPointInJac + 3];

				SePointOps_F64.transform(worldToView, worldPt4, cameraPt);
			} else {
				worldPt3.x = input[columnOfPointInJac];
				worldPt3.y = input[columnOfPointInJac + 1];
				worldPt3.z = input[columnOfPointInJac + 2];

				SePointOps_F64.transform(worldToView, worldPt3, cameraPt);
			}

			jacRowX = observationIndex*2;
			jacRowY = jacRowX+1;

			//============ Partial of camera parameters
			if( !camera.known ) {
				int N = camera.model.getIntrinsicCount();
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z,
						pointGradX, pointGradY, true, calibGradX, calibGradY);

				int location = indexLastView-indexFirstView+cameraParamStartIndex;
				for (int j = 0; j < N; j++) {
					tripletView.addItemCheck(jacRowX,location+j,calibGradX[j]);
					tripletView.addItemCheck(jacRowY,location+j,calibGradY[j]);
				}
			} else {
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z, pointGradX, pointGradY,
						false, null, null);
			}
			//============ Partial of worldPt
			if( structure.isHomogenous() ) {
				partialPointH(viewIndex, view, columnOfPointInJac);
			} else {
				partialPoint3(viewIndex, view, columnOfPointInJac);
			}

			observationIndex++;
		}
		return observationIndex;
	}

	private int computeRigidPoints(int observationIndex, int viewIndex,
								   SceneStructureMetric.View view,
								   SceneStructureMetric.Camera camera,
								   int cameraParamStartIndex)
	{
		SceneObservations.View obsView = observations.viewsRigid[viewIndex];

		for (int i = 0; i < obsView.size(); i++) {
			int featureIndex = obsView.point.get(i);
			int rigidIndex = structure.lookupRigid[featureIndex];
			SceneStructureMetric.Rigid rigid = structure.rigids[rigidIndex];
			int pointIndex = featureIndex-rigid.indexFirst; // index of point in rigid body

			if( structure.isHomogenous() ) {
				rigid.getPoint(pointIndex,rigidPt4);
				SePointOps_F64.transform(rigid.objectToWorld, rigidPt4, worldPt3);
			} else {
				rigid.getPoint(pointIndex,rigidPt3);
				SePointOps_F64.transform(rigid.objectToWorld, rigidPt3, worldPt3);
			}
			SePointOps_F64.transform(worldToView, worldPt3, cameraPt);

			jacRowX = observationIndex*2;
			jacRowY = jacRowX+1;

			//============ Partial of camera parameters
			if( !camera.known ) {
				int N = camera.model.getIntrinsicCount();
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z,
						pointGradX, pointGradY, true, calibGradX, calibGradY);

				int location = indexLastView-indexFirstView+cameraParamStartIndex;
				for (int j = 0; j < N; j++) {
					tripletView.addItemCheck(jacRowX,location+j,calibGradX[j]);
					tripletView.addItemCheck(jacRowY,location+j,calibGradY[j]);
				}
			} else {
				camera.model.jacobian(cameraPt.x, cameraPt.y, cameraPt.z, pointGradX, pointGradY,
						false, null, null);
			}

			//============ Partial of world to view
			if( !view.known ) {
				partialViewSE3(viewIndex, view, worldPt3.x, worldPt3.y, worldPt3.z, 1);
			}

			//============ Partial of body to world
			// R2*(R1*X+T1)+T2
			// [R1|T1] = object to world. X = fixed point in rigid body
			// [R2|T2] = world to view
			// partial R1 is R2*(@R1*X)
			// partial T1 is R2*(@T1)
			if( !rigid.known ) {
				if( structure.isHomogenous() ) {
					partialRigidSE3(rigidIndex, rigidPt4.x,rigidPt4.y,rigidPt4.z,rigidPt4.w);
				} else {
					partialRigidSE3(rigidIndex, rigidPt3.x,rigidPt3.y,rigidPt3.z,1);
				}
			}

			observationIndex++;
		}
		return observationIndex;
	}


	private void partialPoint3(int viewIndex, SceneStructureMetric.View view, int columnOfPointInJac) {
		// partial of (R*X + T) with respect to X is a 3 by 3 matrix
		// This turns out to be just R
		// grad F(G(X)) = 2 x 3 matrix which is then multiplied by R
		addToJacobian(tripletPoint,columnOfPointInJac,pointGradX,pointGradY,worldToView.R);

		partialViewSE3(viewIndex, view, worldPt3.x, worldPt3.y, worldPt3.z, 1);
	}

	private void partialPointH(int viewIndex, SceneStructureMetric.View view, int columnOfPointInJac) {
		// partial of (R*[x,y,z]' + T*w) with respect to X=[x,y,z,w] is a 3 by 4 matrix, [R|T]
		//
		// grad F(G(X)) = 2 x 4 matrix which is then multiplied by R
		addToJacobian(tripletPoint,columnOfPointInJac,pointGradX,pointGradY,worldToView.R);
		addToJacobian(tripletPoint,columnOfPointInJac+3,pointGradX,pointGradY,worldToView.T);

		partialViewSE3(viewIndex, view, worldPt4.x, worldPt4.y, worldPt4.z, worldPt4.w);
	}

	private void partialViewSE3(int viewIndex, SceneStructureMetric.View view,
								double X, double Y, double Z , double W) {
		if( !view.known ) {
			int col = viewParameterIndexes[viewIndex];

			//============== Partial of view rotation parameters
			final int N = jacSO3.getParameterLength();
			for (int i = 0; i < jacSO3.getParameterLength(); i++) {
				addToJacobian(tripletView, col+i, pointGradX, pointGradY, jacSO3.getPartial(i), X,Y,Z);
			}

			//============== Partial of view translation parameters
			tripletView.addItemCheck(jacRowX,col+N  , pointGradX[0]*W); tripletView.addItem(jacRowY,col+N  , pointGradY[0]*W);
			tripletView.addItemCheck(jacRowX,col+N+1, pointGradX[1]*W); tripletView.addItem(jacRowY,col+N+1, pointGradY[1]*W);
			tripletView.addItemCheck(jacRowX,col+N+2, pointGradX[2]*W); tripletView.addItem(jacRowY,col+N+2, pointGradY[2]*W);
		}
	}

	private void partialRigidSE3(int rigidIndex,
								double X, double Y, double Z , double W) {
		int col = rigidParameterIndexes[rigidIndex]+indexFirstRigid;

		JacobianSo3_F64 jac = jacRigidS03[rigidIndex];

		//============== Partial of view rotation parameters
		final int N = jac.getParameterLength();
		for (int i = 0; i < N; i++) {
			CommonOps_DDRM.mult(worldToView.R,jac.getPartial(i),RR);
			addToJacobian(tripletPoint, col+i, pointGradX, pointGradY, RR, X,Y,Z);
		}

		//============== Partial of view translation parameters
		// Apply rotation matrix to gradX and gradY.
		// RX = gradX'*R
		double RX0 = worldToView.R.data[0]*pointGradX[0] + worldToView.R.data[3]*pointGradX[1] + worldToView.R.data[6]*pointGradX[2];
		double RX1 = worldToView.R.data[1]*pointGradX[0] + worldToView.R.data[4]*pointGradX[1] + worldToView.R.data[7]*pointGradX[2];
		double RX2 = worldToView.R.data[2]*pointGradX[0] + worldToView.R.data[5]*pointGradX[1] + worldToView.R.data[8]*pointGradX[2];
		// RY = gradY'*R
		double RY0 = worldToView.R.data[0]*pointGradY[0] + worldToView.R.data[3]*pointGradY[1] + worldToView.R.data[6]*pointGradY[2];
		double RY1 = worldToView.R.data[1]*pointGradY[0] + worldToView.R.data[4]*pointGradY[1] + worldToView.R.data[7]*pointGradY[2];
		double RY2 = worldToView.R.data[2]*pointGradY[0] + worldToView.R.data[5]*pointGradY[1] + worldToView.R.data[8]*pointGradY[2];

		tripletPoint.addItemCheck(jacRowX,col+N  , RX0*W); tripletPoint.addItem(jacRowY,col+N  , RY0*W);
		tripletPoint.addItemCheck(jacRowX,col+N+1, RX1*W); tripletPoint.addItem(jacRowY,col+N+1, RY1*W);
		tripletPoint.addItemCheck(jacRowX,col+N+2, RX2*W); tripletPoint.addItem(jacRowY,col+N+2, RY2*W);
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

	private void addToJacobian(DMatrixSparseTriplet tripplet, int col , double a[], double b[],
							   DMatrixRMaj R , double X, double Y, double Z  ) {

		double x = R.data[0]*X + R.data[1]*Y + R.data[2]*Z;
		double y = R.data[3]*X + R.data[4]*Y + R.data[5]*Z;
		double z = R.data[6]*X + R.data[7]*Y + R.data[8]*Z;

		tripplet.addItem(jacRowX,col,a[0]*x + a[1]*y + a[2]*z);
		tripplet.addItem(jacRowY,col,b[0]*x + b[1]*y + b[2]*z);
	}


	private void addToJacobian(DMatrixSparseTriplet tripplet, int col , double a[], double b[], Vector3D_F64 X  ) {
		tripplet.addItem(jacRowX,col,a[0]*X.x + a[1]*X.y + a[2]*X.z);
		tripplet.addItem(jacRowY,col,b[0]*X.x + b[1]*X.y + b[2]*X.z);
	}
}
