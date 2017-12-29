/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.app.qrcode.CreateQrCodeDocumentPDF;
import boofcv.app.qrcode.CreateQrCodeGui;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Application for generating calibration targets
 *
 * @author Peter Abeles
 */
// TODO Support image output
public class CreateQrCodeDocument {

	@Option(name = "-t", aliases = {"--Text","--Message"},
			usage="Specifies the message(s) to encode. For each message at least one QR Code will be added to the paper(s)")
	public List<String> messages;

	@Option(name="-m",aliases = {"--Mask"}, usage="Specify which mask to use. Most people shouldn't use this flag. Options: 000, 001, 010, 011, 100, 101, 110, 111")
	private String _mask=null;
	public QrCodeMaskPattern mask;

	@Option(name="-e",aliases = {"--Error"}, usage="Error correction level. Options: L,M,Q,H. Robustness: 7%, 15%, 25%, 30%, respectively ")
	private String _error="M";
	public QrCode.ErrorLevel error;

	@Option(name="-v",aliases = {"--Version"}, usage="QR-Code version. Determines size and amount of data. If unspecified it will be automatically selected based on the data. Values 1 to 40.")
	public int version=-1;

	@Option(name="-n",aliases = {"--Encoding"}, usage="Type of data that can be encoded. Default is auto select. Options: NUMERIC, ALPHANUMERIC, BYTE, KANJI")
	private String _encoding = "AUTO";
	public QrCode.Mode encoding;

	@Option(name="-u",aliases = {"--Units"}, usage="Name of document units.  default: cm")
	private String _unit = Unit.CENTIMETER.abbreviation;
	private Unit unit;

	@Option(name="-p",aliases = {"--PaperSize"}, usage="Size of paper used.  See below for predefined document sizes.  "
	+"You can manually specify any size using the following notation. W:H  where W is the width and H is the height.  "
	+"Values of W and H is specified with <number><unit abbreviation>, e.g. 6cm or 6, the unit is optional.  If no unit"
	+" are specified the default document units are used.")
	private String _paperSize = PaperSize.LETTER.name;
	public PaperSize paperSize;

	@Option(name="-w",aliases = {"--MarkerWidth"}, usage="Width of the QR Code.  In document units.")
	private float markerWidth=3;

	@Option(name="-s",aliases = {"--Space"}, usage="Spacing between the markers.  In document units.")
	private float spaceBetween =2;

	@Option(name="-o",aliases = {"--OutputName"}, usage="Name of output file.  E.g. qrcode.pdf")
	public String fileName = "qrcode";

	@Option(name="-i",aliases = {"--DisablePrintInfo"}, usage="Disable printing information about the calibration target")
	public boolean disablePrintInfo = false;

	@Option(name="--GridFill", usage="Flag to turn on filling the entire document with a grid of qr codes")
	public boolean gridFill = false;

	@Option(name="--HideInfo", usage="Flag that's used to turn off the printing of extra information")
	public boolean hideInfo = false;

	@Option(name="--GUI", usage="Ignore all other command line arguments and switch to GUI mode")
	private boolean guiMode = false;

	private static void printHelpExit( CmdLineParser parser ) {
		parser.getProperties().withUsageWidth(120);
		parser.printUsage(System.out);
		System.out.println();
		System.out.println("Document Types");
		for( PaperSize p : PaperSize.values() ) {
			System.out.printf("  %12s  %5.0f %5.0f %s\n",p.getName(),p.width,p.height,p.unit.abbreviation);
		}
		System.out.println();
		System.out.println("Units");
		for( Unit u : Unit.values() ) {
			System.out.printf("  %12s  %3s\n",u,u.abbreviation);
		}

		System.out.println();
		System.out.println("Examples:"); // TODO WRITE
		System.out.println();

		System.exit(1);
	}

	private static void failExit( String message ) {
		System.err.println(message);
		System.exit(1);
	}

	public void finishParsing() {

		mask = _mask == null ? null : QrCodeMaskPattern.lookupMask(_mask);
		error = _error == null ? null : QrCode.ErrorLevel.lookup(_error);

		if( version == 0 || version > 40 || version < -1 ) {
			System.err.println("Version must be from 1 to 40 or set to -1 for auto select");
			System.exit(1);
		}

		encoding = _encoding == null ? null : QrCode.Mode.lookup(_encoding);

		unit = Unit.lookup(_unit);
		if( unit == null ) {
			System.err.println("Must specify a valid unit or use default");
			System.exit(1);
		}
		paperSize = PaperSize.lookup(_paperSize);
		if( paperSize == null ) {
			String words[] = _paperSize.split(":");
			if( words.length != 2) failExit("Expected two value+unit separated by a :");
			LengthUnit w = new LengthUnit(words[0]);
			LengthUnit h = new LengthUnit(words[1]);
			if( w.unit != h.unit ) failExit("Same units must be specified for width and height");
			paperSize = new PaperSize(w.length,h.length,w.unit);
		}

		if( markerWidth <= 0 )
			failExit("Must specify a shape width more than zero");
	}

	public void run() throws IOException {

		if( messages == null || messages.size() == 0 ) {
			System.err.println("Need to specify a message");
			return;
		}

		System.out.println("   paper     : "+paperSize);
		System.out.println("   version   : "+version);
		System.out.println("   info      : "+(!disablePrintInfo));

		List<QrCode> markers = new ArrayList<>();
		for( String message : messages ) {
			QrCodeEncoder encoder = new QrCodeEncoder();
			if( mask != null )
				encoder.setMask(mask);
			encoder.setError(error!=null?error: QrCode.ErrorLevel.M);
			if( version > 0 )
				encoder.setVersion(version);

			if( encoding != null ) {
				switch( encoding ) {
					case NUMERIC:encoder.numeric(message);break;
					case ALPHANUMERIC:encoder.alphanumeric(message);break;
					case BYTE:encoder.bytes(message);break;
					case KANJI:encoder.kanji(message);break;
					default: throw new RuntimeException("Unknown mode");
				}
			} else {
				encoder.encodeAuto(message);
			}
			markers.add(encoder.fixate());
		}

		CreateQrCodeDocumentPDF renderer = new CreateQrCodeDocumentPDF(fileName,paperSize,unit);
		renderer.markerWidth = markerWidth;
		renderer.spaceBetween = spaceBetween;
		renderer.gridFill = gridFill;
		renderer.showInfo = !hideInfo;
		renderer.render(markers);
	}

	public static void main(String[] args) {
		CreateQrCodeDocument generator = new CreateQrCodeDocument();
		CmdLineParser parser = new CmdLineParser(generator);
		try {
			parser.parseArgument(args);
			if( generator.guiMode ) {
				new CreateQrCodeGui();
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
