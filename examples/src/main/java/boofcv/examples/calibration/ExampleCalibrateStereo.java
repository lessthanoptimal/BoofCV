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
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.abst.geo.calibration.CalibrateStereoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Example of how to calibrate a stereo camera system using a planar calibration grid given a set of images.
 * Intrinsic camera parameters are estimated for both cameras individually, then extrinsic parameters
 * for the two cameras relative to each other are found   This example does not rectify the images, which is
 * required for some algorithms. See {@link boofcv.examples.stereo.ExampleRectifyCalibratedStereo}. Both square grid and chessboard targets
 * are demonstrated in this example. See calibration tutorial for a discussion of different target types and how to
 * collect good calibration images.
 *
 * All the image processing and calibration is taken care of inside of {@link CalibrateStereoPlanar}.  The code below
 * loads calibration images as inputs, calibrates, and saves results to an XML file.  See in code comments for tuning
 * and implementation issues.
 *
 * @see boofcv.examples.stereo.ExampleRectifyCalibratedStereo
 * @see CalibrateStereoPlanar
 *
 * @author Peter Abeles
 */
public class ExampleCalibrateStereo {

	// Detects the target and calibration point inside the target
	DetectorFiducialCalibration detector;

	// List of calibration images
	List<String> left;
	List<String> right;

	/**
	 * Square grid target taken by a PtGrey Bumblebee camera.
	 */
	public void setupBumblebeeSquare() {
		// Creates a detector and specifies its physical characteristics
		detector = FactoryFiducialCalibration.squareGrid(new ConfigSquareGrid(4, 3, 30, 30));

		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Square");

		left = UtilIO.listByPrefix(directory, "left");
		right = UtilIO.listByPrefix(directory, "right");
	}

	/**
	 * Chessboard target taken by a PtGrey Bumblebee camera.
	 */
	public void setupBumblebeeChess() {
		// Creates a detector and specifies its physical characteristics
		detector = FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));

		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");

		left = UtilIO.listByPrefix(directory, "left");
		right = UtilIO.listByPrefix(directory, "right");
	}

	/**
	 * Process calibration images, compute intrinsic parameters, save to a file
	 */
	public void process() {
		// Declare and setup the calibration algorithm
		CalibrateStereoPlanar calibratorAlg = new CalibrateStereoPlanar(detector.getLayout());
		calibratorAlg.configure(true, 2, false);

		// ensure the lists are in the same order
		Collections.sort(left);
		Collections.sort(right);

		for( int i = 0; i < left.size(); i++ ) {
			BufferedImage l = UtilImageIO.loadImage(left.get(i));
			BufferedImage r = UtilImageIO.loadImage(right.get(i));

			GrayF32 imageLeft = ConvertBufferedImage.convertFrom(l,(GrayF32)null);
			GrayF32 imageRight = ConvertBufferedImage.convertFrom(r,(GrayF32)null);

			CalibrationObservation calibLeft,calibRight;
			if( !detector.process(imageLeft)) {
				System.out.println("Failed to detect target in "+left.get(i));
				continue;
			}
			calibLeft = detector.getDetectedPoints();
			if( !detector.process(imageRight)) {
				System.out.println("Failed to detect target in "+right.get(i));
				continue;
			}
			calibRight = detector.getDetectedPoints();

			calibratorAlg.addPair(calibLeft, calibRight);
		}

		// Process and compute calibration parameters
		StereoParameters stereoCalib = calibratorAlg.process();

		// print out information on its accuracy and errors
		calibratorAlg.printStatistics();

		// save results to a file and print out
		CalibrationIO.save(stereoCalib, "stereo.yaml");
		stereoCalib.print();

		// Note that the stereo baseline translation will be specified in the same units as the calibration grid.
		// Which is in millimeters (mm) in this example.
	}

	public static void main( String args[] ) {
		ExampleCalibrateStereo alg = new ExampleCalibrateStereo();

		// Select which set of targets to use
		alg.setupBumblebeeChess();
//		alg.setupBumblebeeSquare();

		// compute and save results
		alg.process();
	}
}
