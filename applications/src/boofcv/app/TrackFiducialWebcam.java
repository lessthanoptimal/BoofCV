/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import com.github.sarxos.webcam.Webcam;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import static boofcv.io.image.UtilImageIO.loadImage;

/**
 * Actively tracks and displays found fiducials live in a video stream from a webcam.
 *
 * @author Peter Abeles
 */
public class TrackFiducialWebcam {
	public static void main(String[] args) {

		String nameIntrinsic = null;
		int cameraId = 0;

		if (args.length >= 1) {
			cameraId = Integer.parseInt(args[0]);
		}
		if (args.length >= 2) {
			nameIntrinsic = args[1];
		} else {
			System.out.println();
			System.out.println("SERIOUSLY YOU NEED TO CALIBRATE THE CAMERA YOURSELF!");
			System.out.println("There will be a lot more jitter and inaccurate pose");
			System.out.println();
		}

		System.out.println();
		System.out.println("camera ID = "+cameraId);
		System.out.println("intrinsic file = " + nameIntrinsic);
		System.out.println();

		Webcam webcam = Webcam.getWebcams().get(cameraId);
		UtilWebcamCapture.adjustResolution(webcam, 640, 480);
		webcam.open();

		// Load intrinsic camera parameters for the camera
		// SERIOUSLY, YOU NEED TO CALIBRATE YOUR CAMERA AND USE THE FILE YOU GENERATE
		IntrinsicParameters param;

		// just make up some reasonable parameters for a webcam and assume no lens distortion
		if (nameIntrinsic == null) {
			param = new IntrinsicParameters();
			Dimension d = webcam.getDevice().getResolution();
			param.width = d.width; param.height = d.height;
			param.cx = d.width/2;
			param.cy = d.height/2;
			param.fx = param.cx/Math.tan(UtilAngle.degreeToRadian(35)); // assume 70 degree FOV
			param.fy = param.cx/Math.tan(UtilAngle.degreeToRadian(35));
		} else {
			param = UtilIO.loadXML(nameIntrinsic);
		}


		// Uncomment to select different detectors
//		FiducialDetector<ImageFloat32> detector = FactoryFiducial.squareBinaryRobust(new ConfigFiducialBinary(0.1), 6, ImageFloat32.class);
//		FiducialDetector<ImageFloat32> detector = FactoryFiducial.calibChessboard(new ConfigChessboard(5, 7), 0.03, ImageFloat32.class);
//		FiducialDetector<ImageFloat32> detector = FactoryFiducial.calibSquareGrid(new ConfigSquareGrid(5, 7), 0.03, ImageFloat32.class);

		String patternPath = UtilIO.getPathToBase()+"data/applet/fiducial/image/";
		SquareImage_to_FiducialDetector<ImageFloat32> detector =
				FactoryFiducial.squareImageRobust(new ConfigFiducialImage(),6, ImageFloat32.class);
		detector.addTarget(loadImage(patternPath+"ke.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"dog.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"yu.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"yu_inverted.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"pentarose.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"text_boofcv.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"leaf01.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"leaf02.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"hand01.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"chicken.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"h2o.png", ImageFloat32.class),100,2.5);
		detector.addTarget(loadImage(patternPath+"yinyang.png", ImageFloat32.class),100,2.5);

		detector.setIntrinsic(param);

		ImageFloat32 gray = new ImageFloat32(param.width,param.height);
		ImagePanel gui = new ImagePanel(param.width,param.height);
		ShowImages.showWindow(gui,"Fiducials").setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		Font font = new Font("Serif", Font.BOLD, 24);


		while( true ) {
			BufferedImage frame = webcam.getImage();

			ConvertBufferedImage.convertFrom(frame,gray);

			detector.detect(gray);

			// display the results
			Graphics2D g2 = frame.createGraphics();
			Se3_F64 targetToSensor = new Se3_F64();
			for (int i = 0; i < detector.totalFound(); i++) {
				detector.getFiducialToCamera(i, targetToSensor);
				double width = detector.getWidth(i);
				int id = detector.getId(i);

				// Computer the center of the fiducial in pixel coordinates
				Point2D_F64 p = new Point2D_F64();
				Point3D_F64 c = new Point3D_F64();
				SePointOps_F64.transform(targetToSensor, c, c);
				PerspectiveOps.convertNormToPixel(param, c.x / c.z, c.y / c.z, p);

				// Draw the ID number approximately in the center
				FontMetrics metrics = g2.getFontMetrics(font);
				String text = Integer.toString(id);
				Rectangle2D r = metrics.getStringBounds(text,null);
				g2.setColor(Color.ORANGE);
				g2.setFont(font);
				g2.drawString(text,(float)(p.x-r.getWidth()/2),(float)(p.y+r.getHeight()/2));

				// draw a cube to show orientation and location
				VisualizeFiducial.drawCube(targetToSensor, param, width, g2);
			}
			if( nameIntrinsic == null ) {
				g2.setColor(Color.RED);
				g2.setFont(font);
				g2.drawString("Uncalibrated",10,20);
			}

			gui.setBufferedImageSafe(frame);
			gui.repaint();
		}
	}
}
