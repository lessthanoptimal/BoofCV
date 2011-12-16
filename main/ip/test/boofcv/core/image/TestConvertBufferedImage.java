/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.core.image;

import boofcv.alg.misc.ImageInterleavedTestingOps;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInterleavedInt8;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestConvertBufferedImage {

	Random rand = new Random(234);

	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void addMultiSpectralTests() {
		fail("add these tests");
	}

	@Test
	public void extractInterlacedInt8() {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);

		ImageInterleavedInt8 found = ConvertBufferedImage.extractInterlacedInt8(origImg);

		assertEquals(imgWidth, found.width);
		assertEquals(imgHeight, found.height);
		assertEquals(3, found.numBands);
		assertTrue(found.data != null);
		assertEquals(imgWidth * imgHeight * 3, found.data.length);
	}

	@Test
	public void extractInterlacedInt8_fail() {
		try {
			BufferedImage origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
			ConvertBufferedImage.extractInterlacedInt8(origImg);
			fail("Should hbe the wrong type");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void extractImageInt8() {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);

		ImageUInt8 found = ConvertBufferedImage.extractImageInt8(origImg);

		assertEquals(imgWidth, found.width);
		assertEquals(imgHeight, found.height);
		assertTrue(found.data != null);
		assertEquals(imgWidth * imgHeight, found.data.length);
	}

	@Test
	public void extractImageInt8_fail() {
		try {
			BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 2, rand);
			ConvertBufferedImage.extractImageInt8(origImg);
			fail("Should have had an unexpected number of bands");
		} catch (IllegalArgumentException e) {
		}

		try {
			BufferedImage origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
			ConvertBufferedImage.extractImageInt8(origImg);
			fail("Should hbe the wrong type");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void extractBuffered_InterleavedInt8() {
		// test it with 3 bands
		ImageInterleavedInt8 srcImg = new ImageInterleavedInt8(imgWidth, imgHeight, 3);
		ImageInterleavedTestingOps.randomize(srcImg, rand);

		BufferedImage img = ConvertBufferedImage.extractBuffered(srcImg);

		BoofTesting.checkEquals(img, srcImg);

		// now test it with a single band
		srcImg = new ImageInterleavedInt8(imgWidth, imgHeight, 1);
		ImageInterleavedTestingOps.randomize(srcImg, rand);
		img = ConvertBufferedImage.extractBuffered(srcImg);
		BoofTesting.checkEquals(img, srcImg);

	}

	@Test
	public void extractBuffered_Int8() {
		// use a signed image because it is checked against a byte array
		ImageUInt8 srcImg = new ImageUInt8(imgWidth, imgHeight);
		ImageTestingOps.randomize(srcImg, rand, 0, 100);

		BufferedImage img = ConvertBufferedImage.extractBuffered(srcImg);

		BoofTesting.checkEquals(img, srcImg);
	}

	@Test
	public void convertFrom_generic() {
		// test single band

		// test multi-spectral
		fail("implement");
	}

	@Test
	public void convertFromSingle() {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);

		ImageUInt8 imgInt8 = ConvertBufferedImage.convertFromSingle(origImg, (ImageUInt8) null, ImageUInt8.class);
		assertEquals(imgWidth,imgInt8.width);
		assertEquals(imgHeight,imgInt8.height);

		ImageFloat32 imgF32 = ConvertBufferedImage.convertFromSingle(origImg, (ImageFloat32) null, ImageFloat32.class);
		assertEquals(imgWidth,imgF32.width);
		assertEquals(imgHeight,imgF32.height);
	}

	@Test
	public void convertFromMulti() {
		fail("implement");
	}

	@Test
	public void convertFrom_Int8() {
		ImageUInt8 dstImg = new ImageUInt8(imgWidth, imgHeight);

		BoofTesting.checkSubImage(this, "convertFrom_Int8", false, dstImg);
	}

	public void convertFrom_Int8(ImageUInt8 dstImg) {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);
		ConvertBufferedImage.convertFrom(origImg, dstImg);

		BoofTesting.checkEquals(origImg, dstImg);
	}

	@Test
	public void convertFrom_F32() {
		ImageFloat32 dstImg = new ImageFloat32(imgWidth, imgHeight);

		BoofTesting.checkSubImage(this, "convertFrom_F32", false, dstImg);
	}

	public void convertFrom_F32(ImageFloat32 dstImg) {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);
		ConvertBufferedImage.convertFrom(origImg, dstImg);

		BoofTesting.checkEquals(origImg, dstImg, 1e-3f);
	}

	@Test
	public void convertTo_Int8() {
		ImageUInt8 srcImg = new ImageUInt8(imgWidth, imgHeight);
		ImageTestingOps.randomize(srcImg, rand, 0, 100);

		BoofTesting.checkSubImage(this, "convertTo_Int8", true, srcImg);
	}

	public void convertTo_Int8(ImageUInt8 srcImg) {
		BufferedImage dstImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);
		ConvertBufferedImage.convertTo(srcImg, dstImg);

		BoofTesting.checkEquals(dstImg, srcImg);
	}

	@Test
	public void convertTo_F32() {
		ImageFloat32 srcImg = new ImageFloat32(imgWidth, imgHeight);
		ImageTestingOps.randomize(srcImg, rand, 0, 100);

		BoofTesting.checkSubImage(this, "convertTo_F32", true, srcImg);
	}

	public void convertTo_F32( ImageFloat32 srcImg) {
		BufferedImage dstImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);
		ConvertBufferedImage.convertTo(srcImg, dstImg);

		BoofTesting.checkEquals(dstImg, srcImg, 1f);
	}

	@Test
	public void convertTo_JComponent() {
		JLabel label = new JLabel("Hi");
		// need to give it a size
		label.setBounds(0, 0, imgWidth, imgHeight);
		label.setPreferredSize(new Dimension(imgWidth, imgHeight));

		BufferedImage found = ConvertBufferedImage.convertTo(label, null);

		// see if it is the expected size
		assertEquals(found.getWidth(), label.getWidth());
		assertEquals(found.getHeight(), label.getHeight());

		// see if it blows up with an input is provided
		ConvertBufferedImage.convertTo(label, found);

		// could check to see that the pixels are not all uniform....
	}
}
