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

package boofcv.app;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.CalibrationDetector;
import boofcv.app.calib.AssistedCalibration;
import boofcv.app.calib.AssistedCalibrationGui;
import boofcv.app.calib.ComputeGeometryScore;
import boofcv.factory.calib.FactoryCalibrationTarget;
import boofcv.gui.calibration.MonoPlanarPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Application for easily calibrating a webcam using a live stream
 *
 * @author Peter Abeles
 */
public class CameraCalibration extends BaseStandardInputApp {

	String inputDirectory;
	String outputFileName = "intrinsic.xml";
	CalibrationDetector detector;
	boolean zeroSkew = true;
	int numRadial = 2;
	boolean tangential = false;

	boolean visualize = true;

	void printHelp() {
		System.out.println("./application <output file> <Input Options> <Calibration Parameters> <Fiducial Type> <Fiducial Specific Options> ");
		System.out.println();
		System.out.println("<output file>                        file name for output");
		System.out.println("                                     DEFAULT: \"intrinsic.xml\"");
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
		System.out.println("Calibration Parameters:");
		System.out.println();
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
		System.out.println();
		System.out.println("Flags for CHESSBOARD:");
		System.out.println("  --Grid=<rows>:<columns>            Specifies number of rows and columns in the target");
		System.out.println();
		System.out.println("Flags for SQUAREGRID:");
		System.out.println("  --Grid=<rows>:<columns>            Specifies number of black square rows and columns in the target");
		System.out.println("  --SquareSpace=<square>:<space>     Specifies side of a square and space between square");
		System.out.println("                                     Only the ratio matters.");
		System.out.println("                                     Default: 1:1 square = 1 and space = 1");
		System.out.println();
	}

	void parse( String []args ) {
		if( args.length < 1 ) {
			throw new RuntimeException("Must specify some arguments");
		}

		cameraId = -1; // override default value of zero so that its easy to tell if a camera was slected
		for( int i = 0; i < args.length; i++ ) {
			String arg = args[i];

			if( arg.startsWith("--") ) {
				if (!checkCameraFlag(arg)) {
					splitFlag(arg);
					if( flagName.compareToIgnoreCase("Directory") == 0 ) {
						inputDirectory = parameters;
						inputType = InputType.IMAGE;
					} else if( flagName.compareToIgnoreCase("Visualize") == 0 ) {
						visualize = Boolean.parseBoolean(parameters);
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
			} else if( i == 0 ) {
				outputFileName = arg;
			} else {
				throw new RuntimeException("Unknown fiducial type "+arg);
			}
		}
	}

	void parseChessboard( int index , String []args ) {
		int numRows=0,numColumns=0;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if( !arg.startsWith("--") ) {
				throw new  RuntimeException("Expected flags for chessboard");
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

		detector = FactoryCalibrationTarget.detectorChessboard(config);
	}

	void parseSquareGrid( int index , String []args ) {
		int numRows=0,numColumns=0;
		double square=1,space=1;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if( !arg.startsWith("--") ) {
				throw new  RuntimeException("Expected flags for chessboard");
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

		detector = FactoryCalibrationTarget.detectorSquareGrid(config);
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
	private void handleDirectory() {
		final CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(detector);

		calibrationAlg.configure( zeroSkew, numRadial, tangential);

		File directory = new File(inputDirectory);
		if( !directory.exists() ) {
			System.err.println("Input directory doesn't exist!");
			System.err.println("  "+inputDirectory);
			System.exit(0);
		}
		List<File> files = Arrays.asList(directory.listFiles());
		Collections.sort(files);

		final MonoPlanarPanel gui = visualize ? new MonoPlanarPanel() : null;

		boolean first = true;
		for( File f : files ){
			if( f.isDirectory() || f.isHidden())
				continue;

			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null )
				continue;

			ImageFloat32 image = ConvertBufferedImage.convertFrom(buffered,(ImageFloat32)null);

			if( gui != null ) {
				gui.addImage(f.getName(),buffered);
				if( first ) {
					first = false;
					ShowImages.showWindow(gui,"Monocular Calibration",true);
				}
			}

			if( !calibrationAlg.addImage(image) )
				System.err.println("Failed to detect target in "+f.getName());
		}

		// process and compute intrinsic parameters
		final IntrinsicParameters intrinsic = calibrationAlg.process();

		if( gui != null ) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					gui.setObservations(calibrationAlg.getObservations());
					gui.setResults(calibrationAlg.getErrors());
					gui.setCalibration(calibrationAlg.getZhangParam());
					gui.setCorrection(intrinsic);
					gui.repaint();
				}
			});

		}

		// save results to a file and print out
		UtilIO.saveXML(intrinsic,outputFileName);

		calibrationAlg.printStatistics();
		System.out.println();
		System.out.println("--- Intrinsic Parameters ---");
		System.out.println();
		intrinsic.print();
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

		ImageFloat32 gray = new ImageFloat32(webcam.getViewSize().width,webcam.getViewSize().height);

		AssistedCalibration assisted = new AssistedCalibration(detector,quality,gui);
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

			inputDirectory = AssistedCalibration.IMAGE_DIRECTORY;
			outputFileName = new File(AssistedCalibration.OUTPUT_DIRECTORY, "intrinsic.xml").getPath();

			handleDirectory();
		}
	}

	public static void main(String[] args) {
		CameraCalibration app = new CameraCalibration();
		try {
			app.parse(args);
		} catch( RuntimeException e ) {
			app.printHelp();
			System.out.println();
			System.out.println(e.getMessage());
			System.exit(0);
		}
		app.process();
	}
}
