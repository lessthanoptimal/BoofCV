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

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCreateFiducialSquareBinary extends CommonFiducialPdfChecks {

	private final ConfigThreshold configThreshold = ConfigThreshold.fixed(125);

	public void createDocument( String args ) throws InterruptedException {
		// suppress stdout
		out.out = new PrintStream(new OutputStream(){@Override public void write( int b ){}});
		CreateFiducialSquareBinary.main(args.split("\\s+"));
		out.used = false; // this will ignore the stdout usage which is unavoidable
		err.used = false;
	}

	@Test void single_pdf() throws IOException, InterruptedException {
		// don't need to specif spacing
		int expected = 234;
		createDocument(String.format("--PaperSize letter --OutputFile %s -w 3 -n %d",
				document_name + ".pdf", expected));
		GrayF32 gray = loadPdfAsGray();

		ConfigFiducialBinary config = new ConfigFiducialBinary(30);
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(config, configThreshold, GrayF32.class);

		detector.detect(gray);

		assertEquals(1, detector.totalFound());
		assertEquals(expected, detector.getId(0));
	}

	@Test void grid() throws IOException, InterruptedException {
		int[] expected = new int[]{234, 123};
		createDocument(String.format("--GridFill --DrawGrid --PaperSize letter --OutputFile %s -w 5 -s 2 -n %d -n %d",
				document_name + ".pdf", expected[0], expected[1]));
		GrayF32 gray = loadPdfAsGray();

		ConfigFiducialBinary config = new ConfigFiducialBinary(30);
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(config, configThreshold, GrayF32.class);

		detector.detect(gray);

		assertEquals(9, detector.totalFound());
		for (int i = 0; i < detector.totalFound(); i++) {
			assertEquals(expected[i%2], detector.getId(i));
		}
	}

	/**
	 * Adjust the border and the number of grid elements
	 */
	@Test void customized_pdf() throws IOException, InterruptedException {
		int[] expected = new int[]{234, 23233};
		double border = 0.1;
		int gridWidth = 5;
		createDocument(String.format("-bw %.1f --PatternGridWidth %d --GridFill --PaperSize letter --OutputFile %s -w 5 -s 2 -n %d -n %d",
				border, gridWidth, document_name + ".pdf", expected[0], expected[1]));
		GrayF32 gray = loadPdfAsGray();

		ConfigFiducialBinary config = new ConfigFiducialBinary(30);
		config.borderWidthFraction = border;
		config.gridWidth = gridWidth;
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(config, configThreshold, GrayF32.class);

		detector.detect(gray);

		assertEquals(9, detector.totalFound());
		for (int i = 0; i < detector.totalFound(); i++) {
			assertEquals(expected[i%2], detector.getId(i));
		}
	}

	/**
	 * Create two images
	 */
	@Test void create_png() throws InterruptedException {
		int[] expected = new int[]{0, 234, 678};
		createDocument(String.format("--OutputFile %s -w 200 -s 20 -n %d -n %d -n %d",
				document_name + ".png", expected[0], expected[1], expected[2]));
		for (int i = 0; i < expected.length; i++) {
			GrayF32 gray = loadPngAsGray(document_name + expected[i] + ".png");

			ConfigFiducialBinary config = new ConfigFiducialBinary(30);
			FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(config, configThreshold, GrayF32.class);

			detector.detect(gray);
			assertEquals(1, detector.totalFound());
			assertEquals(expected[i], detector.getId(0));
		}
	}
}
