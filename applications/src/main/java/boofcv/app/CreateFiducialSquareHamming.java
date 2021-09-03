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
import boofcv.app.fiducials.CreateSquareHammingDocumentImage;
import boofcv.app.fiducials.CreateSquareHammingDocumentPDF;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.generate.Unit;
import boofcv.gui.BoofSwingUtil;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;

/**
 * Outputs an PDF document describing a square hamming fiducial that encodes the specified number
 *
 * @author Peter Abeles
 */
public class CreateFiducialSquareHamming extends BaseFiducialSquare {

	@Option(name = "-e", aliases = {"--Encoding"}, usage = "Encoding for hamming markers")
	String encodingName = HammingDictionary.ARUCO_MIP_25h7.name();

	@Option(name = "-m", aliases = {"--Markers"}, usage = "Specifies which markers to encode", handler = LongArrayOptionHandler.class)
	public Long[] numbers = new Long[0];

	public final ConfigHammingMarker config = new ConfigHammingMarker();

	@Override public void finishParsing() {
		super.finishParsing();
		HammingDictionary dictionary = HammingDictionary.valueOf(encodingName);
		config.setTo(ConfigHammingMarker.loadDictionary(dictionary));
	}

	@Override
	protected CreateFiducialDocumentImage createRendererImage( String filename ) {
		var ret = new CreateSquareHammingDocumentImage(filename);
		ret.config.setTo(config);
		ret.setWhiteBorder((int)spaceBetween);
		return ret;
	}

	@Override
	protected CreateFiducialDocumentPDF createRendererPdf( String documentName, PaperSize paper, Unit units ) {
		var ret = new CreateSquareHammingDocumentPDF(documentName, paper, units);
		ret.config.setTo(config);
		return ret;
	}

	@Override
	protected void callRenderPdf( CreateFiducialDocumentPDF renderer ) throws IOException {
		DogArray_I32 markerIDs = createMarkerIDs();
		((CreateSquareHammingDocumentPDF)renderer).render(markerIDs);
	}

	@NotNull private DogArray_I32 createMarkerIDs() {
		DogArray_I32 markerIDs = new DogArray_I32();
		for (int i = 0; i < this.numbers.length; i++) {
			markerIDs.add(this.numbers[i].intValue());
		}
		for (int markerID = rangeLower; markerID <= rangeUpper; markerID++) {
			markerIDs.add(markerID);
		}
		return markerIDs;
	}

	@Override
	protected void callRenderImage( CreateFiducialDocumentImage renderer ) {
		DogArray_I32 markerIDs = createMarkerIDs();
		((CreateSquareHammingDocumentImage)renderer).render(markerIDs);
	}

	@Override
	protected void printHelp( CmdLineParser parser ) {
		super.printHelp(parser);

		System.out.println("Renders 6 markers in PNG format 220x220 pixels, 20 pixel white border");
		System.out.println("-e ARUCO_ORIGINAL --Range 0:5 -w 200 -s 20 -o hamming.png");
		System.out.println();
		System.out.println("Creates a PDF document the fills in a grid from these three fiducials");
		System.out.println("5cm with 2cm space between fiducials.");
		System.out.println("-n 1 -n 0 -n 10 -u cm -w 5 -s 2 --GridFill -o hamming.pdf");
		System.out.println();
		System.out.println("Opens a GUI");
		System.out.println("--GUI");

		System.exit(-1);
	}

	public static void main( String[] args ) {
		var generator = new CreateFiducialSquareHamming();
		var parser = new CmdLineParser(generator);

		if (args.length == 0) {
			generator.printHelp(parser);
		}

		try {
			parser.parseArgument(args);
			if (generator.guiMode) {
				BoofSwingUtil.invokeNowOrLater(CreateFiducialSquareHammingGui::new);
			} else {
				if (generator.numbers == null) {
					System.err.println("Must specify at least one number");
					System.exit(1);
				}
				generator.finishParsing();
				generator.run();
			}
		} catch (CmdLineException e) {
			// handling of wrong arguments
			generator.printHelp(parser);
			System.err.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
