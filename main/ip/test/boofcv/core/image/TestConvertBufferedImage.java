/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageInterleavedTestingOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.*;
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
	public void extractInterleavedU8() {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);

		InterleavedU8 found = ConvertBufferedImage.extractInterleavedU8(origImg);

		assertEquals(0, found.startIndex);
		assertEquals(imgWidth*3, found.stride);
		assertEquals(imgWidth, found.width);
		assertEquals(imgHeight, found.height);
		assertEquals(3, found.numBands);
		assertTrue(found.data != null);
		assertEquals(imgWidth * imgHeight * 3, found.data.length);

		// test a sub-image input
		origImg = origImg.getSubimage(1,2,5,6);

		found = ConvertBufferedImage.extractInterleavedU8(origImg);

		assertEquals(2*10*3+3, found.startIndex);
		assertEquals(imgWidth*3, found.stride);
		assertEquals(5, found.width);
		assertEquals(6, found.height);
		assertEquals(3, found.numBands);
	}

	@Test(expected=IllegalArgumentException.class)
	public void extractInterlacedInt8_indexed() {
		BufferedImage origImg = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_BYTE_INDEXED);

		ConvertBufferedImage.extractInterleavedU8(origImg);
	}

	@Test
	public void extractInterleavedU8_fail() {
		try {
			BufferedImage origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
			ConvertBufferedImage.extractInterleavedU8(origImg);
			fail("Should hbe the wrong type");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void extractImageUInt8() {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);

		ImageUInt8 found = ConvertBufferedImage.extractImageUInt8(origImg);

		assertEquals(imgWidth, found.width);
		assertEquals(imgHeight, found.height);
		assertTrue(found.data != null);
		assertEquals(imgWidth * imgHeight, found.data.length);

		// test a sub-image input
		origImg = origImg.getSubimage(1,2,5,6);

		found = ConvertBufferedImage.extractImageUInt8(origImg);

		assertEquals(2*10+1, found.startIndex);
		assertEquals(imgWidth, found.stride);
		assertEquals(5, found.width);
		assertEquals(6, found.height);
	}

	@Test(expected=IllegalArgumentException.class)
	public void extractImageInt8_indexed() {
		BufferedImage origImg = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_BYTE_INDEXED);

		ConvertBufferedImage.extractImageUInt8(origImg);
	}

	@Test
	public void extractImageInt8_fail() {
		try {
			BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);
			ConvertBufferedImage.extractImageUInt8(origImg);
			fail("Should have had an unexpected number of bands");
		} catch (IllegalArgumentException e) {
		}

		try {
			BufferedImage origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
			ConvertBufferedImage.extractImageUInt8(origImg);
			fail("Should be the wrong type");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void extractBuffered_InterleavedU8() {
		// test it with 3 bands
		InterleavedU8 srcImg = new InterleavedU8(imgWidth, imgHeight, 3);
		ImageInterleavedTestingOps.randomize(srcImg, rand);

		BufferedImage img = ConvertBufferedImage.extractBuffered(srcImg);

		BoofTesting.checkEquals(img, srcImg);

		// now test it with a single band
		srcImg = new InterleavedU8(imgWidth, imgHeight, 1);
		ImageInterleavedTestingOps.randomize(srcImg, rand);
		img = ConvertBufferedImage.extractBuffered(srcImg);
		BoofTesting.checkEquals(img, srcImg);

	}

	@Test
	public void extractBuffered_Int8() {
		// use a signed image because it is checked against a byte array
		ImageUInt8 srcImg = new ImageUInt8(imgWidth, imgHeight);
		ImageMiscOps.fillUniform(srcImg, rand, 0, 100);

		BufferedImage img = ConvertBufferedImage.extractBuffered(srcImg);

		BoofTesting.checkEquals(img, srcImg);
	}

	/**
	 * Predeclare an image to convert the buffered image into and step through each data type and image type
	 */
	@Test
	public void convertFrom_single_ms() {
		Class[] types = new Class[]{ImageUInt8.class, ImageFloat32.class};

		for (Class t : types) {
			for (int i = 0; i < 2; i++) {
				ImageBase image;
				if (i == 0) {
					image = GeneralizedImageOps.createSingleBand(t, imgWidth, imgHeight);
				} else {
					image = new MultiSpectral(t, imgWidth, imgHeight, 3);
				}

				BoofTesting.checkSubImage(this, "convertFrom_single_ms", false, image);
			}
		}
	}

	public void convertFrom_single_ms(ImageBase dstImg) {
		BufferedImage origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
		ConvertBufferedImage.convertFrom(origImg, dstImg,false);

		BoofTesting.checkEquals(origImg, dstImg, false, 1e-3f);
	}

	/**
	 * Ensures that the orderRgb flag is correctly handled
	 */
	@Test
	public void convertFrom_single_ms_orderRgb() {
		Class []bandTypes = new Class[]{ImageUInt8.class,ImageFloat32.class};
		for( Class b : bandTypes ) {
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_3BYTE_BGR,b,true);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_4BYTE_ABGR,b,true);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_INT_ARGB,b,true);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_INT_RGB,b,true);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_INT_BGR,b,true);

			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_3BYTE_BGR,b,false);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_4BYTE_ABGR,b,false);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_INT_ARGB,b,false);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_INT_RGB,b,false);
			convertFrom_single_ms_orderRgb(BufferedImage.TYPE_INT_BGR,b,false);
		}
	}

	public void convertFrom_single_ms_orderRgb( int buffType , Class bandType , boolean reorder ) {
		BufferedImage input = TestConvertRaster.createByteBuffByType(imgWidth, imgHeight, buffType, rand);

		int numBands = input.getRaster().getNumBands();

		MultiSpectral output = new MultiSpectral(bandType,imgWidth,imgHeight,numBands);

		ConvertBufferedImage.convertFrom(input, output, reorder);

		// this always returns it in ARGB order
		checkBandOrder(buffType, reorder, input, numBands, output);
	}

	@Test
	public void convertFromSingle() {
		BufferedImage origImg;

		for( int i = 0; i < 5; i++ ) {

			if( i == 0 )
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);
			else if( i == 1 )
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);
			else if( i == 2 )
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 4, rand);
			else if( i == 3 )
				origImg = TestConvertRaster.createByteIndexed(imgWidth, imgHeight, rand );
			else
				origImg = TestConvertRaster.createIntBuff(imgWidth,imgHeight,rand);

			for( int j = 0; j < 2; j++ ) {
				if( j == 1 ) {
					origImg = origImg.getSubimage(1,2,imgWidth-1,imgHeight-2);
				}

				ImageUInt8 imgInt8 = ConvertBufferedImage.convertFromSingle(origImg, null, ImageUInt8.class);
				assertEquals(origImg.getWidth(), imgInt8.width);
				assertEquals(origImg.getHeight(), imgInt8.height);
				BoofTesting.checkEquals(origImg, imgInt8, false, 1);

				ImageFloat32 imgF32 = ConvertBufferedImage.convertFromSingle(origImg, null, ImageFloat32.class);
				assertEquals(origImg.getWidth(), imgF32.width);
				assertEquals(origImg.getHeight(), imgF32.height);
				BoofTesting.checkEquals(origImg, imgF32, false, 1);
			}
		}
	}

	/**
	 * Not all types support conversion into 16 bit images, so the special case of 16bit image are handled here
	 */
	@Test
	public void convertFromSingle_I16() {
		BufferedImage origImg = TestConvertRaster.createShortBuff(imgWidth, imgHeight, rand);

		for( int j = 0; j < 2; j++ ) {
			if( j == 1 ) {
				origImg = origImg.getSubimage(1,2,imgWidth-1,imgHeight-2);
			}

			ImageUInt16 imgU16 = ConvertBufferedImage.convertFromSingle(origImg, null, ImageUInt16.class);
			assertEquals(origImg.getWidth(), imgU16.width);
			assertEquals(origImg.getHeight(), imgU16.height);
			BoofTesting.checkEquals(origImg, imgU16, false, 1);

			ImageSInt16 imgS16 = ConvertBufferedImage.convertFromSingle(origImg, null, ImageSInt16.class);
			assertEquals(origImg.getWidth(), imgS16.width);
			assertEquals(origImg.getHeight(), imgS16.height);
			BoofTesting.checkEquals(origImg, imgS16, false, 1);
		}
	}

	@Test
	public void convertFromMulti() {
		BufferedImage origImg;

		for( int i = 0; i < 4; i++ ) {

			if( i == 0 )
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);
			else if( i == 1 )
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);
			else if( i == 2 )
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 4, rand);
			else if( i == 3 )
				origImg = TestConvertRaster.createByteIndexed(imgWidth, imgHeight, rand );
			else
				origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);

			for( int j = 0; j < 2; j++ ) {
				if( j == 1 ) {
					origImg = origImg.getSubimage(1,2,imgWidth-1,imgHeight-2);
				}
				MultiSpectral<ImageUInt8> imgInt8 = ConvertBufferedImage.convertFromMulti(origImg, null, false, ImageUInt8.class);
				assertEquals(origImg.getWidth(), imgInt8.width);
				assertEquals(origImg.getHeight(), imgInt8.height);
				BoofTesting.checkEquals(origImg, imgInt8, false, 1);

				MultiSpectral<ImageFloat32> imgF32 = ConvertBufferedImage.convertFromMulti(origImg, null, false,ImageFloat32.class);
				assertEquals(origImg.getWidth(), imgF32.width);
				assertEquals(origImg.getHeight(), imgF32.height);
				BoofTesting.checkEquals(origImg, imgF32,false,  1);
			}
		}
	}

	/**
	 * Create an image and convert it into a buffered image
	 */
	@Test
	public void convertTo_single_ms() {
		Class[] types = new Class[]{ImageUInt8.class, ImageUInt16.class, ImageFloat32.class};

		for (Class t : types) {
			for (int i = 0; i < 2; i++) {
				ImageBase image;
				if (i == 0) {
					image = GeneralizedImageOps.createSingleBand(t, imgWidth, imgHeight);
				} else {
					if( t == ImageUInt16.class )
						continue; // convert into 16bit gray scale buffered images isn't supported yet
					image = new MultiSpectral(t, imgWidth, imgHeight, 3);
				}
				GImageMiscOps.fillUniform(image, rand, 0, 100);

				BoofTesting.checkSubImage(this, "convertTo_single_ms", false, image);
			}
		}
	}

	public void convertTo_single_ms(ImageBase srcImg) {
		BufferedImage dstImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
		ConvertBufferedImage.convertTo(srcImg, dstImg,false);

		BoofTesting.checkEquals(dstImg, srcImg,false,  1);
	}

	/**
	 * Ensures that the orderRgb flag is correctly handled
	 */
	@Test
	public void convertTo_single_ms_orderRgb() {
		Class []bandTypes = new Class[]{ImageUInt8.class,ImageFloat32.class};
		for( Class b : bandTypes ) {
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, true);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, true);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_INT_ARGB, b, true);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_INT_RGB, b, true);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_INT_BGR, b, true);

			convertTo_single_ms_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, false);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, false);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_INT_ARGB, b, false);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_INT_RGB, b, false);
			convertTo_single_ms_orderRgb(BufferedImage.TYPE_INT_BGR,b,false);
		}
	}

	public void convertTo_single_ms_orderRgb( int buffType , Class bandType , boolean reorder ) {
		BufferedImage output = TestConvertRaster.createByteBuffByType(imgWidth, imgHeight, buffType, rand);

		int numBands = output.getRaster().getNumBands();

		MultiSpectral input = new MultiSpectral(bandType,imgWidth,imgHeight,numBands);

		ConvertBufferedImage.convertTo(input, output, reorder);

		checkBandOrder(buffType, reorder, output, numBands, input);
	}

	private void checkBandOrder(int buffType, boolean reorder, BufferedImage imageA, int numBands, MultiSpectral imageB) {
		// this always returns it in ARGB order
		int found = imageA.getRGB(5,6);

		if( reorder ) {
			if( numBands == 4 ) {
				assertTrue(Math.abs(((found>>24)&0xFF) - GeneralizedImageOps.get(imageB.getBand(3), 5, 6)) < 1e-8 );
				assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB.getBand(0),5,6)) < 1e-8 );
				assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB.getBand(1),5,6)) < 1e-8 );
				assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB.getBand(2),5,6)) < 1e-8 );
			} else {
				assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB.getBand(0),5,6)) < 1e-8 );
				assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB.getBand(1),5,6)) < 1e-8 );
				assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB.getBand(2),5,6)) < 1e-8 );
			}
		} else {
			if( numBands == 4 ){
				if( buffType == BufferedImage.TYPE_4BYTE_ABGR ) {
					assertTrue(Math.abs(((found>>24)&0xFF) - GeneralizedImageOps.get(imageB.getBand(0),5,6)) < 1e-8 );
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB.getBand(3),5,6)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB.getBand(2),5,6)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB.getBand(1),5,6)) < 1e-8 );
				} else if( buffType == BufferedImage.TYPE_INT_ARGB ) {
					assertTrue(Math.abs(((found>>24)&0xFF) - GeneralizedImageOps.get(imageB.getBand(0),5,6)) < 1e-8 );
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB.getBand(1),5,6)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB.getBand(2),5,6)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB.getBand(3),5,6)) < 1e-8 );
				} else {
					throw new RuntimeException("Unknown type");
				}
			} else {
				if( buffType == BufferedImage.TYPE_INT_BGR || buffType == BufferedImage.TYPE_3BYTE_BGR  ) {
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB.getBand(2),5,6)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB.getBand(1),5,6)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB.getBand(0),5,6)) < 1e-8 );
				} else {
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB.getBand(0),5,6)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB.getBand(1),5,6)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB.getBand(2),5,6)) < 1e-8 );
				}
			}
		}
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

	@Test
	public void orderBandsIntoRGB() {
		MultiSpectral<ImageUInt8> input = new MultiSpectral<ImageUInt8>(ImageUInt8.class, 10, 10, 3);

		ImageUInt8 band0 = input.getBand(0);
		ImageUInt8 band1 = input.getBand(1);
		ImageUInt8 band2 = input.getBand(2);

		// test no swap first
		BufferedImage orig = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.orderBandsIntoRGB(input, orig);
		assertTrue(band0 == input.getBand(0));
		assertTrue(band1 == input.getBand(1));
		assertTrue(band2 == input.getBand(2));

		// check swaps now
		orig = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
		ConvertBufferedImage.orderBandsIntoRGB(input, orig);
		assertTrue(band2 == input.getBand(0));
		assertTrue(band1 == input.getBand(1));
		assertTrue(band0 == input.getBand(2));

		orig = new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR);
		ConvertBufferedImage.orderBandsIntoRGB(input, orig);
		assertTrue(band0 == input.getBand(0));
		assertTrue(band1 == input.getBand(1));
		assertTrue(band2 == input.getBand(2));

		// 4-band images
		input = new MultiSpectral<ImageUInt8>(ImageUInt8.class, 10, 10, 4);

		band0 = input.getBand(0);
		band1 = input.getBand(1);
		band2 = input.getBand(2);
		ImageUInt8 band3 = input.getBand(3);

		orig = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
		ConvertBufferedImage.orderBandsIntoRGB(input, orig);
		assertTrue(band3 == input.getBand(0));
		assertTrue(band2 == input.getBand(1));
		assertTrue(band1 == input.getBand(2));
		assertTrue(band0 == input.getBand(3));

		band0 = input.getBand(0);
		band1 = input.getBand(1);
		band2 = input.getBand(2);
		band3 = input.getBand(3);
		orig = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		ConvertBufferedImage.orderBandsIntoRGB(input, orig);
		assertTrue(band1 == input.getBand(0));
		assertTrue(band2 == input.getBand(1));
		assertTrue(band3 == input.getBand(2));
		assertTrue(band0 == input.getBand(3));
	}

	@Test
	public void isSubImage() {
		BufferedImage a = new BufferedImage(20,30,BufferedImage.TYPE_BYTE_GRAY);

		assertFalse(ConvertBufferedImage.isSubImage(a));

		BufferedImage b = a.getSubimage(0,0,20,29);
		assertTrue(ConvertBufferedImage.isSubImage(b));

		b = a.getSubimage(0,0,19,30);
		assertTrue(ConvertBufferedImage.isSubImage(b));

		b = a.getSubimage(0,1,20,29);
		assertTrue(ConvertBufferedImage.isSubImage(b));

		b = a.getSubimage(1,0,19,30);
		assertTrue(ConvertBufferedImage.isSubImage(b));
	}
}
