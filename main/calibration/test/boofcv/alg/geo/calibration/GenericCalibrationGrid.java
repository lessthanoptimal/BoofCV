/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class GenericCalibrationGrid {

	public static PlanarCalibrationTarget createStandardConfig() {
		return FactoryPlanarCalibrationTarget.gridSquare(4, 5, 30, 30);
	}

	public static DenseMatrix64F createStandardCalibration() {
		DenseMatrix64F K = new DenseMatrix64F(3,3);

		double c_x = 255;
		double c_y = 260;
		double a = 1250;
		double b = 900;
		double c = 1.09083;

		K.set(0,0,a);
		K.set(0,1,c);
		K.set(0,2,c_x);
		K.set(1,1,b);
		K.set(1,2,c_y);
		K.set(2,2,1);

		return K;
	}

	public static List<Point3D_F64> gridPoints3D( PlanarCalibrationTarget config )
	{
		List<Point3D_F64> ret = new ArrayList<Point3D_F64>();

		List<Point2D_F64> obs2D = config.points;

		for( Point2D_F64 p2 : obs2D ) {
			ret.add(new Point3D_F64(p2.x,p2.y,0));
		}

		return ret;
	}

	public static List<Point2D_F64> observations( Se3_F64 motion , PlanarCalibrationTarget config )
	{
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		List<Point2D_F64> obs2D = config.points;

		for( Point2D_F64 p2 : obs2D ) {
			Point3D_F64 p3 = new Point3D_F64(p2.x,p2.y,0);

			Point3D_F64 t = SePointOps_F64.transform(motion,p3,null);

			ret.add( new Point2D_F64(t.x/t.z,t.y/t.z));
		}

		return ret;
	}

	public static List<Point2D_F64> observations( DenseMatrix64F H, PlanarCalibrationTarget config )
	{
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		List<Point2D_F64> obs2D = config.points;

		for( Point2D_F64 p2 : obs2D ) {
			Point2D_F64 t = new Point2D_F64();

			GeometryMath_F64.mult(H, p2, t);

			ret.add( t );
		}

		return ret;
	}

	/**
	 * Creates a homography as defined in Section 2.2 in Zhang99.
	 *
	 * H = K*[r1 r2 t]
	 */
	public static DenseMatrix64F computeHomography(DenseMatrix64F K, DenseMatrix64F R, Vector3D_F64 T)
	{
		DenseMatrix64F M = new DenseMatrix64F(3,3);
		CommonOps.extract(R, 0, 3, 0, 1, M, 0, 0);
		CommonOps.extract(R, 0, 3, 1, 2, M, 0, 1);
		M.set(0, 2, T.x);
		M.set(1, 2, T.y);
		M.set(2, 2, T.z);

		DenseMatrix64F H = new DenseMatrix64F(3,3);
		CommonOps.mult(K,M,H);

		return H;
	}

	/**
	 * Creates several random uncalibrated homographies
	 * @param K Calibration matrix
	 * @param N Number of homographies
	 */
	public static List<DenseMatrix64F> createHomographies( DenseMatrix64F K , int N , Random rand ) {
		List<DenseMatrix64F> homographies = new ArrayList<DenseMatrix64F>();

		for( int i = 0; i < N; i++ ) {
			Vector3D_F64 T = new Vector3D_F64();
			T.x = rand.nextGaussian()*200;
			T.y = rand.nextGaussian()*200;
			T.z = rand.nextGaussian()*50-1000;

			double rotX = (rand.nextDouble()-0.5)*0.1;
			double rotY = (rand.nextDouble()-0.5)*0.1;
			double rotZ = (rand.nextDouble()-0.5)*0.1;

			DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(rotX, rotY, rotZ, null);

			DenseMatrix64F H = computeHomography(K, R, T);
			homographies.add(H);
		}

		return homographies;
	}

	static Zhang99Parameters createStandardParam(boolean zeroSkew, int numSkew, int numView, Random rand) {
		Zhang99Parameters ret = new Zhang99Parameters(zeroSkew,numSkew,numView);

		DenseMatrix64F K = createStandardCalibration();
		ret.a = K.get(0,0);
		ret.b = K.get(1,1);
		ret.c = K.get(0,1);
		ret.x0 = K.get(0,2);
		ret.y0 = K.get(1,2);
		if( zeroSkew ) ret.c = 0;

		ret.distortion = new double[numSkew];
		for( int i = 0; i < numSkew;i++ ) {
			ret.distortion[i] = rand.nextGaussian()*0.001;
		}

		for(Zhang99Parameters.View v : ret.views ) {
			double rotX = (rand.nextDouble()-0.5)*0.1;
			double rotY = (rand.nextDouble()-0.5)*0.1;
			double rotZ = (rand.nextDouble()-0.5)*0.1;
			DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(rotX,rotY,rotZ,null);
			RotationMatrixGenerator.matrixToRodrigues(R,v.rotation);

			double x = rand.nextGaussian()*5;
			double y = rand.nextGaussian()*5;
			double z = rand.nextGaussian()*5-750;

			v.T.set(x,y,z);
		}
		return ret;
	}

	/**
	 * Creates a set of observed points in pixel coordinates given zhang parameters and a calibration
	 * grid.
	 */
	public static List<List<Point2D_F64>> createObservations( Zhang99Parameters config,
															  List<Point2D_F64> grid)
	{
		List<List<Point2D_F64>> ret = new ArrayList<List<Point2D_F64>>();

		Point3D_F64 cameraPt = new Point3D_F64();
		Point2D_F64 calibratedPt = new Point2D_F64();

		for( Zhang99Parameters.View v : config.views ) {
			List<Point2D_F64> obs = new ArrayList<Point2D_F64>();
			Se3_F64 se = new Se3_F64();
			RotationMatrixGenerator.rodriguesToMatrix(v.rotation,se.getR());
			se.T = v.T;

			for( Point2D_F64 grid2D : grid ) {
				Point3D_F64 grid3D = new Point3D_F64(grid2D.x,grid2D.y,0);

				// Put the point in the camera's reference frame
				SePointOps_F64.transform(se,grid3D, cameraPt);

				// calibrated pixel coordinates
				calibratedPt.x = cameraPt.x/ cameraPt.z;
				calibratedPt.y = cameraPt.y/ cameraPt.z;

				// apply radial distortion
				CalibrationPlanarGridZhang99.applyDistortion(calibratedPt, config.distortion);

				// convert to pixel coordinates
				double x = config.a*calibratedPt.x + config.c*calibratedPt.y + config.x0;
				double y = config.b*calibratedPt.y + config.y0;

				obs.add(new Point2D_F64(x,y));
			}
			ret.add(obs);
		}
		return ret;
	}
}
