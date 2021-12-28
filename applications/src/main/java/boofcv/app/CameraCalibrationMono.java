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

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.alg.fiducial.calib.ConfigCalibrationTarget;
import boofcv.app.calib.AssistedCalibration;
import boofcv.app.calib.AssistedCalibrationGui;
import boofcv.demonstrations.calibration.CalibrateMonocularPlanarApp;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.controls.CalibrationModelPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraModelType;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import com.github.sarxos.webcam.Webcam;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static boofcv.app.calib.AssistedCalibration.IMAGE_DIRECTORY;
import static boofcv.app.calib.AssistedCalibration.OUTPUT_DIRECTORY;

/**
 * Application for easily calibrating a webcam using a live stream
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CameraCalibrationMono extends BaseStandardInputApp {

	protected String inputPattern;
	protected String outputFilePath = "intrinsic.yaml";
	protected boolean zeroSkew = true;
	protected int numRadial = 2;
	protected boolean tangential = false;
	protected CameraModelType modeType = CameraModelType.BROWN;
	protected FormatType formatType = FormatType.BOOFCV;

	protected ConfigCalibrationTarget configTarget = new ConfigCalibrationTarget();

	// parameters for Kannala-Brandt
	protected int kbNumSymmetric = 5;
	protected int kbNumAsymmetric = 0;

	protected boolean GUI = false;
	protected boolean saveLandmarks = false;
	protected boolean justDetect = false;
	protected boolean verbose = false;

	public void printHelp() {
		System.out.println("./application <Input Options> <Calibration Parameters> <Fiducial Type> <Fiducial Specific Options> ");
		System.out.println();
		System.out.println("  Used for calibrating a single camera and/or saving detected landmarks on calibration targets to disk.");
		System.out.println("  When calibrating, a variety of different fiducial/target types are available as well as");
		System.out.println("  different lens models. The detected landmarks (e.g. pixel coordinates of chessboard corners)");
		System.out.println("  can be optionally saved.");
		System.out.println();
		System.out.println("  --GUI                              Turns on GUI mode and ignores other options.");
		System.out.println("  --Verbose                          Verbose print to stdout.");
		System.out.println();
		System.out.println("  --Output=<path>                    file name for output");
		System.out.println("                                     DEFAULT: \"" + outputFilePath + "\"");
		System.out.println();
		System.out.println("Input: File Options:  ");
		System.out.println();
		System.out.println("  --Input=<path>                     Directory or glob pattern or regex pattern.");
		System.out.println("                                     Glob example: 'glob:data/**/left*.jpg'");
		System.out.println("                                     Regex example: 'regex:data/\\w+/left\\d+.jpg'");
		System.out.println("                                     If not a pattern then it's assumed to be a path. All files");
		System.out.println("                                     with known image extensions in their name as added,");
		System.out.println("                                     e.g. jpg, png");
		System.out.println();
		System.out.println("Input: Webcam Options:  ");
		System.out.println();
		System.out.println("  --Camera=<int|String>              Opens the specified camera using WebcamCapture ID");
		System.out.println("                                     or device string.");
		System.out.println("  --Resolution=<width>:<height>      Specifies camera image resolution.");
		System.out.println();
		System.out.println("Output Options");
		System.out.println();
		System.out.println("  --Format=<string>                  Format of output calibration file.");
		System.out.println("                                     ( boofcv, opencv )");
		System.out.println("                                     DEFAULT: " + formatType);
		System.out.println("  --SaveLandmarks                    Save detected landmarks on calibration target");
		System.out.println("                                     in a directory with the same base name as output file");
		System.out.println("  --JustDetect                       Detect targets and save landmarks");
		System.out.println();
		System.out.println("Calibration Parameters:");
		System.out.println();
		System.out.println("  --Model=<string>                   Specifies the camera model to use.");
		System.out.println("                                     ( brown, universal, kannala_brandt )");
		System.out.println("                                     DEFAULT: " + modeType);
		System.out.println("  --ZeroSkew=<true/false>            Can it assume zero skew?");
		System.out.println("                                     DEFAULT: " + zeroSkew);
		System.out.println("  --NumRadial=<int>                  Number of radial coefficients");
		System.out.println("                                     DEFAULT: " + numRadial);
		System.out.println("  --Tangential=<true/false>          Should it include tangential terms?");
		System.out.println("                                     DEFAULT: " + tangential);
		System.out.println("  --KbSymmetric=<int>                Symmetric coeffients for Kannala-Brandt");
		System.out.println("                                     DEFAULT: " + kbNumSymmetric);
		System.out.println("  --KbASymmetric=<int>               Asymmetric coeffients for Kannala-Brandt");
		System.out.println("                                     DEFAULT: " + kbNumAsymmetric);
		System.out.println();
		System.out.println("Fiducial Types:");
		System.out.println("   ECOCHECK");
		System.out.println("   CHESSBOARD");
		System.out.println("   SQUAREGRID");
		System.out.println("   CIRCLE_HEX");
		System.out.println("   CIRCLE_REG");
		System.out.println();
		System.out.println("Flags for ECOCHECK:");
		System.out.println("  --Format=<String>                  Format in shorthand. E.g. 9x7e3n1");
		System.out.println();
		System.out.println("Flags for CHESSBOARD:");
		System.out.println("  --Grid=<rows>:<columns>            Specifies number of rows and columns.");
		System.out.println("                                     Unlike OpenCV, count squares not inner corners.");
		System.out.println();
		System.out.println("Flags for SQUAREGRID:");
		System.out.println("  --Grid=<rows>:<columns>            Specifies number of rows and columns");
		System.out.println("  --SquareSpace=<square>:<space>     Specifies side of a square and space between square");
		System.out.println("                                     Only the ratio matters.");
		System.out.println("                                     Default: 1:1 square = 1 and space = 1");
		System.out.println();
		System.out.println("Flags for CIRCLE_HEX:");
		System.out.println("  --Grid=<rows>:<columns>            Specifies number of rows and columns");
		System.out.println("  --CenterDistance=<length>          Distance between circle centers");
		System.out.println("  --Diameter=<length>                Diameter of a circle");
		System.out.println();
		System.out.println("Flags for CIRCLE_REG:");
		System.out.println("  --Grid=<rows>:<columns>            Specifies number of rows and columns");
		System.out.println("  --CenterDistance=<length>          Specifies how far apart the center of two circles are along an axis");
		System.out.println("  --Diameter=<length>                Diameter of a circle");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  --GUI");
		System.out.println("          Opens a GUI and let's you do all the configurations there");
		System.out.println("  --Input=path/to/input/ --Model=brown CHESSBOARD --Grid=7:5");
		System.out.println("          Calibrates using a brown model and a chessboard target with a 7x5 grid.");
		System.out.println("  --Visualize --SaveLandmarks --Input=path/to/input/ CHESSBOARD --Grid=7:5");
		System.out.println("          The same, but saves the corners and visualizes the results.");
		System.out.println("  \"--Input=glob:stereo_data/left**.jpg\" --Model=brown CHESSBOARD --Grid=7:5");
		System.out.println("          Uses a glob pattern to find all left images in a directory");
		System.out.println();
	}

	public void parse( String[] args ) {
		if (args.length < 1) {
			throw new RuntimeException("Must specify some arguments");
		}

		configTarget.type = null;

		cameraId = -1; // override default value of zero so that its easy to tell if a camera was slected
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.startsWith("--")) {
				if (arg.compareToIgnoreCase("--GUI") == 0) {
					GUI = true;
				} else if (arg.compareToIgnoreCase("--Verbose") == 0) {
					verbose = true;
				} else if (arg.compareToIgnoreCase("--SaveLandmarks") == 0) {
					saveLandmarks = true;
				} else if (arg.compareToIgnoreCase("--JustDetect") == 0) {
					saveLandmarks = true;
					justDetect = true;
				} else if (arg.toLowerCase().startsWith("--output")) {
					splitFlag(arg);
					outputFilePath = BoofMiscOps.handlePathTilde(parameters);
				} else if (!checkCameraFlag(arg)) {
					splitFlag(arg);
					if (flagName.compareToIgnoreCase("Input") == 0) {
						inputPattern = BoofMiscOps.handlePathTilde(parameters);
						inputType = InputType.IMAGE;
					} else if (flagName.compareToIgnoreCase("Model") == 0) {
						if (parameters.compareToIgnoreCase("pinhole") == 0) {
							modeType = CameraModelType.BROWN;
						} else if (parameters.compareToIgnoreCase("universal") == 0) {
							modeType = CameraModelType.UNIVERSAL;
						} else if (parameters.toLowerCase().startsWith("kannala")) {
							modeType = CameraModelType.KANNALA_BRANDT;
						} else {
							throw new RuntimeException("Unknown model type " + parameters);
						}
					} else if (flagName.compareToIgnoreCase("Format") == 0) {
						if (parameters.compareToIgnoreCase("boofcv") == 0) {
							formatType = FormatType.BOOFCV;
						} else if (parameters.compareToIgnoreCase("opencv") == 0) {
							formatType = FormatType.OPENCV;
						} else {
							throw new RuntimeException("Unknown model type " + parameters);
						}
					} else if (flagName.compareToIgnoreCase("ZeroSkew") == 0) {
						zeroSkew = Boolean.parseBoolean(parameters);
					} else if (flagName.compareToIgnoreCase("NumRadial") == 0) {
						numRadial = Integer.parseInt(parameters);
					} else if (flagName.compareToIgnoreCase("Tangential") == 0) {
						tangential = Boolean.parseBoolean(parameters);
					} else if (flagName.compareToIgnoreCase("KbSymmetric") == 0) {
						kbNumSymmetric = Integer.parseInt(parameters);
					} else if (flagName.compareToIgnoreCase("KbASymmetric") == 0) {
						kbNumAsymmetric = Integer.parseInt(parameters);
					} else {
						throw new RuntimeException("Unknown input option " + flagName);
					}
				}
			} else if (arg.compareToIgnoreCase("ECOCHECK") == 0) {
				parseECoCheck(i + 1, args);
				break;
			} else if (arg.compareToIgnoreCase("CHESSBOARD") == 0) {
				parseChessboard(i + 1, args);
				break;
			} else if (arg.compareToIgnoreCase("SQUAREGRID") == 0) {
				parseSquareGrid(i + 1, args);
				break;
			} else if (arg.compareToIgnoreCase("CIRCLE_HEX") == 0) {
				parseCircle(i + 1, args, true);
				break;
			} else if (arg.compareToIgnoreCase("CIRCLE_REG") == 0) {
				parseCircle(i + 1, args, false);
				break;
			} else {
				throw new RuntimeException("Unknown fiducial type " + arg);
			}
		}

		if (formatType == FormatType.OPENCV && modeType != CameraModelType.BROWN) {
			throw new RuntimeException("Can only save calibration in OpenCV format if pinhole model");
		}
	}

	protected void parseECoCheck( int index, String[] args ) {
		configTarget.type = CalibrationPatterns.ECOCHECK;
		String format = null;

		for (; index < args.length; index++) {
			String arg = args[index];

			if (!arg.startsWith("--")) {
				throw new RuntimeException("Expected flags for chessboard. Should start with '--'");
			}

			splitFlag(arg);
			if (flagName.compareToIgnoreCase("Format") == 0) {
				format = parameters;
			} else {
				throw new RuntimeException("Unknown image option " + flagName);
			}
		}

		if (format == null) {
			throw new RuntimeException("You must specify the ECoCheck marker using short format, e.g. 9x7e3n1");
		}

		if (verbose)
			System.out.println("ecocheck: " + format);

		configTarget.ecocheck = ConfigECoCheckMarkers.parse(format, 1.0);
	}

	protected void parseChessboard( int index, String[] args ) {
		configTarget.type = CalibrationPatterns.CHESSBOARD;
		int numRows = 0, numColumns = 0;

		for (; index < args.length; index++) {
			String arg = args[index];

			if (!arg.startsWith("--")) {
				throw new RuntimeException("Expected flags for chessboard. Should start with '--'");
			}

			splitFlag(arg);
			if (flagName.compareToIgnoreCase("Grid") == 0) {
				String[] words = parameters.split(":");
				if (words.length != 2) throw new RuntimeException("Expected two values for rows and columns");
				numRows = Integer.parseInt(words[0]);
				numColumns = Integer.parseInt(words[1]);
			} else {
				throw new RuntimeException("Unknown image option " + flagName);
			}
		}

		if (numRows <= 0 || numColumns <= 0) {
			throw new RuntimeException("Rows and columns must be specified and > 0");
		}

		if (verbose)
			System.out.println("chessboard: " + numRows + " x " + numColumns);

		configTarget.grid.setTo(numRows, numColumns, 1, -1);
	}

	protected void parseSquareGrid( int index, String[] args ) {
		configTarget.type = CalibrationPatterns.SQUARE_GRID;
		int numRows = 0, numColumns = 0;
		double square = 1, space = 1;

		for (; index < args.length; index++) {
			String arg = args[index];

			if (!arg.startsWith("--")) {
				throw new RuntimeException("Expected flags for square grid. Should start with '--'");
			}

			splitFlag(arg);
			if (flagName.compareToIgnoreCase("Grid") == 0) {
				String[] words = parameters.split(":");
				if (words.length != 2) throw new RuntimeException("Expected two values for rows and columns");
				numRows = Integer.parseInt(words[0]);
				numColumns = Integer.parseInt(words[1]);
			} else if (flagName.compareToIgnoreCase("SquareSpace") == 0) {
				String[] words = parameters.split(":");
				if (words.length != 2) throw new RuntimeException("Expected two values for square and space");
				square = Double.parseDouble(words[0]);
				space = Double.parseDouble(words[1]);
			} else {
				throw new RuntimeException("Unknown image option " + flagName);
			}
		}

		if (numRows <= 0 || numColumns <= 0) {
			throw new RuntimeException("Rows and columns must be specified and > 0");
		}
		if (square <= 0 || space <= 0) {
			throw new RuntimeException("square and space width must be specified and > 0");
		}

		if (verbose)
			System.out.println("squaregrid: " + numRows + " x " + numColumns + " square/space = " + (square/space));

		configTarget.grid.setTo(numRows, numColumns, square, space);
	}

	protected void parseCircle( int index, String[] args, boolean hexagonal ) {
		int numRows = 0, numColumns = 0;
		double diameter = -1, centerDistance = -1;

		for (; index < args.length; index++) {
			String arg = args[index];

			if (!arg.startsWith("--")) {
				throw new RuntimeException("Expected flags for radius grid. Should start with '--'");
			}

			splitFlag(arg);
			if (flagName.compareToIgnoreCase("Grid") == 0) {
				String[] words = parameters.split(":");
				if (words.length != 2) throw new RuntimeException("Expected two values for rows and columns");
				numRows = Integer.parseInt(words[0]);
				numColumns = Integer.parseInt(words[1]);
			} else if (flagName.compareToIgnoreCase("CenterDistance") == 0) {
				centerDistance = Double.parseDouble(parameters);
			} else if (flagName.compareToIgnoreCase("Diameter") == 0) {
				diameter = Double.parseDouble(parameters);
			} else {
				throw new RuntimeException("Unknown image option " + flagName);
			}
		}

		if (numRows <= 0 || numColumns <= 0) {
			throw new RuntimeException("Rows and columns must be specified and > 0");
		}
		if (diameter <= 0 || centerDistance <= 0) {
			throw new RuntimeException("diameter and center distance must be specified and > 0");
		}

		if (hexagonal) {
			System.out.println("circle hexagonal: " + numRows + " x " + numColumns + " diameter = " + diameter + " center distance = " + centerDistance);
			configTarget.type = CalibrationPatterns.CIRCLE_HEXAGONAL;
			configTarget.grid.setTo(numRows, numColumns, diameter, centerDistance);
		} else {
			System.out.println("circle regular: " + numRows + " x " + numColumns + " diameter = " + diameter + " center distance = " + centerDistance);
			configTarget.type = CalibrationPatterns.CIRCLE_GRID;
			configTarget.grid.setTo(numRows, numColumns, diameter, centerDistance);
		}
	}

	public void process() {
		// If a GUI was requested just launch the GUI app
		if (GUI) {
			launchCalibrationApp(new ArrayList<>());
			return;
		}

		switch (inputType) {
			case VIDEO -> throw new RuntimeException("Calibration from video not supported");
			case IMAGE -> handleDirectory();
			case WEBCAM -> handleWebcam();
			default -> {
				printHelp();
				System.out.println();
				System.err.println("Input method is not specified");
			}
		}
	}

	// TODO break this function up into its own class so that it isn't one massive function?
	protected void handleDirectory() {
		File outputDirectory = null;
		PrintStream summaryDetection = null;

		List<String> imagePath = UtilIO.listSmartImages(inputPattern, true);

		if (imagePath.isEmpty()) {
			System.err.println("No images found. Check path, glob, or regex pattern");
			System.err.println("  " + inputPattern);
			System.exit(1);
			return;
		}

		if (verbose)
			System.out.println("Total images found: " + imagePath.size());

		if (GUI) {
			launchCalibrationApp(imagePath);
			return;
		}

		final DetectSingleFiducialCalibration detector = FactoryFiducialCalibration.genericSingle(configTarget);
		final CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector.getLayout());

		switch (modeType) {
			case BROWN -> calibrationAlg.configurePinhole(zeroSkew, numRadial, tangential);
			case UNIVERSAL -> calibrationAlg.configureUniversalOmni(zeroSkew, numRadial, tangential);
			case KANNALA_BRANDT -> calibrationAlg.configureKannalaBrandt(zeroSkew, kbNumSymmetric, kbNumAsymmetric);
			default -> throw new RuntimeException("Unknown model type: " + modeType);
		}

		// If configured to do so, create directory to store more verbose information
		if (saveLandmarks) {
			String baseName = FilenameUtils.getBaseName(outputFilePath);
			baseName += "_landmarks";
			String name = baseName;
			// keep on trying to create the output directory until it succeeds
			for (int i = 0; i < 10_000; i++) {
				outputDirectory = new File(new File(outputFilePath).getParent(), name);
				if (outputDirectory.exists()) {
					name = baseName + i;
				}
			}
			Objects.requireNonNull(outputDirectory);
			BoofMiscOps.checkTrue(outputDirectory.mkdirs());
			if (verbose)
				System.out.println("Saving landmarks to " + outputDirectory.getPath());

			try {
				summaryDetection = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(outputDirectory, "summary_detection.csv"))));
				summaryDetection.println("# Summary of success or failure for each image it processed");
				summaryDetection.println("# (file name),(true = successful, false = failed)");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		final List<File> imagesSuccess = new ArrayList<>();
		final List<File> imagesFailed = new ArrayList<>();

		for (String path : imagePath) {
			File f = new File(path);
			if (f.isDirectory() || f.isHidden())
				continue;

			final BufferedImage buffered = UtilImageIO.loadImage(path);
			if (buffered == null) {
				System.err.println("Failed to open 'image' file: " + path);
				continue;
			}

			GrayF32 image = ConvertBufferedImage.convertFrom(buffered, (GrayF32)null);

			if (detector.process(image)) {
				imagesSuccess.add(f);
				if (summaryDetection != null)
					summaryDetection.println(f.getPath() + ",true");
				// if configured to do so, save the landmarks to disk
				if (outputDirectory != null) {
					CalibrationIO.saveLandmarksCsv(f.getPath(), detector.getClass().getSimpleName(),
							detector.getDetectedPoints(),
							new File(outputDirectory, FilenameUtils.getBaseName(f.getName()) + ".csv"));
				}
				calibrationAlg.addImage(detector.getDetectedPoints());
				if (verbose)
					System.out.println("  Detection successful " + f.getPath());
			} else {
				imagesFailed.add(f);
				if (summaryDetection != null)
					summaryDetection.println(f.getPath() + ",false");
				if (verbose)
					System.out.println("  Detection FAILED " + f.getPath());
			}
		}

		// Done detecting targets so close this file
		if (summaryDetection != null)
			summaryDetection.close();

		if (verbose) {
			System.out.println("Detected targets in " + imagesSuccess.size() +
					" / " + (imagesFailed.size() + imagesSuccess.size()) + " images");
		}

		if (justDetect) {
			if (verbose)
				System.out.println("Just detecting calibration targets! Exiting now");
			return;
		}

		// process and compute intrinsic parameters
		try {
			final CameraModel intrinsic = calibrationAlg.process();

			// Save calibration statistics to disk
			if (outputDirectory != null) {
				try {
					PrintStream out = new PrintStream(new File(outputDirectory, "stats.txt"));
					calibrationAlg.printStatistics(out);
					out.close();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			if (verbose) {
				calibrationAlg.printStatistics(System.out);
				System.out.println();
				System.out.println("--- " + modeType + " Parameters ---");
				System.out.println();
			}

			if (modeType == CameraModelType.BROWN) {
				CameraPinholeBrown m = (CameraPinholeBrown)intrinsic;
				switch (formatType) {
					case BOOFCV -> CalibrationIO.save(m, outputFilePath);
					case OPENCV -> CalibrationIO.saveOpencv(m, outputFilePath);
					default -> throw new IllegalArgumentException("Unknown format");
				}
			} else {
				CalibrationIO.save(intrinsic, outputFilePath);
			}
			if (verbose) intrinsic.print();

			if (verbose) {
				System.out.println();
				System.out.println("Save file format: " + formatType);
				System.out.println("Destination:      " + outputFilePath);
				System.out.println();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void launchCalibrationApp( List<String> imagePath ) {
		SwingUtilities.invokeLater(() -> {
			var app = new CalibrateMonocularPlanarApp();
			// Configure the application using the commandline arguments
			if (configTarget.type != null)
				app.getConfigurePanel().getTargetPanel().setConfigurationTo(configTarget);
			CalibrationModelPanel modelPanel = app.getConfigurePanel().getModelPanel();
			switch (modeType) {
				case BROWN -> modelPanel.setToBrown(zeroSkew, numRadial, tangential);
				case UNIVERSAL -> modelPanel.setToUniversal(zeroSkew, numRadial, tangential);
				case KANNALA_BRANDT -> modelPanel.setToKannalaBrandt(zeroSkew, kbNumSymmetric, kbNumAsymmetric);
			}

			app.window = ShowImages.showWindow(app, "Monocular Planar Calibration", true);
			app.window.setJMenuBar(app.menuBar);

			if (!imagePath.isEmpty()) {
				// it needs a directory. images could be in multiple directories, this just picks the first one
				File directory = new File(imagePath.get(0)).getParentFile();

				// start processing!
				new Thread(() -> app.processImages(directory, imagePath)).start();
			}
		});
	}

	/**
	 * Captures calibration data live using a webcam and a GUI to assist the user
	 */
	public void handleWebcam() {
		// Webcam can only be processed in GUI mode
		GUI = true;

		final Webcam webcam = openSelectedCamera();
		if (desiredWidth > 0 && desiredHeight > 0)
			UtilWebcamCapture.adjustResolution(webcam, desiredWidth, desiredHeight);

		webcam.open();

		// close the webcam gracefully on exit
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (webcam.isOpen()) {
				System.out.println("Closing webcam");
				webcam.close();
			}
		}));

		var gui = new AssistedCalibrationGui(webcam.getViewSize());
		JFrame frame = ShowImages.showWindow(gui, "Webcam Calibration", true);

		GrayF32 gray = new GrayF32(webcam.getViewSize().width, webcam.getViewSize().height);

		if (desiredWidth > 0 && desiredHeight > 0) {
			if (gray.width != desiredWidth || gray.height != desiredHeight)
				System.err.println("Actual camera resolution does not match desired. Actual: " + gray.width + " " + gray.height +
						"  Desired: " + desiredWidth + " " + desiredHeight);
		}

		var assisted = new AssistedCalibration(gui, OUTPUT_DIRECTORY, IMAGE_DIRECTORY);

		// If the user specified a target type use that as the default
		if (configTarget.type != null)
			SwingUtilities.invokeLater(() -> assisted.gui.getTargetPanel().configPanel.setConfigurationTo(configTarget));

		assisted.init(gray.width, gray.height);

		BufferedImage image;
		while ((image = webcam.getImage()) != null && !assisted.isFinished()) {
			ConvertBufferedImage.convertFrom(image, gray);

			try {
				assisted.process(gray, image);
			} catch (RuntimeException e) {
				e.printStackTrace();
				System.err.println("BUG!!! saving image to crash_image.png");
				UtilImageIO.saveImage(image, "crash_image.png");
				throw e;
			}
		}
		webcam.close();
		if (assisted.isFinished()) {
			frame.setVisible(false);

			// Update target description to whatever the user selected
			assisted.gui.getTargetPanel().configPanel.updateConfig(configTarget);

			inputPattern = new File(OUTPUT_DIRECTORY, IMAGE_DIRECTORY).getPath();
			outputFilePath = new File(OUTPUT_DIRECTORY, "intrinsic.yaml").getPath();
			handleDirectory();
		}
	}

	public enum FormatType {
		BOOFCV,
		OPENCV
	}

	public static void main( String[] args ) {
		var app = new CameraCalibrationMono();
		boolean failed = true;
		try {
			if (args.length > 0) {
				app.parse(args);
				app.process();
				failed = false;
			}
		} catch (RuntimeException e) {
			System.err.println();
			System.err.println(e.getMessage());
			System.exit(1);
		} finally {
			if (failed) {
				app.printHelp();
				System.exit(1);
			}
		}
	}
}
