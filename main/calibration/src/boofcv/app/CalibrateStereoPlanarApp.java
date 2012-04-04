/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.app;

import boofcv.alg.geo.calibration.FactoryPlanarCalibrationTarget;
import boofcv.alg.geo.calibration.ParametersZhang99;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.fitting.se.FitSpecialEuclideanOps_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Given a sequence of observations from a stereo camera compute the intrinsic calibration
 * of each camera and the extrinsic calibration between the two cameras.  A Planar calibration
 * grid is used, which must be completely visible in all images.
 *
 * @author Peter Abeles
 */
// todo handle a bad image gracefully
public class CalibrateStereoPlanarApp {

	// images for left and right cameras
	List<ImageFloat32> listLeft = new ArrayList<ImageFloat32>();
	List<ImageFloat32> listRight = new ArrayList<ImageFloat32>();

	// location of the left in left and right images
	List<Se3_F64> viewLeft = new ArrayList<Se3_F64>();
	List<Se3_F64> viewRight = new ArrayList<Se3_F64>();

	CalibrateMonoPlanar monoCalib;


	public CalibrateStereoPlanarApp( PlanarCalibrationDetector detector )
	{
		monoCalib = new CalibrateMonoPlanar(detector);
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
		monoCalib.configure(target,assumeZeroSkew,numRadialParam);
	}

	/**
	 * Adds a pair of images that observed the same target.
	 *
	 * @param left Image of left target.
	 * @param right Image of right target.
	 */
	public void addPair( ImageFloat32 left ,ImageFloat32 right ) {
		listLeft.add(left);
		listRight.add(right);
	}

	/**
	 * Compute stereo calibration parameters
	 *
	 * @return Stereo calibration parameters
	 */
	public StereoParameters process() {

		// calibrate left and right cameras
		IntrinsicParameters leftCalib = calibrateMono(listLeft,viewLeft);
		IntrinsicParameters rightCalib = calibrateMono(listRight,viewRight);

		// fit motion from right to left
		Se3_F64 leftToRight = computeLeftToRight();

		return new StereoParameters(leftCalib,rightCalib,leftToRight);
	}

	/**
	 * Compute intrinsic calibration for one of the cameras
	 */
	private IntrinsicParameters calibrateMono( List<ImageFloat32> images , List<Se3_F64> location )
	{
		monoCalib.reset();
		for( int i = 0; i < images.size(); i++ )
			if( !monoCalib.addImage(images.get(i)) )
				throw new RuntimeException("Can't handle failed detect yet");

		IntrinsicParameters intrinsic = monoCalib.process();

		ParametersZhang99 zhangParam = monoCalib.getFound();

		for( ParametersZhang99.View v : zhangParam.views ) {
			Se3_F64 pose = new Se3_F64();
			RotationMatrixGenerator.rodriguesToMatrix(v.rotation,pose.getR());
			pose.getT().set(v.T);
			location.add(pose);
		}

		return intrinsic;
	}

	/**
	 * Computes the motion by projecting all the grid points onto the left and right
	 * view then computing the transform from the point clouds.  Computing the "average"
	 * rotation matrix is tricky, but this is idiot proof.
	 *
	 * @return Transform from left to right view.
	 */
	private Se3_F64 computeLeftToRight() {
		List<Point2D_F64> points2D = monoCalib.getTarget().points;
		List<Point3D_F64> points3D = new ArrayList<Point3D_F64>();

		for( Point2D_F64 p : points2D ) {
			points3D.add( new Point3D_F64(p.x,p.y,0));
		}

		List<Point3D_F64> left = new ArrayList<Point3D_F64>();
		List<Point3D_F64> right = new ArrayList<Point3D_F64>();

		for( int i = 0; i < viewLeft.size(); i++ ) {
			Se3_F64 worldToLeft = viewLeft.get(i);
			Se3_F64 worldToRight = viewRight.get(i);

			for( Point3D_F64 p : points3D ) {
				Point3D_F64 l = SePointOps_F64.transform(worldToLeft,p,null);
				Point3D_F64 r = SePointOps_F64.transform(worldToRight,p,null);

				left.add(l);
				right.add(r);
			}
		}

		return FitSpecialEuclideanOps_F64.fitPoints3D(left,right);
	}

	public static void main( String args[] ) {
				PlanarCalibrationDetector detector = new WrapPlanarGridTarget(3,4);
//		PlanarCalibrationDetector detector = new WrapPlanarChessTarget(3,4,4);

		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(3, 4, 30,30);
//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(8, 8, 1, 7 / 18);
//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);

		CalibrateStereoPlanarApp app = new CalibrateStereoPlanarApp(detector);

//		app.reset();
		app.configure(target,false,2);

//		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";
		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Square";

		List<String> left = CalibrateMonoPlanarApp.directoryList(directory, "left");
		List<String> right = CalibrateMonoPlanarApp.directoryList(directory,"right");

		// ensure the lists are in the same order
		Collections.sort(left);
		Collections.sort(right);

		for( int i = 0; i < left.size(); i++ ) {
			BufferedImage l = UtilImageIO.loadImage(left.get(i));
			BufferedImage r = UtilImageIO.loadImage(right.get(i));

			ImageFloat32 imageLeft = ConvertBufferedImage.convertFrom(l,(ImageFloat32)null);
			ImageFloat32 imageRight = ConvertBufferedImage.convertFrom(r,(ImageFloat32)null);

			app.addPair(imageLeft,imageRight);
		}
		StereoParameters stereoCalib = app.process();

		// save results to a file and print out
		BoofMiscOps.saveXML(stereoCalib, "stereo.xml");
//		stereoCalib.print();
	}
}
