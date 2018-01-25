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
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Example of how to calibrate a single (monocular) fisheye camera using a high level interface. This example
 * for the most part follows the same routine as {@link ExampleCalibrateMonocular}. Fisheye cameras tend to require
 * more images to properly calibrate. Often people will use larger calibration targets too that are easier to
 * see at a distance and cover more of the fisheye's camera large FOV.
 *
 * @see CalibrateMonoPlanar
 *
 * @author Peter Abeles
 */
public class ExampleCalibrateFisheye {
	public static void main( String args[] ) {
		DetectorFiducialCalibration detector;
		List<String> images;

		// Circle based calibration targets not not recommended because the sever lens distortion will change
		// the apparent location of tangent points.

		// Square Grid example
//		detector = FactoryFiducialCalibration.squareGrid(new ConfigSquareGrid(4, 3, 30, 30));
//		images = UtilIO.listAll(UtilIO.pathExample("calibration/fisheye/square_grid"));

//		 Chessboard Example
		detector = FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));
		images = UtilIO.listAll(UtilIO.pathExample("calibration/fisheye/chessboard"));

		// Declare and setup the calibration algorithm
		CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector.getLayout());

		// tell it type type of target and which parameters to estimate
		calibrationAlg.configureUniversalOmni( true, 2, false);

		// it's also possible to fix the mirror offset parameter
		// 0 = pinhole camera. 1 = fisheye
//		calibrationAlg.configureUniversalOmni( true, 2, false,1.0);

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
		CameraUniversalOmni intrinsic = calibrationAlg.process();

		// save results to a file and print out
		CalibrationIO.save(intrinsic, "fisheye.yaml");

		calibrationAlg.printStatistics();
		System.out.println();
		System.out.println("--- Intrinsic Parameters ---");
		System.out.println();
		intrinsic.print();
	}
}
