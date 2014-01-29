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

package boofcv.alg.misc;

import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImageStatistics {
	int width = 10;
	int height = 15;
	Random rand = new Random(234);

	@Test
	public void checkAll() {
		int numExpected = 9*8;
		Method methods[] = ImageStatistics.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;
			try {
				System.out.println(m.getName());

				if( m.getName().compareTo("min") == 0 ) {
					testMin(m);
				} else if( m.getName().compareTo("max") == 0 ) {
					testMax(m);
				} else if( m.getName().compareTo("maxAbs") == 0 ) {
					testMaxAbs(m);
				} else if( m.getName().compareTo("sum") == 0 ) {
					testSum(m);
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
				} else {
					throw new RuntimeException("Unknown function: "+m.getName());
				}
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if(numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numExpected);
	}

	private boolean isTestMethod(Method m ) {

		Class param[] = m.getParameterTypes();

		if( param.length < 1 )
			return false;

		return ImageBase.class.isAssignableFrom(param[0]);
	}

	private void testMaxAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,20);
			a.set(0,3,-100);
		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
			a.set(0,3,100);
		}

		Number o = (Number)m.invoke(null,input);

		assertEquals(100,o.doubleValue(),1e-8);

	}

	private void testMax( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,-5);
			a.set(0,3,-2);
			Number o = (Number)m.invoke(null,input);
			assertEquals(-2,o.doubleValue(),1e-8);

		} else {
			GImageMiscOps.fillUniform(input, rand, 0,20);
			a.set(0,3,100);
			Number o = (Number)m.invoke(null,input);
			assertEquals(100,o.doubleValue(),1e-8);
		}
	}

	private void testMin( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(input);

		if( input.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(input, rand, -20,-5);
			a.set(0,3,-30);
			Number o = (Number)m.invoke(null,input);
			assertEquals(-30,o.doubleValue(),1e-8);

		} else {
			GImageMiscOps.fillUniform(input, rand, 5,20);
			a.set(0,3,1);
			Number o = (Number)m.invoke(null,input);
			assertEquals(1,o.doubleValue(),1e-8);
		}
	}

	private void testSum( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		Object result = m.invoke(null,inputA);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(inputA);

		double expected = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				expected += a.get(j,i).doubleValue();
			}
		}

		double found = ((Number)result).doubleValue();
		assertEquals(expected,found,1e-4);
	}

	private void testMean( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		Object result = m.invoke(null, inputA);

		double total = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				total += GeneralizedImageOps.get(inputA,j,i);
			}
		}

		double mean = total/(width*height);

		assertEquals(mean,((Number)result).doubleValue(),1e-4);
	}

	private void testVariance( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,20);
		}

		double mean = 2.5;

		Object result = m.invoke(null,inputA,mean);

		double total = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double d = GeneralizedImageOps.get(inputA,j,i) - mean;
				total += d*d;
			}
		}

		double var = total/(width*height);

		assertEquals(var, ((Number) result).doubleValue(), 1e-4);
	}

	private void testMeanDiffSq(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( inputA.getDataType().isSigned() ) {
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
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);
				total += (a-b)*(a-b);
			}
		}

		double expected = total/(width*height);

		assertEquals(expected, ((Number) result).doubleValue(), 1e-4);
	}

	private void testMeanDiffAbs(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageSingleBand inputB = GeneralizedImageOps.createSingleBand(paramTypes[1], width, height);

		if( inputA.getDataType().isSigned() ) {
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
				double a = GeneralizedImageOps.get(inputA,j,i);
				double b = GeneralizedImageOps.get(inputB,j,i);
				total += Math.abs(a-b);
			}
		}

		double expected = total/(width*height);

		assertEquals(expected, ((Number) result).doubleValue(), 1e-4);
	}

	private void testHistogram(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand inputA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		int histogram[] = new int[ 100 ];
		// it should be zeroed
		for( int i = 0; i < histogram.length; i++ )
			histogram[i] = 100;

		int minValue;
		if( inputA.getDataType().isSigned() ) {
			GImageMiscOps.fillUniform(inputA, rand, -20,20);
			m.invoke(null,inputA,-20,histogram);
			minValue = -20;
		} else {
			GImageMiscOps.fillUniform(inputA, rand, 0,40);
			m.invoke(null,inputA,histogram);
			minValue = 0;
		}

		// manually compute the histogram
		int expected[] = new int[ 100 ];
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double a = GeneralizedImageOps.get(inputA,j,i);
				expected[ -minValue + (int)a ]++;
			}
		}

		for( int i = 0; i < 100; i++ ) {
			assertEquals("index "+i,expected[i],histogram[i]);
		}
	}

}
