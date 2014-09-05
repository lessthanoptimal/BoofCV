/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.fiducial;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.fiducial.CalibrationFiducialDetector;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.MousePauseHelper;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The 6-DOF pose of calibration targets can be estimated very accurately[*] once a camera has been calibrated.
 * In this example the high level FiducialDetector interface is used with a chessboard calibration target to
 * process a video sequence. Once the pose of the target is known the location of each calibration point is
 * found in the camera frame and visualized.
 *
 * [*] Accuracy is dependent on a variety of factors. Calibration targets are primarily designed to be viewed up close
 * and their accuracy drops with range, as can be seen in this example.
 *
 * @author Peter Abeles
 */
public class ExamplePoseOfCalibrationTarget {

	public static void main( String args[] ) {

		// Load camera calibration
		IntrinsicParameters intrinsic =
				UtilIO.loadXML("../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/intrinsic.xml");

		// load the video file
		String fileName = "../data/applet/tracking/chessboard_SonyDSC_01.mjpeg";
		SimpleImageSequence<ImageFloat32> video =
				DefaultMediaManager.INSTANCE.openVideo(fileName, ImageType.single(ImageFloat32.class));

		// Let's use the FiducialDetector interface since it is much easier than coding up
		// the entire thing ourselves.  Look at FiducialDetector's code if you want to understand how it works.
		double sizeOfSquareInMeters = 0.03;
		CalibrationFiducialDetector<ImageFloat32> detector =
				FactoryFiducial.calibChessboard(new ConfigChessboard(5, 4),sizeOfSquareInMeters,ImageFloat32.class);

		detector.setIntrinsic(intrinsic);

		// Get the 2D coordinate of calibration points for visualization purposes
		List<Point2D_F64> calibPts = detector.getCalibrationPoints();

		// Set up visualization
		JPanel gui = new JPanel();
		PointCloudViewer viewer = new PointCloudViewer(intrinsic, 0.01);
		// make the view more interest.  From the side.
		DenseMatrix64F rotY = RotationMatrixGenerator.rotY(-Math.PI/2.0,null);
		viewer.setWorldToCamera(new Se3_F64(rotY,new Vector3D_F64(0.75,0,1.25)));
		ImagePanel imagePanel = new ImagePanel(intrinsic.width, intrinsic.height);
		gui.add(BorderLayout.WEST, imagePanel); gui.add(BorderLayout.CENTER, viewer);
		ShowImages.showWindow(gui,"Calibration Target Pose");

		// Allows the user to click on the image and pause
		MousePauseHelper pauseHelper = new MousePauseHelper(gui);

		// saves the target's center location
		List<Point3D_F64> path = new ArrayList<Point3D_F64>();

		// Process each frame in the video sequence
		Se3_F64 targetToCamera = new Se3_F64();
		while( video.hasNext() ) {

			// detect calibration points
			detector.detect(video.next());

			if( detector.totalFound() == 0 )
				throw new RuntimeException("Failed to detect target");

			detector.getFiducialToWorld(0,targetToCamera);

			// Visualization.  Show a path with green points and the calibration points in black
			viewer.reset();

			Point3D_F64 center = new Point3D_F64();
			SePointOps_F64.transform(targetToCamera,center,center);
			path.add(center);

			for( Point3D_F64 p : path ) {
				viewer.addPoint(p.x,p.y,p.z,0x00FF00);
			}

			for( int j = 0; j < calibPts.size(); j++ ) {
				Point2D_F64 p = calibPts.get(j);
				Point3D_F64 p3 = new Point3D_F64(p.x,p.y,0);
				SePointOps_F64.transform(targetToCamera,p3,p3);
				viewer.addPoint(p3.x,p3.y,p3.z,0);
			}

			imagePanel.setBufferedImage((BufferedImage) video.getGuiImage());
			viewer.repaint();
			imagePanel.repaint();

			BoofMiscOps.pause(30);
			while( pauseHelper.isPaused() ) {
				BoofMiscOps.pause(30);
			}
		}
	}
}
