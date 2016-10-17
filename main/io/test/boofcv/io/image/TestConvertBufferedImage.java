/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.io.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageInterleavedTestingOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
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
	public void checkInputs() {
		BufferedImage found;

		found = ConvertBufferedImage.checkInputs(new GrayU8(10,10),null);
		assertTrue(found.getType() == BufferedImage.TYPE_BYTE_GRAY);
		found = ConvertBufferedImage.checkInputs(new GrayS8(10,10),null);
		assertTrue(found.getType() == BufferedImage.TYPE_BYTE_GRAY);
		found = ConvertBufferedImage.checkInputs(new GrayU16(10,10),null);
		assertTrue(found.getType() == BufferedImage.TYPE_USHORT_GRAY);
		found = ConvertBufferedImage.checkInputs(new GrayS16(10,10),null);
		assertTrue(found.getType() == BufferedImage.TYPE_USHORT_GRAY);

		// not really what to do about floating point images.  No equivalent BufferedImage.  Just assume its a regular
		// gray input image with pixel values from 0 to 255
		found = ConvertBufferedImage.checkInputs(new GrayF32(10, 10), null);
		assertTrue(found.getType() == BufferedImage.TYPE_BYTE_GRAY);
		found = ConvertBufferedImage.checkInputs(new GrayF64(10,10),null);
		assertTrue(found.getType() == BufferedImage.TYPE_BYTE_GRAY);

		found = ConvertBufferedImage.checkInputs(new Planar(GrayU8.class,3,10,10),null);
		assertTrue(found.getType() == BufferedImage.TYPE_INT_RGB);
	}

	@Test
	public void extractInterleavedU8() {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);

		InterleavedU8 found = ConvertBufferedImage.extractInterleavedU8(origImg);

		assertEquals(0, found.startIndex);
		assertEquals(imgWidth*3, found.stride);
		assertEquals(imgWidth, found.width);
		assertEquals(imgHeight, found.height);
		assertEquals(3, found.numBands);
		assertEquals(3, found.getImageType().getNumBands());

		assertTrue(found.data != null);
		assertEquals(imgWidth * imgHeight * 3, found.data.length);

		// test a sub-image input
		origImg = origImg.getSubimage(1,2,5,6);

		found = ConvertBufferedImage.extractInterleavedU8(origImg);

		assertEquals(2 * 10 * 3 + 3, found.startIndex);
		assertEquals(imgWidth*3, found.stride);
		assertEquals(5, found.width);
		assertEquals(6, found.height);
		assertEquals(3, found.numBands);
	}

	@Test(expected=IllegalArgumentException.class)
	public void extractInterleavedInt8_indexed() {
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
	public void extractGrayU8() {
		BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);

		GrayU8 found = ConvertBufferedImage.extractGrayU8(origImg);

		assertEquals(imgWidth, found.width);
		assertEquals(imgHeight, found.height);
		assertTrue(found.data != null);
		assertEquals(imgWidth * imgHeight, found.data.length);

		// test a sub-image input
		origImg = origImg.getSubimage(1,2,5,6);

		found = ConvertBufferedImage.extractGrayU8(origImg);

		assertEquals(2*10+1, found.startIndex);
		assertEquals(imgWidth, found.stride);
		assertEquals(5, found.width);
		assertEquals(6, found.height);
	}

	@Test(expected=IllegalArgumentException.class)
	public void extractImageInt8_indexed() {
		BufferedImage origImg = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_BYTE_INDEXED);

		ConvertBufferedImage.extractGrayU8(origImg);
	}

	@Test
	public void extractImageInt8_fail() {
		try {
			BufferedImage origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);
			ConvertBufferedImage.extractGrayU8(origImg);
			fail("Should have had an unexpected number of bands");
		} catch (IllegalArgumentException e) {
		}

		try {
			BufferedImage origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
			ConvertBufferedImage.extractGrayU8(origImg);
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

		BoofTesting.checkEquals(img, srcImg,false,1e-4f);

		// now test it with a single band
		srcImg = new InterleavedU8(imgWidth, imgHeight, 1);
		ImageInterleavedTestingOps.randomize(srcImg, rand);
		img = ConvertBufferedImage.extractBuffered(srcImg);
		BoofTesting.checkEquals(img, srcImg,false,1e-4f);

	}

	@Test
	public void extractBuffered_Int8() {
		// use a signed image because it is checked against a byte array
		GrayU8 srcImg = new GrayU8(imgWidth, imgHeight);
		ImageMiscOps.fillUniform(srcImg, rand, 0, 100);

		BufferedImage img = ConvertBufferedImage.extractBuffered(srcImg);

		BoofTesting.checkEquals(img, srcImg);
	}

	/**
	 * Ensures that the orderRgb flag is correctly handled
	 */
	@Test
	public void convertFrom_PL_orderRgb() {
		Class []bandTypes = new Class[]{GrayU8.class,GrayF32.class};
		for( Class b : bandTypes ) {
			convertFrom_PL_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, true);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, true);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_INT_ARGB, b, true);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_INT_RGB, b, true);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_INT_BGR, b, true);

			convertFrom_PL_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, false);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, false);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_INT_ARGB, b, false);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_INT_RGB, b, false);
			convertFrom_PL_orderRgb(BufferedImage.TYPE_INT_BGR, b, false);
		}
	}

	public void convertFrom_PL_orderRgb(int buffType, Class bandType, boolean reorder) {
		BufferedImage input = TestConvertRaster.createBufferedByType(imgWidth, imgHeight, buffType, rand);

		int numBands = input.getRaster().getNumBands();

		Planar output = new Planar(bandType,imgWidth,imgHeight,numBands);

		ConvertBufferedImage.convertFrom(input, output, reorder);

		// this always returns it in ARGB order
		checkBandOrder(buffType, reorder, input, numBands, output);
	}

	@Test
	public void convertFrom_interlaced_orderRgb() {
		Class []bandTypes = new Class[]{InterleavedU8.class,InterleavedF32.class};
		for( Class b : bandTypes ) {
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, true);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, true);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_INT_ARGB, b, true);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_INT_RGB, b, true);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_INT_BGR, b, true);

			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, false);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, false);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_INT_ARGB, b, false);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_INT_RGB, b, false);
			convertFrom_interlaced_orderRgb(BufferedImage.TYPE_INT_BGR, b, false);
		}
	}

	public void convertFrom_interlaced_orderRgb(int buffType, Class type, boolean reorder) {
		BufferedImage input = TestConvertRaster.createBufferedByType(imgWidth, imgHeight, buffType, rand);

		int numBands = input.getRaster().getNumBands();

		ImageInterleaved output = GeneralizedImageOps.createInterleaved(type,imgWidth, imgHeight,numBands);

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

				GrayU8 imgInt8 = ConvertBufferedImage.convertFromSingle(origImg, null, GrayU8.class);
				assertEquals(origImg.getWidth(), imgInt8.width);
				assertEquals(origImg.getHeight(), imgInt8.height);
				BoofTesting.checkEquals(origImg, imgInt8, false, 1);

				GrayF32 imgF32 = ConvertBufferedImage.convertFromSingle(origImg, null, GrayF32.class);
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

			GrayU16 imgU16 = ConvertBufferedImage.convertFromSingle(origImg, null, GrayU16.class);
			assertEquals(origImg.getWidth(), imgU16.width);
			assertEquals(origImg.getHeight(), imgU16.height);
			BoofTesting.checkEquals(origImg, imgU16, false, 1);

			GrayS16 imgS16 = ConvertBufferedImage.convertFromSingle(origImg, null, GrayS16.class);
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
				Planar<GrayU8> imgInt8 = ConvertBufferedImage.convertFromMulti(origImg, null, false, GrayU8.class);
				assertEquals(origImg.getWidth(), imgInt8.width);
				assertEquals(origImg.getHeight(), imgInt8.height);
				BoofTesting.checkEquals(origImg, imgInt8, false, 1);

				Planar<GrayF32> imgF32 = ConvertBufferedImage.convertFromMulti(origImg, null, false,GrayF32.class);
				assertEquals(origImg.getWidth(), imgF32.width);
				assertEquals(origImg.getHeight(), imgF32.height);
				BoofTesting.checkEquals(origImg, imgF32,false,  1);
			}
		}
	}

	@Test
	public void convertFromInterleaved() {
		BufferedImage origImg;

		for( int i = 0; i < 4; i++ ) {

			int numBands;
			if (i == 0) {
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 1, rand);
				numBands = 1;
			} else if (i == 1) {
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 3, rand);
				numBands = 3;
			} else if (i == 2) {
				origImg = TestConvertRaster.createByteBuff(imgWidth, imgHeight, 4, rand);
				numBands = 4;
			} else if( i == 3 ) {
				origImg = TestConvertRaster.createByteIndexed(imgWidth, imgHeight, rand);
				numBands = 3;
			} else {
				origImg = TestConvertRaster.createIntBuff(imgWidth, imgHeight, rand);
				numBands = 3;
			}

			InterleavedU8 imgInt8 = new InterleavedU8(imgWidth,imgHeight,numBands);
			InterleavedF32 imgF32 = new InterleavedF32(imgWidth,imgHeight,numBands);

			for( int j = 0; j < 2; j++ ) {
				if( j == 1 ) {
					origImg = origImg.getSubimage(1,2,imgWidth-1,imgHeight-2);
					imgInt8 = imgInt8.subimage(1,2,imgWidth,imgHeight);
					imgF32 = imgF32.subimage(1, 2, imgWidth, imgHeight);
				}
//				System.out.println(i+" "+j);
				ConvertBufferedImage.convertFromInterleaved(origImg, imgInt8, false);
				BoofTesting.checkEquals(origImg, imgInt8, false, 1);

				ConvertBufferedImage.convertFromInterleaved(origImg, imgF32, false);
				BoofTesting.checkEquals(origImg, imgF32, false, 1);
			}
		}
	}

	/**
	 * Create an image and convert it into a buffered image
	 */
	@Test
	public void convertTo_SB() {
		Class[] types = new Class[]{GrayU8.class, GrayU16.class, GrayF32.class};

		for (Class t : types) {
			ImageBase image = GeneralizedImageOps.createSingleBand(t, imgWidth, imgHeight);

			GImageMiscOps.fillUniform(image, rand, 0, 100);
			BoofTesting.checkSubImage(this, "convertTo", false, image);
		}
	}

	@Test
	public void convertTo_PL() {
		Class[] types = new Class[]{GrayU8.class, GrayF32.class};

		for (Class t : types) {
			ImageBase image = new Planar(t, imgWidth, imgHeight, 3);
			GImageMiscOps.fillUniform(image, rand, 0, 100);

			BoofTesting.checkSubImage(this, "convertTo", false, image);
		}
	}

	@Test
	public void convertTo_IL() {
		Class[] types = new Class[]{InterleavedU8.class, InterleavedF32.class};

		for (Class t : types) {
			ImageBase image = GeneralizedImageOps.createInterleaved(t, imgWidth, imgHeight, 3);
			GImageMiscOps.fillUniform(image, rand, 0, 100);

			BoofTesting.checkSubImage(this, "convertTo", false, image);
		}
	}

	public void convertTo(ImageBase input ) {
		convertTo(input, BufferedImage.TYPE_3BYTE_BGR);
//		convertTo(input, BufferedImage.TYPE_4BYTE_ABGR); // commented out 4 band images just to make things easier
//		convertTo(input, BufferedImage.TYPE_INT_ARGB);
		convertTo(input, BufferedImage.TYPE_INT_RGB);
		convertTo(input, BufferedImage.TYPE_INT_BGR);
	}

	public void convertTo( ImageBase input , int buffType  ) {
		BufferedImage output = TestConvertRaster.createBufferedByType(imgWidth, imgHeight, buffType, rand);

		ConvertBufferedImage.convertTo(input, output, false);

		BoofTesting.checkEquals(output, input, false, 1);
	}

	/**
	 * Ensures that the orderRgb flag is correctly handled
	 */
	@Test
	public void convertTo_PL_orderRgb() {
		Class []bandTypes = new Class[]{GrayU8.class,GrayF32.class};
		for( Class b : bandTypes ) {
			convertTo_PL_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, true);
			convertTo_PL_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, true);
			convertTo_PL_orderRgb(BufferedImage.TYPE_INT_ARGB, b, true);
			convertTo_PL_orderRgb(BufferedImage.TYPE_INT_RGB, b, true);
			convertTo_PL_orderRgb(BufferedImage.TYPE_INT_BGR, b, true);

			convertTo_PL_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, false);
			convertTo_PL_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, false);
			convertTo_PL_orderRgb(BufferedImage.TYPE_INT_ARGB, b, false);
			convertTo_PL_orderRgb(BufferedImage.TYPE_INT_RGB, b, false);
			convertTo_PL_orderRgb(BufferedImage.TYPE_INT_BGR, b, false);
		}
	}

	public void convertTo_PL_orderRgb(int buffType, Class bandType, boolean reorder) {
		BufferedImage output = TestConvertRaster.createBufferedByType(imgWidth, imgHeight, buffType, rand);

		int numBands = output.getRaster().getNumBands();

		Planar input = new Planar(bandType,imgWidth,imgHeight,numBands);

		double pixel[] = new double[numBands];
		for (int i = 0; i < numBands; i++) {
			pixel[i] = (i+1)*10;
		}
		GeneralizedImageOps.setM(input, 5, 6, pixel);

		ConvertBufferedImage.convertTo(input, output, reorder);

		checkBandOrder(buffType, reorder, output, numBands, input);
	}

	@Test
	public void convertTo_interleaved_orderRgb() {
		Class []bandTypes = new Class[]{InterleavedI8.class,InterleavedF32.class};
		for( Class b : bandTypes ) {
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, true);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, true);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_INT_ARGB, b, true);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_INT_RGB, b, true);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_INT_BGR, b, true);

			convertTo_interleaved_orderRgb(BufferedImage.TYPE_3BYTE_BGR, b, false);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_4BYTE_ABGR, b, false);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_INT_ARGB, b, false);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_INT_RGB, b, false);
			convertTo_interleaved_orderRgb(BufferedImage.TYPE_INT_BGR, b, false);
		}
	}

	public void convertTo_interleaved_orderRgb( int buffType , Class bandType , boolean reorder ) {
		BufferedImage output = TestConvertRaster.createBufferedByType(imgWidth, imgHeight, buffType, rand);

		int numBands = output.getRaster().getNumBands();

		ImageInterleaved input = GeneralizedImageOps.createInterleaved(bandType, imgWidth, imgHeight, numBands);

		double pixel[] = new double[numBands];
		for (int i = 0; i < numBands; i++) {
			pixel[i] = (i+1)*10;
		}
		GeneralizedImageOps.setM(input, 5, 6, pixel);

		ConvertBufferedImage.convertTo(input, output, reorder);

		checkBandOrder(buffType, reorder, output, numBands, input);
	}

	private void checkBandOrder(int buffType, boolean reorder, BufferedImage imageA, int numBands,
								ImageMultiBand imageB) {
		// this always returns it in ARGB order
		int found = imageA.getRGB(5,6);

		assertTrue(found != 0);

		if( reorder ) {
			if( numBands == 4 ) {
				assertTrue(Math.abs(((found>>24)&0xFF) - GeneralizedImageOps.get(imageB,5,6,3)) < 1e-8 );
				assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB,5,6,0)) < 1e-8 );
				assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB,5,6,1)) < 1e-8 );
				assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB,5,6,2)) < 1e-8 );
			} else {
				assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB,5,6,0)) < 1e-8 );
				assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB,5,6,1)) < 1e-8 );
				assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB,5,6,2)) < 1e-8 );
			}
		} else {
			if( numBands == 4 ){
				if( buffType == BufferedImage.TYPE_4BYTE_ABGR ) {
					assertTrue(Math.abs(((found>>24)&0xFF) - GeneralizedImageOps.get(imageB,5,6,0)) < 1e-8 );
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB,5,6,3)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB,5,6,2)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB,5,6,1)) < 1e-8 );
				} else if( buffType == BufferedImage.TYPE_INT_ARGB ) {
					assertTrue(Math.abs(((found>>24)&0xFF) - GeneralizedImageOps.get(imageB,5,6,0)) < 1e-8 );
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB,5,6,1)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB,5,6,2)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB,5,6,3)) < 1e-8 );
				} else {
					throw new RuntimeException("Unknown type");
				}
			} else {
				if( buffType == BufferedImage.TYPE_INT_BGR || buffType == BufferedImage.TYPE_3BYTE_BGR  ) {
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB,5,6,2)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB,5,6,1)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB,5,6,0)) < 1e-8 );
				} else {
					assertTrue(Math.abs(((found>>16)&0xFF) - GeneralizedImageOps.get(imageB,5,6,0)) < 1e-8 );
					assertTrue(Math.abs(((found>>8)&0xFF) -  GeneralizedImageOps.get(imageB,5,6,1)) < 1e-8 );
					assertTrue(Math.abs((found&0xFF) -       GeneralizedImageOps.get(imageB,5,6,2)) < 1e-8 );
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
		Planar<GrayU8> input = new Planar<>(GrayU8.class, 10, 10, 3);

		GrayU8 band0 = input.getBand(0);
		GrayU8 band1 = input.getBand(1);
		GrayU8 band2 = input.getBand(2);

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
		input = new Planar<>(GrayU8.class, 10, 10, 4);

		band0 = input.getBand(0);
		band1 = input.getBand(1);
		band2 = input.getBand(2);
		GrayU8 band3 = input.getBand(3);

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

	@Test
	public void stripAlphaChannel() {
		BufferedImage a = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_4BYTE_ABGR);
		BufferedImage b = ConvertBufferedImage.stripAlphaChannel(a);

		assertTrue(a != b);
		assertEquals(3,b.getRaster().getNumBands());

		BufferedImage c = ConvertBufferedImage.stripAlphaChannel(b);
		assertTrue(b == c);

		a = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_BYTE_GRAY);
		c = ConvertBufferedImage.stripAlphaChannel(a);
		assertTrue(a == c);

		a = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_3BYTE_BGR);
		c = ConvertBufferedImage.stripAlphaChannel(a);
		assertTrue(a == c);
	}
}
