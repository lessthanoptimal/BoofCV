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

package boofcv.examples.geometry;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.alg.geo.calibration.Zhang99ComputeTargetHomography;
import boofcv.alg.geo.calibration.Zhang99DecomposeHomography;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.gui.d3.PointCloudViewer;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The 6-DOF pose of calibration targets relative to the camera can be estimated very accurately once a camera
 * has been calibrated.  This example demonstrates how detect a calibration target and convert it into a rigid body
 * transformation from target's frame into the camera's frame.  Orientation can be uniquely estimated for some
 * calibration grid patterns.  If the pattern is symmetric then the pose can only be estimated up to the symmetry.
 *
 * @author Peter Abeles
 */
public class ExamplePoseOfCalibrationTarget {

	public static void main( String args[] ) {

		// Load camera calibration
		IntrinsicParameters intrinsic =
				BoofMiscOps.loadXML("../data/applet/calibration/mono/Sony_DSC-HX5V_Chess/intrinsic.xml");

		int width = intrinsic.width; int height = intrinsic.height;

		// load the video file
		String fileName = "../data/applet/tracking/chessboard_SonyDSC_01.mjpeg";
		SimpleImageSequence<ImageFloat32> video =
				DefaultMediaManager.INSTANCE.openVideo(fileName, ImageType.single(ImageFloat32.class));

		// Detects the target and calibration point inside the target
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5, 4));
		// specify target's shape.  This also specifies where the center of the target's coordinate system is.
		// Look at source code to be sure, but it is probably the target's center.  You can change this by
		// creating your own target.. Note z=0 is assumed
		double sizeOfSquareInMeters = 0.03;
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(5, 4, sizeOfSquareInMeters);
		// Computes the homography
		Zhang99ComputeTargetHomography computeH = new Zhang99ComputeTargetHomography(target);
		// decomposes the homography
		Zhang99DecomposeHomography decomposeH = new Zhang99DecomposeHomography();

		// Need to remove lens distortion for accurate pose estimation
		PointTransform_F64 distortToUndistorted = LensDistortionOps.transformRadialToPixel_F64(intrinsic);

		// convert the intrinsic into matrix format
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(intrinsic,null);

		// Set up visualization
		JPanel gui = new JPanel();
		PointCloudViewer viewer = new PointCloudViewer(K, 0.01);
		ImagePanel imagePanel = new ImagePanel();
		viewer.setPreferredSize(new Dimension(width,height));
		imagePanel.setPreferredSize(new Dimension(width, height));
		gui.add(BorderLayout.WEST, imagePanel); gui.add(BorderLayout.CENTER, viewer);
		ShowImages.showWindow(gui,"Calibration Target Pose");

		// saves the target's center location
		List<Point3D_F64> path = new ArrayList<Point3D_F64>();

		// Process each frame in the video sequence
		while( video.hasNext() ) {

			// detect calibration points
			if( !detector.process(video.next()) )
				throw new RuntimeException("Failed to detect target");

			// Remove lens distortion from detected calibration points
			List<Point2D_F64> points = detector.getPoints();
			for( Point2D_F64 p : points ) {
				distortToUndistorted.compute(p.x,p.y,p);
			}

			// Compute the homography
			if( !computeH.computeHomography(points) )
				throw new RuntimeException("Can't compute homography");

			DenseMatrix64F H = computeH.getHomography();

			// compute camera pose from the homography matrix
			decomposeH.setCalibrationMatrix(K);
			Se3_F64 targetToCamera = decomposeH.decompose(H);

			// Visualization.  Show a path with green points and the calibration points in black
			viewer.reset();

			Point3D_F64 center = new Point3D_F64();
			SePointOps_F64.transform(targetToCamera,center,center);
			path.add(center);

			for( Point3D_F64 p : path ) {
				viewer.addPoint(p.x,p.y,p.z,0x00FF00);
			}

			for( int j = 0; j < target.points.size(); j++ ) {
				Point2D_F64 p = target.points.get(j);
				Point3D_F64 p3 = new Point3D_F64(p.x,p.y,0);
				SePointOps_F64.transform(targetToCamera,p3,p3);
				viewer.addPoint(p3.x,p3.y,p3.z,0);
			}

			imagePanel.setBufferedImage((BufferedImage) video.getGuiImage());
			viewer.repaint();
			imagePanel.repaint();

			BoofMiscOps.pause(20);
		}
	}
}
