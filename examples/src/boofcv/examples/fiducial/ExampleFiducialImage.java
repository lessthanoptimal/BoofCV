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

package boofcv.examples.fiducial;

import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.image.BufferedImage;

import static boofcv.io.image.UtilImageIO.loadImage;

/**
 * Detects square binary fiducials inside an image, writes out there pose, and visualizes a virtual flat cube
 * above them in the input image.
 *
 * @author Peter Abeles
 */
public class ExampleFiducialImage {
	public static void main(String[] args) {

		String imagePath   = "../data/applet/fiducial/image/examples/";
		String patternPath = "../data/applet/fiducial/image/patterns/";

		String imageName = "image00.jpg";
//		String imageName = "image01.jpg";
//		String imageName = "image02.jpg";

		// load the lens distortion parameters and the input image
		IntrinsicParameters param = UtilIO.loadXML(imagePath + "intrinsic.xml");
		BufferedImage input = loadImage(imagePath + imageName);
		ImageFloat32 original = ConvertBufferedImage.convertFrom(input, true, ImageType.single(ImageFloat32.class));

		// Detect the fiducial
		SquareImage_to_FiducialDetector<ImageFloat32> detector = FactoryFiducial.
				squareImageRobust(new ConfigFiducialImage(), 6, ImageFloat32.class);
//				squareImageFast(new ConfigFiducialImage(0.1), 100, ImageFloat32.class);

		// give it a description of all the targets
		double width = 4; // 4 cm
		detector.addPattern(loadImage(patternPath + "ke.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "dog.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "yu.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "yu_inverted.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "pentarose.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "text_boofcv.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "leaf01.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "leaf02.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "hand01.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "chicken.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "h2o.png", ImageFloat32.class), 100, width);
		detector.addPattern(loadImage(patternPath + "yinyang.png", ImageFloat32.class), 100, width);

		detector.setIntrinsic(param);

		detector.detect(original);

		// print the results
		Graphics2D g2 = input.createGraphics();
		Se3_F64 targetToSensor = new Se3_F64();
		for (int i = 0; i < detector.totalFound(); i++) {
			System.out.println("Target ID = "+detector.getId(i));
			detector.getFiducialToCamera(i, targetToSensor);
			System.out.println("Location:");
			System.out.println(targetToSensor);

			VisualizeFiducial.drawNumbers(targetToSensor,param,detector.getId(i), g2);
			VisualizeFiducial.drawCube(targetToSensor,param,detector.getWidth(i),g2);
		}

		ShowImages.showWindow(input,"Fiducials",true);

	}
}
