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
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCreateFiducialSquareBinary extends CommonFiducialPdfChecks {

	private final ConfigThreshold configThreshold = ConfigThreshold.fixed(125);

	public void createDocument( String args ) throws IOException, InterruptedException {
		CreateFiducialSquareBinary.main(args.split("\\s+"));
	}

	public GrayF32 loadImageGray() throws IOException {
		BufferedImage image = loadImage();
		GrayF32 gray = new GrayF32(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image,gray);
		return gray;
	}

	@Test
	public void single() throws IOException, InterruptedException {
		int expected = 234;
		createDocument(String.format("-PrintInfo -PrintGrid -PageSize=letter -OutputFile=%s 3 %d",
				document_name+".pdf",expected));
		GrayF32 gray = loadImageGray();

		ConfigFiducialBinary config = new ConfigFiducialBinary(30);
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(config,configThreshold,GrayF32.class);

		detector.detect(gray);

		assertEquals(1,detector.totalFound());
		assertEquals(expected,detector.getId(0));
	}

	@Test
	public void grid() throws IOException, InterruptedException {
		int expected[] = new int []{234,123};
		createDocument(String.format("-PrintInfo -PrintGrid  -Grid=fill -PageSize=letter -OutputFile=%s 3 %d %d",
				document_name+".pdf",expected[0],expected[1]));
		GrayF32 gray = loadImageGray();

		ConfigFiducialBinary config = new ConfigFiducialBinary(30);
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(config,configThreshold,GrayF32.class);

		detector.detect(gray);

		assertEquals(9,detector.totalFound());
		for (int i = 0; i < detector.totalFound(); i++) {
			assertEquals(expected[i%2],detector.getId(i));
		}
	}

	/**
	 * Adjust the border and the number of grid elements
	 */
	@Test
	public void customized() throws IOException, InterruptedException {
		int expected[] = new int []{234,23233};
		double border = 0.1;
		int gridWidth = 5;
		createDocument(String.format("-BlackBorder=%f -BinaryGridWidth=%d -Grid=fill -PageSize=letter -OutputFile=%s 3 %d %d",
				border,gridWidth,document_name+".pdf",expected[0],expected[1]));
		GrayF32 gray = loadImageGray();

		ConfigFiducialBinary config = new ConfigFiducialBinary(30);
		config.borderWidthFraction = border;
		config.gridWidth = gridWidth;
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareBinary(config,configThreshold,GrayF32.class);

		detector.detect(gray);

		assertEquals(9,detector.totalFound());
		for (int i = 0; i < detector.totalFound(); i++) {
			assertEquals(expected[i%2],detector.getId(i));
		}
	}

}