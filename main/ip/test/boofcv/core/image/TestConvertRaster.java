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
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.Test;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;
import sun.awt.image.ShortInterleavedRaster;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConvertRaster {

	Random rand = new Random(234);

	int imgWidth = 10;
	int imgHeight = 20;

	int numMethods = 30;

	/**
	 * Use reflections to test all the functions.
	 */
	@Test
	public void performTests() {
		Method methods[] = ConvertRaster.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if (!isTestMethod(m))
				continue;

			System.out.println("Examining: " + m.getName());
			if (m.getName().contains("bufferedTo"))
				testBufferedTo(m);
			else if (m.getName().contains("ToBuffered"))
				testImageTo(m);
			else
				throw new RuntimeException("Unknown convert type.");

			numFound++;
		}

		// update this as needed when new functions are added
		if (numMethods != numFound)
			throw new RuntimeException("Unexpected number of methods: Found " + numFound + "  expected " + numMethods);
	}

	/**
	 * There is a bug where gray scale images are mangled by getRGB().  There is a work around in
	 * the code.
	 * <p/>
	 * Java Bug ID: 5051418
	 */
	@Test
	public void checkGrayBug_To() {
		BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_BYTE_GRAY);

		img.getRaster().getDataBuffer().setElem(0, 101);

		int RGB = img.getRGB(0, 0);
		int r = RGB & 0xFF;

		// this is the bug in Java, if this ever is false then a miracle has happened
		// and this line should be commented out
		assertTrue(r != 101);

		// test several image types
		ImageUInt8 out = new ImageUInt8(5, 5);
		ConvertRaster.bufferedToGray(img, out);
		assertEquals(101, out.get(0, 0));

		ImageFloat32 outF = new ImageFloat32(5, 5);
		ConvertRaster.bufferedToGray(img, outF);
		assertEquals(101, outF.get(0, 0), 1e-4);
	}

	/**
	 * There isn't an RGB bug check here since RGB values make no sense with 16bit bands,  Just
	 * checks to see if the unsigned short value is preserved
	 */
	@Test
	public void checkGray_To_U16() {
		BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_USHORT_GRAY);

		img.getRaster().getDataBuffer().setElem(0, 2005);

		ImageUInt16 out = new ImageUInt16(5, 5);
		ConvertRaster.bufferedToGray(img, out);
		assertEquals(2005, out.get(0, 0));
	}

	private boolean isTestMethod(Method m) {
		Class<?> types[] = m.getParameterTypes();

		if (types.length != 2)
			return false;

		if (ImageBase.class.isAssignableFrom(types[0]) ||
				ImageBase.class.isAssignableFrom(types[1]))
			return true;

		return false;
	}

	private void testBufferedTo(Method m) {
		Class paramTypes[] = m.getParameterTypes();

		BufferedImage input[];

		input = createBufferedTestImages(paramTypes[0]);

		for (int i = 0; i < input.length; i++) {
			// regular image
			ImageBase output = createImage(m, paramTypes[1], input[i]);
			BoofTesting.checkSubImage(this, "performBufferedTo", true, m, input[i], output);

			// subimage input
			BufferedImage subimage = input[i].getSubimage(1,2,imgWidth-1,imgHeight-2);
			output = createImage(m, paramTypes[1], subimage);
			BoofTesting.checkSubImage(this, "performBufferedTo", true, m, subimage, output);
		}
	}

	private ImageBase createImage(Method m, Class imageType, BufferedImage inputBuff) {

		int numBands = inputBuff.getRaster().getNumBands();

		ImageBase output;
		if (ImageSingleBand.class.isAssignableFrom(imageType)) {
			output = GeneralizedImageOps.createSingleBand(imageType, inputBuff.getWidth(), inputBuff.getHeight());
		} else {
			Class type;
			if (m.getName().contains("U8")) {
				type = ImageUInt8.class;
			} else if (m.getName().contains("F32")) {
				type = ImageFloat32.class;
			} else {
				throw new IllegalArgumentException("Unexpected: " + m.getName());
			}

			output = new MultiSpectral(type, inputBuff.getWidth(), inputBuff.getHeight(), numBands);
		}
		return output;
	}

	/**
	 * Creates a set of test BufferedImages with the appropriate rasters and number of bytes/channels.
	 */
	private BufferedImage[] createBufferedTestImages(Class<?> paramType) {
		BufferedImage[] input;
		if (paramType == ByteInterleavedRaster.class) {
			// the code is handled different when a different number of channels is used
			input = new BufferedImage[]{
					createByteBuffByType(imgWidth, imgHeight, BufferedImage.TYPE_3BYTE_BGR, rand),
					createByteBuffByType(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR, rand),
					createByteBuffByType(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY, rand)};
		} else if (paramType == IntegerInterleavedRaster.class) {
			input = new BufferedImage[]{
					createByteBuffByType(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB, rand),
					createByteBuffByType(imgWidth, imgHeight,BufferedImage.TYPE_INT_BGR, rand),
					createByteBuffByType(imgWidth, imgHeight,BufferedImage.TYPE_INT_RGB, rand)};
		} else if( paramType == ShortInterleavedRaster.class ) {
			input = new BufferedImage[]{createShortBuff(imgWidth, imgHeight, rand)};
		} else if (paramType == BufferedImage.class) {
			// just pick an arbitrary image type here
			input = new BufferedImage[]{createIntBuff(imgWidth, imgHeight, rand)};
		} else {
			throw new RuntimeException("Unknown raster type: " + paramType.getSimpleName());
		}
		return input;
	}

	public void performBufferedTo(Method m, BufferedImage input, ImageBase output) {
		try {
			if (Raster.class.isAssignableFrom(m.getParameterTypes()[0])) {
				m.invoke(null, input.getRaster(), output);

				// read directly from raster if the raster is an input
				if( MultiSpectral.class.isAssignableFrom(output.getClass()) )
					BoofTesting.checkEquals(input.getRaster(),(MultiSpectral)output,1);
				else
					BoofTesting.checkEquals(input, output, false, 1f);
			} else {
				m.invoke(null, input, output);
				BoofTesting.checkEquals(input, output, false, 1f);
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void testImageTo(Method m) {

		Class paramTypes[] = m.getParameterTypes();

		BufferedImage output[] = createBufferedTestImages(paramTypes[1]);

		for (int i = 0; i < output.length; i++) {
			ImageBase input = createImage(m, paramTypes[0], output[i]);
			GImageMiscOps.fillUniform(input, rand, 0, 50);

			BoofTesting.checkSubImage(this, "performImageTo", true, m, input, output[i]);
		}
	}

	public void performImageTo(Method m, ImageBase input, BufferedImage output) {
		try {
			if (Raster.class.isAssignableFrom(m.getParameterTypes()[1])) {
				m.invoke(null, input, output.getRaster());

				// read directly from raster if the raster is an input
				if( MultiSpectral.class.isAssignableFrom(input.getClass()) )
					BoofTesting.checkEquals(output.getRaster(),(MultiSpectral)input,1);
				else
					BoofTesting.checkEquals(output, input,false,  1f);
			} else {
				m.invoke(null, input, output);
				BoofTesting.checkEquals(output, input, false, 1f);
			}

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}


	public static BufferedImage createByteBuff(int width, int height, int numBands, Random rand) {
		BufferedImage ret;

		if (numBands == 1) {
			ret = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		} else if (numBands == 3) {
			ret = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else if (numBands == 4) {
			ret = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		} else {
			throw new RuntimeException("Unexpected number of bands");
		}

		randomize(ret, rand);

		return ret;
	}

	public static BufferedImage createByteBuffByType(int width, int height, int type, Random rand) {
		BufferedImage ret = new BufferedImage(width, height, type);

		randomize(ret, rand);

		return ret;
	}

	public static BufferedImage createByteIndexed(int width, int height, Random rand) {
		BufferedImage ret = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_INDEXED);

		randomize(ret, rand);

		return ret;
	}

	public static BufferedImage createIntBuff(int width, int height, Random rand) {
		BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		randomize(ret, rand);
		return ret;
	}

	public static BufferedImage createShortBuff(int width, int height, Random rand) {
		BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
		randomize(ret, rand);
		return ret;
	}

	public static void randomize(BufferedImage img, Random rand) {
		for (int i = 0; i < img.getWidth(); i++) {
			for (int j = 0; j < img.getHeight(); j++) {
				img.setRGB(i, j, rand.nextInt() & 0xFFFFFF);
			}
		}
	}
}
