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
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.app.calib.CreateECoCheckDocumentPDF;
import boofcv.app.calib.CreateHammingChessboardDocumentPDF;
import boofcv.app.calib.CreateHammingGridDocumentPDF;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.generate.LengthUnit;
import boofcv.generate.Unit;
import georegression.struct.point.Point2D_F64;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Application for generating calibration targets
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateCalibrationTarget {
	@Option(name = "-t", aliases = {"--Type"}, usage = "Type of calibration target.")
	String _type;
	CalibrationPatterns type;

	@Option(name = "-r", aliases = {"--Rows"}, usage = "Number of rows")
	int rows = -1;

	@Option(name = "-c", aliases = {"--Columns"}, usage = "Number of columns")
	int columns = -1;

	@Option(name = "-u", aliases = {"--Units"}, usage = "Name of document units. default: cm")
	String _unit = Unit.CENTIMETER.abbreviation;
	Unit unit = Unit.UNKNOWN;

	@Option(name = "-p", aliases = {"--PaperSize"}, usage = "Size of paper used. See below for predefined document sizes. "
			+ "You can manually specify any size using the following notation. W:H  where W is the width and H is the height. "
			+ "Values of W and H is specified with <number><unit abbreviation>, e.g. 6cm or 6, the unit is optional. If no unit"
			+ " are specified the default document units are used.")
	String _paperSize = PaperSize.LETTER.name;
	PaperSize paperSize;

	@Option(name = "-w", aliases = {"--ShapeWidth"}, usage = "Width of the shape or diameter if a circle. In document units.")
	float shapeWidth = -1;

	@Option(name = "-s", aliases = {"--Space"}, usage = "Spacing between the shapes. In document units.")
	float shapeSpace = -1;

	@Option(name = "-d", aliases = {"--CenterDistance"}, usage = "Distance between circle centers. In document units.")
	float centerDistance = -1;

	@Option(name = "-o", aliases = {"--OutputName"}, usage = "Name of output file. E.g. chessboard for chessboard.pdf")
	String fileName = "target";

	@Option(name = "-i", aliases = {"--DisablePrintInfo"}, usage = "Disable printing information about the calibration target")
	boolean disablePrintInfo = false;

	@Option(name = "-e", aliases = {"--Encoding"}, usage = "Encoding for hamming markers")
	String encodingName = HammingDictionary.ARUCO_MIP_25h7.name();

	@Option(name = "--EncodingOffset", usage = "Start encoding markers at this index")
	int encodingOffset = 0;

	@Option(name = "--MarkerScale", usage = "Scale of marker relative to square for hamming chessboard")
	float markerScale = 0.7f;

	@Option(name = "--ChessboardOdd", usage = "Switches chessboard from an even to odd pattern")
	private boolean chessboardOdd = false;

	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	private boolean guiMode = false;

	@Option(name = "--Landmarks", usage = "Save location of landmarks on saved document in landmarks.txt")
	boolean saveLandmarks = false;

	@Option(name = "--NumMarkers", usage = "If the target type supports multiple markers, how many should it create")
	int numMarkers = 1;

	@Option(name = "--ECoCheckError", usage = "Error correction level for ECoCheck. 0 is no error correction. 0 to 9")
	int chessBitsError = new ConfigECoCheckMarkers().errorCorrectionLevel;

	@Option(name = "--ECoCheckChecksum", usage = "Checksum bits. 0 to 8.")
	int ecocheckChecksum = new ConfigECoCheckMarkers().checksumBits;

	@Option(name = "--Printer", usage = "Send to printer instead of saving to a file")
	boolean sendToPrinter = false;

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);
		System.out.println();
		System.out.println("Target Types");
		for (CalibrationPatterns p : CalibrationPatterns.values()) {
			System.out.println("  " + p);
		}
		System.out.println();
		System.out.println("Document Types");
		for (PaperSize p : PaperSize.values()) {
			System.out.printf("  %12s  %5.0f %5.0f %s\n", p.getName(), p.width, p.height, p.getUnit().abbreviation);
		}
		System.out.println();
		System.out.println("Units");
		for (Unit u : Unit.values()) {
			System.out.printf("  %12s  %3s\n", u, u.abbreviation);
		}

		System.out.println();
		System.out.println("Examples:");
		System.out.println("-r 24 -c 28 -o target -t CIRCLE_HEXAGONAL -u cm -w 1 -d 1.2 -p LETTER");
		System.out.println("          circle hexagonal, grid 24x28, 1cm diameter, 1.2cm separation, on letter paper");
		System.out.println();
		System.out.println("-r 16 -c 12 -o target -t CIRCLE_GRID -u cm -w 1 -d 1.5 -p LETTER");
		System.out.println("          circle grid, grid 16x12, 1cm diameter, 1.5cm distance, on letter paper");
		System.out.println();
		System.out.println("-r 4 -c 3 -o target -t SQUARE_GRID -u cm -w 3 -s 3 -p LETTER");
		System.out.println("          square grid, grid 4x3, 3cm squares, 3cm space, on letter paper");
		System.out.println();
		System.out.println("-r 7 -c 5 -o target -t CHESSBOARD -u cm -w 3 -p LETTER");
		System.out.println("          chessboard, grid 7x5, 3cm squares, on letter paper");
		System.out.println();

		System.exit(1);
	}

	private static void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}

	private void finishParsing( CmdLineParser parser ) {
		if (guiMode) {
			new CreateCalibrationTargetGui();
			return;
		}

		if (rows <= 0 || columns <= 0 || _type == null)
			printHelpExit(parser);
		for (CalibrationPatterns p : CalibrationPatterns.values()) {
			if (_type.compareToIgnoreCase(p.name()) == 0) {
				type = p;
				break;
			}
		}
		if (type == null) {
			failExit("Must specify a known document type " + _type);
		}

		Unit unit = Unit.lookup(_unit);
		if (unit == Unit.UNKNOWN) {
			System.err.println("Must specify a valid unit or use default");
			System.exit(1);
			return; // make it very obvious to the compiler that this does not continue
		}
		this.unit = unit;
		PaperSize paperSize = PaperSize.lookup(_paperSize);
		if (paperSize == null) {
			String[] words = _paperSize.split(":");
			if (words.length != 2) failExit("Expected two value+unit separated by a :");
			var w = new LengthUnit(words[0]);
			var h = new LengthUnit(words[1]);
			if (w.unit != h.unit) failExit("Same units must be specified for width and height");
			paperSize = new PaperSize(w.length, h.length, w.getUnit());
		}
		this.paperSize = paperSize;

		if (rows <= 0 || columns <= 0)
			failExit("Must the number of rows and columns");

		if (shapeWidth <= 0)
			failExit("Must specify a shape width more than zero");

		switch (type) {
			case SQUARE_GRID -> {
				if (centerDistance > 0)
					failExit("Don't specify center distance for square type targets, use shape space instead");
				if (shapeSpace <= 0)
					shapeSpace = shapeWidth;
			}
			case CHESSBOARD -> {
				if (centerDistance > 0)
					failExit("Don't specify center distance for chessboard targets");
				if (shapeSpace > 0)
					failExit("Don't specify center distance for chessboard targets");
			}

			case ECOCHECK -> {
				if (centerDistance > 0)
					failExit("Don't specify center distance for chessboard targets");
				if (shapeSpace > 0)
					failExit("Don't specify center distance for chessboard targets");
				if (chessBitsError < 0 || chessBitsError > 9)
					failExit("Error level must be 0 to 9, inclusive");
				if (ecocheckChecksum < 0 || ecocheckChecksum > 8)
					failExit("Checksum must be 0 to 8, inclusive");
				if (numMarkers <= 0)
					failExit("Must specify at least one marker");
				if (rows < 4 || columns < 3)
					failExit("Grid is too small");
			}

			case HAMMING_CHESSBOARD -> {
				if (centerDistance > 0)
					failExit("Don't specify center distance for chessboard targets");
				if (shapeSpace > 0)
					failExit("Don't specify center distance for chessboard targets");
			}

			case HAMMING_GRID -> {
				if (centerDistance > 0)
					failExit("Don't specify center distance for square type targets, use shape space instead");
				if (shapeSpace <= 0)
					shapeSpace = (float)new ConfigHammingGrid(new ConfigHammingMarker()).spaceToSquare;
			}

			case CIRCLE_HEXAGONAL -> {
				if (shapeSpace > 0)
					failExit("Don't specify space for circle type targets, use center distance instead");
				if (centerDistance <= 0)
					centerDistance = shapeWidth*2;
			}
			case CIRCLE_GRID -> {
				if (shapeSpace > 0)
					failExit("Don't specify space for circle type targets, use center distance instead");
				if (centerDistance <= 0)
					centerDistance = shapeWidth*2;
			}
			default -> throw new RuntimeException("Unknown type " + type);
		}
	}

	public void run() throws IOException {
		String suffix = ".pdf";
		System.out.println("Saving to " + fileName + suffix);

		System.out.println("   paper     : " + paperSize);
		System.out.println("   type      : " + type);
		System.out.println("   rows      : " + rows);
		System.out.println("   columns   : " + columns);
		System.out.println("   info      : " + !disablePrintInfo);

		if (type == CalibrationPatterns.ECOCHECK) {
			CreateECoCheckDocumentPDF doc = ecoCheckToPdf(fileName + suffix,
					paperSize, unit, rows, columns, shapeWidth, numMarkers, chessBitsError, ecocheckChecksum);
			doc.showInfo = !disablePrintInfo;
			if (sendToPrinter) {
				doc.sendToPrinter();
			} else {
				doc.saveToDisk();
			}
			if (saveLandmarks) {
				// make sure its saves the corners in the expected units
				doc.getG().squareWidth = shapeWidth;
				doc.getG().saveCornerLocations(doc.utils.markers.get(0));
				saveLandmarks(doc.markerWidth, doc.markerHeight, doc.getG().corners,
						String.format("EcoCheck rows=%d cols=%d err=%d csum=%d markers=%d",
								rows, columns, chessBitsError, ecocheckChecksum, numMarkers));
			}
			return;
		} else if (type == CalibrationPatterns.HAMMING_CHESSBOARD) {
			CreateHammingChessboardDocumentPDF doc = hammingChessToPdf(fileName + suffix,
					paperSize, unit, rows, columns, chessboardOdd, shapeWidth, markerScale, encodingName, encodingOffset);
			doc.showInfo = !disablePrintInfo;
			if (sendToPrinter) {
				doc.sendToPrinter();
			} else {
				doc.saveToDisk();
			}

			if (saveLandmarks) {
				// make sure its saves the corners in the expected units
				doc.getG().squareWidth = 1;
				doc.config.squareSize = shapeWidth;
				doc.getG().saveCornerLocations();
				saveLandmarks(doc.config.getMarkerWidth(), doc.config.getMarkerHeight(), doc.getG().corners,
						String.format("Hamming Chessboard rows=%d cols=%d even=%s dict=%s",
								doc.config.numRows, doc.config.numCols, doc.config.chessboardEven, doc.config.markers.dictionary));
			}
			return;
		} else if (type == CalibrationPatterns.HAMMING_GRID) {
			CreateHammingGridDocumentPDF doc = hammingGridToPdf(fileName + suffix,
					paperSize, unit, rows, columns, shapeWidth, shapeSpace, encodingName, encodingOffset);
			doc.showInfo = !disablePrintInfo;
			if (sendToPrinter) {
				doc.sendToPrinter();
			} else {
				doc.saveToDisk();
			}
			if (saveLandmarks) {
				// make sure its saves the corners in the expected units
				doc.getG().squareWidth = 1;
				doc.config.squareSize = shapeWidth;
				doc.getG().saveCornerLocations();
				saveLandmarks(doc.config.getMarkerWidth(), doc.config.getMarkerHeight(), doc.getG().corners,
						String.format("Hamming Grid rows=%d cols=%d space=%f dict=%s",
								doc.config.numRows, doc.config.numCols, doc.config.spaceToSquare, doc.config.markers.dictionary));
			}
			return;
		}

		var generator = new CreateCalibrationTargetGenerator(fileName + suffix, paperSize, rows, columns, unit);
		generator.sendToPrinter = sendToPrinter;
		generator.setShowInfo(!disablePrintInfo);

		switch (type) {
			case CHESSBOARD -> generator.chessboard(shapeWidth);
			case SQUARE_GRID -> generator.squareGrid(shapeWidth, shapeSpace);
			case CIRCLE_HEXAGONAL -> generator.circleHexagonal(shapeWidth, centerDistance);
			case CIRCLE_GRID -> generator.circleGrid(shapeWidth, centerDistance);
			default -> throw new RuntimeException("Unknown target type");
		}
	}

	public void saveLandmarks( double markerWidth, double markerHeight, List<Point2D_F64> corners, String description ) {
		double paperWidth = paperSize.convertWidth(unit);
		double paperHeight = paperSize.convertHeight(unit);

		double offsetX = paperWidth/2.0 - markerWidth/2.0;
		double offsetY = paperHeight/2.0 - markerHeight/2.0;

		try (PrintStream out = new PrintStream(fileName + "_landmarks.txt")) {
			out.println("# " + description);
			out.println("# Calibration target landmark locations");
			out.println("paper=" + paperSize.name);
			out.println("units=" + unit.name());
			out.println("count=" + corners.size());
			for (int cornerID = 0; cornerID < corners.size(); cornerID++) {
				Point2D_F64 p = corners.get(cornerID);
				out.printf("%d %.8f %.8f\n", cornerID, offsetX + p.x, offsetY + p.y);
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static CreateECoCheckDocumentPDF ecoCheckToPdf( String outputFile, PaperSize paper, Unit units,
														   int rows, int columns, float squareWidth,
														   int numMarkers, int errorLevel, int checksum ) throws IOException {
		ConfigECoCheckMarkers config = ConfigECoCheckMarkers.singleShape(rows, columns, numMarkers, squareWidth);
		config.errorCorrectionLevel = errorLevel;
		config.checksumBits = checksum;

		var utils = new ECoCheckUtils();
		utils.setParametersFromConfig(config);
		utils.fixate();

		var doc = new CreateECoCheckDocumentPDF(outputFile, paper, units);
		doc.markerWidth = (columns - 1)*squareWidth;
		doc.markerHeight = (rows - 1)*squareWidth;
		doc.spaceBetween = squareWidth/2.0f;
		doc.squareWidth = squareWidth;
		doc.render(utils);
		return doc;
	}

	public static CreateHammingChessboardDocumentPDF hammingChessToPdf( String outputFile, PaperSize paper, Unit units,
																		int rows, int columns, boolean chessboardEven,
																		float squareWidth, float markerScale,
																		String encoding, int markerOffset ) throws IOException {
		HammingDictionary dictionary = HammingDictionary.valueOf(encoding);
		ConfigHammingChessboard config = ConfigHammingChessboard.create(dictionary, rows, columns, 1.0);
		config.markerOffset = markerOffset;
		config.markerScale = markerScale;
		config.squareSize = squareWidth;
		config.chessboardEven = chessboardEven;

		var doc = new CreateHammingChessboardDocumentPDF(outputFile, paper, units);
		doc.markerWidth = columns*squareWidth;
		doc.markerHeight = rows*squareWidth;
		doc.spaceBetween = squareWidth/2.0f;
		doc.squareWidth = squareWidth;
		doc.render(config);
		return doc;
	}

	public static CreateHammingGridDocumentPDF hammingGridToPdf( String outputFile, PaperSize paper, Unit units,
																 int rows, int columns,
																 float squareWidth, float spaceToSquare,
																 String encoding, int markerOffset ) throws IOException {
		HammingDictionary dictionary = HammingDictionary.valueOf(encoding);
		ConfigHammingGrid config = ConfigHammingGrid.create(dictionary, rows, columns, 1.0, spaceToSquare);
		config.markerOffset = markerOffset;
		config.spaceToSquare = spaceToSquare;
		config.squareSize = squareWidth;

		var doc = new CreateHammingGridDocumentPDF(outputFile, paper, units);
		doc.markerWidth = (float)config.getMarkerWidth();
		doc.markerHeight = (float)config.getMarkerHeight();
		doc.spaceBetween = squareWidth/2.0f;
		doc.squareWidth = squareWidth;
		doc.render(config);
		return doc;
	}

	public static void main( String[] args ) {
		var generator = new CreateCalibrationTarget();
		var parser = new CmdLineParser(generator);
		try {
			parser.parseArgument(args);
			generator.finishParsing(parser);
			if (!generator.guiMode)
				generator.run();
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
