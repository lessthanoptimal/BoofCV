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

package boofcv.examples.calibration;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Example of how to calibrate a single (monocular) camera using a high level interface that processes images of planar
 * calibration targets.  The entire calibration target must be observable in the image and for best results images
 * should be in focus and not blurred.  For a lower level example of camera calibration which processes a set of
 * observed calibration points see {@link ExampleCalibrateMonocular}.
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

	// Detects the target and calibration point inside the target
	DetectorFiducialCalibration detector;

	// List of calibration images
	List<String> images;

	/**
	 * Images from Zhang's website.  Square grid pattern.
	 */
	private void setupZhang99() {
		// Creates a detector and specifies its physical characteristics
		detector = FactoryFiducialCalibration.detectorSquareGrid(new ConfigSquareGrid(8, 8, 0.5, 7.0 / 18.0));

		// load image list
		String directory = UtilIO.pathExample("calibration/mono/PULNiX_CCD_6mm_Zhang");
		images = BoofMiscOps.directoryList(directory,"CalibIm");
	}

	/**
	 * Images collected from a Bumblee Bee stereo camera.  Large amounts of radial distortion. Chessboard pattern.
	 */
	private void setupBumbleBee() {
		// Creates a detector and specifies its physical characteristics
		detector = FactoryFiducialCalibration.detectorChessboard(new ConfigChessboard(7, 5, 30));

		// load image list
		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");
		images = BoofMiscOps.directoryList(directory,"left");
	}

	/**
	 * Process calibration images, compute intrinsic parameters, save to a file
	 */
	public void process() {

		// Declare and setup the calibration algorithm
		CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector);

		// tell it type type of target and which parameters to estimate
		calibrationAlg.configure( true, 2, false);

		for( String n : images ) {
			BufferedImage input = UtilImageIO.loadImage(n);
			if( n != null ) {
				GrayF32 image = ConvertBufferedImage.convertFrom(input,(GrayF32)null);
				if( !calibrationAlg.addImage(image) )
					System.err.println("Failed to detect target in "+n);
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


	public static void main( String args[] ) {
		ExampleCalibrateMonocular alg = new ExampleCalibrateMonocular();

		// which target should it process
//		alg.setupZhang99();
		alg.setupBumbleBee();

		// compute and save results
		alg.process();
	}
}
