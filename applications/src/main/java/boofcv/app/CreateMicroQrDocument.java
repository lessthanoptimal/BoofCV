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

import boofcv.alg.fiducial.microqr.MicroQrCode;
import boofcv.alg.fiducial.microqr.MicroQrCodeEncoder;
import boofcv.alg.fiducial.microqr.MicroQrCodeMaskPattern;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.app.micrqr.CreateMicroQrDocumentImage;
import boofcv.app.micrqr.CreateMicroQrDocumentPDF;
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
 * Application for generating Micro QR Code markers
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateMicroQrDocument {

	@Option(name = "-t", aliases = {"--Text", "--Message"},
			usage = "Specifies the message(s) to encode. For each message at least one QR Code will be added to the paper(s)")
	public List<String> messages = new ArrayList<>();

	@Option(name = "-m", aliases = {"--Mask"}, usage = "Specify which mask to use. Most people shouldn't use this flag. Options: 00, 01, 10, 11")
	protected @Nullable String _mask = null;
	public @Nullable MicroQrCodeMaskPattern mask;

	@Option(name = "-e", aliases = {"--Error"}, usage = "Error correction level. Options: L,M,Q. Robustness: 7%, 15%, 25%, respectively ")
	protected String _error = "";
	public @Nullable MicroQrCode.ErrorLevel error;

	@Option(name = "-v", aliases = {"--Version"}, usage =
			"Micro QR-Code version. Determines size and amount of data. If unspecified it will be automatically selected based on the data. Values 1 to 4.")
	public int version = -1;

	@Option(name = "-n", aliases = {"--Encoding"}, usage =
			"Type of data that can be encoded. Default is auto select. Options: NUMERIC, ALPHANUMERIC, BYTE, KANJI")
	protected String _encoding = "AUTO";
	public @Nullable QrCode.Mode encoding;

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
	public float moduleWidth = -1;

	@Option(name = "-s", aliases = {"--Space"}, usage = "Spacing between the fiducials. In document units.")
	public float spaceBetween = 2;

	@Option(name = "-o", aliases = {"--OutputName"}, usage = "Name of output file. Extension determines file type. E.g. qrcode.pdf. " +
			"Valid extensions are pdf, png, jpg, gif, bmp")
	public String fileName = "microqr";

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
		System.out.println("   creates PDF with a grid that will fill the entire page composed of two micro qr codes on letter sized paper");
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

		mask = _mask == null ? null : MicroQrCodeMaskPattern.lookupMask(_mask);
		if (_error.isEmpty()) {
			error = null;
		} else {
			error = MicroQrCode.ErrorLevel.lookup(_error);
		}

		if (version == 0 || version > 4 || version < -1) {
			failExit("Version must be from 1 to 4 or set to -1 for auto select");
		}

		encoding = QrCode.Mode.lookup(_encoding);

		getFileTypeFromFileName();

		if (fileType.equals("pdf")) {
			if (moduleWidth < 0 && markerWidth < 0) {
				throw new RuntimeException("Must specify moduleWidth or markerWidth");
			} else if (markerWidth < 0) {
				markerWidth = moduleWidth*QrCode.totalModules(version);
			}

			unit = unit == Unit.UNKNOWN ? Unit.lookup(_unit) : unit;
			if (unit == Unit.UNKNOWN) {
				failExit("Must specify a valid unit or use default");
			}
			PaperSize paperSize = PaperSize.lookup(_paperSize);
			if (paperSize == null) {
				String[] words = _paperSize.split(":");
				if (words.length != 2) failExit("Expected two value+unit separated by a :");
				var w = new LengthUnit(words[0], unit);
				var h = new LengthUnit(words[1], unit);
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
			System.out.println("   info         : " + !hideInfo);
			System.out.println("   units        : " + unit);
			System.out.println("   marker width : " + markerWidth + " (" + unit.abbreviation + ")");
		} else {
			System.out.println("   Document  : Image");
//			System.out.println("   marker width : " + markerWidth + " (pixels)");
//			System.out.println("   white border : " + spaceBetween + " (pixels)");
		}
		System.out.println();

		var markers = new ArrayList<MicroQrCode>();
		for (String message : messages) {
			var encoder = new MicroQrCodeEncoder();
			if (mask != null)
				encoder.setMask(mask);
			encoder.setError(error);

			if (encoding != null) {
				switch (encoding) {
					case NUMERIC -> encoder.addNumeric(message);
					case ALPHANUMERIC -> encoder.addAlphanumeric(message);
					case BYTE -> encoder.addBytes(message);
					case KANJI -> encoder.addKanji(message);
					default -> throw new RuntimeException("Unknown mode");
				}
			} else {
				encoder.addAutomatic(message);
			}

			try {
				MicroQrCode qr = encoder.fixate();
				markers.add(qr);

				System.out.println("   Message");
				System.out.println("     length    : " + qr.message.length());
				System.out.println("     version   : " + qr.version);
				System.out.println("     encoding  : " + qr.mode);
				System.out.println("     error     : " + qr.error);
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
				var renderer = new CreateMicroQrDocumentPDF(fileName, paperSize, unit);
				renderer.markerWidth = markerWidth;
				renderer.spaceBetween = gridFill || markers.isEmpty() ? spaceBetween : 0.0f;
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
				var renderer = new CreateMicroQrDocumentImage(fileName, 20);
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
		var generator = new CreateMicroQrDocument();
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
