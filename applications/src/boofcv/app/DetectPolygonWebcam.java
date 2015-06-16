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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import com.github.sarxos.webcam.Webcam;
import georegression.metric.UtilAngle;
import georegression.struct.shapes.Polygon2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Actively tracks and displays found fiducials live in a video stream from a webcam.
 *
 * @author Peter Abeles
 */
public class DetectPolygonWebcam {
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
			param.fx = param.cx/Math.tan(UtilAngle.degreeToRadian(30)); // assume 60 degree FOV
			param.fy = param.cx/Math.tan(UtilAngle.degreeToRadian(30));
		} else {
			param = UtilIO.loadXML(nameIntrinsic);
		}

		ConfigPolygonDetector config = new ConfigPolygonDetector(4);
		config.configRefineLines.sampleRadius = 2;
		config.configRefineLines.maxIterations = 30;


		InputToBinary<ImageFloat32> inputToBinary =
//				FactoryThresholdBinary.globalOtsu(0,255,true,ImageFloat32.class);
//				FactoryThresholdBinary.globalEntropy(0,255,true,ImageFloat32.class);
				FactoryThresholdBinary.adaptiveSquare(10,0,true,ImageFloat32.class);
		BinaryPolygonConvexDetector<ImageFloat32> detector = FactoryShapeDetector.
				polygon(inputToBinary, new ConfigPolygonDetector(4), ImageFloat32.class);


		ImageFloat32 gray = new ImageFloat32(640,480);
		ImagePanel gui = new ImagePanel(640,480);
		ShowImages.showWindow(gui,"Fiducials") ;

		while( true ) {
			BufferedImage frame = webcam.getImage();

			ConvertBufferedImage.convertFrom(frame,gray);

			detector.process(gray);

			// display the results
			Graphics2D g2 = frame.createGraphics();

			List<Polygon2D_F64> shapes = detector.getFound().toList();

			g2.setStroke(new BasicStroke(4));
			g2.setColor(Color.RED);

			for (int i = 0; i < shapes.size(); i++) {
				Polygon2D_F64 poly = shapes.get(i);


				for (int j = 0; j < poly.size(); j++) {
					int k = (j+1)%poly.size();

					int x0 = (int)(poly.get(j).x+0.5);
					int y0 = (int)(poly.get(j).y+0.5);

					int x1 = (int)(poly.get(k).x+0.5);
					int y1 = (int)(poly.get(k).y+0.5);

					g2.drawLine(x0,y0,x1,y1);
				}
			}

			gui.setBufferedImageSafe(frame);
			gui.repaint();
		}
	}
}
