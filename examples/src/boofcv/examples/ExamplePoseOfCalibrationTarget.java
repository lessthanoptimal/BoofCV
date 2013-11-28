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

package boofcv.examples;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.alg.geo.calibration.Zhang99ComputeTargetHomography;
import boofcv.alg.geo.calibration.Zhang99DecomposeHomography;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.awt.image.BufferedImage;

/**
 * The 6-DOF pose of calibration targets relative to the camera can be estimated very accurately once a camera
 * has been calibrated.  This example demonstrates how detect a calibration target and convert it into a rigid body
 * transformation from target's frame into the camera's frame.  If the target is symmetric then the pose can only
 * be solved up the ambiguity.
 *
 * @author Peter Abeles
 */
public class ExamplePoseOfCalibrationTarget {

	public static void main( String args[] ) {

		String directory = "../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/";

		IntrinsicParameters intrinsic = BoofMiscOps.loadXML(directory+"intrinsic.xml");
		BufferedImage image = UtilImageIO.loadImage(directory+"frame06.jpg");

		ImageFloat32 gray = ConvertBufferedImage.convertFrom(image, (ImageFloat32) null);

		// Detects the target and calibration point inside the target
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5, 7));
		// specify target's shape.  This also specifies where the center of the target's coordinate system is.
		// Look at source code to be sure, but it is probably the target's center.  You can change this by
		// creating your own target.. Note z=0 is assumed
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(5, 7, 10);
		// Computes the homography
		Zhang99ComputeTargetHomography computeH = new Zhang99ComputeTargetHomography(target);
		// decomposes the homography
		Zhang99DecomposeHomography decomposeH = new Zhang99DecomposeHomography();

		// convert the intrinisic calibration into matrix format
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(intrinsic,null);

		// detect calibration points
		if( !detector.process(gray) )
			throw new RuntimeException("Failed to detect target");

		// Compute the homography
		if( !computeH.computeHomography(detector.getPoints()) )
			throw new RuntimeException("Can't compute homography");

		DenseMatrix64F H = computeH.getHomography();

		// compute camera pose from the homography matrix
		decomposeH.setCalibrationMatrix(K);
		Se3_F64 targetToCamera = decomposeH.decompose(H);

		System.out.println("Translation: " + targetToCamera.getT());
		System.out.println("Orientation: " + targetToCamera.getR());
	}
}
