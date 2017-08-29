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

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.SquareImage_to_FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialImage;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCreateFiducialSquareImage extends CommonFiducialPdfChecks {

	private final ConfigThreshold configThreshold = ConfigThreshold.fixed(125);

	private final static String names[] =
			new String[]{"temp0.jpg","temp1.jpg"};

	public void createDocument( String args ) throws IOException, InterruptedException {
		CreateFiducialSquareImage.main(args.split("\\s+"));
	}

	public GrayF32 loadImageGray() throws IOException {
		BufferedImage image = loadImage();
		GrayF32 gray = new GrayF32(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image,gray);

		return gray;
	}

	@BeforeClass
	public static void before() {
		BufferedImage output = new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = output.createGraphics();
		g2.setColor(Color.WHITE);

		g2.fillRect(0,100,100,30);
		g2.fillRect(70,130,30,70);

		UtilImageIO.saveImage(output, names[0]);

		g2.fillOval(100,100,50,50);
		UtilImageIO.saveImage(output, names[1]);
	}

	@AfterClass
	public static void cleanUpImages() {
		for( String s : names) {
			new File(s).delete();
		}
	}

	private SquareImage_to_FiducialDetector<GrayF32> createDetector(ConfigFiducialImage config) {
		SquareImage_to_FiducialDetector<GrayF32> detector = FactoryFiducial.squareImage(config,configThreshold,GrayF32.class);
		for( String s : names) {
			detector.addPatternImage(UtilImageIO.loadImage(s, GrayF32.class), 125, 30);
		}
		return detector;
	}

	@Test
	public void single() throws IOException, InterruptedException {
		createDocument(String.format("-PrintInfo -PageSize=letter -OutputFile=%s 4 %s",
				document_name+".pdf", names[0]));
		GrayF32 gray = loadImageGray();

		ConfigFiducialImage config = new ConfigFiducialImage();
		FiducialDetector<GrayF32> detector = createDetector(config);

		detector.detect(gray);

		assertEquals(1,detector.totalFound());
		assertEquals(0,detector.getId(0));
	}

	@Test
	public void grid() throws IOException, InterruptedException {
		createDocument(String.format("-PrintInfo -Grid=fill -PageSize=letter -OutputFile=%s 3 %s %s",
				document_name+".pdf", names[0], names[1]));
		GrayF32 gray = loadImageGray();

		ConfigFiducialImage config = new ConfigFiducialImage();
		FiducialDetector<GrayF32> detector = createDetector(config);

		detector.detect(gray);

		assertEquals(9,detector.totalFound());
		for (int i = 0; i < detector.totalFound(); i++) {
			assertEquals(i%2,detector.getId(i));
		}
	}

}