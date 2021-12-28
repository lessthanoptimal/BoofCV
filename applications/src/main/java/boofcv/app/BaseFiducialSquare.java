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

import boofcv.app.fiducials.CreateFiducialDocumentImage;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.generate.LengthUnit;
import boofcv.generate.Unit;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;

/**
 * <p>
 * Base class for generating square fiducial PDF documents for printing. Fiducials are placed in a regular grid.
 * The width of each element in the grid is the fiducial's width and a white border. The  * grid starts in the
 * page's lower left and corner.
 * </p>
 *
 * <pre>
 * Border:  The border is a no-go zone where the fiducial can't be printed inside of. This is only taken in account
 *          when automatic centering or layout of the grid on the page is requested.
 * Offset: Where the fiducial is offset inside the page. Always used. If centering is requested then the offset
 *         is automatically computed and any user provided value ignored.
 * PrintInfo: If true it will draw a string above the fiducial with the fiducial's  name and it's size
 * </pre>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class BaseFiducialSquare {

	@Option(name = "-u", aliases = {"--Units"}, usage = "Name of document units. default: cm")
	protected String _unit = Unit.CENTIMETER.abbreviation;
	public Unit unit = Unit.UNKNOWN;

	@Option(name = "-p", aliases = {"--PaperSize"}, usage = "Size of paper used. See below for predefined document sizes. "
			+ "You can manually specify any size using the following notation. W:H  where W is the width and H is the height. "
			+ "Values of W and H is specified with <number><unit abbreviation>, e.g. 6cm or 6, the unit is optional. If no unit"
			+ " are specified the default document units are used.")
	protected String _paperSize = PaperSize.LETTER.name;
	public PaperSize paperSize;

	@Option(name = "-w", aliases = {"--MarkerWidth"}, usage = "Width of each marker. In document units.")
	public float markerWidth = -1;

	@Option(name = "-s", aliases = {"--Space"}, usage = "Spacing between the fiducials. In document units.")
	public float spaceBetween = 0;

	@Option(name = "-o", aliases = {"--OutputFile"}, usage = "Name of output file. Extension determines file type. E.g. qrcode.pdf. " +
			"Valid extensions are pdf, png, jpg, gif, bmp")
	public String fileName = "qrcode";

	@Option(name = "--DisablePrintInfo", usage = "Disable printing information about the calibration target")
	public boolean disablePrintInfo = false;

	@Option(name = "--GridFill", usage = "Flag to turn on filling the entire document with a grid of qr codes")
	public boolean gridFill = false;

	@Option(name = "--DrawGrid", usage = "Draws a line showing the grid")
	public boolean drawGrid = false;

	@Option(name = "--HideInfo", usage = "Flag that's used to turn off the printing of extra information")
	public boolean hideInfo = false;

	@Option(name = "--GUI", usage = "Ignore all other command line arguments and switch to GUI mode")
	public boolean guiMode = false;

	@Option(name = "--Range", usage = "Range of markers. E.g. 5:20 will generate markers from 5 to 20, inclusive")
	String stringRange = "";

	int rangeLower = -1;
	int rangeUpper = -2;

	// if true it will send a document to the printer instead of saving it
	public boolean sendToPrinter = false;
	// specifies the file type
	public String fileType;

	public Unit getUnit() {
		return unit;
	}

	public void setUnit( Unit unit ) {
		this.unit = unit;
	}

	public void run() throws IOException {
		getFileTypeFromFileName();

		System.out.println("   File Name    : " + fileName);
		if (fileType.equals("pdf")) {
			printPdfInfo();
		} else {
			printImageInfo();
		}
		System.out.println();

		if ("pdf".equals(fileType)) {
			CreateFiducialDocumentPDF renderer = createRendererPdf(fileName, paperSize, unit);
			renderer.markerWidth = markerWidth;
			renderer.spaceBetween = spaceBetween;
			renderer.gridFill = gridFill;
			renderer.drawGrid = drawGrid;
			renderer.showInfo = !hideInfo;
			callRenderPdf(renderer);
			if (sendToPrinter) {
				renderer.sendToPrinter();
			} else {
				renderer.saveToDisk();
			}
		} else {
			CreateFiducialDocumentImage renderer = createRendererImage(fileName);
			renderer.markerWidth = (int)markerWidth;
			callRenderImage(renderer);
		}
	}

	protected void printImageInfo() {
		System.out.println("   Document      : Image");
		System.out.println("   white border  : " + spaceBetween + " (pixels)");
		System.out.println("   marker width  : " + markerWidth + " (pixels)");
	}

	protected void printPdfInfo() {
		System.out.println("   Document      : PDF");
		System.out.println("   paper         : " + paperSize);
		System.out.println("   info          : " + !disablePrintInfo);
		System.out.println("   units         : " + unit);
		System.out.println("   marker width  : " + markerWidth + " (" + unit.abbreviation + ")");
	}

	protected abstract CreateFiducialDocumentImage createRendererImage( String filename );

	protected abstract CreateFiducialDocumentPDF createRendererPdf( String documentName, PaperSize paper, Unit units );

	protected abstract void callRenderPdf( CreateFiducialDocumentPDF renderer ) throws IOException;

	protected abstract void callRenderImage( CreateFiducialDocumentImage renderer );

	private void getFileTypeFromFileName() {
		fileType = FilenameUtils.getExtension(fileName);
		if (fileType.length() == 0) {
			fileType = "pdf";
			fileName += ".pdf";
		}
		fileType = fileType.toLowerCase();
	}

	private static void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}

	public void finishParsing() {
		getFileTypeFromFileName();

		if (markerWidth < 0) {
			throw new RuntimeException("Must specify markerWidth");
		}

		parseRange();

		if (fileType.equals("pdf")) {
			if (spaceBetween == 0)
				spaceBetween = markerWidth/4;


			unit = unit == Unit.UNKNOWN ? Unit.lookup(_unit) : unit;
			if (unit == Unit.UNKNOWN) {
				failExit("Must specify a valid unit or use default");
			}
			PaperSize paperSize = PaperSize.lookup(_paperSize);
			if (paperSize == null) {
				String[] words = _paperSize.split(":");
				if (words.length != 2) failExit("Expected two value+unit separated by a :");
				LengthUnit w = new LengthUnit(words[0]);
				LengthUnit h = new LengthUnit(words[1]);
				if (w.unit != h.unit) failExit("Same units must be specified for width and height");
				paperSize = new PaperSize(w.length, h.length, w.unit);
			}
			this.paperSize = paperSize;
		}
	}

	private void parseRange() {
		if (stringRange.isEmpty())
			return;

		String[] words = stringRange.split(":");
		if (words.length != 2) {
			System.out.println("Invalid range");
			return;
		}
		rangeLower = Integer.parseInt(words[0]);
		rangeUpper = Integer.parseInt(words[1]);
	}

	protected void printHelp( CmdLineParser parser ) {
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
