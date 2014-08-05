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
import boofcv.struct.image.ImageInterleaved;
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
public class TestImageMiscOps {

	int width = 10;
	int height = 15;
	int numBands = 3;
	Random rand = new Random(234);

	@Test
	public void checkAll() {
		int numExpected = 12*6 + 8*2;
		Method methods[] = ImageMiscOps.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;
			try {
//				System.out.println(m.getName());
				if( m.getName().compareTo("copy") == 0 ) {
					testCopy(m);
				} else if( m.getName().compareTo("fill") == 0 ) {
					testFill(m);
				} else if( m.getName().compareTo("fillBorder") == 0 ) {
					testFillBorder(m);
				} else if( m.getName().compareTo("fillRectangle") == 0 ) {
					testFillRectangle(m);
				} else if( m.getName().compareTo("fillUniform") == 0 ) {
					testFillUniform(m);
				} else if( m.getName().compareTo("fillGaussian") == 0 ) {
					testFillGaussian(m);
				} else if( m.getName().compareTo("addUniform") == 0 ) {
				    testAddUniform(m);
				} else if( m.getName().compareTo("addGaussian") == 0 ) {
					testAddGaussian(m);
				} else if( m.getName().compareTo("flipVertical") == 0 ) {
					testFlipVertical(m);
				} else if( m.getName().compareTo("flipHorizontal") == 0 ) {
					testFlipHorizontal(m);
				} else if( m.getName().compareTo("rotateCW") == 0 ) {
					testRotateCW(m);
				} else if( m.getName().compareTo("rotateCCW") == 0 ) {
					testRotateCCW(m);
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

		for( int i = 0; i < param.length; i++ ) {
			if( ImageBase.class.isAssignableFrom(param[i] ))
				return true;
		}
		return false;
	}

	private void testCopy( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand src = GeneralizedImageOps.createSingleBand(paramTypes[6], width, height);
		ImageSingleBand dst = GeneralizedImageOps.createSingleBand(paramTypes[7], width, height);

		GImageMiscOps.fillUniform(src, rand, 0,20);
		GImageMiscOps.fillUniform(dst, rand, 0,20);

		int w = 5,h=8;
		int x0=1,y0=2;
		int x1=3,y1=4;
		m.invoke(null,1,2,3,4,w,h,src,dst);

		GImageSingleBand a = FactoryGImageSingleBand.wrap(src);
		GImageSingleBand b = FactoryGImageSingleBand.wrap(dst);
		for( int i = 0; i < h; i++ ) {
			for( int j = 0; j < w; j++ ) {
				assertEquals(a.get(x0+j,y0+i).doubleValue(),b.get(x1+j,y1+i).doubleValue(),1e-4);
			}
		}
	}

	private void testFill( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		if( ImageSingleBand.class.isAssignableFrom(paramTypes[0])) {
			testFill_Single(m);
		} else {
			testFill_Interleaved(m);
		}
	}

	private void testFill_Single( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GImageMiscOps.fillUniform(orig, rand, 0,20);

		if( orig.getDataType().isInteger()) {
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

	private void testFill_Interleaved(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, numBands);
		GImageMiscOps.fillUniform(orig, rand, 0,20);

		if( orig.getDataType().isInteger()) {
			m.invoke(null,orig,10);
		} else {
			m.invoke(null,orig,10.0f);
		}

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for( int band = 0; band < numBands; band++ ) {
					double value = GeneralizedImageOps.get(orig,j,i,band);
					assertEquals(10.0,value,1e-4);
				}
			}
		}
	}

	private void testFillBorder( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GImageMiscOps.fill(orig, 4);

		int r = 2;
		if( orig.getDataType().isInteger()) {
			m.invoke(null,orig,5,r);
		} else {
			m.invoke(null,orig,5,r);
		}

		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				if( j < r || i < r || j >= width-r || i >= height-r )
					assertEquals(i+" "+j,5,a.get(j,i).doubleValue(),1e-4);
				else
					assertEquals(4,a.get(j,i).doubleValue(),1e-4);
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

		if( orig.getDataType().isInteger() ) {
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

	private void testFillUniform( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		if( ImageSingleBand.class.isAssignableFrom(paramTypes[0])) {
			testFillUniform_Single(m);
		} else {
			testFillUniform_Interleaved(m);
		}
	}

	private void testFillUniform_Single(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( orig.getDataType().isInteger() ) {
			if( orig.getDataType().isSigned() )
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
				assertTrue("value = "+value,value>=-10 && value < 10);
				if( value == 0 )
					numZero++;
			}
		}

		assertTrue( numZero < width*height );
	}

	private void testFillUniform_Interleaved(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, numBands);

		if( orig.getDataType().isInteger() ) {
			if( orig.getDataType().isSigned() )
				m.invoke(null,orig,rand,-10,10);
			else {
				m.invoke(null,orig,rand,1,10);
			}
		} else {
			m.invoke(null,orig,rand,-10,10);
		}

		int numZero = 0;

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for( int band = 0; band < numBands; band++ ) {
					double value = GeneralizedImageOps.get(orig,j,i,band);
					assertTrue("value = "+value,value>=-10 && value < 10);
					if( value == 0 )
						numZero++;
				}
			}
		}

		assertTrue( numZero < width*height*numBands );
	}

	private void testFillGaussian(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( orig.getDataType().isSigned() )
			m.invoke(null,orig,rand,0,5,-2,2);
		else {
			m.invoke(null,orig,rand,5,7,0,12);
		}

		int numZero = 0;

		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();

				if( orig.getDataType().isSigned() ) {
					assertTrue("value = "+value,value>=-2 && value <= 2);
				} else {
					assertTrue("value = "+value,value>=0 && value <= 12);
				}

				if( value == 0 )
					numZero++;
			}
		}

		assertTrue( numZero < width*height );
	}

	private void testAddUniform( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GImageMiscOps.fill(orig,1);

		if( orig.getDataType().isInteger() ) {
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

		GImageMiscOps.fill(orig,mean);
		m.invoke(null,orig,rand,2.0,0,255);

		double stdev2 = 0;
		GImageSingleBand a = FactoryGImageSingleBand.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				stdev2 += (value-mean)*(value-mean);
			}
		}

		GImageMiscOps.fill(orig,mean);
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

	private void testFlipVertical(Method m) throws InvocationTargetException, IllegalAccessException {

		// test with an even and odd height
		testFlipVertical(m,height);
		testFlipVertical(m,height+1);
	}

	private void testFlipVertical(Method m, int height) throws IllegalAccessException, InvocationTargetException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand imgA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageMiscOps.fillUniform(imgA,rand,0,100);
		ImageSingleBand imgB = (ImageSingleBand)imgA.clone();

		m.invoke(null,imgB);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double valA = GeneralizedImageOps.get(imgA,x,height-y-1);
				double valB = GeneralizedImageOps.get(imgB,x,y);
				assertTrue(valA==valB);
			}
		}
	}

	private void testFlipHorizontal(Method m) throws InvocationTargetException, IllegalAccessException {

		// test with an even and odd height
		testFlipHorizontal(m, width);
		testFlipHorizontal(m, width + 1);
	}

	private void testFlipHorizontal(Method m, int width) throws IllegalAccessException, InvocationTargetException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand imgA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageMiscOps.fillUniform(imgA,rand,0,100);
		ImageSingleBand imgB = (ImageSingleBand)imgA.clone();

		m.invoke(null,imgB);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double valA = GeneralizedImageOps.get(imgA,width-x-1,y);
				double valB = GeneralizedImageOps.get(imgB,x,y);
				assertTrue(valA==valB);
			}
		}
	}

	public void testRotateCW( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand a = GeneralizedImageOps.createSingleBand(paramTypes[0], 3, 2);
		ImageSingleBand b = GeneralizedImageOps.createSingleBand(paramTypes[0], 2, 3);

		for (int i = 0; i < 6; i++) {
			GeneralizedImageOps.set(a,i%3,i/3,i);
		}

		m.invoke(null,a,b);

		assertEquals(3,GeneralizedImageOps.get(b,0,0),1e-8);
		assertEquals(0,GeneralizedImageOps.get(b,1,0),1e-8);
		assertEquals(4,GeneralizedImageOps.get(b,0,1),1e-8);
		assertEquals(1,GeneralizedImageOps.get(b,1,1),1e-8);
		assertEquals(5,GeneralizedImageOps.get(b,0,2),1e-8);
		assertEquals(2,GeneralizedImageOps.get(b,1,2),1e-8);
	}

	public void testRotateCCW( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageSingleBand a = GeneralizedImageOps.createSingleBand(paramTypes[0], 3, 2);
		ImageSingleBand b = GeneralizedImageOps.createSingleBand(paramTypes[0], 2, 3);

		for (int i = 0; i < 6; i++) {
			GeneralizedImageOps.set(a,i%3,i/3,i);
		}

		m.invoke(null,a,b);

		assertEquals(2,GeneralizedImageOps.get(b,0,0),1e-8);
		assertEquals(5,GeneralizedImageOps.get(b,1,0),1e-8);
		assertEquals(1,GeneralizedImageOps.get(b,0,1),1e-8);
		assertEquals(4,GeneralizedImageOps.get(b,1,1),1e-8);
		assertEquals(0,GeneralizedImageOps.get(b,0,2),1e-8);
		assertEquals(3,GeneralizedImageOps.get(b,1,2),1e-8);
	}
}
