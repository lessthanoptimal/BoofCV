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

package boofcv.alg.misc;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImageStatistics extends BoofStandardJUnit {
	int width = 10;
	int height = 15;
	int numBands = 2;

	@Test void checkAll() {
		int numExpected = 11*8 + 8*8;
		Method[] methods = ImageStatistics.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;
			try {
//				System.out.println(m);

				if( m.getName().compareTo("min") == 0 ) {
					testMin(m);
				} else if( m.getName().compareTo("max") == 0 ) {
					testMax(m);
				} else if( m.getName().compareTo("maxAbs") == 0 ) {
					testMaxAbs(m);
				} else if( m.getName().compareTo("sum") == 0 ) {
					testSum(m);
				} else if( m.getName().compareTo("sumAbs") == 0 ) {
					testSumAbs(m);
				} else if( m.getName().compareTo("mean") == 0 ) {
					testMean(m);
				} else if( m.getName().compareTo("variance") == 0 ) {
					testVariance(m);
				} else if( m.getName().compareTo("meanDiffSq") == 0 ) {
					testMeanDiffSq(m);
				} else if( m.getName().compareTo("meanDiffAbs") == 0 ) {
					testMeanDiffAbs(m);
				} else if( m.getName().compareTo("histogram") == 0 ) {
					testHistogram(m);
				} else if( m.getName().compareTo("histogramScaled") == 0 ) {
					testHistogramScaled(m);
				} else {
					throw new RuntimeException("Unknown function: "+m.getName());
				}
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if(numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numExpected);
	}

	private boolean isTestMethod(Method m ) {

		Class[] param = m.getParameterTypes();

		if( param.length < 1 )
			return false;

		return ImageBase.class.isAssignableFrom(param[0]);
	}

	private void testMaxAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		if( input.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,19);
			GeneralizedImageOps.setB(input,0,3,0,-100);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,19);
			GeneralizedImageOps.setB(input,0,3,0,100);
		}

		Number o = (Number)m.invoke(null,input);

		assertEquals(100,o.doubleValue(),1e-8);

	}

	private void testMax( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		if( input.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,-5);
			GeneralizedImageOps.setB(input,0,3,0,-2);
			Number o = (Number)m.invoke(null,input);
			assertEquals(-2,o.doubleValue(),1e-8);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,200);

			double maxValue = input.getImageType().getDataType().getMaxValue();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double before = GeneralizedImageOps.get(input,x,y,0);
					GeneralizedImageOps.setB(input,x,y,0,maxValue);
					Number o = (Number)m.invoke(null,input);
					assertEquals(maxValue,o.doubleValue(),1e-8);
					GeneralizedImageOps.setB(input,x,y,0,before);
				}
			}
		}
	}

	private void testMin( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase input = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		if( input.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,-5);
			GeneralizedImageOps.setB(input,0,3,0,-30);
			Number o = (Number)m.invoke(null,input);
			assertEquals(-30,o.doubleValue(),1e-8);

		} else {
			double maxValue = input.getImageType().getDataType().getMaxValue();
			GImageMiscOps.fillUniform(input, rand, 100,maxValue);

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double before = GeneralizedImageOps.get(input,x,y,0);
					GeneralizedImageOps.setB(input,x,y,0,5);
					Number o = (Number)m.invoke(null,input);
					assertEquals(5,o.doubleValue(),1e-8);
					GeneralizedImageOps.setB(input,x,y,0,before);
				}
			}
		}
	}

	private void testSum( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if( inputA.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		Object result = m.invoke(null,inputA);

		double expected = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int k = 0; k < numBands; k++) {
					expected += GeneralizedImageOps.get(inputA,j,i,k);
				}
			}
		}

		assertEqualsAdjustTol(inputA, (Number) result, expected);
	}

	private void testSumAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if( inputA.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		Object result = m.invoke(null,inputA);

		double expected = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int k = 0; k < numBands; k++) {
					expected += Math.abs(GeneralizedImageOps.get(inputA,j,i,k));
				}
			}
		}

		assertEqualsAdjustTol(inputA, (Number) result, expected);
	}

	private void testMean( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if( inputA.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		Object result = m.invoke(null,inputA);

		double expected = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int k = 0; k < numBands; k++) {
					expected += GeneralizedImageOps.get(inputA,j,i,k);
				}
			}
		}

		expected /= (width*height*numBands);

		assertEqualsAdjustTol(inputA, (Number) result, expected);
	}

	private void testVariance( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		double mean = 2.5;

		Object result = paramTypes[1] == double.class ? m.invoke(null,inputA,mean) :
				m.invoke(null,inputA,(float)mean);

		double total = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double d = GeneralizedImageOps.get(inputA,j,i) - mean;
				total += d*d;
			}
		}

		double var = total/(width*height);

		assertEqualsAdjustTol(inputA, (Number) result, var);
	}

	private void testMeanDiffSq(Method m) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase inputB = GeneralizedImageOps.createImage(paramTypes[1], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if( inputA.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,19);
			GImageMiscOps.fillUniform(inputB, rand, -20,19);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,19);
			GImageMiscOps.fillUniform(inputB, rand, 0,19);
		}

		Object result = m.invoke(null,inputA,inputB);

		double total = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int k = 0; k < numBands; k++) {
					double a = GeneralizedImageOps.get(inputA,j,i,k);
					double b = GeneralizedImageOps.get(inputB,j,i,k);
					total += (a-b)*(a-b);
				}
			}
		}

		double expected = total/(width*height*numBands);

		assertEqualsAdjustTol(inputA, (Number) result, expected);
	}

	private void assertEqualsAdjustTol(ImageBase inputA, Number result, double expected) {
		if( inputA.imageType.getDataType().isInteger() ) {
			assertEquals( (int)expected, result.intValue());
		} else {
			double tol;
			if( inputA.imageType.getDataType().getNumBits() == 32 ){
				tol = UtilEjml.TEST_F32;
			} else {
				tol = UtilEjml.TEST_F64;
			}

			// adjust for the number's scale. Was getting false positives for 32
			tol = Math.max(1,Math.abs(expected))*tol;

			assertEquals(expected, result.doubleValue(), tol);
		}
	}

	private void testMeanDiffAbs(Method m) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageBase inputA = GeneralizedImageOps.createImage(paramTypes[0], width, height, numBands);
		ImageBase inputB = GeneralizedImageOps.createImage(paramTypes[1], width, height, numBands);

		int numBands = inputA.getImageType().getNumBands();

		if( inputA.getImageType().getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
			GImageMiscOps.fillUniform(inputB, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
			GImageMiscOps.fillUniform(inputB, rand, 0,20);
		}

		Object result = m.invoke(null,inputA,inputB);

		double total = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int k = 0; k < numBands; k++) {
					double a = GeneralizedImageOps.get(inputA, j, i, k);
					double b = GeneralizedImageOps.get(inputB, j, i, k);
					total += Math.abs(a - b);
				}
			}
		}

		double expected = total/(width*height*numBands);

		assertEqualsAdjustTol(inputA, (Number) result, expected);
	}

	private void testHistogram(Method m) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		int[] histogram = new int[ 40 ];
		// it should be zeroed
		for( int i = 0; i < histogram.length; i++ )
			histogram[i] = 100;

		int minValue;
		if( inputA.getDataType().isSigned() ) {
			minValue = -20;
			GImageMiscOps.fillUniform(inputA, rand, minValue,19);
			m.invoke(null,inputA,minValue,histogram);
		} else {
			minValue = 5;
			GImageMiscOps.fillUniform(inputA, rand, minValue,minValue+histogram.length-1);
			m.invoke(null,inputA,minValue,histogram);
		}

		// manually compute the histogram
		int[] expected = new int[ histogram.length ];
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				expected[ (int)(-minValue + a) ]++;
			}
		}

		for( int i = 0; i < 40; i++ ) {
			assertEquals(expected[i],histogram[i],"index "+i);
		}
	}

	private void testHistogramScaled(Method m) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		int[] histogram = new int[ 40 ];
		// it should be zeroed
		for( int i = 0; i < histogram.length; i++ )
			histogram[i] = 100;

		int minValue,maxValue;
		if( inputA.getDataType().isSigned() ) {
			minValue = -20; maxValue = 59;
			GImageMiscOps.fillUniform(inputA, rand, minValue,maxValue);
			m.invoke(null,inputA,minValue,maxValue,histogram);
		} else {
			minValue = 5; maxValue = 79;
			GImageMiscOps.fillUniform(inputA, rand, minValue,maxValue);
			m.invoke(null,inputA,minValue,maxValue,histogram);
		}

		// manually compute the histogram
		int[] expected = new int[ histogram.length ];
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				expected[ (int)(histogram.length*(a-minValue)/(maxValue-minValue+1)) ]++;
			}
		}

		for( int i = 0; i < histogram.length; i++ ) {
			assertEquals(expected[i],histogram[i],"index "+i);
		}
	}

}
