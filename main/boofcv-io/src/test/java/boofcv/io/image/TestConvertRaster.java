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

package boofcv.io.image;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.impl.ImplConvertRaster;
import boofcv.struct.image.*;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.awt.image.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests select functions in ConvertRaster.
 *
 * @author Peter Abeles
 */
public class TestConvertRaster extends CompareIdenticalFunctions {

	Random rand = new Random(234);

	int imgWidth = 10;
	int imgHeight = 20;

	int numMethods = 44;

	TestConvertRaster() {
		super(ConvertRaster.class, ImplConvertRaster.class);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();
		Object[] params = new Object[types.length];

		for (int i = 0; i < types.length; i++) {
			Class c = types[i];

			if( c.isAssignableFrom(ImageBase.class)) {
				params[i] = GeneralizedImageOps.createImage(c,imgWidth,imgHeight,3);
				GImageMiscOps.fillUniform((ImageBase)params[i],rand,0,200);
			}
		}

		return new Object[][]{params};
	}

	/**
	 * There is a bug where gray scale images are mangled by getRGB(). There is a work around in
	 * the code.
	 * <p/>
	 * Java Bug ID: 5051418
	 */
	@Test
	void checkGrayBug_To() {
		BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_BYTE_GRAY);

		img.getRaster().getDataBuffer().setElem(0, 101);

		int RGB = img.getRGB(0, 0);
		int r = RGB & 0xFF;

		// this is the bug in Java, if this ever is false then a miracle has happened
		// and this line should be commented out
		assertTrue(r != 101);

		// test several image types
		GrayU8 out = new GrayU8(5, 5);
		ConvertRaster.bufferedToGray((DataBufferByte)img.getRaster().getDataBuffer(),img.getRaster(), out);
		assertEquals(101, out.get(0, 0));

		GrayF32 outF = new GrayF32(5, 5);
		ConvertRaster.bufferedToGray((DataBufferByte)img.getRaster().getDataBuffer(),img.getRaster(), outF);
		assertEquals(101, outF.get(0, 0), 1e-4);
	}

	private void testBufferedTo(Method m) {
		Class paramTypes[] = m.getParameterTypes();

		BufferedImage input[];

		input = createBufferedTestImages(paramTypes[0]);

		boolean canSubImage = !System.getProperty("java.version").startsWith("1.9");

		for (int i = 0; i < input.length; i++) {
			// regular image
			Class imageType = paramTypes.length == 2 ? paramTypes[1] : paramTypes[2];
			ImageBase output = createImage(m, imageType, input[i]);
			BoofTesting.checkSubImage(this, "performBufferedTo", true, m, input[i], output);

			if( canSubImage ) {
				// subimage input
				BufferedImage subimage = input[i].getSubimage(1, 2, imgWidth - 1, imgHeight - 2);
				output = createImage(m, imageType, subimage);
				BoofTesting.checkSubImage(this, "performBufferedTo", true, m, subimage, output);
			}
		}
	}

	private ImageBase createImage(Method m, Class imageType, BufferedImage inputBuff) {

		int numBands = inputBuff.getRaster().getNumBands();

		ImageBase output;
		if (ImageGray.class.isAssignableFrom(imageType)) {
			output = GeneralizedImageOps.createSingleBand(imageType, inputBuff.getWidth(), inputBuff.getHeight());
		} else if (ImageInterleaved.class.isAssignableFrom(imageType)) {
			if( m.getName().contains("Gray")) {
				output = GeneralizedImageOps.createInterleaved(imageType, inputBuff.getWidth(), inputBuff.getHeight(), 1);
			} else {
				output = GeneralizedImageOps.createInterleaved(imageType, inputBuff.getWidth(), inputBuff.getHeight(), numBands);
			}
		} else {
			Class type;
			if (m.getName().contains("U8")) {
				type = GrayU8.class;
			} else if (m.getName().contains("F32")) {
				type = GrayF32.class;
			} else {
				throw new IllegalArgumentException("Unexpected: " + m.getName());
			}

			output = new Planar(type, inputBuff.getWidth(), inputBuff.getHeight(), numBands);
		}
		return output;
	}

	/**
	 * Creates a set of test BufferedImages with the appropriate rasters and number of bytes/channels.
	 */
	private BufferedImage[] createBufferedTestImages(Class<?> paramType) {
		BufferedImage[] input;
		if (paramType == DataBufferByte.class) {
			// the code is handled different when a different number of channels is used
			input = new BufferedImage[]{
					createBufferedByType(imgWidth, imgHeight, BufferedImage.TYPE_3BYTE_BGR, rand),
					createBufferedByType(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR, rand),
					createBufferedByType(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_GRAY, rand)};
		} else if (paramType == DataBufferInt.class) {
			input = new BufferedImage[]{
					createBufferedByType(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB, rand),
					createBufferedByType(imgWidth, imgHeight, BufferedImage.TYPE_INT_BGR, rand),
					createBufferedByType(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB, rand)};
		} else if( paramType == DataBufferUShort.class ) {
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
			if (DataBuffer.class.isAssignableFrom(m.getParameterTypes()[0])) {
				m.invoke(null, input.getRaster().getDataBuffer(),input.getRaster(), output);

				// read directly from raster if the raster is an input
				if( ImageMultiBand.class.isAssignableFrom(output.getClass()) )
					BufferedImageChecks.checkEquals(input.getRaster(),(ImageMultiBand)output,1);
				else
					BufferedImageChecks.checkEquals(input, output, false, 1f);
			} else {
				m.invoke(null, input, output);
				BufferedImageChecks.checkEquals(input, output, false, 1f);
			}
		} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
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
			if (DataBuffer.class.isAssignableFrom(m.getParameterTypes()[1])) {
				m.invoke(null, input, output.getRaster().getDataBuffer(), output.getRaster());

				// read directly from raster if the raster is an input
				if (Planar.class.isAssignableFrom(input.getClass()))
					BufferedImageChecks.checkEquals(output.getRaster(), (Planar) input, 1);
				else
					BufferedImageChecks.checkEquals(output, input, false, 1f);
			} else if (Raster.class.isAssignableFrom(m.getParameterTypes()[1])) {
				m.invoke(null, input, output.getRaster());

				// read directly from raster if the raster is an input
				if( Planar.class.isAssignableFrom(input.getClass()) )
					BufferedImageChecks.checkEquals(output.getRaster(),(Planar)input,1);
				else
					BufferedImageChecks.checkEquals(output, input,false,  1f);
			} else {
				m.invoke(null, input, output);
				BufferedImageChecks.checkEquals(output, input, false, 1f);
			}

		} catch (IllegalAccessException | InvocationTargetException e) {
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

	public static BufferedImage createBufferedByType(int width, int height, int type, Random rand) {
		BufferedImage ret = new BufferedImage(width, height, type);

		randomize(ret, rand);

		return ret;
	}

	public static BufferedImage createByteIndexed(int width, int height, Random rand) {
		BufferedImage ret = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_INDEXED);

		randomize(ret, rand);

		return ret;
	}

	public static BufferedImage createByteBinary(int width, int height, Random rand) {
		BufferedImage ret = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_BINARY);

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
		WritableRaster raster = img.getRaster();
		DataBuffer buffer = raster.getDataBuffer();
		if( buffer.getDataType() == DataBuffer.TYPE_BYTE ) {
			byte[] data = ((DataBufferByte)buffer).getData();
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte)rand.nextInt();
			}
			img.setRGB(0,0,img.getRGB(0,0));
		} else {
			for (int i = 0; i < img.getWidth(); i++) {
				for (int j = 0; j < img.getHeight(); j++) {
					img.setRGB(i, j, rand.nextInt() & 0xFFFFFF);
				}
			}
		}
	}


	@Test void orderBandsIntoRGB() {
		Planar<GrayU8> input = new Planar<>(GrayU8.class, 10, 10, 3);

		GrayU8 band0 = input.getBand(0);
		GrayU8 band1 = input.getBand(1);
		GrayU8 band2 = input.getBand(2);

		// test no swap first
		BufferedImage orig = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
		ConvertRaster.orderBandsIntoRGB(input, orig);
		assertTrue(band0 == input.getBand(0));
		assertTrue(band1 == input.getBand(1));
		assertTrue(band2 == input.getBand(2));

		// check swaps now
		orig = new BufferedImage(10, 10, BufferedImage.TYPE_3BYTE_BGR);
		ConvertRaster.orderBandsIntoRGB(input, orig);
		assertTrue(band2 == input.getBand(0));
		assertTrue(band1 == input.getBand(1));
		assertTrue(band0 == input.getBand(2));

		orig = new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR);
		ConvertRaster.orderBandsIntoRGB(input, orig);
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
		ConvertRaster.orderBandsIntoRGB(input, orig);
		assertTrue(band3 == input.getBand(0));
		assertTrue(band2 == input.getBand(1));
		assertTrue(band1 == input.getBand(2));
		assertTrue(band0 == input.getBand(3));

		band0 = input.getBand(0);
		band1 = input.getBand(1);
		band2 = input.getBand(2);
		band3 = input.getBand(3);
		orig = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		ConvertRaster.orderBandsIntoRGB(input, orig);
		assertTrue(band1 == input.getBand(0));
		assertTrue(band2 == input.getBand(1));
		assertTrue(band3 == input.getBand(2));
		assertTrue(band0 == input.getBand(3));
	}
}
