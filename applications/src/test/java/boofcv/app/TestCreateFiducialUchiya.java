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

import boofcv.abst.fiducial.Uchiya_to_FiducialDetector;
import boofcv.alg.fiducial.dots.RandomDotMarkerGenerator;
import boofcv.factory.fiducial.ConfigUchiyaMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestCreateFiducialUchiya extends CommonFiducialPdfChecks {

	CreateFiducialRandomDot defaults = new CreateFiducialRandomDot();

	public TestCreateFiducialUchiya() {
		document_name = "uchiya";
	}

	public void createDocument( String args ) {
		// suppress stdout
		out.out = new PrintStream(new OutputStream(){@Override public void write( int b ){}});
		CreateFiducialRandomDot.main(args.split("\\s+"));
		out.used = false; // this will ignore the stdout usage which is unavoidable
		err.used = false;
	}

	@Test void case0() throws IOException {
		int N = 4;
		createDocument("--MarkerBorder -w 8 -um 4 -n 30 -o uchiya.pdf");
		BufferedImage image = loadPDF();

		GrayU8 gray = new GrayU8(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		ConfigUchiyaMarker config = new ConfigUchiyaMarker();
		config.markerWidth = 8.0;
		config.markerHeight = 8.0;
		Uchiya_to_FiducialDetector<GrayU8> detector = FactoryFiducial.randomDots(config, GrayU8.class);

		Random rand = new Random(defaults.randomSeed);
		for (int i = 0; i < N; i++) {
			detector.addMarker(RandomDotMarkerGenerator.createRandomMarker(
					rand, 30, config.markerWidth, config.markerHeight, defaults.dotDiameter));
		}

		detector.detect(gray);

		checkResults(N, detector);
	}

	@Test void case1() throws IOException {
		int N = 8;
		createDocument("-rs 4445 -w 5 -um 8 -n 22 -dd 0.6 -o uchiya.pdf");
		BufferedImage image = loadPDF();

		GrayU8 gray = new GrayU8(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

//		ShowImages.showBlocking(gray, "Foo", 5_000);

		ConfigUchiyaMarker config = new ConfigUchiyaMarker();
		config.markerWidth = 5.0;
		config.markerHeight = 5.0;
		Uchiya_to_FiducialDetector<GrayU8> detector = FactoryFiducial.randomDots(config, GrayU8.class);

		Random rand = new Random(4445);
		for (int i = 0; i < N; i++) {
			detector.addMarker(RandomDotMarkerGenerator.createRandomMarker(
					rand, 22, config.markerWidth, config.markerHeight, 0.6));
		}

		detector.detect(gray);

		checkResults(N, detector);
	}

	@Test
	void rectangleMarker() throws IOException {
		int N = 4;
		createDocument("--MarkerBorder -w 5 -h 10 -um 4 -n 30 -o uchiya.pdf");
		BufferedImage image = loadPDF();

		GrayU8 gray = new GrayU8(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		ConfigUchiyaMarker config = new ConfigUchiyaMarker();
		config.markerWidth = 5.0;
		config.markerHeight = 10.0;
		Uchiya_to_FiducialDetector<GrayU8> detector = FactoryFiducial.randomDots(config, GrayU8.class);

		Random rand = new Random(defaults.randomSeed);
		for (int i = 0; i < N; i++) {
			detector.addMarker(RandomDotMarkerGenerator.createRandomMarker(
					rand, 30, config.markerWidth, config.markerHeight, defaults.dotDiameter));
		}

		detector.detect(gray);

		checkResults(N, detector);
	}

	private void checkResults( int n, Uchiya_to_FiducialDetector<GrayU8> detector ) {
		assertEquals(n, detector.totalFound());
		int[] count = new int[n];
		for (int i = 0; i < n; i++) {
			count[(int)detector.getId(i)]++;
		}
		for (int i = 0; i < n; i++) {
			assertEquals(1, count[i]);
		}
	}
}
