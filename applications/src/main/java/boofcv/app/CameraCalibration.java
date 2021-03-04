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

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.app.calib.AssistedCalibration;
import boofcv.app.calib.AssistedCalibrationGui;
import boofcv.app.calib.ComputeGeometryScore;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.calibration.CalibratedPlanarPanel;
import boofcv.gui.calibration.FisheyePlanarPanel;
import boofcv.gui.calibration.MonoPlanarPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.javacv.UtilOpenCV;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;
import com.github.sarxos.webcam.Webcam;
import org.apache.commons.io.FilenameUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static boofcv.app.calib.AssistedCalibration.IMAGE_DIRECTORY;
import static boofcv.app.calib.AssistedCalibration.OUTPUT_DIRECTORY;

/**
 * Application for easily calibrating a webcam using a live stream
 *
 * @author Peter Abeles
 */
public class CameraCalibration extends BaseStandardInputApp {

	protected String inputPattern;
	protected String outputFileName = "intrinsic.yaml";
	protected DetectorFiducialCalibration detector;
	protected boolean zeroSkew = true;
	protected int numRadial = 2;
	protected boolean tangential = false;
	protected ModelType modeType = ModelType.BROWN;
	protected FormatType formatType = FormatType.BOOFCV;

	protected boolean GUI = false;
	protected boolean visualize = false;
	protected boolean saveLandmarks = false;
	protected boolean justDetect = false;
	protected boolean verbose = false;

	public void printHelp() {
		System.out.println("./application <output file> <Input Options> <Calibration Parameters> <Fiducial Type> <Fiducial Specific Options> ");
		System.out.println();
		System.out.println("  Used for calibrating a single camera and/or saving detected landmarks on calibration targets to disk.");
		System.out.println("  When calibrating, a variety of different fiducial/target types are available as well as");
		System.out.println("  different lens models. The detected landmarks (e.g. pixel coordinates of chessboard corners)");
		System.out.println("  can be optionally saved.");
		System.out.println();
		System.out.println("  --GUI                              Turns on GUI mode and ignores other options.");
		System.out.println("  --Verbose                          Verbose print to stdout.");
		System.out.println();
		System.out.println("<output file>                        file name for output");
		System.out.println("                                     DEFAULT: \"" + outputFileName + "\"");
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
		System.out.println("  --Visualize                        Turns on visualization in a GUI of final results");
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
		System.out.println("                                     ( brown, universal )");
		System.out.println("                                     DEFAULT: " + modeType);
		System.out.println("  --ZeroSkew=<true/false>            Can it assume zero skew?");
		System.out.println("                                     DEFAULT: " + zeroSkew);
		System.out.println("  --NumRadial=<int>                  Number of radial coefficients");
		System.out.println("                                     DEFAULT: " + numRadial);
		System.out.println("  --Tangential=<true/false>          Should it include tangential terms?");
		System.out.println("                                     DEFAULT: " + tangential);
		System.out.println();
		System.out.println("Fiducial Types:");
		System.out.println("   CHESSBOARD");
		System.out.println("   SQUAREGRID");
		System.out.println("   CIRCLE_HEX");
		System.out.println("   CIRCLE_REG");
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
		System.out.println("  java -jar applications.jar CameraCalibration --GUI");
		System.out.println("          Opens a GUI and let's you do all the configurations there");
		System.out.println("  java -jar applications.jar CameraCalibration --Input=path/to/input/ --Model=brown CHESSBOARD --Grid=7:5");
		System.out.println("          Calibrates using a brown model and a chessboard target with a 7x5 grid.");
		System.out.println("  java -jar applications.jar CameraCalibration --Visualize --SaveLandmarks --Input=path/to/input/ CHESSBOARD --Grid=7:5");
		System.out.println("          The same, but saves the corners and visualizes the results.");
		System.out.println("  java -jar applications.jar CameraCalibration \"--Input=glob:stereo_data/left**.jpg\" --Model=brown CHESSBOARD --Grid=7:5");
		System.out.println("          Uses a glob pattern to find all left images in a directory");
		System.out.println();
	}

	public void parse( String[] args ) {
		if (args.length < 1) {
			throw new RuntimeException("Must specify some arguments");
		}

		cameraId = -1; // override default value of zero so that its easy to tell if a camera was slected
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.startsWith("--")) {
				if (arg.compareToIgnoreCase("--GUI") == 0) {
					GUI = true;
				} else if (arg.compareToIgnoreCase("--Verbose") == 0) {
					verbose = true;
				} else if (arg.compareToIgnoreCase("--Visualize") == 0) {
					visualize = true;
				} else if (arg.compareToIgnoreCase("--SaveLandmarks") == 0) {
					saveLandmarks = true;
				} else if (arg.compareToIgnoreCase("--JustDetect") == 0) {
					saveLandmarks = true;
					justDetect = true;
				} else if (!checkCameraFlag(arg)) {
					splitFlag(arg);
					if (flagName.compareToIgnoreCase("Input") == 0) {
						inputPattern = BoofMiscOps.handlePathTilde(parameters);
						inputType = InputType.IMAGE;
					} else if (flagName.compareToIgnoreCase("Model") == 0) {
						if (parameters.compareToIgnoreCase("pinhole") == 0) {
							modeType = ModelType.BROWN;
						} else if (parameters.compareToIgnoreCase("universal") == 0) {
							modeType = ModelType.UNIVERSAL;
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
					} else {
						throw new RuntimeException("Unknown input option " + flagName);
					}
				}
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
			} else if (i == 0) {
				outputFileName = arg;
			} else {
				throw new RuntimeException("Unknown fiducial type " + arg);
			}
		}

		if (formatType == FormatType.OPENCV && modeType != ModelType.BROWN) {
			throw new RuntimeException("Can only save calibration in OpenCV format if pinhole model");
		}
	}

	protected void parseChessboard( int index, String[] args ) {
		int numRows = 0, numColumns = 0;

		for (; index < args.length; index++) {
			String arg = args[index];

			if (!arg.startsWith("--")) {
				throw new RuntimeException("Expected flags for chessboard.  Should start with '--'");
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

		ConfigGridDimen config = new ConfigGridDimen(numRows, numColumns, 1);

		detector = FactoryFiducialCalibration.chessboardX(null, config);
	}

	protected void parseSquareGrid( int index, String[] args ) {
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

		ConfigGridDimen config = new ConfigGridDimen(numRows, numColumns, square, space);

		detector = FactoryFiducialCalibration.squareGrid(null, config);
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
			ConfigGridDimen config = new ConfigGridDimen(numRows, numColumns, diameter, centerDistance);

			detector = FactoryFiducialCalibration.circleHexagonalGrid(null, config);
		} else {
			System.out.println("circle regular: " + numRows + " x " + numColumns + " diameter = " + diameter + " center distance = " + centerDistance);
			ConfigGridDimen config = new ConfigGridDimen(numRows, numColumns, diameter, centerDistance);

			detector = FactoryFiducialCalibration.circleRegularGrid(null, config);
		}
	}

	public void process() {
		if (detector == null) {
			printHelp();
			System.out.println();
			System.err.println("Must specify the type of fiducial to use for calibration!");
			System.exit(0);
		}

		switch (inputType) {
			case VIDEO -> throw new RuntimeException("Calibration from video not supported");
			case IMAGE -> handleDirectory();
			case WEBCAM -> handleWebcam();
			default -> {
				printHelp();
				System.out.println();
				System.err.println("Input method is not specified");
				System.exit(0);
			}
		}
	}

	// TODO break this function up into its own class so that it isn't one massive function?
	protected void handleDirectory() {
		File outputDirectory = null;
		PrintStream summaryDetection = null;

		final CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector.getLayout());
		final CalibratedPlanarPanel gui;
		ProcessThread monitor = null;

		switch (modeType) {
			case BROWN -> calibrationAlg.configurePinhole(zeroSkew, numRadial, tangential);
			case UNIVERSAL -> calibrationAlg.configureUniversalOmni(zeroSkew, numRadial, tangential);
			default -> throw new RuntimeException("Unknown model type: " + modeType);
		}

		if (visualize) {
			gui = switch (modeType) {
				case BROWN -> new MonoPlanarPanel();
				case UNIVERSAL -> new FisheyePlanarPanel();
				default -> throw new RuntimeException("Unknown model type: " + modeType);
			};
			monitor = new ProcessThread(gui);
			monitor.start();
		} else {
			gui = null;
		}

		List<String> imagePath = UtilIO.listSmartImages(inputPattern,true);

		if (imagePath.isEmpty()) {
			System.err.println("No images found. Check path, glob, or regex pattern");
			System.err.println("  " + inputPattern);
			System.exit(1);
			return;
		}

		if (verbose)
			System.out.println("Total images found: "+imagePath.size());

		// If configured to do so, create directory to store more verbose information
		if (saveLandmarks) {
			String baseName = FilenameUtils.getBaseName(outputFileName);
			baseName += "_landmarks";
			String name = baseName;
			// keep on trying to create the output directory until it succeeds
			for (int i = 0; i < 10_000; i++) {
				outputDirectory = new File(new File(outputFileName).getParent(), name);
				if (outputDirectory.exists()) {
					name = baseName + i;
				}
			}
			BoofMiscOps.checkTrue(outputDirectory.mkdirs());
			if (verbose)
				System.out.println("Saving landmarks to "+outputDirectory.getPath());

			try {
				summaryDetection = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File(outputDirectory, "summary_detection.csv"))));
				summaryDetection.println("# Summary of success or failure for each image it processed");
				summaryDetection.println("# (file name),(true = successful, false = failed)");
			} catch( IOException e ){
				throw new UncheckedIOException(e);
			}
		}

		if (monitor != null) {
			monitor.setMessage(0, "Loading images");
		}

		final List<File> imagesSuccess = new ArrayList<>();
		final List<File> imagesFailed = new ArrayList<>();

		boolean first = true;
		for (String path : imagePath) {
			File f = new File(path);
			if (f.isDirectory() || f.isHidden())
				continue;

			final BufferedImage buffered = UtilImageIO.loadImage(path);
			if (buffered == null) {
				System.err.println("Failed to open 'image' file: "+path);
				continue;
			}

			GrayF32 image = ConvertBufferedImage.convertFrom(buffered, (GrayF32)null);

			if (monitor != null) {
				monitor.setMessage(0, f.getName());

				if (first) {
					first = false;
					// should do this more intelligently based on image resolution
					int width = Math.min(1000, image.getWidth());
					int height = Math.min(width*image.height/image.width, image.getHeight());

					gui.mainView.setPreferredSize(new Dimension(width, height));
					gui.showImageProcessed(buffered);
					ShowImages.showWindow(gui, "Monocular Calibration", true);
				} else {
					BoofSwingUtil.invokeNowOrLater(() -> gui.showImageProcessed(buffered));
				}
			}

			if (detector.process(image)) {
				imagesSuccess.add(f);
				if (summaryDetection!=null)
					summaryDetection.println(f.getPath()+",true");
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
				if (summaryDetection!=null)
					summaryDetection.println(f.getPath()+",false");
				if (verbose)
					System.out.println("  Detection FAILED " + f.getPath());
			}
		}

		// Done detecting targets so close this file
		if (summaryDetection!=null)
			summaryDetection.close();

		if (verbose) {
			System.out.println("Detected targets in " + imagesSuccess.size() +
					" / " + (imagesFailed.size() + imagesSuccess.size()) + " images");
		}

		if (justDetect) {
			if (verbose)
				System.out.println("Just detecting calibration targets! Exiting now");
			System.exit(0);
			return;
		}

		if (monitor != null) {
			monitor.setMessage(1, "Computing intrinsics");
		}

		// process and compute intrinsic parameters
		try {
			final CameraModel intrinsic = calibrationAlg.process();

			if (monitor != null) {
				monitor.stopThread();

				if (imagesFailed.size() > 0) {
					JOptionPane.showMessageDialog(gui, "Failed to detect in " + imagesFailed.size() + " images");
				}

				SwingUtilities.invokeLater(() -> {
					gui.setImages(imagesSuccess);
					gui.setImagesFailed(imagesFailed);
					gui.setObservations(calibrationAlg.getObservations());
					gui.setResults(calibrationAlg.getErrors());
					gui.setCalibration(calibrationAlg.getIntrinsic(), calibrationAlg.getStructure());
					gui.setCorrection(intrinsic);
					gui.repaint();
				});
			}

			// Save calibration statistics to disk
			if (outputDirectory!=null) {
				try {
					PrintStream out = new PrintStream(new File(outputDirectory, "stats.txt"));
					calibrationAlg.printStatistics(out);
					out.close();
				} catch( IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			if (verbose) {
				calibrationAlg.printStatistics(System.out);
				System.out.println();
				System.out.println("--- " + modeType + " Parameters ---");
				System.out.println();

				switch (modeType) {
					case BROWN -> {
						CameraPinholeBrown m = (CameraPinholeBrown)intrinsic;
						switch (formatType) {
							case BOOFCV -> CalibrationIO.save(m, outputFileName);
							case OPENCV -> UtilOpenCV.save(m, outputFileName);
						}
						m.print();
					}
					case UNIVERSAL -> {
						CameraUniversalOmni m = (CameraUniversalOmni)intrinsic;
						CalibrationIO.save(m, outputFileName);
						m.print();
					}
					default -> throw new RuntimeException("Unknown model type. " + modeType);
				}
				System.out.println();
				System.out.println("Save file format " + formatType);
				System.out.println();
			}
		} catch (RuntimeException e) {
			if (visualize)
				BoofSwingUtil.warningDialog(gui, e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public static class ProcessThread extends ProgressMonitorThread {
		public ProcessThread( Component parent ) {
			super(new ProgressMonitor(parent, "Computing Calibration", "", 0, 2));
		}

		public void setMessage( final int state, final String message ) {
			SwingUtilities.invokeLater(() -> {
				monitor.setProgress(state);
				monitor.setNote(message);
			});
		}

		@Override public void doRun() {}
	}

	/**
	 * Captures calibration data live using a webcam and a GUI to assist the user
	 */
	public void handleWebcam() {
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

		ComputeGeometryScore quality = new ComputeGeometryScore(zeroSkew, detector.getLayout());
		AssistedCalibrationGui gui = new AssistedCalibrationGui(webcam.getViewSize());
		JFrame frame = ShowImages.showWindow(gui, "Webcam Calibration", true);

		GrayF32 gray = new GrayF32(webcam.getViewSize().width, webcam.getViewSize().height);

		if (desiredWidth > 0 && desiredHeight > 0) {
			if (gray.width != desiredWidth || gray.height != desiredHeight)
				System.err.println("Actual camera resolution does not match desired.  Actual: " + gray.width + " " + gray.height +
						"  Desired: " + desiredWidth + " " + desiredHeight);
		}

		AssistedCalibration assisted = new AssistedCalibration(detector, quality, gui, OUTPUT_DIRECTORY, IMAGE_DIRECTORY);
		assisted.init(gray.width, gray.height);

		BufferedImage image;
		while ((image = webcam.getImage()) != null && !assisted.isFinished()) {
			ConvertBufferedImage.convertFrom(image, gray);

			try {
				assisted.process(gray, image);
			} catch (RuntimeException e) {
				System.err.println("BUG!!! saving image to crash_image.png");
				UtilImageIO.saveImage(image, "crash_image.png");
				throw e;
			}
		}
		webcam.close();
		if (assisted.isFinished()) {
			frame.setVisible(false);

			inputPattern = new File(OUTPUT_DIRECTORY, IMAGE_DIRECTORY).getPath();
			outputFileName = new File(OUTPUT_DIRECTORY, "intrinsic.yaml").getPath();
			handleDirectory();
		}
	}

	public enum ModelType {
		BROWN,
		UNIVERSAL
	}

	public enum FormatType {
		BOOFCV,
		OPENCV
	}

	public static void main( String[] args ) {
		CameraCalibration app = new CameraCalibration();
		boolean failed = true;
		try {
			if (args.length > 0) {
				app.parse(args);
				if (app.GUI) {
					new CameraCalibrationGui();
				} else {
					app.process();
				}
				failed = false;
			}
		} catch (RuntimeException e) {
			System.out.println();
			System.out.println(e.getMessage());
		} finally {
			if (failed) {
				app.printHelp();
				System.exit(0);
			}
		}
	}
}
