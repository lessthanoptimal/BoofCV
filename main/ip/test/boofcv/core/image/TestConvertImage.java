/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
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
				checkAverage(m,inputType,outputType);
			}
			count++;
		}

		assertEquals(8*7+8,count);
	}

	private void checkConvert( Method m , Class inputType , Class outputType ) {
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

		BoofTesting.checkSubImage(this,"checkConvert",true,m,input,output);
	}

	public void checkConvert( Method m , ImageSingleBand<?> input , ImageSingleBand<?> output ) {
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

	private void checkAverage( Method m , Class inputType , Class outputType ) {
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

			BoofTesting.checkSubImage(this,"checkAverage",true,m,input,output);
		}
	}

	public void checkAverage( Method m , MultiSpectral<?> input , ImageSingleBand<?> output ) {
		try {
			// check it with a non-null output
			ImageSingleBand<?> ret = (ImageSingleBand<?>)m.invoke(null,input,output);
			assertTrue(ret == output);
			checkAverage(input, ret);

			// check it with a null output
			ret = (ImageSingleBand<?>)m.invoke(null,input,null);
			checkAverage(input, ret);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkAverage(  MultiSpectral<?> input , ImageSingleBand<?> found ) {
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

	/**
	 * If the two images are both int or float then set a low tolerance, otherwise set the tolerance to one pixel
	 */
	private double selectTolerance( ImageSingleBand a , ImageSingleBand b ) {
		if( a.getDataType().isInteger() == b.getDataType().isInteger() )
			return 1e-4;
		else
			return 1;
	}
}
