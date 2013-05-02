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

package boofcv.abst.calib;

import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.alg.geo.calibration.Zhang99Parameters;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Given a sequence of observations from a stereo camera compute the intrinsic calibration
 * of each camera and the extrinsic calibration between the two cameras.  A Planar calibration
 * grid is used, which must be completely visible in all images.
 * </p>
 *
 * <p>
 * Calibration is performed by first independently determining the intrinsic parameters of each camera as well as
 * their extrinsic parameters relative to the calibration grid.  Then the extrinsic parameters between the two cameras
 * is found by creating two point clouds composed of the calibration points in each camera's view.  Then the rigid
 * body motion is found which transforms one point cloud into the other.
 * </p>
 *
 * <p>
 * See comments in {@link CalibrateMonoPlanar} about when the y-axis should be inverted.
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibrateStereoPlanar {

	// transform from world to camera in each view
	List<Se3_F64> viewLeft = new ArrayList<Se3_F64>();
	List<Se3_F64> viewRight = new ArrayList<Se3_F64>();

	// calibrates the left and right camera image
	CalibrateMonoPlanar calibLeft;
	CalibrateMonoPlanar calibRight;

	/**
	 * Configures stereo calibration
	 *
	 * @param detector Target detection algorithm.
	 * @param flipY If true the y-axis will be inverted to ensure the assumed coordinate system is being used.
	 *                Most of the time this will be false.
	 */
	public CalibrateStereoPlanar(PlanarCalibrationDetector detector, boolean flipY)
	{
		calibLeft = new CalibrateMonoPlanar(detector,flipY);
		calibRight = new CalibrateMonoPlanar(detector,flipY);
	}

	/**
	 * Puts the class into its initial state.
	 */
	public void reset() {
		viewLeft.clear();
		viewRight.clear();
		calibLeft.reset();
		calibRight.reset();
	}

	/**
	 * Specify calibration assumptions.
	 *
	 * @param target Describes the calibration target.
	 * @param assumeZeroSkew If true zero skew is assumed.
	 * @param numRadialParam Number of radial parameters
	 */
	public void configure( PlanarCalibrationTarget target ,
						   boolean assumeZeroSkew ,
						   int numRadialParam )
	{
		calibLeft.configure(target,assumeZeroSkew,numRadialParam);
		calibRight.configure(target,assumeZeroSkew,numRadialParam);
	}

	/**
	 * Adds a pair of images that observed the same target.
	 *
	 * @param left Image of left target.
	 * @param right Image of right target.
	 */
	public boolean addPair( ImageFloat32 left , ImageFloat32 right ) {

		if( !calibLeft.addImage(left) )
			return false;

		if( !calibRight.addImage(right ) ) {
			calibLeft.removeLatestImage();
			return false;
		}
		return true;
	}

	/**
	 * Compute stereo calibration parameters
	 *
	 * @return Stereo calibration parameters
	 */
	public StereoParameters process() {

		// calibrate left and right cameras
		IntrinsicParameters leftParam = calibrateMono(calibLeft,viewLeft);
		IntrinsicParameters rightParam = calibrateMono(calibRight,viewRight);

		// fit motion from right to left
		Se3_F64 rightToLeft = computeRightToLeft();

		return new StereoParameters(leftParam,rightParam,rightToLeft);
	}

	/**
	 * Compute intrinsic calibration for one of the cameras
	 */
	private IntrinsicParameters calibrateMono( CalibrateMonoPlanar calib , List<Se3_F64> location )
	{
		IntrinsicParameters intrinsic = calib.process();

		Zhang99Parameters zhangParam = calib.getZhangParam();

		for( Zhang99Parameters.View v : zhangParam.views ) {
			Se3_F64 pose = new Se3_F64();
			RotationMatrixGenerator.rodriguesToMatrix(v.rotation,pose.getR());
			pose.getT().set(v.T);
			location.add(pose);
		}

		return intrinsic;
	}

	/**
	 * Creates two 3D point clouds for the left and right camera using the known calibration points and camera
	 * calibration.  Then find the optimal rigid body transform going from the right to left views.
	 *
	 * @return Transform from right to left view.
	 */
	private Se3_F64 computeRightToLeft() {
		// location of points in the world coordinate system
		List<Point2D_F64> points2D = calibLeft.getTarget().points;
		List<Point3D_F64> points3D = new ArrayList<Point3D_F64>();

		for( Point2D_F64 p : points2D ) {
			points3D.add( new Point3D_F64(p.x,p.y,0));
		}

		// create point cloud in each view
		List<Point3D_F64> left = new ArrayList<Point3D_F64>();
		List<Point3D_F64> right = new ArrayList<Point3D_F64>();

		for( int i = 0; i < viewLeft.size(); i++ ) {
			Se3_F64 worldToLeft = viewLeft.get(i);
			Se3_F64 worldToRight = viewRight.get(i);

			// These points can really be arbitrary and don't have to be target points
			for( Point3D_F64 p : points3D ) {
				Point3D_F64 l = SePointOps_F64.transform(worldToLeft, p, null);
				Point3D_F64 r = SePointOps_F64.transform(worldToRight, p, null);

				left.add(l);
				right.add(r);
			}
		}

		// find the transform from right to left cameras
		return FitSpecialEuclideanOps_F64.fitPoints3D(right,left);
	}

	public CalibrateMonoPlanar getCalibLeft() {
		return calibLeft;
	}

	public CalibrateMonoPlanar getCalibRight() {
		return calibRight;
	}

	public boolean isConvertToRightHanded() {
		return calibLeft.isFlipY();
	}

	public void printStatistics() {
		System.out.println("********** LEFT ************");
		calibLeft.printStatistics();
		System.out.println("********** RIGHT ************");
		calibRight.printStatistics();
	}
}
