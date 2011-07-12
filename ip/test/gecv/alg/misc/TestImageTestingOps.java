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

package gecv.alg.misc;

import gecv.core.image.FactorySingleBandImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.SingleBandImage;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;
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
		int numExpected = 40;
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

		if( param.length < 2 )
			return false;

		return ImageBase.class.isAssignableFrom(param[0]);
	}

	private void testFill( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase orig = GecvTesting.createImage(paramTypes[0],width,height);
		GeneralizedImageOps.randomize(orig, rand, 0,20);

		if( orig.getTypeInfo().isInteger()) {
			m.invoke(null,orig,10);
		} else {
			m.invoke(null,orig,10.0f);
		}

		SingleBandImage a = FactorySingleBandImage.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				assertEquals(10.0,a.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testFillRectangle( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase orig = GecvTesting.createImage(paramTypes[0],width,height);

		int x0 = 2;
		int y0 = 3;
		int width = 5;
		int height = 6;

		if( orig.getTypeInfo().isInteger() ) {
			m.invoke(null,orig,10,x0,y0,width,height);
		} else {
			m.invoke(null,orig,10.0f,x0,y0,width,height);
		}

		SingleBandImage a = FactorySingleBandImage.wrap(orig);
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
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase orig = GecvTesting.createImage(paramTypes[0],width,height);

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

		SingleBandImage a = FactorySingleBandImage.wrap(orig);
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
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase orig = GecvTesting.createImage(paramTypes[0],width,height);
		GeneralizedImageOps.fill(orig,1);

		if( orig.getTypeInfo().isInteger() ) {
			m.invoke(null,orig,rand,1,10);
		} else {
			m.invoke(null,orig,rand,1,10);
		}

		SingleBandImage a = FactorySingleBandImage.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				assertTrue(value>=-2 && value <= 11);
			}
		}
	}

	private void testAddGaussian( Method m ) throws InvocationTargetException, IllegalAccessException {

		double mean = 10;

		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase orig = GecvTesting.createImage(paramTypes[0],width,height);

		GeneralizedImageOps.fill(orig,mean);
		m.invoke(null,orig,rand,2.0);

		double stdev2 = 0;
		SingleBandImage a = FactorySingleBandImage.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				stdev2 += (value-mean)*(value-mean);
			}
		}

		GeneralizedImageOps.fill(orig,mean);
		m.invoke(null,orig,rand,10.0);

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

		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase imgA = GecvTesting.createImage(paramTypes[0],width,height);

		GeneralizedImageOps.fill(imgA,10);
		ImageBase imgB = imgA.clone();

		SingleBandImage b = FactorySingleBandImage.wrap(imgB);
		b.set(5,5,20);

		Number error = (Number)m.invoke(null,imgA,imgB);
		double expected = (10.0*10.0)/(width*height);

		assertEquals(expected,error.doubleValue(),1e-8);
	}
}
