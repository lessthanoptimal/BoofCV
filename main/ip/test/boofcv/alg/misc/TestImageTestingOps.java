/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageSingleBand;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestImageTestingOps {

	int width = 10;
	int height = 15;
	Random rand = new Random(234);

	@Test
	public void checkAll() {
		int numExpected = 46;
		Method methods[] = ImageTestingOps.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;
			try {
				if( m.getName().compareTo("fill") == 0 ) {
					testFill(m);
				} else if( m.getName().compareTo("fillRectangle") == 0 ) {
					testFillRectangle(m);
				} else if( m.getName().compareTo("randomize") == 0 ) {
					testRandomize(m);
				} else if( m.getName().compareTo("addUniform") == 0 ) {
				    testAddUniform(m);
				} else if( m.getName().compareTo("addGaussian") == 0 ) {
					testAddGaussian(m);
				} else if( m.getName().compareTo("computeMeanSquaredError") == 0 ) {
					testComputeMSE(m);
				} else if( m.getName().compareTo("flipVertical") == 0 ) {
					testFlipVertical(m);
				} else {
					throw new RuntimeException("Unknown function");
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

		Class<?> param[] = m.getParameterTypes();

		if( param.length < 1 )
			return false;

		return ImageSingleBand.class.isAssignableFrom(param[0]);
	}

	private void testFill( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GeneralizedImageOps.randomize(orig, rand, 0,20);

		if( orig.getTypeInfo().isInteger()) {
			m.invoke(null,orig,10);
		} else {
			m.invoke(null,orig,10.0f);
		}

		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				assertEquals(10.0,a.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testFillRectangle( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		int x0 = 2;
		int y0 = 3;
		int width = 5;
		int height = 6;

		if( orig.getTypeInfo().isInteger() ) {
			m.invoke(null,orig,10,x0,y0,width,height);
		} else {
			m.invoke(null,orig,10.0f,x0,y0,width,height);
		}

		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				if( j < x0 || i < y0 || i >= (x0+width) || j >= (y0+height ))
					assertEquals(j+" "+i,0.0,a.get(j,i).doubleValue(),1e-4);
				else
					assertEquals(10.0,a.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testRandomize( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( orig.getTypeInfo().isInteger() ) {
			if( orig.getTypeInfo().isSigned() )
				m.invoke(null,orig,rand,-10,10);
			else {
				m.invoke(null,orig,rand,1,10);
			}
		} else {
			m.invoke(null,orig,rand,-10,10);
		}

		int numZero = 0;

		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				assertTrue(value>=-10 && value <= 10);
				if( value == 0 )
					numZero++;
			}
		}

		assertTrue( numZero < width*height );
	}

	private void testAddUniform( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GeneralizedImageOps.fill(orig,1);

		if( orig.getTypeInfo().isInteger() ) {
			m.invoke(null,orig,rand,1,10);
		} else {
			m.invoke(null,orig,rand,1,10);
		}

		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				assertTrue(value>=-2 && value <= 11);
			}
		}
	}

	private void testAddGaussian( Method m ) throws InvocationTargetException, IllegalAccessException {

		double mean = 10;

		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GeneralizedImageOps.fill(orig,mean);
		m.invoke(null,orig,rand,2.0,0,255);

		double stdev2 = 0;
		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				stdev2 += (value-mean)*(value-mean);
			}
		}

		GeneralizedImageOps.fill(orig,mean);
		m.invoke(null,orig,rand,10.0,0,255);

		double stdev10 = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				stdev10 += (value-mean)*(value-mean);
			}
		}

		// see if the gaussian with the larger variance creates a noisier image
		assertTrue(stdev2<stdev10);
	}

	private void testComputeMSE( Method m ) throws InvocationTargetException, IllegalAccessException {

		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand imgA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GeneralizedImageOps.fill(imgA,10);
		ImageSingleBand imgB = imgA.clone();

		GImageSingleBand b = FactoryGImageSingleBand.wrap(imgB);
		b.set(5,5,20);

		Number error = (Number)m.invoke(null,imgA,imgB);
		double expected = (10.0*10.0)/(width*height);

		assertEquals(expected,error.doubleValue(),1e-8);
	}

	private void testFlipVertical(Method m) throws InvocationTargetException, IllegalAccessException {

		// test with an even and odd height
		testFlipVertical(m,height);
		testFlipVertical(m,height+1);
	}

	private void testFlipVertical(Method m, int height) throws IllegalAccessException, InvocationTargetException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand imgA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GeneralizedImageOps.randomize(imgA,rand,0,100);
		ImageSingleBand imgB = imgA.clone();

		m.invoke(null,imgB);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double valA = GeneralizedImageOps.get(imgA,x,height-y-1);
				double valB = GeneralizedImageOps.get(imgB,x,y);
				assertTrue(valA==valB);
			}
		}
	}
}
