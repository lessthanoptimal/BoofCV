/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.calibration;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Example of how to calibrate a single (monocular) camera using a high level interface. Depending on the calibration
 * target detector and target type, the entire target might need to be visible in the image. All camera images
 * should be in focus and that target evenly spread through out the images. In particular the edges of the image
 * should be covered.
 *
 * After processing both intrinsic camera parameters and lens distortion are estimated.  Square grid and chessboard
 * targets are demonstrated by this example. See calibration tutorial for a discussion of different target types
 * and how to collect good calibration images.
 *
 * All the image processing and calibration is taken care of inside of {@link CalibrateMonoPlanar}.  The code below
 * loads calibration images as inputs, calibrates, and saves results to an XML file.  See in code comments for tuning
 * and implementation issues.
 *
 * @see CalibrateMonoPlanar
 *
 * @author Peter Abeles
 */
public class ExampleCalibrateMonocular {
	public static void main( String args[] ) {
		DetectorFiducialCalibration detector;
		List<String> images;

		// Regular Circle Example
//		detector = FactoryFiducialCalibration.circleRegularGrid(new ConfigCircleRegularGrid(8, 10, 1.5, 2.5));
//		images = UtilIO.listByPrefix(UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_CircleRegular"),"image");

		// Hexagonal Circle Example
//		detector = FactoryFiducialCalibration.circleHexagonalGrid(new ConfigCircleHexagonalGrid(24, 28, 1, 1.2));
//		images = UtilIO.listByPrefix(UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_CircleHexagonal"),"image");

		// Square Grid example
//		detector = FactoryFiducialCalibration.squareGrid(new ConfigSquareGrid(4, 3, 30, 30));
//		images = UtilIO.listByPrefix(UtilIO.pathExample("calibration/stereo/Bumblebee2_Square"),"left");

		// Chessboard Example
		detector = FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));
		images = UtilIO.listByPrefix(UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess"),"left");

		// Declare and setup the calibration algorithm
		CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector.getLayout());

		// tell it type type of target and which parameters to estimate
		calibrationAlg.configurePinhole( true, 2, false);

		for( String n : images ) {
			BufferedImage input = UtilImageIO.loadImage(n);
			if( input != null ) {
				GrayF32 image = ConvertBufferedImage.convertFrom(input,(GrayF32)null);
				if( detector.process(image)) {
					calibrationAlg.addImage(detector.getDetectedPoints().copy());
				} else {
					System.err.println("Failed to detect target in " + n);
				}
			}
		}
		// process and compute intrinsic parameters
		CameraPinholeRadial intrinsic = calibrationAlg.process();

		// save results to a file and print out
		CalibrationIO.save(intrinsic, "intrinsic.yaml");

		calibrationAlg.printStatistics();
		System.out.println();
		System.out.println("--- Intrinsic Parameters ---");
		System.out.println();
		intrinsic.print();
	}
}
