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
import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCreateFiducialSquareImage extends CommonFiducialPdfChecks {

	private final ConfigThreshold configThreshold = ConfigThreshold.fixed(125);

	private final static String[] names =
			new String[]{"temp0.jpg", "temp1.jpg"};

	private void createDocument( String args ) throws IOException {
		// suppress stdout
		out.out = new PrintStream(new OutputStream(){@Override public void write( int b ){}});
		CreateFiducialSquareImage.main(args.split("\\s+"));
		out.used = false; // this will ignore the stdout usage which is unavoidable
		err.used = false;
	}

	private GrayF32 loadImageGray() throws IOException {
		BufferedImage image = loadPDF();
		GrayF32 gray = new GrayF32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		return gray;
	}

	@BeforeEach
	void before() {
		BufferedImage output = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = output.createGraphics();
		g2.setColor(Color.WHITE);

		g2.fillRect(0, 100, 100, 30);
		g2.fillRect(70, 130, 30, 70);

		UtilImageIO.saveImage(output, names[0]);

		g2.fillOval(100, 100, 50, 50);
		UtilImageIO.saveImage(output, names[1]);
	}

	@AfterEach
	void cleanUpImages() {
		for (String s : names) {
			new File(s).delete();
		}
	}

	private SquareImage_to_FiducialDetector<GrayF32> createDetector( ConfigFiducialImage config ) {
		SquareImage_to_FiducialDetector<GrayF32> detector = FactoryFiducial.squareImage(config, configThreshold, GrayF32.class);
		for (String s : names) {
			detector.addPatternImage(Objects.requireNonNull(UtilImageIO.loadImage(s, GrayF32.class)), 125, 30);
		}
		return detector;
	}

	@Test
	void single_pdf() throws IOException {
		createDocument(String.format("--PaperSize letter --OutputFile %s -w 6 -i %s",
				document_name + ".pdf", names[0]));
		GrayF32 gray = loadImageGray();

		ConfigFiducialImage config = new ConfigFiducialImage();
		FiducialDetector<GrayF32> detector = createDetector(config);

		detector.detect(gray);

		assertEquals(1, detector.totalFound());
		assertEquals(0, detector.getId(0));
	}

	@Test
	void grid_pdf() throws IOException {
		createDocument(String.format("--PaperSize letter --GridFill --OutputFile %s -w 5 -s 2 -i %s -i %s",
				document_name + ".pdf", names[0], names[1]));
		GrayF32 gray = loadImageGray();

		ConfigFiducialImage config = new ConfigFiducialImage();
		FiducialDetector<GrayF32> detector = createDetector(config);

		detector.detect(gray);

		assertEquals(9, detector.totalFound());
		for (int i = 0; i < detector.totalFound(); i++) {
			assertEquals(i%2, detector.getId(i));
		}
	}

	@Test
	void multiple_png() throws IOException {
		createDocument(String.format("--OutputFile %s -w 200 -s 20 -i %s -i %s",
				document_name + ".png", names[0], names[1]));

		ConfigFiducialImage config = new ConfigFiducialImage();
		FiducialDetector<GrayF32> detector = createDetector(config);
		for (int i = 0; i < names.length; i++) {
			GrayF32 gray = loadPngAsGray(document_name + names[i] + ".png");

			detector.detect(gray);
			assertEquals(1, detector.totalFound());
			assertEquals(i, detector.getId(0));
		}
	}
}
