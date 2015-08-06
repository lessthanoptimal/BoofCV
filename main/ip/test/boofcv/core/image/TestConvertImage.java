/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestConvertImage {

	Random rand = new Random(34);
	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void checkAllConvert() {
		int count = 0;
		Method methods[] = ConvertImage.class.getMethods();

		for (Method m : methods) {

			Class[] inputTypes = m.getParameterTypes();
			if( inputTypes.length != 2 || !ImageBase.class.isAssignableFrom(inputTypes[1]))
				continue;

			Class<?> inputType = inputTypes[0];
			Class<?> outputType = inputTypes[1];

//			System.out.println(m.getName()+" "+inputType.getSimpleName()+" "+outputType.getSimpleName()+" "+m.getReturnType());
			
			// make sure the return type equals the output type
			assertTrue(outputType == m.getReturnType());

			if( m.getName().contains("convert") ) {
				checkConvert(m,inputType,outputType);
			} else {
				if( inputType == MultiSpectral.class )
					checkMultiAverage(m, inputType, outputType);
				else
					checkInterleavedAverage(m, inputType, outputType);
			}
			count++;
		}

		assertEquals(8*7 + 8*7+8+8,count);
	}

	private void checkConvert( Method m , Class inputType , Class outputType ) {
		if( ImageSingleBand.class.isAssignableFrom(inputType) ) {
			checkConvertSingle(m,inputType,outputType);
		} else {
			checkConvertInterleaved(m, inputType, outputType);
		}

	}
	private void checkConvertSingle( Method m , Class inputType , Class outputType ) {
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(inputType, imgWidth, imgHeight);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(outputType, imgWidth, imgHeight);

		boolean inputSigned = true;
		boolean outputSigned = true;

		if( ImageInteger.class.isAssignableFrom(inputType) )
			inputSigned = input.getDataType().isSigned();
		if( ImageInteger.class.isAssignableFrom(outputType) )
			outputSigned = output.getDataType().isSigned();

	   // only provide signed numbers of both data types can handle them
		if( inputSigned && outputSigned ) {
			GImageMiscOps.fillUniform(input, rand, -10, 10);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		BoofTesting.checkSubImage(this,"checkConvertSingle",true,m,input,output);
	}

	private void checkConvertInterleaved( Method m , Class inputType , Class outputType ) {
		ImageInterleaved input = GeneralizedImageOps.createInterleaved(inputType, imgWidth, imgHeight,2);
		ImageInterleaved output = GeneralizedImageOps.createInterleaved(outputType, imgWidth, imgHeight, 2);

		boolean inputSigned = true;
		boolean outputSigned = true;

		if( input.getImageType().getDataType().isInteger() )
			inputSigned = input.getDataType().isSigned();
		if( output.getImageType().getDataType().isInteger() )
			outputSigned = output.getDataType().isSigned();

		// only provide signed numbers of both data types can handle them
		if( inputSigned && outputSigned ) {
			GImageMiscOps.fillUniform(input, rand, -10, 10);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		BoofTesting.checkSubImage(this, "checkConvertInterleaved", true, m, input, output);
	}

	public void checkConvertSingle( Method m , ImageSingleBand<?> input , ImageSingleBand<?> output ) {
		try {
			double tol = selectTolerance(input,output);

			// check it with a non-null output
			ImageSingleBand<?> ret = (ImageSingleBand<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			BoofTesting.assertEquals(input, ret, tol);

			// check it with a null output
			ret = (ImageSingleBand<?>)m.invoke(null,input,null);
			BoofTesting.assertEquals(input, ret, tol);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public void checkConvertInterleaved( Method m , ImageInterleaved<?> input , ImageInterleaved<?> output ) {
		try {
			double tol = selectTolerance(input,output);

			// check it with a non-null output
			ImageInterleaved<?> ret = (ImageInterleaved<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			BoofTesting.assertEquals(input, ret, tol);

			// check it with a null output
			ret = (ImageInterleaved<?>)m.invoke(null,input,null);
			BoofTesting.assertEquals(input, ret, tol);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkMultiAverage(Method m, Class inputType, Class outputType) {
		if( inputType != MultiSpectral.class )
			fail("Expected MultiSpectral image");

		ImageSingleBand output = GeneralizedImageOps.createSingleBand(outputType, imgWidth, imgHeight);

		boolean signed = true;

		if( ImageInteger.class.isAssignableFrom(outputType) )
			signed = output.getDataType().isSigned();

		for( int numBands = 1; numBands <= 3; numBands++ ) {
			MultiSpectral input = new MultiSpectral(outputType,imgWidth,imgHeight,numBands);

			// only provide signed numbers of both data types can handle them
			if( signed ) {
				GImageMiscOps.fillUniform(input, rand, -10, 10);
			} else {
				GImageMiscOps.fillUniform(input, rand, 0, 20);
			}

			BoofTesting.checkSubImage(this,"checkMultiAverage",true,m,input,output);
		}
	}

	public void checkMultiAverage(Method m, MultiSpectral<?> input, ImageSingleBand<?> output) {
		try {
			// check it with a non-null output
			ImageSingleBand<?> ret = (ImageSingleBand<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			checkMultiAverage(input, ret);

			// check it with a null output
			ret = (ImageSingleBand<?>)m.invoke(null,input,null);
			checkMultiAverage(input, ret);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkMultiAverage(MultiSpectral<?> input, ImageSingleBand<?> found) {
		int numBands = input.getNumBands();
		for( int y = 0; y < imgHeight; y++ ){
			for( int x = 0; x < imgWidth; x++ ) {
				double sum = 0;
				for( int b = 0; b < numBands; b++ ) {
					sum += GeneralizedImageOps.get(input.getBand(b),x,y);
				}
				assertEquals(sum/numBands,GeneralizedImageOps.get(found, x, y),1);
			}
		}
	}

	private void checkInterleavedAverage(Method m, Class inputType, Class outputType) {
		if( inputType.isAssignableFrom(ImageInterleaved.class) )
			fail("Expected ImageInterleaved image");

		ImageSingleBand output = GeneralizedImageOps.createSingleBand(outputType, imgWidth, imgHeight);

		boolean signed = true;

		if( ImageInteger.class.isAssignableFrom(outputType) )
			signed = output.getDataType().isSigned();

		for( int numBands = 1; numBands <= 3; numBands++ ) {
			ImageInterleaved input = GeneralizedImageOps.createInterleaved(inputType,imgWidth,imgHeight,numBands);

			// only provide signed numbers of both data types can handle them
			if( signed ) {
				GImageMiscOps.fillUniform(input, rand, -10, 10);
			} else {
				GImageMiscOps.fillUniform(input, rand, 0, 20);
			}

			BoofTesting.checkSubImage(this,"checkInterleavedAverage",true,m,input,output);
		}
	}

	public void checkInterleavedAverage(Method m, ImageInterleaved input, ImageSingleBand<?> output) {
		try {
			// check it with a non-null output
			ImageSingleBand<?> ret = (ImageSingleBand<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			checkInterleavedAverage(input, ret);

			// check it with a null output
			ret = (ImageSingleBand<?>)m.invoke(null,input,null);
			checkInterleavedAverage(input, ret);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkInterleavedAverage(ImageInterleaved input, ImageSingleBand<?> found) {
		int numBands = input.getNumBands();
		for( int y = 0; y < imgHeight; y++ ){
			for( int x = 0; x < imgWidth; x++ ) {
				double sum = 0;
				for( int b = 0; b < numBands; b++ ) {
					sum += GeneralizedImageOps.get(input,x,y,b);
				}
				assertEquals(sum/numBands,GeneralizedImageOps.get(found, x, y),1);
			}
		}
	}

	/**
	 * If the two images are both int or float then set a low tolerance, otherwise set the tolerance to one pixel
	 */
	private double selectTolerance( ImageSingleBand a , ImageSingleBand b ) {
		if( a.getDataType().isInteger() == b.getDataType().isInteger() )
			return 1e-4;
		else
			return 1;
	}

	/**
	 * If the two images are both int or float then set a low tolerance, otherwise set the tolerance to one pixel
	 */
	private double selectTolerance( ImageInterleaved a , ImageInterleaved b ) {
		if( a.getDataType().isInteger() == b.getDataType().isInteger() )
			return 1e-4;
		else
			return 1;
	}
}
