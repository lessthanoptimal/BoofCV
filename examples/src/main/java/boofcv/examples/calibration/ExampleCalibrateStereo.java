/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.CalibrateStereoPlanar;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.abst.geo.calibration.MultiToSingleFiducialCalibration;
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
 * All the image processing and calibration is taken care of inside of {@link CalibrateStereoPlanar}. The code below
 * loads calibration images as inputs, calibrates, and saves results to an XML file. See in code comments for tuning
 * and implementation issues.
 *
 * @author Peter Abeles
 * @see boofcv.examples.stereo.ExampleRectifyCalibratedStereo
 * @see CalibrateStereoPlanar
 */
public class ExampleCalibrateStereo {

	// Detects the target and calibration point inside the target
	DetectSingleFiducialCalibration detector;

	// List of calibration images
	List<String> left;
	List<String> right;

	/**
	 * ECoCheck target taken by a Zed stereo camera
	 */
	public void setupECoCheck() {
		// Creates a detector and specifies its physical characteristics
		detector = new MultiToSingleFiducialCalibration(FactoryFiducialCalibration.
				ecocheck(null, ConfigECoCheckMarkers.singleShape(9, 7, 1, 30)));

		String directory = UtilIO.pathExample("calibration/stereo/Zed_ecocheck");

		left = UtilIO.listByPrefix(directory, "left", null);
		right = UtilIO.listByPrefix(directory, "right", null);
	}

	/**
	 * Square grid target taken by a PtGrey Bumblebee camera.
	 */
	public void setupSquareGrid() {
		// Creates a detector and specifies its physical characteristics
		detector = FactoryFiducialCalibration.squareGrid(null, new ConfigGridDimen(4, 3, 30, 30));

		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Square");

		left = UtilIO.listByPrefix(directory, "left", null);
		right = UtilIO.listByPrefix(directory, "right", null);
	}

	/**
	 * Chessboard target taken by a PtGrey Bumblebee camera.
	 */
	public void setupChessboard() {
		// Creates a detector and specifies its physical characteristics
		detector = FactoryFiducialCalibration.chessboardX(null, new ConfigGridDimen(7, 5, 30));

		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");

		left = UtilIO.listByPrefix(directory, "left", null);
		right = UtilIO.listByPrefix(directory, "right", null);
	}

	/**
	 * Process calibration images, compute intrinsic parameters, save to a file
	 */
	public void process() {
		// Declare and setup the calibration algorithm
		var calibratorAlg = new CalibrateStereoPlanar(detector.getLayout());
		calibratorAlg.configure(/*zero skew*/true, /* radial */4, /* tangential */false);

		// Uncomment to print more information to stdout
//		calibratorAlg.setVerbose(System.out,null);

		// ensure the lists are in the same order
		Collections.sort(left);
		Collections.sort(right);

		for (int i = 0; i < left.size(); i++) {
			BufferedImage l = UtilImageIO.loadImageNotNull(left.get(i));
			BufferedImage r = UtilImageIO.loadImageNotNull(right.get(i));

			GrayF32 imageLeft = ConvertBufferedImage.convertFrom(l, (GrayF32)null);
			GrayF32 imageRight = ConvertBufferedImage.convertFrom(r, (GrayF32)null);

			CalibrationObservation calibLeft, calibRight;
			if (!detector.process(imageLeft)) {
				System.out.println("Failed to detect target in " + left.get(i));
				continue;
			}
			calibLeft = detector.getDetectedPoints();
			if (!detector.process(imageRight)) {
				System.out.println("Failed to detect target in " + right.get(i));
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

	public static void main( String[] args ) {
		var alg = new ExampleCalibrateStereo();

		// Strongly recommended that ECoCheck target is used as it allows you to entirely fill in left and right
		// stereo images since it allows for partially observed targets
		alg.setupECoCheck();
//		alg.setupChessboard();
//		alg.setupSquareGrid();

		// compute and save results
		alg.process();
	}
}
