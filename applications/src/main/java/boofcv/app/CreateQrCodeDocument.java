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

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeEncoder;
import boofcv.alg.fiducial.qrcode.QrCodeMaskPattern;
import boofcv.app.qrcode.CreateQrCodeDocumentImage;
import boofcv.app.qrcode.CreateQrCodeDocumentPDF;
import boofcv.app.qrcode.CreateQrCodeGui;
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
 * Application for generating QR Code markers
 *
 * @author Peter Abeles
 */
// TODO Support multiple QR's in GUI
@SuppressWarnings({"NullAway.Init"})
public class CreateQrCodeDocument extends BaseMarkerDocument {

	@Option(name = "-t", aliases = {"--Text", "--Message"},
			usage = "Specifies the message(s) to encode. For each message at least one QR Code will be added to the paper(s)")
	public List<String> messages = new ArrayList<>();

	@Option(name = "-m", aliases = {"--Mask"}, usage = "Specify which mask to use. Most people shouldn't use this flag. Options: 000, 001, 010, 011, 100, 101, 110, 111")
	protected @Nullable String _mask = null;
	public @Nullable QrCodeMaskPattern mask;

	@Option(name = "-e", aliases = {"--Error"}, usage = "Error correction level. Options: L,M,Q,H. Robustness: 7%, 15%, 25%, 30%, respectively ")
	protected String _error = "M";
	public @Nullable QrCode.ErrorLevel error;

	@Option(name = "-v", aliases = {"--Version"}, usage =
			"QR-Code version. Determines size and amount of data. If unspecified it will be automatically selected based on the data. Values 1 to 40.")
	public int version = -1;

	@Option(name = "-n", aliases = {"--Encoding"}, usage =
			"Type of data that can be encoded. Default is auto select. Options: NUMERIC, ALPHANUMERIC, BYTE, KANJI")
	protected String _encoding = "AUTO";
	public @Nullable QrCode.Mode encoding;

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

		System.out.println("-t \"http://boofcv.org\" -t \"Hello There!\" -p LETTER -w 5 --GridFill -o document.pdf");
		System.out.println("   creates PDF with a grid that will fill the entire page composed of two qr codes on letter sized paper");
		System.out.println();
		System.out.println("-t \"http://boofcv.org\" -t \"Hello There!\" -o document.png");
		System.out.println("   Creates two png images names document0.png and document1.png");
		System.out.println();
		System.exit(1);
	}

	private static void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}

	public void finishParsing() {
		mask = _mask == null ? null : QrCodeMaskPattern.lookupMask(_mask);
		error = QrCode.ErrorLevel.lookup(_error);

		if (version == 0 || version > 40 || version < -1) {
			failExit("Version must be from 1 to 40 or set to -1 for auto select");
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
			if (unit == Unit.UNKNOWN ) {
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

		List<QrCode> markers = new ArrayList<>();
		for (String message : messages) {
			QrCodeEncoder encoder = new QrCodeEncoder();
			if (mask != null)
				encoder.setMask(mask);
			encoder.setError(error);
			if (version > 0)
				encoder.setVersion(version);

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
			QrCode qr = encoder.fixate();
			markers.add(qr);

			System.out.println("   Message");
			System.out.println("     length    : " + qr.message.length());
			System.out.println("     version   : " + qr.version);
			System.out.println("     encoding  : " + qr.mode);
			System.out.println("     error     : " + qr.error);
		}

		switch (fileType) {
			case "pdf" -> {
				Objects.requireNonNull(unit);
				CreateQrCodeDocumentPDF renderer = new CreateQrCodeDocumentPDF(fileName, paperSize, unit);
				renderer.markerWidth = markerWidth;
				renderer.spaceBetween = gridFill || markers.size() > 1 ? spaceBetween : 0.0f;
				renderer.gridFill = gridFill;
				renderer.drawGrid = drawGrid;
				renderer.showInfo = !hideInfo;
				renderer.render(markers);
				if (sendToPrinter) {
					renderer.sendToPrinter();
				} else
					renderer.saveToDisk();
			}
			default -> {
				// TODO support the ability to specify how large the QR code is in pixels
				CreateQrCodeDocumentImage renderer = new CreateQrCodeDocumentImage(fileName, 20);
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
		CreateQrCodeDocument generator = new CreateQrCodeDocument();
		CmdLineParser parser = new CmdLineParser(generator);

		if (args.length == 0) {
			printHelpExit(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				BoofSwingUtil.invokeNowOrLater(CreateQrCodeGui::new);
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
