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

import boofcv.abst.fiducial.AztecCodeDetector;
import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests create calibration target application. Invokes the application to create a target. Then converts
 * the postscript document into an image. After the image has been generated it is then processed by the fiducial
 * detector.
 *
 * @author Peter Abeles
 */
public class TestCreateAztecCodeDocument extends CommonFiducialPdfChecks {

	public void createDocument( String args ) {
		// suppress stdout
		out.out = new PrintStream(new OutputStream(){@Override public void write( int b ){}});
		CreateAztecCodeDocument.main(args.split("\\s+"));
		out.used = false; // this will ignore the stdout usage which is unavoidable
		err.used = false;
	}

	@Test void single() throws IOException {
		createDocument("-t http://boofcv.org -p LETTER -w 5 -o target.pdf");
		BufferedImage image = loadPDF();

		GrayF32 gray = ConvertBufferedImage.convertFrom(image, (GrayF32)null);

//		ShowImages.showBlocking(gray,"Rendered", 10_000, true);

		AztecCodeDetector<GrayF32> detector = FactoryFiducial.aztec(null, GrayF32.class);

		detector.process(gray);
		List<AztecCode> found = detector.getDetections();

		checkFound(found, "http://boofcv.org");
	}

	@Test void two() throws IOException {
		createDocument("-t http://boofcv.org -t second -p LETTER -w 5 -o target.pdf");
		BufferedImage image = loadPDF();

		GrayF32 gray = ConvertBufferedImage.convertFrom(image, (GrayF32)null);

//		ShowImages.showBlocking(gray,"Rendered", 10_000, true);

		AztecCodeDetector<GrayF32> detector = FactoryFiducial.aztec(null, GrayF32.class);

		detector.process(gray);
		List<AztecCode> found = detector.getDetections();

		assertEquals(2, found.size());

		checkFound(found, "http://boofcv.org", "second");
	}

	@Test void gridAndCustomDocument() throws IOException {
		createDocument("-t http://boofcv.org -t second -p 14cm:14cm --GridFill -w 5 -o target.pdf");
		BufferedImage image = loadPDF();

		GrayF32 gray = ConvertBufferedImage.convertFrom(image, (GrayF32)null);

//		ShowImages.showBlocking(gray,"Rendered", 10_000, true);

		AztecCodeDetector<GrayF32> detector = FactoryFiducial.aztec(null, GrayF32.class);

		detector.process(gray);
		List<AztecCode> found = detector.getDetections();

		assertEquals(4, found.size());

		checkFound(found, "http://boofcv.org", "second");
	}

	/** Single marker with no border should be created */
	@Test void noAddedBorder() throws IOException {
		createDocument("-t 1234567 -p 10.5:10.5 -w 10 -o target.pdf");

		GrayF32 gray = ConvertBufferedImage.convertFrom(loadPDF(), GrayF32.class, true);

		AztecCodeDetector<GrayF32> detector = FactoryFiducial.aztec(null, GrayF32.class);

		detector.process(gray);

		checkFound(detector.getDetections(), "1234567");
	}

	private void checkFound( List<AztecCode> found, String... messages ) {
		boolean[] matches = new boolean[messages.length];
		int total = 0;

		for (int i = 0; i < found.size(); i++) {
			int which = -1;
			for (int j = 0; j < messages.length; j++) {
				if (messages[j].equals(found.get(i).message)) {
					which = j;
				}
			}

			if (which >= 0) {
				if (!matches[which]) {
					total++;
				}
				matches[which] = true;
			}
		}

		assertEquals(messages.length, total);
	}
}
