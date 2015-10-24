/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.fiducial.BinaryFiducialGridSize;
import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageUInt8;
import com.github.sarxos.webcam.Webcam;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.GrowQueue_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WebcamTrackFiducial extends BaseWebcamApp {

	public static final int DEFAULT_THRESHOLD = 100;

	String intrinsicPath;

	FiducialDetector<ImageUInt8> detector;

	void printHelp() {
		System.out.println("./application <Camera Options> <Fiducial Type> <Fiducial Specific Options>");
		System.out.println();
		System.out.println("Camera Options:  (Optional)");
		System.out.println();
		System.out.println("  --Camera=<int>                     Opens the specified camera using WebcamCapture ID");
		System.out.println("                                     DEFAULT: Whatever WebcamCapture opens");
		System.out.println("  --Resolution=<width>:<height>      Specifies the image resolution.");
		System.out.println("                                     DEFAULT: Who knows or intrinsic, if specified");
		System.out.println("  --Intrinsic=<path>                 Specifies location of the intrinsic parameters file.");
		System.out.println("                                     DEFAULT: Make a crude guess.");
		System.out.println();
		System.out.println("Fiducial Types:");
		System.out.println("   BINARY");
		System.out.println("   IMAGE");
		System.out.println("   CHESSBOARD");
		System.out.println("   SQUAREGRID");
		System.out.println();
		System.out.println("Flags for BINARY:");
		System.out.println();
		System.out.println("  --Robust=<true/false>              If slower but more robust technique should be used");
		System.out.println("                                     DEFAULT: true");
		System.out.println("  --Size=<float>                     Specifies the size of all the fiducials");
		System.out.println("                                     DEFAULT: 1");
		System.out.println("  --GridWidth=<int | ALL>            Specifies how many squares to expect in the fiducial.");
		System.out.println("                                     Valid options: 3,4,5 and ALL");
		System.out.println("                                     Using ALL will attempt detection with all sizes ");
		System.out.println("                                     but keep in mind this will be slow and less accurate");
		System.out.println();
		System.out.println("Flags for IMAGE:");
		System.out.println();
		System.out.println("  --Robust=<true/false>              If slower but more robust technique should be used");
		System.out.println("                                     DEFAULT: true");
		System.out.println("  --Image=<size float>:<image path>  Adds a single image with the specified size");
		System.out.println("                                     Can be called multiple times for several images");
		System.out.println("Flags for CHESSBOARD:");
		System.out.println();
		System.out.println("  --Shape=<rows int>:<cols int>      Number of rows/columns it expects to see");
		System.out.println("  --SquareWidth=<float>              The width of each square");
		System.out.println("                                     Can be called multiple times for several images");
		System.out.println("                                     DEFAULT: 1");
		System.out.println("Flags for SQUAREGRID:");
		System.out.println();
		System.out.println("  --Shape=<rows int>:<cols int>      Number of rows/columns it expects to see");
		System.out.println("  --SquareWidth=<float>              The width of each square");
		System.out.println("                                     DEFAULT: 1");
		System.out.println("  --Space=<float>                    The space between each square");
		System.out.println("                                     DEFAULT: Same as SquareWidth");
		System.out.println();
		System.out.println("Examples:");
		System.out.println();
		System.out.println("./application BINARY --Size=1 --GridWidth=4");
		System.out.println("        Opens the default camera at default resolution looking for binary patterns with a width of 1");
		System.out.println();
		System.out.println("./application --Camera=1 --Resolution=640:480 BINARY --Robust=false --Size=1 --GridWidth=ALL");
		System.out.println("        Opens the default camera at default resolution looking for binary patterns with a width of 1");
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
		BinaryFiducialGridSize gridSize = BinaryFiducialGridSize.FOUR_BY_FOUR; // if null, means all.

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
				if("ALL".compareToIgnoreCase(parameters) == 0) {
					gridSize = null; // Use ALL
				} else {
					gridSize = BinaryFiducialGridSize.gridSizeForWidth(Integer.parseInt(parameters == null ? "4" :parameters));
				}
			} else {
				throw new RuntimeException("Unknown image option "+flagName);
			}
		}

		System.out.println("binary: robust = "+robust+" size = "+size + " grid width = " + (gridSize == null ? "ALL" : gridSize));

		ConfigFiducialBinary configFid = new ConfigFiducialBinary();
		configFid.targetWidth = size;
		configFid.gridSize = gridSize;
		configFid.squareDetector.minimumEdgeIntensity = 10;

		ConfigThreshold configThreshold ;

		if( robust )
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_SQUARE, 10);
		else
			configThreshold = ConfigThreshold.fixed(DEFAULT_THRESHOLD);

		detector = FactoryFiducial.squareBinary(configFid, configThreshold, ImageUInt8.class);
	}

	void parseImage( int index , String []args ) {
		boolean robust=true;

		List<String> paths = new ArrayList<String>();
		GrowQueue_F64 sizes = new GrowQueue_F64();

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
			} else {
				throw new RuntimeException("Unknown image option "+flagName);
			}
		}

		if( paths.isEmpty() )
			throw new RuntimeException("Need to specify patterns");

		System.out.println("image: robust = "+robust+" total patterns = "+paths.size());

		ConfigFiducialImage config = new ConfigFiducialImage();
		config.squareDetector.minimumEdgeIntensity = 10;

		ConfigThreshold configThreshold;

		if( robust )
			configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_SQUARE, 10);
		else
			configThreshold = ConfigThreshold.fixed(DEFAULT_THRESHOLD);

		SquareImage_to_FiducialDetector<ImageUInt8> detector =
				FactoryFiducial.squareImage(config, configThreshold, ImageUInt8.class);

		for (int i = 0; i < paths.size(); i++) {
			BufferedImage buffered = UtilImageIO.loadImage(paths.get(i));
			if( buffered == null )
				throw new RuntimeException("Can't find pattern "+paths.get(i));

			ImageUInt8 pattern = new ImageUInt8(buffered.getWidth(),buffered.getHeight());

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
		ConfigChessboard config = new ConfigChessboard(cols,rows,width);

		detector = FactoryFiducial.calibChessboard(config, ImageUInt8.class);
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
		ConfigSquareGrid config = new ConfigSquareGrid(cols,rows,width,space);

		detector = FactoryFiducial.calibSquareGrid(config, ImageUInt8.class);
	}

	private static IntrinsicParameters handleIntrinsic(IntrinsicParameters intrinsic, int width, int height) {
		if( intrinsic == null ) {
			System.out.println();
			System.out.println("SERIOUSLY YOU NEED TO CALIBRATE THE CAMERA YOURSELF!");
			System.out.println("There will be a lot more jitter and inaccurate pose");
			System.out.println();

			return PerspectiveOps.createIntrinsic(width, height, 35);
		} else {
			if( intrinsic.width != width || intrinsic.height != height ) {
				double ratioW = width/(double)intrinsic.width;
				double ratioH = height/(double)intrinsic.height;

				if( Math.abs(ratioW-ratioH) > 1e-8 )
					throw new RuntimeException("Can't adjust intrinsic parameters because camera ratios are different");
				PerspectiveOps.scaleIntrinsic(intrinsic,ratioW);
			}
			return intrinsic;
		}
	}

	private void process() {

		IntrinsicParameters intrinsic = intrinsicPath == null ? null : (IntrinsicParameters)UtilIO.loadXML(intrinsicPath);

		Webcam webcam = Webcam.getWebcams().get(cameraId);
		if( desiredWidth > 0 && desiredHeight > 0 )
			UtilWebcamCapture.adjustResolution(webcam, desiredWidth, desiredHeight);
		else if( intrinsic != null ) {
			System.out.println("Using intrinsic parameters for resolution "+intrinsic.width+" "+intrinsic.height);
			UtilWebcamCapture.adjustResolution(webcam, intrinsic.width, intrinsic.height);
		}
		webcam.open();

		ImagePanel gui = new ImagePanel();
		gui.setPreferredSize(webcam.getViewSize());
		ShowImages.showWindow(gui,"Fiducial Detector",true);

		int actualWidth = (int)webcam.getViewSize().getWidth();
		int actualHeight = (int)webcam.getViewSize().getHeight();

		ImageUInt8 gray = new ImageUInt8(actualWidth,actualHeight);

		intrinsic = handleIntrinsic(intrinsic, actualWidth, actualHeight);
		detector.setIntrinsic(intrinsic);

		Font font = new Font("Serif", Font.BOLD, 24);

		Se3_F64 fiducialToCamera = new Se3_F64();
		BufferedImage image;
		while( (image = webcam.getImage()) != null ) {
			ConvertBufferedImage.convertFrom(image, gray);

			try {
				detector.detect(gray);
			} catch( RuntimeException e ) {
				System.err.println("BUG!!! saving image to crash_image.png");
				UtilImageIO.saveImage(image,"crash_image.png");
				throw e;
			}
			Graphics2D g2 = image.createGraphics();

			for (int i = 0; i < detector.totalFound(); i++) {
				detector.getFiducialToCamera(i,fiducialToCamera);
				int id = detector.getId(i);
				double width = detector.getWidth(i);

				VisualizeFiducial.drawCube(fiducialToCamera,intrinsic,width,3,g2);
				VisualizeFiducial.drawLabelCenter(fiducialToCamera,intrinsic,""+id,g2);
			}

			if( intrinsicPath == null ) {
				g2.setColor(Color.RED);
				g2.setFont(font);
				g2.drawString("Uncalibrated",10,20);
			}

			gui.setBufferedImage(image);
		}
	}

	public static void main(String[] args) {
		WebcamTrackFiducial app = new WebcamTrackFiducial();
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
