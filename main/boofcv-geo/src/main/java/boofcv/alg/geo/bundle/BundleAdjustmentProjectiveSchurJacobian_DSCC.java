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
import boofcv.abst.geo.bundle.BundleAdjustmentSchur_DSCC;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.ops.ConvertDMatrixStruct;

import javax.annotation.Nullable;

/**
 * Computes the Jacobian for {@link BundleAdjustmentSchur_DSCC} using sparse matrices
 * in EJML. Parameterization is done using the format in {@link CodecSceneStructureProjective}.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentProjectiveSchurJacobian_DSCC
		implements BundleAdjustmentSchur_DSCC.Jacobian<SceneStructureProjective>
{
	private SceneStructureProjective structure;
	private BundleAdjustmentObservations observations;

	// work space for jacobian
	private DMatrixRMaj worldToView = new DMatrixRMaj(3,4);

	// number of views with parameters that are going to be adjusted
	private int numViewsUnknown;

	// total number of parameters being optimized
	private int numParameters;

	// length of a 3D point. 3 = regular, 4 = homogenous
	private int lengthPoint;

	// feature location in world coordinates
	private Point4D_F64 worldPt = new Point4D_F64();
	// feature location in camera coordinates
	private Point3D_F64 cameraPt = new Point3D_F64();

	// index in parameters of the first point
	private int indexFirstView;
	// view to parameter index
	private int viewParameterIndexes[];

	// Jacobian matrix index of x and y partial
	private int jacRowX,jacRowY;

	// reference to output Jacobian matrix
	private DMatrixSparseTriplet tripletPoint = new DMatrixSparseTriplet();
	private DMatrixSparseTriplet tripletView = new DMatrixSparseTriplet();

	// Storage for gradients
	private double pointGradX[] = new double[4];
	private double pointGradY[] = new double[4];
	private double camGradX[] = new double[12];
	private double camGradY[] = new double[12];

	@Override
	public void configure(SceneStructureProjective structure , BundleAdjustmentObservations observations ) {
		this.structure = structure;
		this.observations = observations;

		if( !structure.isHomogenous() ) {
			worldPt.w = 1;
			lengthPoint = 3;
		} else {
			lengthPoint = 4;
		}

		numViewsUnknown = structure.getUnknownViewCount();

		indexFirstView = structure.points.length*lengthPoint;
		numParameters = indexFirstView + numViewsUnknown*12;

		viewParameterIndexes = new int[structure.views.length];
		int index = 0;
		for (int i = 0; i < structure.views.length; i++) {
			viewParameterIndexes[i] = index;
			if( !structure.views[i].known ) {
				index += 12;
			}
		}
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
		int numPointParam = structure.points.length*lengthPoint;
		int numViewParam = numParameters-numPointParam; // view + camera

		tripletPoint.reshape(numRows,numPointParam);
		tripletView.reshape(numRows,numViewParam);

		int observationIndex = 0;
		// first decode the transformation
		for( int viewIndex = 0; viewIndex < structure.views.length; viewIndex++ ) {
			SceneStructureProjective.View view = structure.views[viewIndex];

			if( !view.known ) {
				int paramIndex = viewParameterIndexes[viewIndex]+indexFirstView;
				for (int i = 0; i < 12; i++) {
					worldToView.data[i] = input[paramIndex++];
				}
			} else {
				worldToView.set(view.worldToView);
			}

			BundleAdjustmentObservations.View obsView = observations.views[viewIndex];

			for (int i = 0; i < obsView.size(); i++) {
				int featureIndex = obsView.point.get(i);
				int columnOfPointInJac = featureIndex*lengthPoint;

				worldPt.x = input[columnOfPointInJac];
				worldPt.y = input[columnOfPointInJac+1];
				worldPt.z = input[columnOfPointInJac+2];
				if( structure.isHomogenous() ) {
					worldPt.w = input[columnOfPointInJac+3];
				}

				PerspectiveOps.renderPixel(worldToView,worldPt,cameraPt);


				if (view.known) {
					if( structure.isHomogenous())
						partialCameraModelH(worldPt.x, worldPt.y, worldPt.z, worldPt.w,
								worldToView, pointGradX, pointGradY, null, null);
					else
						partialCameraModel(worldPt.x, worldPt.y, worldPt.z,
								worldToView, pointGradX, pointGradY, null, null);
				} else {
					if( structure.isHomogenous())
						partialCameraModelH(worldPt.x, worldPt.y, worldPt.z, worldPt.w,
								worldToView, pointGradX, pointGradY, camGradX, camGradY);
					else
						partialCameraModel(worldPt.x, worldPt.y, worldPt.z,
								worldToView, pointGradX, pointGradY, camGradX, camGradY);
				}

				jacRowX = observationIndex*2;
				jacRowY = jacRowX+1;

				//============ Partial of worldPt
				// partial of x' = (1/z)*P*X with respect to X is a 2 by 3|4 matrix
				addToJacobian(tripletPoint,columnOfPointInJac,lengthPoint,pointGradX,pointGradY);

				if( !view.known ) {
					// partial of x' = (1/z)*P*X with respect to P is a 2 by 12 matrix
					int col = viewParameterIndexes[viewIndex];
					addToJacobian(tripletView,col,12,camGradX,camGradY);
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

	static void partialCameraModel(double X , double Y , double Z ,
								   DMatrixRMaj P ,
								   double pixelGradX[], double pixelGradY[] ,
								   @Nullable double camGradX[], @Nullable double camGradY[] )
	{
		double P11 = P.data[0], P12 = P.data[1], P13 = P.data[2 ], P14 = P.data[3];
		double P21 = P.data[4], P22 = P.data[5], P23 = P.data[6 ], P24 = P.data[7];
		double P31 = P.data[8], P32 = P.data[9], P33 = P.data[10], P34 = P.data[11];

		double xx = P11*X +P12*Y + P13*Z + P14;
		double yy = P21*X +P22*Y + P23*Z + P24;
		double zz = P31*X +P32*Y + P33*Z + P34;
		double zz2 = zz*zz;

		// (1/z)*P*d(X) + d(1/z)*P*X
		pixelGradX[0] = P11/zz - P31*xx/zz2;
		pixelGradX[1] = P12/zz - P32*xx/zz2;
		pixelGradX[2] = P13/zz - P33*xx/zz2;

		pixelGradY[0] = P21/zz - P31*yy/zz2;
		pixelGradY[1] = P22/zz - P32*yy/zz2;
		pixelGradY[2] = P23/zz - P33*yy/zz2;

		if( camGradX == null || camGradY == null )
			return;

		// (1/z)*D(P)*X + D(1/z)*P*X
		camGradX[0] = X/zz;      camGradX[1] = Y/zz;      camGradX[2 ] = Z/zz;      camGradX[3 ] = 1/zz;
		camGradX[4] = 0;         camGradX[5] = 0;         camGradX[6 ] = 0;         camGradX[7 ] = 0;
		camGradX[8] = -X*xx/zz2; camGradX[9] = -Y*xx/zz2; camGradX[10] = -Z*xx/zz2; camGradX[11] = -xx/zz2;

		camGradY[0] = 0;         camGradY[1] = 0;         camGradY[2 ] = 0;         camGradY[3 ] = 0;
		camGradY[4] = X/zz;      camGradY[5] = Y/zz;      camGradY[6 ] = Z/zz;      camGradY[7 ] = 1/zz;
		camGradY[8] = -X*yy/zz2; camGradY[9] = -Y*yy/zz2; camGradY[10] = -Z*yy/zz2; camGradY[11] = -yy/zz2;
	}

	static void partialCameraModelH(double X , double Y , double Z , double W,
									DMatrixRMaj P ,
									double pixelGradX[], double pixelGradY[] ,
									@Nullable double camGradX[], @Nullable double camGradY[] )
	{
		double P11 = P.data[0], P12 = P.data[1], P13 = P.data[2 ], P14 = P.data[3];
		double P21 = P.data[4], P22 = P.data[5], P23 = P.data[6 ], P24 = P.data[7];
		double P31 = P.data[8], P32 = P.data[9], P33 = P.data[10], P34 = P.data[11];

		double xx = P11*X +P12*Y + P13*Z + P14*W;
		double yy = P21*X +P22*Y + P23*Z + P24*W;
		double zz = P31*X +P32*Y + P33*Z + P34*W;
		double zz2 = zz*zz;

		// (1/z)*P*d(X) + d(1/z)*P*X
		pixelGradX[0] = P11/zz - P31*xx/zz2;
		pixelGradX[1] = P12/zz - P32*xx/zz2;
		pixelGradX[2] = P13/zz - P33*xx/zz2;
		pixelGradX[3] = P14/zz - P34*xx/zz2;

		pixelGradY[0] = P21/zz - P31*yy/zz2;
		pixelGradY[1] = P22/zz - P32*yy/zz2;
		pixelGradY[2] = P23/zz - P33*yy/zz2;
		pixelGradY[3] = P24/zz - P34*yy/zz2;

		if( camGradX == null || camGradY == null )
			return;

		// (1/z)*D(P)*X + D(1/z)*P*X
		camGradX[0] = X/zz;      camGradX[1] = Y/zz;      camGradX[2 ] = Z/zz;      camGradX[3 ] = W/zz;
		camGradX[4] = 0;         camGradX[5] = 0;         camGradX[6 ] = 0;         camGradX[7 ] = 0;
		camGradX[8] = -X*xx/zz2; camGradX[9] = -Y*xx/zz2; camGradX[10] = -Z*xx/zz2; camGradX[11] = -W*xx/zz2;

		camGradY[0] = 0;         camGradY[1] = 0;         camGradY[2 ] = 0;         camGradY[3 ] = 0;
		camGradY[4] = X/zz;      camGradY[5] = Y/zz;      camGradY[6 ] = Z/zz;      camGradY[7 ] = W/zz;
		camGradY[8] = -X*yy/zz2; camGradY[9] = -Y*yy/zz2; camGradY[10] = -Z*yy/zz2; camGradY[11] = -W*yy/zz2;
	}

	private void addToJacobian(DMatrixSparseTriplet tripplet, int col , int length, double a[], double b[]) {
		for (int i = 0; i < length; i++) {
			tripplet.addItem(jacRowX,col+i,a[i]);
			tripplet.addItem(jacRowY,col+i,b[i]);
		}
	}

}
