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

import boofcv.app.markers.CreateSquareMarkerDocumentImage;
import boofcv.app.markers.CreateSquareMarkerDocumentPDF;
import boofcv.misc.LengthUnit;
import boofcv.misc.Unit;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.awt.print.PrinterException;
import java.io.IOException;

/**
 * <p>
 * Base class for generating square fiducial PDF documents for printing.  Fiducials are placed in a regular grid.
 * The width of each element in the grid is the fiducial's width (pattern + black border) and a white border.  The
 * grid starts in the page's lower left and corner.
 * </p>
 *
 * <pre>
 * Border:  The border is a no-go zone where the fiducial can't be printed inside of.  This is only taken in account
 *          when automatic centering or layout of the grid on the page is requested.
 * Offset: Where the fiducial is offset inside the page.  Always used.  If centering is requested then the offset
 *         is automatically computed and any user provided value ignored.
 * PrintInfo: If true it will draw a string above the fiducial with the fiducial's  name and it's size
 * </pre>
 *
 * @author Peter Abeles
 */
public abstract class BaseFiducialSquare {

	// TODO add back print grid

	// threshold for converting to a binary image
	public int threshold = 255 / 2;

	@Option(name = "-u", aliases = {"--Units"}, usage = "Name of document units.  default: cm")
	private String _unit = Unit.CENTIMETER.abbreviation;
	public Unit unit;

	@Option(name = "-p", aliases = {"--PaperSize"}, usage = "Size of paper used.  See below for predefined document sizes.  "
			+ "You can manually specify any size using the following notation. W:H  where W is the width and H is the height.  "
			+ "Values of W and H is specified with <number><unit abbreviation>, e.g. 6cm or 6, the unit is optional.  If no unit"
			+ " are specified the default document units are used.")
	private String _paperSize = PaperSize.LETTER.name;
	public PaperSize paperSize;

	@Option(name = "-w", aliases = {"--MarkerWidth"}, usage = "Width of each marker.  In document units.")
	public float markerWidth = -1;

	@Option(name = "-s", aliases = {"--Space"}, usage = "Spacing between the markers.  In document units.")
	public float spaceBetween = 0;

	@Option(name = "-o", aliases = {"--OutputName"}, usage = "Name of output file. Extension determines file type. E.g. qrcode.pdf. " +
			"Valid extensions are pdf, png, jpg, gif, bmp")
	public String fileName = "qrcode";

	@Option(name = "--DisablePrintInfo", usage = "Disable printing information about the calibration target")
	public boolean disablePrintInfo = false;

	@Option(name = "--GridFill", usage = "Flag to turn on filling the entire document with a grid of qr codes")
	public boolean gridFill = false;

	@Option(name = "--HideInfo", usage = "Flag that's used to turn off the printing of extra information")
	public boolean hideInfo = false;

	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	public boolean guiMode = false;

	// if true it will send a document to the printer instead of saving it
	public boolean sendToPrinter = false;
	// specifies the file type
	public String fileType;

	// how wide the fiducial's black border is relative to its total width
	public float blackBorderFractionalWidth;

	public Unit getUnit() {
		return unit;
	}

	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	public void setBlackBorderFractionalWidth(float blackBorderFractionalWidth) {
		this.blackBorderFractionalWidth = blackBorderFractionalWidth;
	}

	public void run() throws IOException {

		getFileTypeFromFileName();

		if (fileType.equals("pdf")) {
			System.out.println("   Document     : PDF");
			System.out.println("   paper        : " + paperSize);
			System.out.println("   info         : " + (!disablePrintInfo));
			System.out.println("   units        : " + unit);
			System.out.println("   marker width : " + markerWidth + " (" + unit.abbreviation + ")");
		} else {
			System.out.println("   Document  : Image");
		}
		System.out.println();

		switch (fileType) {
			case "pdf": {
				CreateSquareMarkerDocumentPDF renderer = new CreateSquareMarkerDocumentPDF(fileName, paperSize, unit);
				renderer.markerWidth = markerWidth;
				renderer.spaceBetween = spaceBetween;
				renderer.gridFill = gridFill;
				renderer.showInfo = !hideInfo;
				callRenderPdf(renderer);
				if (sendToPrinter) {
					try {
						renderer.sendToPrinter();
					} catch (PrinterException e) {
						throw new RuntimeException(e);
					}
				} else
					renderer.saveToDisk();
			}
			break;

			default: {
				CreateSquareMarkerDocumentImage renderer = new CreateSquareMarkerDocumentImage(fileName);
				renderer.setWhiteBorder((int)spaceBetween);
				renderer.setMarkerWidth((int)markerWidth);
				callRenderImage(renderer);
			}
			break;
		}
	}

	protected abstract void callRenderPdf(CreateSquareMarkerDocumentPDF renderer);

	protected abstract void callRenderImage(CreateSquareMarkerDocumentImage renderer);

	/**
	 * Draws the grid in light grey on the document
	 */
//	private void printGrid() throws IOException {
//		float pageWidth = (float)paper.convertWidth(unit)*UNIT_TO_POINTS;
//		float pageHeight = (float)paper.convertHeight(unit)*UNIT_TO_POINTS;
//
////		pcs.setLineCapStyle(1);
//		pcs.setStrokingColor(0.75);
//
//		for (int i = 0; i <= numCols; i++) {
//			float x = offsetX + i*fiducialTotalWidth;
//			pcs.moveTo(x,0);
//			pcs.lineTo(x,pageHeight);
//		}
//		for (int i = 0; i <= numRows; i++) {
//			float y = offsetY + i*fiducialTotalWidth;
//			pcs.moveTo(0,y);
//			pcs.lineTo(pageWidth,y);
//		}
//		pcs.closeAndStroke();
//	}

//	public static String binaryToHex( GrayU8 binary ) {
//
//		if( binary.width%8 != 0 )
//			throw new RuntimeException("Width must be divisible by 8");
//
//		StringBuilder s = new StringBuilder(binary.width*binary.height/4);
//
//		for (int y = binary.height-1; y >= 0 ; y--) {
//			int i = y*binary.width;
//			for (int x = 0; x < binary.width; x += 8, i+= 8) {
//				int value = 0;
//				for (int j = 0; j < 8; j++) {
//					value |= binary.data[i+j] << (7-j);
//				}
//
//				String hex = Integer.toHexString(value);
//				if( hex.length() == 1 )
//					hex = "0"+hex;
//				s.append(hex);
//			}
//		}
//
//		return s.toString();
//	}
	private void getFileTypeFromFileName() {
		fileType = FilenameUtils.getExtension(fileName);
		if (fileType.length() == 0) {
			fileType = "pdf";
			fileName += ".pdf";
		}
		fileType = fileType.toLowerCase();
	}

	private static void failExit(String message) {
		System.err.println(message);
		System.exit(1);
	}

	public void finishParsing() {
		getFileTypeFromFileName();

		if (fileType.equals("pdf")) {
			if (markerWidth < 0) {
				throw new RuntimeException("Must specify markerWidth");
			}

			unit = unit == null ? Unit.lookup(_unit) : unit;
			if (unit == null) {
				failExit("Must specify a valid unit or use default");
			}
			paperSize = PaperSize.lookup(_paperSize);
			if (paperSize == null) {
				String words[] = _paperSize.split(":");
				if (words.length != 2) failExit("Expected two value+unit separated by a :");
				LengthUnit w = new LengthUnit(words[0]);
				LengthUnit h = new LengthUnit(words[1]);
				if (w.unit != h.unit) failExit("Same units must be specified for width and height");
				paperSize = new PaperSize(w.length, h.length, w.unit);
			}
		}
	}

	protected void printHelp(CmdLineParser parser) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);
		System.out.println();
		System.out.println("Document Types");
		for (PaperSize p : PaperSize.values()) {
			System.out.printf("  %12s  %5.0f %5.0f %s\n", p.getName(), p.width, p.height, p.unit.abbreviation);
		}
		System.out.println();
		System.out.println("Units");
		for (Unit u : Unit.values()) {
			System.out.printf("  %12s  %3s\n", u, u.abbreviation);
		}
		System.out.println();
		System.out.println("Examples:");
		System.out.println();
	}
}
