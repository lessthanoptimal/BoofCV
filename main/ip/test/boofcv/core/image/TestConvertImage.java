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
@SuppressWarnings("Duplicates")
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
			if( inputTypes.length < 2)
				continue;

			Class<?> outputType = inputTypes[inputTypes.length-1];
			if( (inputTypes.length != 5 && inputTypes.length != 2) || !ImageBase.class.isAssignableFrom(outputType))
				continue;

			Class<?> inputType = inputTypes[0];

//			System.out.println(m.getName()+" "+inputType.getSimpleName()+" "+outputType.getSimpleName()+" "+m.getReturnType());
			
			// make sure the return type equals the output type
			assertTrue(outputType == m.getReturnType());

			if( m.getName().contains("convert") ) {
				if( inputTypes.length == 5 ) {
					checkConvertIntegerRange(m,inputTypes);
				} else {
					checkConvert(m, inputType, outputType);
				}
			} else {
				if( inputType == Planar.class )
					checkMultiAverage(m, inputType, outputType);
				else
					checkInterleavedAverage(m, inputType, outputType);
			}
			count++;
		}

		assertEquals(8*7 + 8*7 +8+8+8+8+8,count);
	}

	private void checkConvert( Method m , Class inputType , Class outputType ) {
		if( ImageGray.class.isAssignableFrom(inputType) ) {
			checkConvertSingle(m, inputType, outputType);
		} else if( ImageInterleaved.class.isAssignableFrom(inputType)) {
			if( ImageInterleaved.class.isAssignableFrom(outputType) )
				checkConvertInterleaved(m, inputType, outputType);
			else
				checkConvertInterleavedToMulti(m, inputType, outputType);
		} else {
			checkConvertMultiToInterleaved(m,inputType,outputType);
		}
	}

	private void checkConvertIntegerRange( Method m , Class[] types ) {

		Class inputType = types[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, imgWidth, imgHeight);
		GrayU8 output = new GrayU8(imgWidth,imgHeight);

		boolean inputSigned = true;

		if( GrayI.class.isAssignableFrom(inputType) )
			inputSigned = input.getDataType().isSigned();

	   // only provide signed numbers of both data types can handle them
		if( inputSigned ) {
			GImageMiscOps.fillUniform(input, rand, -10, 10);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0, 20);
		}

		BoofTesting.checkSubImage(this,"checkConvertIntegerRange",true,m,input,output);
	}

	public void checkConvertIntegerRange(Method m , ImageGray<?> input , GrayU8 output ) {
		try {
			GrayU8 ret;
			double tol = selectTolerance(input,output);

			// check it with a non-null output
			ret = invokeConvertIntegerRange(m, input, output);
			assertTrue(ret == output);
			checkResultsIntegerRange(ret);

			// check it with a null output
			ret = invokeConvertIntegerRange(m, input, null);
			checkResultsIntegerRange(ret);

		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private GrayU8 invokeConvertIntegerRange(Method m, ImageGray<?> input, GrayU8 output) throws IllegalAccessException, InvocationTargetException {
		GrayU8 ret;
		if( input.getDataType().isInteger() )
			ret = (GrayU8)m.invoke(null,input,-10,20,8,output);
		else if( input.getDataType().getNumBits() == 32 )
			ret = (GrayU8)m.invoke(null,input,-10.0f,20.0f,8,output);
		else
			ret = (GrayU8)m.invoke(null,input,-10.0,20.0,8,output);
		return ret;
	}

	private void checkResultsIntegerRange( GrayU8 output ) {
		int notZero = 0;
		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				int found = output.get(x,y);
				assertTrue(found>=0 && found < 8);
				if( found != 0 )
					notZero++;
			}
		}
		assertTrue(notZero>0);
	}

	private void checkConvertSingle( Method m , Class inputType , Class outputType ) {
		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, imgWidth, imgHeight);
		ImageGray output = GeneralizedImageOps.createSingleBand(outputType, imgWidth, imgHeight);

		boolean inputSigned = true;
		boolean outputSigned = true;

		if( GrayI.class.isAssignableFrom(inputType) )
			inputSigned = input.getDataType().isSigned();
		if( GrayI.class.isAssignableFrom(outputType) )
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

	public void checkConvertSingle(Method m , ImageGray<?> input , ImageGray<?> output ) {
		try {
			double tol = selectTolerance(input,output);

			// check it with a non-null output
			ImageGray<?> ret = (ImageGray<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			BoofTesting.assertEquals(input, ret, tol);

			// check it with a null output
			ret = (ImageGray<?>)m.invoke(null,input,null);
			BoofTesting.assertEquals(input, ret, tol);

		} catch (IllegalAccessException | InvocationTargetException e) {
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

		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkConvertInterleavedToMulti( Method m , Class inputType , Class outputType ) {
		ImageInterleaved input = GeneralizedImageOps.createInterleaved(inputType, imgWidth, imgHeight,2);

		Class bandType = ImageDataType.typeToSingleClass(input.getDataType());
		Planar output = new Planar(bandType,imgWidth, imgHeight, 2);

		boolean inputSigned = true;

		if( input.getImageType().getDataType().isInteger() )
			inputSigned = input.getDataType().isSigned();

		// only provide signed numbers of both data types can handle them
		if( inputSigned ) {
			GImageMiscOps.fillUniform(input, rand, -10, 10);
		}

		BoofTesting.checkSubImage(this, "checkConvertInterleavedToMulti", true, m, input, output);
	}

	public void checkConvertInterleavedToMulti( Method m , ImageInterleaved<?> input , Planar<?> output ) {
		try {
			double tol = 1e-4;

			// check it with a non-null output
			Planar<?> ret = (Planar<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			BoofTesting.assertEquals(input, ret, tol);

			// check it with a null output
			ret = (Planar<?>)m.invoke(null,input,null);
			BoofTesting.assertEquals(input, ret, tol);

		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkConvertMultiToInterleaved( Method m , Class inputType , Class outputType ) {
		ImageInterleaved output = GeneralizedImageOps.createInterleaved(outputType, imgWidth, imgHeight,2);

		Class bandType = ImageDataType.typeToSingleClass(output.getDataType());
		Planar input = new Planar(bandType,imgWidth, imgHeight, 2);

		boolean signed = true;

		if( input.getImageType().getDataType().isInteger() )
			signed = output.getDataType().isSigned();

		// only provide signed numbers of both data types can handle them
		if( signed ) {
			GImageMiscOps.fillUniform(input, rand, -10, 10);
		}

		BoofTesting.checkSubImage(this, "checkConvertMultiToInterleaved", true, m, input, output);
	}

	public void checkConvertMultiToInterleaved(Method m , Planar<?> input , ImageInterleaved<?> output ) {
		try {
			double tol = 1e-4;

			// check it with a non-null output
			ImageInterleaved<?> ret = (ImageInterleaved<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			BoofTesting.assertEquals(input, ret, tol);

			// check it with a null output
			ret = (ImageInterleaved<?>)m.invoke(null,input,null);
			BoofTesting.assertEquals(input, ret, tol);

		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkMultiAverage(Method m, Class inputType, Class outputType) {
		if( inputType != Planar.class )
			fail("Expected Planar image");

		ImageGray output = GeneralizedImageOps.createSingleBand(outputType, imgWidth, imgHeight);

		boolean signed = true;

		if( GrayI.class.isAssignableFrom(outputType) )
			signed = output.getDataType().isSigned();

		for( int numBands = 1; numBands <= 3; numBands++ ) {
			Planar input = new Planar(outputType,imgWidth,imgHeight,numBands);

			// only provide signed numbers of both data types can handle them
			if( signed ) {
				GImageMiscOps.fillUniform(input, rand, -10, 10);
			} else {
				GImageMiscOps.fillUniform(input, rand, 0, 20);
			}

			BoofTesting.checkSubImage(this,"checkMultiAverage",true,m,input,output);
		}
	}

	public void checkMultiAverage(Method m, Planar<?> input, ImageGray<?> output) {
		try {
			// check it with a non-null output
			ImageGray<?> ret = (ImageGray<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			checkMultiAverage(input, ret);

			// check it with a null output
			ret = (ImageGray<?>)m.invoke(null,input,null);
			checkMultiAverage(input, ret);

		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkMultiAverage(Planar<?> input, ImageGray<?> found) {
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

		ImageGray output = GeneralizedImageOps.createSingleBand(outputType, imgWidth, imgHeight);

		boolean signed = true;

		if( GrayI.class.isAssignableFrom(outputType) )
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

	public void checkInterleavedAverage(Method m, ImageInterleaved input, ImageGray<?> output) {
		try {
			// check it with a non-null output
			ImageGray<?> ret = (ImageGray<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			checkInterleavedAverage(input, ret);

			// check it with a null output
			ret = (ImageGray<?>)m.invoke(null,input,null);
			checkInterleavedAverage(input, ret);

		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkInterleavedAverage(ImageInterleaved input, ImageGray<?> found) {
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
	private double selectTolerance(ImageGray a , ImageGray b ) {
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
