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

package boofcv.app;

import boofcv.BoofVerbose;
import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.geo.calibration.CalibrateStereoPlanar;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.fiducial.calib.ConfigCalibrationTarget;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.demonstrations.calibration.CalibrateStereoPlanarApp;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.calibration.StereoImageSet;
import boofcv.gui.calibration.StereoImageSetList;
import boofcv.gui.calibration.StereoImageSetListSplit;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Camera calibration for stereo cameras using planar targets.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CameraCalibrationStereo {
	@Option(name = "-m", aliases = {"--Marker"}, usage = "Specifies which marker it should detect.")
	public String markerType = CalibrationPatterns.CHESSBOARD.name();

	@Option(name = "--Rows", usage = "Number of rows in the marker")
	public int markerRows = -1;

	@Option(name = "--Cols", usage = "Number of columns in the marker")
	public int markerCols = -1;

	@Option(name = "--ShapeSize", usage = "Size of an individual square or circle in the marker.")
	public double shapeSize = -1;

	@Option(name = "--Spacing", usage = "Spacing between shapes, if applicable.")
	public double shapeSpacing = -1;

	@Option(name = "--ECoCheck", usage = "Specifies short hand configuration string for ECoCheck markers. " +
			"Implicitly sets marker type to ECoCheck. Example: 9x7n1 for a 9x7 grid with default ECC.")
	public String ecocheckShorthand = "";

	@Option(name = "--ZeroSkew", usage = "Assume zero skew in camera model")
	protected boolean zeroSkew = true;

	@Option(name = "--Radial", usage = "Number of radial camera parameters")
	protected int numRadial = 4;

	@Option(name = "--Tangential", usage = "If it should include tangential distortion in camera model.")
	protected boolean tangential = false;

	@Option(name = "--GUI", usage = "Launch a GUI application")
	protected boolean guiMode = false;

	@Option(name = "--SaveLandmarks", usage = "Save detected landmarks")
	protected boolean saveLandmarks = false;

	@Option(name = "--Verbose", usage = "Verbose printing to stdout")
	protected boolean verbose = false;

	@Option(name = "--VerboseDebug", usage = "Print debug information to stdout")
	protected boolean verboseDebug = false;

	@Option(name = "--MarkerPath", usage = "Specifies where a marker configuration file is stored")
	public String markerPath = "";

	@Option(name = "-o", aliases = {"--OutputPath"}, usage = "Where found calibration should be saved. If empty input directory will be used.")
	public String outputPath = "";

	@Option(name = "-i", aliases = {"--Input"}, usage = """
			Fused images. Directory or glob pattern or regex pattern.
			Glob example: 'glob:data/**/frame*.jpg'
			Regex example: 'regex:data/\\w+/frame\\d+.jpg'
			If not a pattern then it's assumed to be a path. All files with known image extensions in their name as added, e.g. jpg, png""")
	public String inputSingle = "";

	@Option(name = "-l", aliases = {"--Left"}, usage = """
			Left images. Directory or glob pattern or regex pattern.
			Glob example: 'glob:data/**/left*.jpg'
			Regex example: 'regex:data/\\w+/left\\d+.jpg'
			If not a pattern then it's assumed to be a path. All files with known image extensions in their name as added, e.g. jpg, png""")
	public String inputLeft = "";

	@Option(name = "-r", aliases = {"--Right"}, usage = """
			Right images. Directory or glob pattern or regex pattern.
			Glob example: 'glob:data/**/right*.jpg'
			Regex example: 'regex:data/\\w+/right\\d+.jpg'
			If not a pattern then it's assumed to be a path. All files with known image extensions in their name as added, e.g. jpg, png""")
	public String inputRight = "";

	ConfigCalibrationTarget configTarget = new ConfigCalibrationTarget();

	boolean targetValid = false;

	void finishParsing() {
		if (!markerPath.isEmpty()) {
			configTarget = UtilIO.loadConfig(new File(markerPath));
		} else {
			parseTargetFromCommandLine();
		}

		if (numRadial < 0) {
			System.err.println("Radial parameters must be positive. " + numRadial);
			System.exit(1);
		}
	}

	private void parseTargetFromCommandLine() {
		if (!ecocheckShorthand.isEmpty()) {
			if (shapeSize <= 0) {
				System.err.println("Must specify the shape's size. size=" + shapeSize);
				System.exit(1);
			}
			configTarget.type = CalibrationPatterns.ECOCHECK;
			configTarget.ecocheck = ConfigECoCheckMarkers.parse(ecocheckShorthand, shapeSize);
		} else {
			try {
				configTarget.type = CalibrationPatterns.valueOf(markerType);
			} catch (IllegalArgumentException e) {
				System.err.println("Unknown marker type: " + markerType);
				for (CalibrationPatterns p : CalibrationPatterns.values()) {
					System.err.println(" " + p.name());
				}
				System.exit(1);
			}

			configTarget.grid.numRows = markerRows;
			configTarget.grid.numCols = markerCols;
			configTarget.grid.shapeSize = shapeSize;
			configTarget.grid.shapeDistance = shapeSpacing;
		}

		// Skip this test in GUI mode since the user can change settings later on
		try {
			configTarget.checkValidity();
			targetValid = true;
		} catch (RuntimeException e) {
			if (!guiMode) {
				System.err.println("Invalid target configuration.");
				System.exit(1);
			}
		}
	}

	/**
	 * Launches a GUI application. Copies over command line arguments as default settings in GUI.
	 */
	void launchGUI() {
		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateStereoPlanarApp();

			// pass in command line arguments
			if (targetValid)
				app.getConfigurePanel().getTargetPanel().setConfigurationTo(configTarget);

			app.window = ShowImages.showWindow(app, "Planar Stereo Calibration", true);
			app.window.setJMenuBar(app.menuBar);

			if (!inputSingle.isEmpty()) {
				app.setMenuBarEnabled(false);
				List<String> listImages = UtilIO.listSmartImages(inputSingle, true);
				int splitX = determineSplitX(listImages);
				new Thread(() -> app.process(listImages, splitX)).start();
			} else if (!inputLeft.isEmpty() && !inputRight.isEmpty()) {
				app.setMenuBarEnabled(false);
				List<String> listLeft = UtilIO.listSmartImages(inputLeft, true);
				List<String> listRight = UtilIO.listSmartImages(inputRight, true);
				new Thread(() -> app.process(listLeft, listRight)).start();
			}
		});
	}

	/**
	 * Loads images and computes stereo calibration
	 */
	void process() {
		// Create detector and calibrator from configurations
		DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.genericSingle(configTarget);
		CalibrateStereoPlanar calibrator = new CalibrateStereoPlanar(detector.getLayout());
		if (verboseDebug) {
			calibrator.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
		}

		calibrator.configure(zeroSkew, numRadial, tangential);

		// Load paths to images
		String inputDir = null;
		StereoImageSet stereoImages;
		if (!inputSingle.isEmpty()) {
			List<String> listImages = UtilIO.listSmartImages(inputSingle, true);
			int splitX = determineSplitX(listImages);
			stereoImages = new StereoImageSetListSplit(listImages, splitX);
			if (!listImages.isEmpty())
				inputDir = new File(listImages.get(0)).getParent();
		} else {
			if (inputLeft.isEmpty() || inputRight.isEmpty()) {
				System.err.println("Left and right images need to be specified");
				System.exit(1);
			}
			List<String> listLeft = UtilIO.listSmartImages(inputLeft, true);
			List<String> listRight = UtilIO.listSmartImages(inputRight, true);
			if (listLeft.size() != listRight.size()) {
				System.err.println("Number of left and right images do not match. " +
						listLeft.size() + " vs " + listRight.size());
				System.exit(1);
			}
			stereoImages = new StereoImageSetList(listLeft, listRight);
			if (!listLeft.isEmpty())
				inputDir = new File(listLeft.get(0)).getParent();
		}
		if (stereoImages.size() == 0) {
			System.err.println("No input images found");
			System.exit(1);
		}

		// If not specified use input directory as output directory
		if (outputPath.isEmpty()) {
			outputPath = new File(Objects.requireNonNull(inputDir), "stereo.yaml").getPath();
		}

		// Detect markers in images and pass to calibrator
		if (verbose) System.out.println("total pairs: " + stereoImages.size());
		GrayF32 image = new GrayF32(1, 1);
		File landmarksPath = new File(new File(outputPath).getParentFile(), "landmarks");
		String detectorName = detector.getClass().getSimpleName();

		if (saveLandmarks && !landmarksPath.exists()) {
			if (verbose) System.out.println("Saving landmarks to " + landmarksPath.getPath());
			BoofMiscOps.checkTrue(landmarksPath.mkdirs());
		}

		for (int frame = 0; frame < stereoImages.size(); frame++) {
			stereoImages.setSelected(frame);
			BufferedImage buffLeft = stereoImages.loadLeft();
			BufferedImage buffRight = stereoImages.loadRight();

			CalibrationObservation calibLeft = detectLandmarks(detector, image, buffLeft);
			CalibrationObservation calibRight = detectLandmarks(detector, image, buffRight);

			// One image in the pair needs to be usable

			if (calibLeft.size() > 4 || calibRight.size() > 4)
				calibrator.addPair(calibLeft, calibRight);

			// Save landmarks to disk if configured to do so
			if (saveLandmarks) {
				String leftName = stereoImages.getLeftName();
				String rightName = stereoImages.getRightName();
				CalibrationIO.saveLandmarksCsv(leftName, detectorName, calibLeft, new File(landmarksPath, leftName + ".csv"));
				CalibrationIO.saveLandmarksCsv(rightName, detectorName, calibLeft, new File(landmarksPath, rightName + ".csv"));
			}

			if (verbose && frame%30 == 29)
				System.out.println();
		}
		if (verbose)
			System.out.println();

		// Compute calibration and save results
		StereoParameters stereo = calibrator.process();
		if (verbose)
			calibrator.printStatistics();

		CalibrationIO.save(stereo, outputPath);
	}

	private int determineSplitX( List<String> listImages ) {
		BufferedImage tmp = UtilImageIO.loadImage(listImages.get(0));
		if (tmp == null) {
			System.err.println("Can't determine image size for split images. Failed to load " + listImages.get(0));
			System.exit(1);
			throw new RuntimeException("Stupid null check");
		}
		return tmp.getWidth()/2;
	}

	private CalibrationObservation detectLandmarks( DetectSingleFiducialCalibration detector,
													GrayF32 image, BufferedImage buffLeft ) {
		ConvertBufferedImage.convertFrom(buffLeft, image);
		String dot = detector.process(image) ? "." : "x";
		if (verbose) System.out.print(dot);
		return detector.getDetectedPoints();
	}

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);

		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.out.println("-i /path/to/directory --ECoCheck 9x7n1 --ShapeSize 30 -o stereo.yaml");
		System.out.println("   Splits images in directory. ECoCheck target. Squares of 30 units. Output to stereo.yaml");
		System.out.println("-l /path/to/left -r /path/to/right --ECoCheck 9x7n1 --ShapeSize 30 -o stereo.yaml");
		System.out.println("   Same but specifies left and right images from two directories");
		System.out.println("-i \"regex:path/to/\\w+\\.jpg\" --ECoCheck 9x7n1 --ShapeSize 30 -o stereo.yaml");
		System.out.println("   Finds all files ending with .jpg in 'path/to' directory");
		System.out.println("-i \"glob:path/to/*.jpg\" --ECoCheck 9x7n1 --ShapeSize 30 -o stereo.yaml");
		System.out.println("   Finds all files ending with .jpg in 'path/to' directory");
		System.out.println("--GUI");
		System.out.println("   Launches GUI applications");
		System.out.println("--GUI -l /path/to/left -r /path/to/right --ECoCheck 9x7n1 --ShapeSize 30 -o stereo.yaml");
		System.out.println("   Launches GUI applications with command line arguments");
		System.exit(1);
	}

	public static void main( String[] args ) {
		var calibrator = new CameraCalibrationStereo();
		var parser = new CmdLineParser(calibrator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			calibrator.finishParsing();

			if (calibrator.guiMode) {
				calibrator.launchGUI();
			} else {
				try {
					calibrator.process();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println();
					System.out.println("Failed! See exception above");
				}
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		}
	}
}
