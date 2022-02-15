/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.alg.fiducial.aztec.AztecEncoder;
import boofcv.app.aztec.CreateAztecCodeDocumentImage;
import boofcv.app.aztec.CreateAztecCodeDocumentPDF;
import boofcv.app.micrqr.CreateMicroQrGui;
import boofcv.generate.LengthUnit;
import boofcv.generate.Unit;
import boofcv.gui.BoofSwingUtil;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Application for generating Aztec Code markers
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateAztecCodeDocument {

	@Option(name = "-t", aliases = {"--Text", "--Message"},
			usage = "Specifies the message(s) to encode. For each message at least one QR Code will be added to the paper(s)")
	public List<String> messages = new ArrayList<>();

	@Option(name = "-e", aliases = {"--Error"}, usage = "Amount of error correction added. Multiple of data bytes.")
	public double errorFraction = -1;

	@Option(name = "-l", aliases = {"--Layers"}, usage = "Number of layers in the data region")
	public int numLayers = -1;

	@Option(name = "--Structure", usage =
			"The marker's structure. COMPACT or FULL")
	protected String _structure = "FULL";
	public @Nullable AztecCode.Structure structure = null;

	@Option(name = "-u", aliases = {"--Units"}, usage = "Name of document units. default: cm")
	protected String _unit = Unit.CENTIMETER.abbreviation;
	public Unit unit = Unit.UNKNOWN;

	@Option(name = "-p", aliases = {"--PaperSize"}, usage = "Size of paper used. See below for predefined document sizes. "
			+ "You can manually specify any size using the following notation. W:H  where W is the width and H is the height. "
			+ "Values of W and H is specified with <number><unit abbreviation>, e.g. 6cm or 6, the unit is optional. If no unit"
			+ " are specified the default document units are used.")
	protected String _paperSize = PaperSize.LETTER.name;
	public PaperSize paperSize;

	@Option(name = "-w", aliases = {"--MarkerWidth"}, usage = "Width of the Micro QR Code. In document units.")
	public float markerWidth = -1;

	@Option(name = "-mw", aliases = {"--ModuleWidth"}, usage = "Specify size of QR Code by its module/cells. In document units.")
	public float squareWidth = -1;

	@Option(name = "-s", aliases = {"--Space"}, usage = "Spacing between the fiducials. In document units.")
	public float spaceBetween = 2;

	@Option(name = "-o", aliases = {"--OutputName"}, usage = "Name of output file. Extension determines file type. E.g. qrcode.pdf. " +
			"Valid extensions are pdf, png, jpg, gif, bmp")
	public String fileName = "microqr";

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

	@Option(name = "--SaveCorners", usage = "Save location of marker corners in the document to corners.txt")
	boolean saveCorners = false;

	// if true it will send a document to the printer instead of saving it
	public boolean sendToPrinter = false;
	// specifies the file type
	public String fileType;

	private static void printHelpExit( CmdLineParser parser ) {
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

		System.out.println("-t \"123-09494\" -t \"Hello There!\" -p LETTER -w 5 --GridFill -o document.pdf");
		System.out.println("   creates PDF with a grid that will fill the entire page composed of two Aztec Codes on letter sized paper");
		System.out.println();
		System.out.println("-t \"123-09494\" -t \"Hello There!\" -o document.png");
		System.out.println("   Creates two png images names document0.png and document1.png");
		System.out.println();
		System.exit(1);
	}

	private static void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}

	public void finishParsing() {
		if (errorFraction != -1 && errorFraction < 0) {
			failExit("Error must be -1 or >= 0");
		}

		try {
			if (structure == null)
				structure = AztecCode.Structure.valueOf(_structure);
		} catch (IllegalArgumentException e) {
			failExit("Structure must be COMPACT or FULL");
		}
		Objects.requireNonNull(structure);

		if (numLayers > structure.getMaxDataLayers()) {
			failExit("For " + structure + " markers the number of layers must be <= " + structure.getMaxDataLayers());
		}

		getFileTypeFromFileName();

		if (fileType.equals("pdf")) {
			if (squareWidth < 0 && markerWidth < 0) {
				throw new RuntimeException("Must specify squareWidth or markerWidth");
			}

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
				paperSize = new PaperSize(w.length, h.length, w.getUnit());
			}
			this.paperSize = paperSize;
		}
	}

	public void run() throws IOException {

		if (messages == null || messages.size() == 0) {
			throw new RuntimeException("Need to specify a message");
		}

		getFileTypeFromFileName();

		System.out.println("   File Name    : " + fileName);
		if (fileType.equals("pdf")) {
			Objects.requireNonNull(unit);
			System.out.println("   Document     : PDF");
			System.out.println("   paper        : " + paperSize);
			System.out.println("   info         : " + !disablePrintInfo);
			System.out.println("   units        : " + unit);
			System.out.println("   marker width : " + markerWidth + " (" + unit.abbreviation + ")");
		} else {
			System.out.println("   Document  : Image");
//			System.out.println("   marker width : " + markerWidth + " (pixels)");
//			System.out.println("   white border : " + spaceBetween + " (pixels)");
		}
		System.out.println();

		Objects.requireNonNull(structure);
		var markers = new ArrayList<AztecCode>();
		for (String message : messages) {
			AztecEncoder encoder = new AztecEncoder().setStructure(structure);
			if (errorFraction >= 0)
				encoder.setEcc(errorFraction);
			if (numLayers > 0)
				encoder.setLayers(numLayers);

			// TODO change to automatic
			encoder.addUpper(message);

			try {
				AztecCode marker = encoder.fixate();
				markers.add(marker);

				System.out.println("   Message");
				System.out.println("     length     : " + marker.message.length());
				System.out.println("     structure  : " + marker.structure);
				System.out.println("     layers     : " + marker.dataLayers);
				System.out.printf("     correction : %.2f\n", marker.getCorrectionLevel());
			} catch (Exception e) {
				System.err.println("Failed fixating: '" + message + "'");
				if (e.getMessage() != null) {
					System.err.println("Description:     '" + e.getMessage() + "'");
				} else {
					e.printStackTrace(System.err);
				}
				System.exit(1);
			}
		}

		switch (fileType) {
			case "pdf" -> {
				Objects.requireNonNull(unit);
				var renderer = new CreateAztecCodeDocumentPDF(fileName, paperSize, unit);
				renderer.markerWidth = markerWidth > 0 ? markerWidth : squareWidth*markers.get(0).getMarkerWidthSquares();
				renderer.spaceBetween = spaceBetween;
				renderer.gridFill = gridFill;
				renderer.drawGrid = drawGrid;
				renderer.showInfo = !hideInfo;
				renderer.render(markers);

				if (saveCorners) {
					renderer.saveLandmarks(renderer.markerWidth, renderer.markerWidth,
							renderer.markers.get(0).bounds.vertexes.toList(),
							"Marker Square Bounding Box", "microqr_corners.txt");
				}

				if (sendToPrinter) {
					renderer.sendToPrinter();
				} else
					renderer.saveToDisk();
			}
			default -> {
				// TODO support the ability to specify how large the QR code is in pixels
				var renderer = new CreateAztecCodeDocumentImage(fileName, 20);
//				renderer.setWhiteBorder((int)spaceBetween);
//				renderer.setMarkerWidth((int)markerWidth);
				renderer.render(markers);
			}
		}
	}

	private void getFileTypeFromFileName() {
		fileType = FilenameUtils.getExtension(fileName);
		if (fileType.length() == 0) {
			fileType = "pdf";
			fileName += ".pdf";
		}
		fileType = fileType.toLowerCase();
	}

	public static void main( String[] args ) {
		var generator = new CreateAztecCodeDocument();
		var parser = new CmdLineParser(generator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				BoofSwingUtil.invokeNowOrLater(CreateMicroQrGui::new);
			} else {
				generator.finishParsing();
				generator.run();
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			System.err.println(e.getMessage());
			printHelpExit(parser);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
