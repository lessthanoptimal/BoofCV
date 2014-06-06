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

package boofcv.examples.calibration;

import boofcv.abst.calib.CalibrateStereoPlanar;
import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;

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
public class ExampleCalibrateStereoPlanar {

	// Detects the target and calibration point inside the target
	PlanarCalibrationDetector detector;

	// Description of the target's physical dimension
	PlanarCalibrationTarget target;

	// List of calibration images
	List<String> left;
	List<String> right;

	// Many 3D operations assumed a right handed coordinate system with +Z pointing out of the image.
	// If the image coordinate system is left handed then the y-axis needs to be flipped to meet
	// that requirement.  Most of the time this is false.
	boolean flipY;

	/**
	 * Square grid target taken by a PtGrey Bumblebee camera.
	 */
	public void setupBumblebeeSquare() {
		// Use the wrapper below for square grid targets.
		detector = FactoryPlanarCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(5,7));
		// Target physical description
		target = FactoryPlanarCalibrationTarget.gridSquare(5, 7, 30,30);

		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Square";

		left = BoofMiscOps.directoryList(directory, "left");
		right = BoofMiscOps.directoryList(directory, "right");

		flipY = false;
	}

	/**
	 * Chessboard target taken by a PtGrey Bumblebee camera.
	 */
	public void setupBumblebeeChess() {
		// Use the wrapper below for chessboard targets.
		detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(5,7));
		// Target physical description
		target = FactoryPlanarCalibrationTarget.gridChess(5, 7, 30);

		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";

		left = BoofMiscOps.directoryList(directory, "left");
		right = BoofMiscOps.directoryList(directory, "right");

		flipY = false;
	}

	/**
	 * Process calibration images, compute intrinsic parameters, save to a file
	 */
	public void process() {
		// Declare and setup the calibration algorithm
		CalibrateStereoPlanar calibratorAlg = new CalibrateStereoPlanar(detector, flipY);
		calibratorAlg.configure(target, true, 2);

		// ensure the lists are in the same order
		Collections.sort(left);
		Collections.sort(right);

		for( int i = 0; i < left.size(); i++ ) {
			BufferedImage l = UtilImageIO.loadImage(left.get(i));
			BufferedImage r = UtilImageIO.loadImage(right.get(i));

			ImageFloat32 imageLeft = ConvertBufferedImage.convertFrom(l,(ImageFloat32)null);
			ImageFloat32 imageRight = ConvertBufferedImage.convertFrom(r,(ImageFloat32)null);

			if( !calibratorAlg.addPair(imageLeft, imageRight) )
				System.out.println("Failed to detect target in "+left.get(i)+" and/or "+right.get(i));
		}

		// Process and compute calibration parameters
		StereoParameters stereoCalib = calibratorAlg.process();

		// print out information on its accuracy and errors
		calibratorAlg.printStatistics();

		// save results to a file and print out
		UtilIO.saveXML(stereoCalib, "stereo.xml");
		stereoCalib.print();

		// Note that the stereo baseline translation will be specified in the same units as the calibration grid.
		// Which is in millimeters (mm) in this example.
	}

	public static void main( String args[] ) {
		ExampleCalibrateStereoPlanar alg = new ExampleCalibrateStereoPlanar();

		// Select which set of targets to use
		alg.setupBumblebeeChess();
//		alg.setupBumblebeeSquare();

		// compute and save results
		alg.process();
	}
}
