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

package boofcv.alg.geo.calibration;

import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class GenericCalibrationGrid extends BoofStandardJUnit {

	public static List<Point2D_F64> standardLayout() {
		return CalibrationDetectorSquareGrid.createLayout(6, 4, 30, 30);
	}

	public static DetectSingleFiducialCalibration createStandardConfig() {
		return FactoryFiducialCalibration.squareGrid(null,new ConfigGridDimen(3, 2, 30, 30));
	}

	public static DMatrixRMaj createStandardCalibration() {
		DMatrixRMaj K = new DMatrixRMaj(3,3);

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

	public static List<Point3D_F64> gridPoints3D( List<Point2D_F64> obs2D )
	{
		List<Point3D_F64> ret = new ArrayList<>();

		for( Point2D_F64 p2 : obs2D ) {
			ret.add(new Point3D_F64(p2.x,p2.y,0));
		}

		return ret;
	}

	public static CalibrationObservation observations( Se3_F64 motion , List<Point2D_F64> obs2D )
	{
		CalibrationObservation ret = new CalibrationObservation();

		for( int i = 0; i < obs2D.size(); i++ ) {
			Point2D_F64 p2 = obs2D.get(i);
			Point3D_F64 p3 = new Point3D_F64(p2.x,p2.y,0);

			Point3D_F64 t = SePointOps_F64.transform(motion,p3,null);

			ret.add( new Point2D_F64(t.x/t.z,t.y/t.z), i);
		}

		return ret;
	}

	public static List<Point2D_F64> observations( DMatrixRMaj H, List<Point2D_F64> obs2D )
	{
		List<Point2D_F64> ret = new ArrayList<>();

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
	public static DMatrixRMaj computeHomography(DMatrixRMaj K, DMatrixRMaj R, Vector3D_F64 T)
	{
		DMatrixRMaj M = new DMatrixRMaj(3,3);
		CommonOps_DDRM.extract(R, 0, 3, 0, 1, M, 0, 0);
		CommonOps_DDRM.extract(R, 0, 3, 1, 2, M, 0, 1);
		M.set(0, 2, T.x);
		M.set(1, 2, T.y);
		M.set(2, 2, T.z);

		DMatrixRMaj H = new DMatrixRMaj(3,3);
		CommonOps_DDRM.mult(K,M,H);

		return H;
	}

	/**
	 * Creates several random uncalibrated homographies
	 * @param K Calibration matrix
	 * @param N Number of homographies
	 */
	public static List<DMatrixRMaj> createHomographies( DMatrixRMaj K , int N , Random rand ) {
		List<DMatrixRMaj> homographies = new ArrayList<>();

		for( int i = 0; i < N; i++ ) {
			Vector3D_F64 T = new Vector3D_F64();
			T.x = rand.nextGaussian()*200;
			T.y = rand.nextGaussian()*200;
			T.z = rand.nextGaussian()*50-1000;

			double rotX = (rand.nextDouble()-0.5)*0.1;
			double rotY = (rand.nextDouble()-0.5)*0.1;
			double rotZ = (rand.nextDouble()-0.5)*0.1;

			DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX, rotY, rotZ, null);

			DMatrixRMaj H = computeHomography(K, R, T);
			homographies.add(H);
		}

		return homographies;
	}
}
