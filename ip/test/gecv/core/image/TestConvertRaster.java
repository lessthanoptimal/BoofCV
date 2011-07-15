/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.core.image;

import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;
import org.junit.Test;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestConvertRaster {

	Random rand = new Random(234);

	int imgWidth = 10;
	int imgHeight = 20;

	int numMethods = 12;

	/**
	 * Use reflections to test all the functions.
	 */
	@Test
	public void performTests() {
		Method methods[] = ConvertRaster.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;

			System.out.println("Examining: "+m.getName());
			if( m.getName().contains("bufferedTo"))
				testBufferedTo(m);
			else if( m.getName().contains("grayTo"))
				testGrayTo(m);
			else
				throw new RuntimeException("Unknown convert type.");

			numFound++;
		}

		// update this as needed when new functions are added
		if(numMethods != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numMethods);
	}

	private boolean isTestMethod( Method m ) {
		Class<?> types[] = m.getParameterTypes();

		if( types.length != 2 )
			return false;

		if( ImageBase.class.isAssignableFrom(types[0]) ||
				ImageBase.class.isAssignableFrom(types[1]) )
		return true;

		return false;
	}

	private void testBufferedTo( Method m ) {
		Class<?> paramTypes[] = m.getParameterTypes();

		BufferedImage input[];

		input = createBufferedTestImages(paramTypes[0]);

		ImageBase output = GeneralizedImageOps.createImage(paramTypes[1],imgWidth,imgHeight);

		for( int i = 0; i < input.length; i++ ) {
			GecvTesting.checkSubImage(this, "performBufferedTo", true, m,input[i],output);
		}

	}

	/**
	 * Creates a set of test BufferedImages with the appropriate rasters and number of bytes/channels.
	 */
	private BufferedImage[] createBufferedTestImages(Class<?> paramType) {
		BufferedImage[] input;
		if( paramType == ByteInterleavedRaster.class ) {
			// the code is handled different when a different number of channels is used
			input = new BufferedImage[]{
					createByteBuff(imgWidth, imgHeight, 3, rand),
				createByteBuff(imgWidth, imgHeight, 1, rand)};
		} else if( paramType == IntegerInterleavedRaster.class ) {
			input = new BufferedImage[]{createIntBuff(imgWidth, imgHeight, rand)};
		} else if( paramType == BufferedImage.class ) {
			// just pick an arbitrary image type here
			input = new BufferedImage[]{createIntBuff(imgWidth, imgHeight, rand)};
		} else {
			throw new RuntimeException("Unknown raster type: "+ paramType.getSimpleName());
		}
		return input;
	}

	public void performBufferedTo( Method m , BufferedImage input , ImageBase output ) {
		try {
			if( Raster.class.isAssignableFrom(m.getParameterTypes()[0]) )
				m.invoke(null,input.getRaster(),output);
			else
			    m.invoke(null,input,output);

			GecvTesting.checkEquals(input, output,1f);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void testGrayTo( Method m ) {

		Class<?> paramTypes[] = m.getParameterTypes();

		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0],imgWidth,imgHeight);
		GeneralizedImageOps.randomize(input, rand, 0,50);

		BufferedImage output[] = createBufferedTestImages(paramTypes[1]);


		for( int i = 0; i < output.length; i++ ) {
			GecvTesting.checkSubImage(this, "performGrayTo", true, m,input,output[i]);
		}
	}

	public void performGrayTo( Method m , ImageBase input , BufferedImage output ) {
		try {
			if( Raster.class.isAssignableFrom(m.getParameterTypes()[1]) )
				m.invoke(null,input,output.getRaster());
			else
			    m.invoke(null,input,output);

			GecvTesting.checkEquals(output,input,1f);
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
		} else {
			ret = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}

		randomize(ret, rand);

		return ret;
	}

	public static BufferedImage createIntBuff(int width, int height, Random rand) {
		BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		randomize(ret, rand);
		return ret;
	}

	public static void randomize(BufferedImage img, Random rand) {
		for (int i = 0; i < img.getWidth(); i++) {
			for (int j = 0; j < img.getHeight(); j++) {
				img.setRGB(i, j, rand.nextInt() & 0xFFFFFF );
			}
		}
	}
}
