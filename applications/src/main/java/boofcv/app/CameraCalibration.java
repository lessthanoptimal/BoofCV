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

package boofcv.app;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigCircleHexagonalGrid;
import boofcv.abst.fiducial.calib.ConfigCircleRegularGrid;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
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
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.javacv.UtilOpenCV;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;
import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static boofcv.app.calib.AssistedCalibration.IMAGE_DIRECTORY;
import static boofcv.app.calib.AssistedCalibration.OUTPUT_DIRECTORY;

/**
 * Application for easily calibrating a webcam using a live stream
 *
 * @author Peter Abeles
 */
public class CameraCalibration extends BaseStandardInputApp {

	protected String inputDirectory;
	protected String outputFileName = "intrinsic.yaml";
	protected DetectorFiducialCalibration detector;
	protected boolean zeroSkew = true;
	protected int numRadial = 2;
	protected boolean tangential = false;
	protected ModelType modeType = ModelType.PINHOLE;
	protected FormatType formatType = FormatType.BOOFCV;

	protected boolean GUI = false;
	protected boolean visualize = true;

	public void printHelp() {
		System.out.println("./application <output file> <Input Options> <Calibration Parameters> <Fiducial Type> <Fiducial Specific Options> ");
		System.out.println();
		System.out.println("  --GUI                              Turns on GUI mode and ignores other options.");
		System.out.println();
		System.out.println("<output file>                        file name for output");
		System.out.println("                                     DEFAULT: \"intrinsic.yaml\"");
		System.out.println();
		System.out.println("Input: File Options:  ");
		System.out.println();
		System.out.println("  --Directory=<path>                 Directory containing calibration images");
		System.out.println("  --Visualize=<true/false>           Should it visualize the results?");
		System.out.println("                                     DEFAULT: true");
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
		System.out.println("                                     ( boofcv , opencv )");
		System.out.println("                                     DEFAULT: boofcv");
		System.out.println();
		System.out.println("Calibration Parameters:");
		System.out.println();
		System.out.println("  --Model=<string>                   Specifies the camera model to use.");
		System.out.println("                                     ( pinhole, universal )");
		System.out.println("                                     DEFAULT: pinhole");
		System.out.println("  --ZeroSkew=<true/false>            Can it assume zero skew?");
		System.out.println("                                     DEFAULT: true");
		System.out.println("  --NumRadial=<int>                  Number of radial coefficients");
		System.out.println("                                     DEFAULT: 2");
		System.out.println("  --Tangential=<true/false>          Should it include tangential terms?");
		System.out.println("                                     DEFAULT: false");
		System.out.println();
		System.out.println("Fiducial Types:");
		System.out.println("   CHESSBOARD");
		System.out.println("   SQUAREGRID");
		System.out.println("   CIRCLE_HEX");
		System.out.println("   CIRCLE_REG");
		System.out.println();
		System.out.println("Flags for CHESSBOARD:");
		System.out.println("  --Grid=<rows>:<columns>            Specifies number of rows and columns");
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
	}

	public void parse( String []args ) {
		if( args.length < 1 ) {
			throw new RuntimeException("Must specify some arguments");
		}

		cameraId = -1; // override default value of zero so that its easy to tell if a camera was slected
		for( int i = 0; i < args.length; i++ ) {
			String arg = args[i];

			if( arg.startsWith("--") ) {
				if( arg.compareToIgnoreCase("--GUI") == 0 ) {
					GUI = true;
				} else if (!checkCameraFlag(arg)) {
					splitFlag(arg);
					if( flagName.compareToIgnoreCase("Directory") == 0 ) {
						inputDirectory = parameters;
						inputType = InputType.IMAGE;
					} else if( flagName.compareToIgnoreCase("Visualize") == 0 ) {
						visualize = Boolean.parseBoolean(parameters);
					} else if( flagName.compareToIgnoreCase("Model") == 0 ) {
						if( parameters.compareToIgnoreCase("pinhole") == 0 ) {
							modeType = ModelType.PINHOLE;
						} else if( parameters.compareToIgnoreCase("universal") == 0 ) {
							modeType = ModelType.UNIVERSAL;
						} else {
							throw new RuntimeException("Unknown model type " + parameters);
						}
					} else if( flagName.compareToIgnoreCase("Format") == 0 ) {
						if( parameters.compareToIgnoreCase("boofcv") == 0 ) {
							formatType = FormatType.BOOFCV;
						} else if( parameters.compareToIgnoreCase("opencv") == 0 ) {
							formatType = FormatType.OPENCV;
						} else {
							throw new RuntimeException("Unknown model type " + parameters);
						}
					} else if( flagName.compareToIgnoreCase("ZeroSkew") == 0 ) {
						zeroSkew = Boolean.parseBoolean(parameters);
					} else if( flagName.compareToIgnoreCase("NumRadial") == 0 ) {
						numRadial = Integer.parseInt(parameters);
					} else if( flagName.compareToIgnoreCase("Tangential") == 0 ) {
						tangential = Boolean.parseBoolean(parameters);
					} else  {
						throw new RuntimeException("Unknown input option " + flagName);
					}
				}
			} else if( arg.compareToIgnoreCase("CHESSBOARD") == 0 ) {
				parseChessboard(i + 1,args);
				break;
			} else if( arg.compareToIgnoreCase("SQUAREGRID") == 0 ) {
				parseSquareGrid(i + 1, args);
				break;
			} else if( arg.compareToIgnoreCase("CIRCLE_HEX") == 0 ) {
				parseCircle(i + 1, args, true);
				break;
			} else if( arg.compareToIgnoreCase("CIRCLE_REG") == 0 ) {
				parseCircle(i + 1, args, false);
				break;
			} else if( i == 0 ) {
				outputFileName = arg;
			} else {
				throw new RuntimeException("Unknown fiducial type "+arg);
			}
		}

		if( formatType == FormatType.OPENCV && modeType != ModelType.PINHOLE ) {
			throw new RuntimeException("Can only save calibration in OpenCV format if pinhole model");
		}
	}

	protected void parseChessboard( int index , String []args ) {
		int numRows=0,numColumns=0;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if( !arg.startsWith("--") ) {
				throw new  RuntimeException("Expected flags for chessboard.  Should start with '--'");
			}

			splitFlag(arg);
			if( flagName.compareToIgnoreCase("Grid") == 0 ) {
				String words[] = parameters.split(":");
				if( words.length != 2 )throw new RuntimeException("Expected two values for rows and columns");
				numRows = Integer.parseInt(words[0]);
				numColumns = Integer.parseInt(words[1]);
			} else {
				throw new RuntimeException("Unknown image option "+flagName);
			}
		}

		if( numRows <= 0 || numColumns <= 0) {
			throw new RuntimeException("Rows and columns must be specified and > 0");
		}

		System.out.println("chessboard: "+numRows+" x "+numColumns);

		ConfigChessboard config = new ConfigChessboard(numRows, numColumns, 1);

		detector = FactoryFiducialCalibration.chessboard(config);
	}

	protected void parseSquareGrid( int index , String []args ) {
		int numRows=0,numColumns=0;
		double square=1,space=1;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if( !arg.startsWith("--") ) {
				throw new  RuntimeException("Expected flags for square grid. Should start with '--'");
			}

			splitFlag(arg);
			if( flagName.compareToIgnoreCase("Grid") == 0 ) {
				String words[] = parameters.split(":");
				if (words.length != 2) throw new RuntimeException("Expected two values for rows and columns");
				numRows = Integer.parseInt(words[0]);
				numColumns = Integer.parseInt(words[1]);
			} else if( flagName.compareToIgnoreCase("SquareSpace") == 0 ) {
				String words[] = parameters.split(":");
				if( words.length != 2 )throw new RuntimeException("Expected two values for square and space");
				square = Double.parseDouble(words[0]);
				space = Double.parseDouble(words[1]);
			} else {
				throw new RuntimeException("Unknown image option "+flagName);
			}
		}

		if( numRows <= 0 || numColumns <= 0) {
			throw new RuntimeException("Rows and columns must be specified and > 0");
		}
		if( square <= 0 || space <= 0) {
			throw new RuntimeException("square and space width must be specified and > 0");
		}

		System.out.println("squaregrid: "+numRows+" x "+numColumns+" square/space = "+(square/space));

		ConfigSquareGrid config = new ConfigSquareGrid(numRows, numColumns, square,space);

		detector = FactoryFiducialCalibration.squareGrid(config);
	}

	protected void parseCircle( int index , String []args , boolean hexagonal) {
		int numRows=0,numColumns=0;
		double diameter=-1,centerDistance=-1;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if( !arg.startsWith("--") ) {
				throw new  RuntimeException("Expected flags for radius grid. Should start with '--'");
			}

			splitFlag(arg);
			if( flagName.compareToIgnoreCase("Grid") == 0 ) {
				String words[] = parameters.split(":");
				if (words.length != 2) throw new RuntimeException("Expected two values for rows and columns");
				numRows = Integer.parseInt(words[0]);
				numColumns = Integer.parseInt(words[1]);
			} else if( flagName.compareToIgnoreCase("CenterDistance") == 0 ) {
				centerDistance = Double.parseDouble(parameters);
			} else if( flagName.compareToIgnoreCase("Diameter") == 0 ) {
				diameter = Double.parseDouble(parameters);
			} else {
				throw new RuntimeException("Unknown image option "+flagName);
			}
		}

		if( numRows <= 0 || numColumns <= 0) {
			throw new RuntimeException("Rows and columns must be specified and > 0");
		}
		if( diameter <= 0 || centerDistance <= 0) {
			throw new RuntimeException("diameter and center distance must be specified and > 0");
		}

		if( hexagonal ) {
			System.out.println("circle hexagonal: "+numRows+" x "+numColumns+" diameter = "+diameter+" center distance = "+centerDistance);
			ConfigCircleHexagonalGrid config = new ConfigCircleHexagonalGrid(numRows, numColumns, diameter, centerDistance);

			detector = FactoryFiducialCalibration.circleHexagonalGrid(config);
		} else {
			System.out.println("circle regular: "+numRows+" x "+numColumns+" diameter = "+diameter+" center distance = "+centerDistance);
			ConfigCircleRegularGrid config = new ConfigCircleRegularGrid(numRows, numColumns, diameter, centerDistance);

			detector = FactoryFiducialCalibration.circleRegularGrid(config);
		}
	}

	public void process() {
		if( detector == null ) {
			printHelp();
			System.out.println();
			System.err.println("Must specify the type of fiducial to use for calibration!");
			System.exit(0);
		}

		switch( inputType ) {
			case VIDEO: throw new RuntimeException("Calibration from video not supported");
			case IMAGE:
				handleDirectory();
				break;

			case WEBCAM:
				handleWebcam();
				break;

			default:
				printHelp();
				System.out.println();
				System.err.println("Input method is not specified");
				System.exit(0);
				break;
		}
	}

	protected void handleDirectory() {
		final CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector.getLayout());
		final CalibratedPlanarPanel gui;
		ProcessThread monitor = null;

		switch( modeType ) {
			case PINHOLE:
				calibrationAlg.configurePinhole( zeroSkew, numRadial, tangential);
				break;

			case UNIVERSAL:
				calibrationAlg.configureUniversalOmni( zeroSkew, numRadial, tangential);
				break;

			default:
				throw new RuntimeException("Unknown model type: "+modeType);
		}

		if( visualize ) {
			switch( modeType ) {
				case PINHOLE: gui = new MonoPlanarPanel(); break;
				case UNIVERSAL: gui = new FisheyePlanarPanel(); break;
				default: throw new RuntimeException("Unknown model type: "+modeType);
			}
			monitor = new ProcessThread(gui);
			monitor.start();
		} else {
			gui = null;
		}

		File directory = new File(inputDirectory);
		if( !directory.exists() ) {
			System.err.println("Input directory doesn't exist!");
			System.err.println("  "+inputDirectory);
			System.exit(0);
		}
		List<File> files = Arrays.asList(directory.listFiles());
		BoofMiscOps.sortFilesByName(files);

		if( files.isEmpty() ) {
			System.err.println("No image files found!");
			System.err.println(inputDirectory);
			System.exit(0);
		}

		if( visualize ) {
			monitor.setMessage(0,"Loading images");
		}

		final List<File> imagesSuccess = new ArrayList<>();
		final List<File> imagesFailed = new ArrayList<>();

		boolean first = true;
		for( File f : files ){
			if( f.isDirectory() || f.isHidden())
				continue;

			final BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				continue;

			GrayF32 image = ConvertBufferedImage.convertFrom(buffered,(GrayF32)null);

			if( visualize ) {
				monitor.setMessage(0,f.getName());

				if( first ) {
					first = false;
					// should do this more intelligently based on image resolution
					int width = Math.min(1000,image.getWidth());
					int height = Math.min(width*image.height/image.width,image.getHeight());

					gui.mainView.setPreferredSize(new Dimension(width,height));
					gui.showImageProcessed(buffered);
					ShowImages.showWindow(gui,"Monocular Calibration",true);
				} else {
					BoofSwingUtil.invokeNowOrLater(new Runnable() {
						@Override
						public void run() {
							gui.showImageProcessed(buffered);
						}
					});
				}
			}

			if( !detector.process(image)) {
				imagesFailed.add(f);
				System.err.println("Failed to detect target in " + f.getName());
			} else {
				calibrationAlg.addImage(detector.getDetectedPoints());
				imagesSuccess.add(f);
			}
		}

		if( visualize ) {
			monitor.setMessage(1,"Computing intrinsics");
		}

		// process and compute intrinsic parameters
		try {
			final CameraModel intrinsic = calibrationAlg.process();

			if( visualize ) {
				monitor.stopThread();

				if( imagesFailed.size() > 0 ) {
					JOptionPane.showMessageDialog(gui,"Failed to detect in "+imagesFailed.size()+" images");
				}

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						gui.setImages(imagesSuccess);
						gui.setImagesFailed(imagesFailed);
						gui.setObservations(calibrationAlg.getObservations());
						gui.setResults(calibrationAlg.getErrors());
						gui.setCalibration(calibrationAlg.getZhangParam());
						gui.setCorrection(intrinsic);
						gui.repaint();
					}
				});
			}

			calibrationAlg.printStatistics();
			System.out.println();
			System.out.println("--- "+modeType+" Parameters ---");
			System.out.println();

			switch( modeType ) {
				case PINHOLE: {
					CameraPinholeRadial m = (CameraPinholeRadial)intrinsic;
					switch (formatType) {
						case BOOFCV: CalibrationIO.save(m, outputFileName); break;
						case OPENCV: UtilOpenCV.save(m, outputFileName); break;
					}
					m.print();
				}break;

				case UNIVERSAL: {
					CameraUniversalOmni m = (CameraUniversalOmni)intrinsic;
					CalibrationIO.save(m, outputFileName);
					m.print();
				}break;

				default:
					throw new RuntimeException("Unknown model type. "+modeType);
			}
			System.out.println();
			System.out.println("Save file format "+formatType);
			System.out.println();
		} catch( RuntimeException e ) {
			if( visualize )
				BoofSwingUtil.warningDialog(gui,e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Displays a progress monitor and updates its state periodically
	 */
	public class ProcessThread extends ProgressMonitorThread
	{
		public ProcessThread(Component parent ) {
			super(new ProgressMonitor(parent, "Computing Calibration", "", 0, 2));
		}

		public void setMessage( final int state , final String message ) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					monitor.setProgress(state);
					monitor.setNote(message);
				}});
		}

		@Override
		public void doRun() {
		}
	}

	/**
	 * Captures calibration data live using a webcam and a GUI to assist the user
	 */
	public void handleWebcam() {
		final Webcam webcam = openSelectedCamera();
		if( desiredWidth > 0 && desiredHeight > 0 )
			UtilWebcamCapture.adjustResolution(webcam, desiredWidth, desiredHeight);

		webcam.open();

		// close the webcam gracefully on exit
		Runtime.getRuntime().addShutdownHook(new Thread(){public void run(){
			if(webcam.isOpen()){System.out.println("Closing webcam");webcam.close();}}});

		ComputeGeometryScore quality = new ComputeGeometryScore(zeroSkew,detector.getLayout());
		AssistedCalibrationGui gui = new AssistedCalibrationGui(webcam.getViewSize());
		JFrame frame = ShowImages.showWindow(gui, "Webcam Calibration", true);

		GrayF32 gray = new GrayF32(webcam.getViewSize().width,webcam.getViewSize().height);

		if( desiredWidth > 0 && desiredHeight > 0 ) {
			if (gray.width != desiredWidth || gray.height != desiredHeight )
				System.err.println("Actual camera resolution does not match desired.  Actual: "+gray.width+" "+gray.height+
				"  Desired: "+desiredWidth+" "+desiredHeight);
		}

		AssistedCalibration assisted = new AssistedCalibration(detector,quality,gui,OUTPUT_DIRECTORY, IMAGE_DIRECTORY);
		assisted.init(gray.width,gray.height);

		BufferedImage image;
		while( (image = webcam.getImage()) != null && !assisted.isFinished()) {
			ConvertBufferedImage.convertFrom(image, gray);

			try {
				assisted.process(gray,image);
			} catch( RuntimeException e ) {
				System.err.println("BUG!!! saving image to crash_image.png");
				UtilImageIO.saveImage(image, "crash_image.png");
				throw e;
			}
		}
		webcam.close();
		if( assisted.isFinished() ) {
			frame.setVisible(false);

			inputDirectory = new File(OUTPUT_DIRECTORY, IMAGE_DIRECTORY).getPath();
			outputFileName = new File(OUTPUT_DIRECTORY, "intrinsic.yaml").getPath();
			handleDirectory();
		}
	}

	public enum ModelType {
		PINHOLE,
		UNIVERSAL
	}

	public enum FormatType {
		BOOFCV,
		OPENCV
	}

	public static void main(String[] args) {
			CameraCalibration app = new CameraCalibration();
			boolean failed = true;
			try {
				if( args.length > 0 ) {
					app.parse(args);
					if( app.GUI ) {
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
				if( failed ) {
					app.printHelp();
					System.exit(0);
				}
			}

	}
}
