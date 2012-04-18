/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.alg.geo.calibration.FactoryPlanarCalibrationTarget;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.app.CalibrateMonoPlanar;
import boofcv.app.PlanarCalibrationDetector;
import boofcv.app.WrapPlanarChessTarget;
import boofcv.app.WrapPlanarGridTarget;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * <p>
 * Example of how to calibrate a single (monocular) camera using a planar calibration grid.  The intrinsic camera
 * parameters and lens distortion are estimated.  Both square grid and chessboard targets are demonstrated by this
 * example. See calibration tutorial for a discussion of different target types and how to collect good calibration
 * images.
 * </p>
 *
 * <<p>
 * All the image processing and calibration is taken care of inside of {@link CalibrateMonoPlanar}.  The code below
 * loads calibration images as inputs, calibrates, and saves results to an XML file.  See in code comments for tuning
 * and implementation issues.
 * </p>
 *
 * @see CalibrateMonoPlanar
 *
 * @author Peter Abeles
 */
public class ExampleCalibrateMonocularPlanar {

	// Detects the target and calibration point inside the target
	PlanarCalibrationDetector detector;

	// Description of the target's physical dimension
	PlanarCalibrationTarget target;

	// List of calibration images
	List<String> images;

	// Most computer images are in a left handed coordinate system.  If set to true it will be changed into
	// a right handed coordinate system to make processing with 3D vision algorithms easier.  Not always needed
	// for processing monocular images.
	boolean isLeftHanded;

	/**
	 * Images from Zhang's website.  Square grid pattern.
	 */
	private void setupZhang99() {
		// Use the wrapper below for square grid targets.
		detector = new WrapPlanarGridTarget(8,8);

		// physical description
		target = FactoryPlanarCalibrationTarget.gridSquare(8, 8, 0.5, 7.0 / 18.0);

		// load image list
		String directory = "../data/evaluation/calibration/mono/PULNiX_CCD_6mm_Zhang";
		images = BoofMiscOps.directoryList(directory,"CalibIm");

		// standard image format
		isLeftHanded = true;
	}

	/**
	 * Images collected from a Bumblee Bee stereo camera.  Large amounts of radial distortion. Chessboard pattern.
	 */
	private void setupBumbleBee() {
		// Use the wrapper below for chessboard targets.  The last parameter adjusts the size of the corner detection
		// region.  TUNE THIS PARAMETER FOR OPTIMAL ACCURACY!
		detector = new WrapPlanarChessTarget(3,4,6);

		// physical description
		target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);

		// load image list
		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";
		images = BoofMiscOps.directoryList(directory,"left");

		// standard image format
		isLeftHanded = true;
	}

	/**
	 * Process calibration images, compute intrinsic parameters, save to a file
	 */
	public void process() {

		// Declare and setup the calibration algorithm
		CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector,isLeftHanded);

		// tell it type type of target and which parameters to estimate
		calibrationAlg.configure(target, true, 2);

		for( String n : images ) {
			BufferedImage input = UtilImageIO.loadImage(n);
			if( n != null ) {
				ImageFloat32 image = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);
				calibrationAlg.addImage(image);
			}
		}
		// process and compute intrinsic parameters
		IntrinsicParameters intrinsic = calibrationAlg.process();

		// save results to a file and print out
		BoofMiscOps.saveXML(intrinsic, "intrinsic.xml");

		calibrationAlg.printStatistics();
		System.out.println();
		System.out.println("--- Intrinsic Parameters ---");
		System.out.println();
		intrinsic.print();
	}


	public static void main( String args[] ) {
		ExampleCalibrateMonocularPlanar alg = new ExampleCalibrateMonocularPlanar();

		// which target should it process
//		alg.setupZhang99();
		alg.setupBumbleBee();

		// compute and save results
		alg.process();
	}
}
