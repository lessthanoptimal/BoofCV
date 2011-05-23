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
import gecv.struct.image.ImageInteger;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestPixelMath {
int width = 10;
	int height = 15;
	Random rand = new Random(234);

	@Test
	public void checkAll() {
		int numExpected = 28;
		Method methods[] = PixelMath.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;
			try {
				System.out.println(m.getName());
				if( m.getName().compareTo("abs") == 0 ) {
					testAbs(m);
				} else if( m.getName().compareTo("maxAbs") == 0 ) {
					testMaxAbs(m);
				} else if( m.getName().compareTo("divide") == 0 ) {
					testDivide(m);
				} else if( m.getName().compareTo("multiply") == 0 ) {
				    testMultiply(m);
				} else if( m.getName().compareTo("plus") == 0 ) {
				    testPlus(m);
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

		return ImageBase.class.isAssignableFrom(param[0]);
	}

	private void testAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase input = GecvTesting.createImage(paramTypes[0],width,height);
		ImageBase output = GecvTesting.createImage(paramTypes[0],width,height);
		GeneralizedImageOps.randomize(input,-20,20,rand);

		m.invoke(null,input,output);

		SingleBandImage a = FactorySingleBandImage.wrap(input);
		SingleBandImage b = FactorySingleBandImage.wrap(output);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				assertEquals(Math.abs(a.get(j,i).doubleValue()),b.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testMaxAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase input = GecvTesting.createImage(paramTypes[0],width,height);

		SingleBandImage a = FactorySingleBandImage.wrap(input);

		if( input.isSigned() ) {
			GeneralizedImageOps.randomize(input,-20,20,rand);
			a.set(0,3,-100);
		} else {
			GeneralizedImageOps.randomize(input,0,20,rand);
			a.set(0,3,100);
		}

		Number o = (Number)m.invoke(null,input);

		assertEquals(100,o.doubleValue(),1e-8);

	}

	private void testDivide( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase input = GecvTesting.createImage(paramTypes[0],width,height);
		ImageBase output = GecvTesting.createImage(paramTypes[0],width,height);
		GeneralizedImageOps.randomize(input,0,20,rand);

		if( input.isSigned() ) {
			GeneralizedImageOps.randomize(input,-20,20,rand);
		} else {
			GeneralizedImageOps.randomize(input,0,20,rand);
		}

		if( input.isInteger() )
			m.invoke(null,input,output,10);
		else
			m.invoke(null,input,output,10.0f);

		SingleBandImage a = FactorySingleBandImage.wrap(input);
		SingleBandImage b = FactorySingleBandImage.wrap(output);
		if( input.isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).intValue()/10,b.get(j,i).intValue());
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).doubleValue()/10f,b.get(j,i).doubleValue(),1e-4);
				}
			}
		}
	}

	private void testMultiply( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase input = GecvTesting.createImage(paramTypes[0],width,height);
		ImageBase output = GecvTesting.createImage(paramTypes[0],width,height);
		GeneralizedImageOps.randomize(input,0,20,rand);

		if( input.isSigned() ) {
			GeneralizedImageOps.randomize(input,-20,20,rand);
		} else {
			GeneralizedImageOps.randomize(input,0,20,rand);
		}

		if( input.isInteger() )
			m.invoke(null,input,output,2);
		else
			m.invoke(null,input,output,2.0f);

		SingleBandImage a = FactorySingleBandImage.wrap(input);
		SingleBandImage b = FactorySingleBandImage.wrap(output);
		if( input.isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).intValue()*2,b.get(j,i).intValue());
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).doubleValue()*2f,b.get(j,i).doubleValue(),1e-4);
				}
			}
		}
	}

	private void testPlus( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class<?> paramTypes[] = m.getParameterTypes();
		ImageBase input = GecvTesting.createImage(paramTypes[0],width,height);
		ImageBase output = GecvTesting.createImage(paramTypes[0],width,height);
		GeneralizedImageOps.randomize(input,0,20,rand);

		if( input.isSigned() ) {
			GeneralizedImageOps.randomize(input,-20,20,rand);
		} else {
			GeneralizedImageOps.randomize(input,0,20,rand);
		}

		if( input.isInteger() )
			m.invoke(null,input,output,2);
		else
			m.invoke(null,input,output,2.0f);

		SingleBandImage a = FactorySingleBandImage.wrap(input);
		SingleBandImage b = FactorySingleBandImage.wrap(output);
		if( input.isInteger() ) {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).intValue()+2,b.get(j,i).intValue());
				}
			}
		} else {
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					assertEquals(a.get(j,i).doubleValue()+2f,b.get(j,i).doubleValue(),1e-4);
				}
			}
		}
	}
}
