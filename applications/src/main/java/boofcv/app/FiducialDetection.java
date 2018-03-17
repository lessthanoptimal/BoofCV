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

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.image.UtilImageIO;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Quaternion_F64;
import org.ddogleg.struct.GrowQueue_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line application for detecting different types of fiducials in different types of input methods.
 *
 * @author Peter Abeles
 */
public class FiducialDetection extends BaseStandardInputApp {

	public static final int DEFAULT_THRESHOLD = 100;

	// path to intrinsic file
	String intrinsicPath;
	// path to where the results should be stored
	String outputPath;

	PrintStream outputFile;

	FiducialDetector<GrayU8> detector;

	void printHelp() {
		System.out.println("java -jar BLAH <Input Flags> <Fiducial Type> <Fiducial Flags>");
		System.out.println();
		System.out.println("   Detects different types of fiducials inside of webcam streams, video files, or still images.");
		System.out.println("   Results are visualized in a window and optionally saved to file.");
		System.out.println();
		System.out.println("----------------------------------- Input Flags -----------------------------------------");
		System.out.println();
		printInputHelp();
		System.out.println();
		System.out.println("----------------------------------- Other Flags -----------------------------------------");
		System.out.println();
		System.out.println("These flags are common for all input methods.  They can be specified any time before the");
		System.out.println("fidcuial flags are specified");
		System.out.println();
		System.out.println("  --Intrinsic=<path>                 Specifies location of the intrinsic parameters file.");
		System.out.println("                                     DEFAULT: Make a crude guess.");
		System.out.println();
		System.out.println("  --OutputFile=<path>                Writes the ID and pose of detected fiducials out to a file");
		System.out.println("                                     File format is described in the file's header.");
		System.out.println();
		System.out.println("----------------------------------- Fiducial Flags --------------------------------------");
		System.out.println();
		System.out.println("Fiducial Types:");
		System.out.println("   BINARY");
		System.out.println("   IMAGE");
		System.out.println("   CHESSBOARD");
		System.out.println("   SQUAREGRID");
		System.out.println();
		System.out.println("Flags for BINARY");
		System.out.println();
		System.out.println("  --Robust=<true/false>              If slower but more robust technique should be used");
		System.out.println("                                     DEFAULT: true");
		System.out.println("  --Size=<float>                     Specifies the size of all the fiducials");
		System.out.println("                                     DEFAULT: 1");
		System.out.println("  --GridWidth=<int>                  Specifies how many inner squares to expect in the fiducial.");
		System.out.println("                                     Valid options: 3 to 8");
		System.out.println("                                     Default is 4");
		System.out.println("  --Border=<float>                   Specifies relative width of the border.");
		System.out.println("                                     DEFAULT: 0.25");
		System.out.println();
		System.out.println("Flags for IMAGE");
		System.out.println();
		System.out.println("  --Robust=<true/false>              If slower but more robust technique should be used");
		System.out.println("                                     DEFAULT: true");
		System.out.println("  --Image=<size float>:<image path>  Adds a single image with the specified size");
		System.out.println("                                     Can be called multiple times for several images");
		System.out.println("  --Border=<float>                   Specifies relative width of the border.");
		System.out.println("                                     DEFAULT: 0.25");
		System.out.println("Flags for CHESSBOARD");
		System.out.println();
		System.out.println("  --Shape=<rows int>:<cols int>      Number of rows/columns it expects to see");
		System.out.println("  --SquareWidth=<float>              The width of each square");
		System.out.println("                                     Can be called multiple times for several images");
		System.out.println("                                     DEFAULT: 1");
		System.out.println("Flags for SQUAREGRID");
		System.out.println();
		System.out.println("  --Shape=<rows int>:<cols int>      Number of rows/columns it expects to see");
		System.out.println("  --SquareWidth=<float>              The width of each square");
		System.out.println("                                     DEFAULT: 1");
		System.out.println("  --Space=<float>                    The space between each square");
		System.out.println("                                     DEFAULT: Same as SquareWidth");
		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.out.println("./application BINARY --Size=1 --GridWidth=3");
		System.out.println("        Opens the default camera at default resolution looking for a 3x3 binary patterns with a width of 1");
		System.out.println();
		System.out.println("./application --Camera=1 --Resolution=640:480 BINARY --Robust=false --Size=1");
		System.out.println("        Opens the camera 1 at a resolution of 640x480 using a fast thresholding technique, ");
		System.out.println("        looking for 4x4 binary patterns with a width of 1");
		System.out.println();
		System.out.println("./application -ImageFile=image.jpeg BINARY");
		System.out.println("        Opens \"image.jpg\" and detects binary square fiducials inside of it");
		System.out.println();
	}

	void parse( String []args ) {
		if( args.length < 1 ) {
			throw new RuntimeException("Must specify some arguments");
		}

		for( int i = 0; i < args.length; i++ ) {
			String arg = args[i];

			if( arg.startsWith("--") ) {
				if( !checkCameraFlag(arg) ) {
					if( flagName.compareToIgnoreCase("Intrinsic") == 0 ) {
						intrinsicPath = parameters;
					} else if( flagName.compareToIgnoreCase("OutputFile") == 0 ) {
						outputPath = parameters;
					} else {
						throw new RuntimeException("Unknown camera option "+flagName);
					}
				}
			} else if( arg.compareToIgnoreCase("BINARY") == 0 ) {
				parseBinary(i+1,args);
				break;
			} else if( arg.compareToIgnoreCase("IMAGE") == 0 ) {
				parseImage(i + 1, args);
				break;
			} else if( arg.compareToIgnoreCase("CHESSBOARD") == 0 ) {
				parseChessboard(i + 1,args);
				break;
			} else if( arg.compareToIgnoreCase("SQUAREGRID") == 0 ) {
				parseSquareGrid(i + 1,args);
				break;
			} else {
				throw new RuntimeException("Unknown fiducial type "+arg);
			}
		}
	}

	void parseBinary( int index , String []args ) {
		boolean robust=true;
		double size=1;
		int gridWidth = 4;
		double borderWidth = 0.25;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if( !arg.startsWith("--") ) {
				throw new  RuntimeException("Expected flags for binary fiducial");
			}

			splitFlag(arg);
			if( flagName.compareToIgnoreCase("Robust") == 0 ) {
				robust = Boolean.parseBoolean(parameters);
			} else if( flagName.compareToIgnoreCase("Size") == 0 ) {
				size = Double.parseDouble(parameters);
			} else if( flagName.compareToIgnoreCase("GridWidth") == 0 ) {
				gridWidth = Integer.parseInt(parameters);
			} else if( flagName.compareToIgnoreCase("Border") == 0 ) {
				borderWidth = Double.parseDouble(parameters);
			} else {
				throw new RuntimeException("Unknown image option "+flagName);
			}
		}

		System.out.println("binary: robust = "+robust+" size = "+size + " grid width = " + gridWidth+" border = "+borderWidth);

		ConfigFiducialBinary configFid = new ConfigFiducialBinary();
		configFid.targetWidth = size;
		configFid.gridWidth = gridWidth;
		configFid.squareDetector.minimumRefineEdgeIntensity = 10;
		configFid.borderWidthFraction = borderWidth;

		ConfigThreshold configThreshold ;

		if( robust )
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 21);
		else
			configThreshold = ConfigThreshold.fixed(DEFAULT_THRESHOLD);

		detector = FactoryFiducial.squareBinary(configFid, configThreshold, GrayU8.class);
	}

	void parseImage( int index , String []args ) {
		boolean robust=true;

		List<String> paths = new ArrayList<>();
		GrowQueue_F64 sizes = new GrowQueue_F64();
		double borderWidth = 0.25;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if( !arg.startsWith("--") ) {
				throw new  RuntimeException("Expected flags for image fiducial");
			}

			splitFlag(arg);
			if( flagName.compareToIgnoreCase("Robust") == 0 ) {
				robust = Boolean.parseBoolean(parameters);
			} else if( flagName.compareToIgnoreCase("Image") == 0 ) {
				String words[] = parameters.split(":");
				if( words.length != 2 )throw new RuntimeException("Expected two for width and image path");
				sizes.add(Double.parseDouble(words[0]));
				paths.add(words[1]);
			} else if( flagName.compareToIgnoreCase("Border") == 0 ) {
				borderWidth = Double.parseDouble(parameters);
			} else {
				throw new RuntimeException("Unknown image option "+flagName);
			}
		}

		if( paths.isEmpty() )
			throw new RuntimeException("Need to specify patterns");

		System.out.println("image: robust = "+robust+" total patterns = "+paths.size()+" border = "+borderWidth);

		ConfigFiducialImage config = new ConfigFiducialImage();
		config.borderWidthFraction = borderWidth;
		ConfigThreshold configThreshold;

		if( robust )
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_MEAN, 21);
		else
			configThreshold = ConfigThreshold.fixed(DEFAULT_THRESHOLD);

		SquareImage_to_FiducialDetector<GrayU8> detector =
				FactoryFiducial.squareImage(config, configThreshold, GrayU8.class);

		for (int i = 0; i < paths.size(); i++) {
			BufferedImage buffered = UtilImageIO.loadImage(paths.get(i));
			if( buffered == null )
				throw new RuntimeException("Can't find pattern "+paths.get(i));

			GrayU8 pattern = new GrayU8(buffered.getWidth(),buffered.getHeight());

			ConvertBufferedImage.convertFrom(buffered, pattern);

			detector.addPatternImage(pattern,125,sizes.get(i));
		}

		this.detector = detector;
	}

	void parseChessboard( int index , String []args ) {

		int rows=-1,cols=-1;
		double width = 1;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if (!arg.startsWith("--")) {
				throw new RuntimeException("Expected flags for chessboard calibration fiducial");
			}

			splitFlag(arg);
			if (flagName.compareToIgnoreCase("Shape") == 0) {
				String words[] = parameters.split(":");
				if( words.length != 2 )throw new RuntimeException("Expected two for rows and columns");
				rows = Integer.parseInt(words[0]);
				cols = Integer.parseInt(words[1]);
			} else if (flagName.compareToIgnoreCase("SquareWidth") == 0) {
				width = Double.parseDouble(parameters);
			} else {
				throw new RuntimeException("Unknown chessboard option "+flagName);
			}
		}

		if( rows < 1 || cols < 1)
			throw new RuntimeException("Must specify number of rows and columns");

		System.out.println("chessboard: rows = "+rows+" columns = "+cols+"  square width "+width);
		ConfigChessboard config = new ConfigChessboard(rows, cols, width);

		detector = FactoryFiducial.calibChessboard(config, GrayU8.class);
	}
	void parseSquareGrid( int index , String []args ) {
		int rows=-1,cols=-1;
		double width = 1, space = -1;

		for(; index < args.length; index++ ) {
			String arg = args[index];

			if (!arg.startsWith("--")) {
				throw new RuntimeException("Expected flags for square grid calibration fiducial");
			}

			splitFlag(arg);
			if (flagName.compareToIgnoreCase("Shape") == 0) {
				String words[] = parameters.split(":");
				if( words.length != 2 )throw new RuntimeException("Expected two for rows and columns");
				rows = Integer.parseInt(words[0]);
				cols = Integer.parseInt(words[1]);
			} else if (flagName.compareToIgnoreCase("SquareWidth") == 0) {
				width = Double.parseDouble(parameters);
			} else if (flagName.compareToIgnoreCase("Space") == 0) {
				space = Double.parseDouble(parameters);
			} else {
				throw new RuntimeException("Unknown square grid option "+flagName);
			}
		}

		if( rows < 1 || cols < 1)
			throw new RuntimeException("Must specify number of rows and columns");

		if( space <= 0 )
			space = width;

		System.out.println("square grid: rows = "+rows+" columns = "+cols+"  square width "+width+"  space "+space);
		ConfigSquareGrid config = new ConfigSquareGrid(rows, cols, width,space);

		detector = FactoryFiducial.calibSquareGrid(config, GrayU8.class);
	}

	private static CameraPinholeRadial handleIntrinsic(CameraPinholeRadial intrinsic, int width, int height) {
		if( intrinsic == null ) {
			System.out.println();
			System.out.println("SERIOUSLY YOU NEED TO CALIBRATE THE CAMERA YOURSELF!");
			System.out.println("There will be a lot more jitter and inaccurate pose");
			System.out.println();

			return PerspectiveOps.createIntrinsic(width, height, 35);
		} else {
			if( intrinsic.width != width || intrinsic.height != height ) {
				System.out.println();
				System.out.println("The image resolution in the intrinsics file doesn't match the input.");
				System.out.println("Massaging the intrinsic for this input.  If the results are poor calibrate");
				System.out.println("your camera at the correct resolution!");
				System.out.println();

				double ratioW = width/(double)intrinsic.width;
				double ratioH = height/(double)intrinsic.height;

				if( Math.abs(ratioW-ratioH) > 1e-8 ) {
					System.err.println("Can't adjust intrinsic parameters because camera ratios are different");
					System.exit(1);
				}
				PerspectiveOps.scaleIntrinsic(intrinsic,ratioW);
			}
			return intrinsic;
		}
	}

	/**
	 * Displays a continuous stream of images
	 */
	private void processStream(CameraPinholeRadial intrinsic , SimpleImageSequence<GrayU8> sequence , ImagePanel gui , long pauseMilli) {

		Font font = new Font("Serif", Font.BOLD, 24);

		Se3_F64 fiducialToCamera = new Se3_F64();
		int frameNumber = 0;
		while( sequence.hasNext() ) {
			long before = System.currentTimeMillis();
			GrayU8 input = sequence.next();
			BufferedImage buffered = sequence.getGuiImage();
			try {
				detector.detect(input);
			} catch( RuntimeException e ) {
				System.err.println("BUG!!! saving image to crash_image.png");
				UtilImageIO.saveImage(buffered,"crash_image.png");
				throw e;
			}

			Graphics2D g2 = buffered.createGraphics();

			for (int i = 0; i < detector.totalFound(); i++) {
				detector.getFiducialToCamera(i,fiducialToCamera);
				long id = detector.getId(i);
				double width = detector.getWidth(i);

				VisualizeFiducial.drawCube(fiducialToCamera,intrinsic,width,3,g2);
				VisualizeFiducial.drawLabelCenter(fiducialToCamera,intrinsic,""+id,g2);
			}
			saveResults(frameNumber++);

			if( intrinsicPath == null ) {
				g2.setColor(Color.RED);
				g2.setFont(font);
				g2.drawString("Uncalibrated",10,20);
			}

			gui.setImage(buffered);

			long after = System.currentTimeMillis();
			long time = Math.max(0,pauseMilli-(after-before));
			if( time > 0 ) {
				try { Thread.sleep(time); } catch (InterruptedException ignore) {}
			}
		}
	}

	/**
	 * Displays a simple image
	 */
	private void processImage(CameraPinholeRadial intrinsic , BufferedImage buffered , ImagePanel gui ) {

		Font font = new Font("Serif", Font.BOLD, 24);

		GrayU8 gray = new GrayU8(buffered.getWidth(),buffered.getHeight());
		ConvertBufferedImage.convertFrom(buffered,gray);

		Se3_F64 fiducialToCamera = new Se3_F64();
		try {
			detector.detect(gray);
		} catch( RuntimeException e ) {
			System.err.println("BUG!!! saving image to crash_image.png");
			UtilImageIO.saveImage(buffered,"crash_image.png");
			throw e;
		}

		Graphics2D g2 = buffered.createGraphics();

		for (int i = 0; i < detector.totalFound(); i++) {
			detector.getFiducialToCamera(i,fiducialToCamera);
			long id = detector.getId(i);
			double width = detector.getWidth(i);

			VisualizeFiducial.drawCube(fiducialToCamera,intrinsic,width,3,g2);
			VisualizeFiducial.drawLabelCenter(fiducialToCamera,intrinsic,""+id,g2);
		}
		saveResults(0);

		if( intrinsicPath == null ) {
			g2.setColor(Color.RED);
			g2.setFont(font);
			g2.drawString("Uncalibrated",10,20);
		}

		gui.setImage(buffered);
	}

	private void saveResults( int frameNumber ) {
		if( outputFile == null )
			return;

		Quaternion_F64 quat = new Quaternion_F64();
		Se3_F64 fiducialToCamera = new Se3_F64();

		outputFile.printf("%d %d",frameNumber,detector.totalFound());
		for (int i = 0; i < detector.totalFound(); i++) {
			long id = detector.getId(i);
			detector.getFiducialToCamera(i,fiducialToCamera);

			ConvertRotation3D_F64.matrixToQuaternion(fiducialToCamera.getR(),quat);

			outputFile.printf(" %d %.10f %.10f %.10f %.10f %.10f %.10f %.10f",id,
					fiducialToCamera.T.x,fiducialToCamera.T.y,fiducialToCamera.T.z,
					quat.w,quat.x,quat.y,quat.z);
		}
		outputFile.println();
	}

	private void process() {
		if( detector == null ) {
			System.err.println("Need to specify which fiducial you wish to detect");
			System.exit(1);
		}

		if( outputPath != null ) {
			try {
				outputFile = new PrintStream(outputPath);
				outputFile.println("# Results from fiducial detection ");
				outputFile.println("# These comments should include the data source and the algorithm used, but I'm busy.");
				outputFile.println("# ");
				outputFile.println("# <frame #> <number of fiducials> <fiducial id> <X> <Y> <Z> <Q1> <Q2> <Q3> <Q4> ...");
				outputFile.println("# ");
				outputFile.println("# The special Euclidean transform saved each fiducial is from fiducial to camera");
				outputFile.println("# (X,Y,Z) is the translation and (Q1,Q2,Q3,Q4) specifies a quaternion");
				outputFile.println("# ");
			} catch (FileNotFoundException e) {
				System.err.println("Failed to open output file.");
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}

		MediaManager media = DefaultMediaManager.INSTANCE;

		CameraPinholeRadial intrinsic = intrinsicPath == null ? null :  (CameraPinholeRadial)CalibrationIO.load(intrinsicPath);

		SimpleImageSequence<GrayU8> sequence = null;
		long pause = 0;
		BufferedImage buffered = null;
		if( inputType == InputType.VIDEO || inputType == InputType.WEBCAM ) {
			if( inputType == InputType.WEBCAM ) {
				String device = getCameraDeviceString();
				sequence = media.openCamera(device,desiredWidth, desiredHeight,ImageType.single(GrayU8.class));
			} else {
				// just assume 30ms is appropriate.  Should let the use specify this number
				pause = 30;
				sequence = media.openVideo(filePath,ImageType.single(GrayU8.class));
				sequence.setLoop(true);
			}
			intrinsic = handleIntrinsic(intrinsic, sequence.getNextWidth(), sequence.getNextHeight());
		} else {
			buffered = UtilImageIO.loadImage(filePath);
			if( buffered == null ) {
				System.err.println("Can't find image or it can't be read.  "+filePath);
				System.exit(1);
			}
			intrinsic = handleIntrinsic(intrinsic, buffered.getWidth(),buffered.getHeight());
		}


		ImagePanel gui = new ImagePanel();
		gui.setPreferredSize(new Dimension(intrinsic.width,intrinsic.height));
		ShowImages.showWindow(gui,"Fiducial Detector",true);
		detector.setLensDistortion(new LensDistortionRadialTangential(intrinsic),intrinsic.width,intrinsic.height);

		if( sequence != null ) {
			processStream(intrinsic,sequence,gui,pause);
		} else {
			processImage(intrinsic,buffered, gui);
		}

	}

	public static void main(String[] args) {
		FiducialDetection app = new FiducialDetection();
		try {
			app.parse(args);
		} catch( RuntimeException e ) {
			app.printHelp();
			System.out.println();
			System.out.println(e.getMessage());
			System.exit(0);
		}
		try {
			app.process();
		} catch( RuntimeException e ) {
			System.out.println();
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
}
