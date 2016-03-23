/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Detects square binary fiducials inside an image, writes out there pose, and visualizes a virtual flat cube
 * above them in the input image.
 *
 * @author Peter Abeles
 */
public class ExampleFiducialBinary {
	public static void main(String[] args) {

		String directory = UtilIO.pathExample("fiducial/binary");

		// load the lens distortion parameters and the input image
		IntrinsicParameters param = UtilIO.loadXML(directory , "intrinsic.xml");
		BufferedImage input = UtilImageIO.loadImage(directory , "image0000.jpg");
//		BufferedImage input = UtilImageIO.loadImage(directory , "image0001.jpg");
//		BufferedImage input = UtilImageIO.loadImage(directory , "image0002.jpg");
		GrayF32 original = ConvertBufferedImage.convertFrom(input,true, ImageType.single(GrayF32.class));

		// Detect the fiducial
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(
				new ConfigFiducialBinary(0.1), ConfigThreshold.local(ThresholdType.LOCAL_SQUARE, 10), GrayF32.class);
//				new ConfigFiducialBinary(0.1), ConfigThreshold.fixed(100),ImageFloat32.class);

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

			VisualizeFiducial.drawCube(targetToSensor,param,detector.getWidth(i), 3, g2);
			VisualizeFiducial.drawLabelCenter(targetToSensor,param,""+detector.getId(i), g2);
		}

		ShowImages.showWindow(input,"Fiducials",true);

	}
}
