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

package boofcv.app;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import com.github.sarxos.webcam.Webcam;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class TrackFiducialWebcam {
	public static void main(String[] args) {
		// Load intrinsic camera parameters for the camera
		// SERIOUSLY, YOU NEED TO CALIBRATE YOUR CAMERA AND USE THE FILE YOU GENERATE
		IntrinsicParameters param = UtilIO.loadXML("../data/applet/fiducial/binary/intrinsic.xml");

		Webcam webcam = Webcam.getWebcams().get(1);
		UtilWebcamCapture.adjustResolution(webcam, 640, 480);
		webcam.open();


		// Detect the fiducial
		FiducialDetector<ImageFloat32> detector = FactoryFiducial.
				squareBinaryRobust(new ConfigFiducialBinary(0.1), 6, ImageFloat32.class);
//				calibChessboard(new ConfigChessboard(5,7), 0.03, ImageFloat32.class);
//				calibSquareGrid(new ConfigSquareGrid(5,7), 0.03, ImageFloat32.class);

		detector.setIntrinsic(param);

		ImageFloat32 gray = new ImageFloat32(640,480);
		ImagePanel gui = new ImagePanel(640,480);
		ShowImages.showWindow(gui,"Fiducials") ;

		while( true ) {
			BufferedImage frame = webcam.getImage();

			ConvertBufferedImage.convertFrom(frame,gray);

			detector.detect(gray);

			// display the results
			Graphics2D g2 = frame.createGraphics();
			Se3_F64 targetToSensor = new Se3_F64();
			for (int i = 0; i < detector.totalFound(); i++) {
				detector.getFiducialToWorld(i, targetToSensor);

				VisualizeFiducial.drawCube(targetToSensor, param, 0.1, g2);
			}

			gui.setBufferedImageSafe(frame);
			gui.repaint();
		}
	}
}
