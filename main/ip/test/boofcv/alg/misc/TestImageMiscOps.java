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

package boofcv.alg.misc;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageInterleaved;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.*;


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
		int numExpected = 19*6 + 4*8;
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
				} else if( m.getName().compareTo("fillBand") == 0 ) {
					testFillBand(m);
				} else if( m.getName().compareTo("insertBand") == 0 ) {
					testInsertBand(m);
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
		ImageGray src = GeneralizedImageOps.createSingleBand(paramTypes[6], width, height);
		ImageGray dst = GeneralizedImageOps.createSingleBand(paramTypes[7], width, height);

		GImageMiscOps.fillUniform(src, rand, 0,20);
		GImageMiscOps.fillUniform(dst, rand, 0,20);

		int w = 5,h=8;
		int x0=1,y0=2;
		int x1=3,y1=4;
		m.invoke(null,1,2,3,4,w,h,src,dst);

		GImageGray a = FactoryGImageGray.wrap(src);
		GImageGray b = FactoryGImageGray.wrap(dst);
		for( int i = 0; i < h; i++ ) {
			for( int j = 0; j < w; j++ ) {
				assertEquals(a.get(x0+j,y0+i).doubleValue(),b.get(x1+j,y1+i).doubleValue(),1e-4);
			}
		}
	}

	private void testFill( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		if( ImageGray.class.isAssignableFrom(paramTypes[0])) {
			testFill_Single(m);
		} else {
			if( paramTypes[1].isArray())
				testFill_Interleaved_array(m);
			else
				testFill_Interleaved(m);
		}
	}

	private void testFill_Single( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GImageMiscOps.fillUniform(orig, rand, 0, 20);

		if( orig.getDataType().isInteger()) {
			m.invoke(null,orig,10);
		} else {
			m.invoke(null,orig,10.0f);
		}

		GImageGray a = FactoryGImageGray.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				assertEquals(10.0,a.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testFill_Interleaved(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, numBands);
		GImageMiscOps.fillUniform(orig, rand, 0, 20);

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

	private void testFill_Interleaved_array(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, numBands);
		GImageMiscOps.fillUniform(orig, rand, 0,20);

		Object array = Array.newInstance(paramTypes[1].getComponentType(), numBands);
		for (int i = 0; i < numBands; i++) {
			Array.set(array, i, 2 * i + 1);
		}
		m.invoke(null, orig, array);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for( int band = 0; band < numBands; band++ ) {
					double value = GeneralizedImageOps.get(orig,j,i,band);
					assertEquals(2*band+1,value,1e-4);
				}
			}
		}
	}

	private void testFillBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, numBands);

		for (int band = 0; band < numBands; band++) {
			GImageMiscOps.fillUniform(orig, rand, 0, 20);
			if( orig.getDataType().isInteger()) {
				m.invoke(null,orig,band,10);
			} else {
				m.invoke(null,orig,band,10.0f);
			}

			int numMatch = 0;
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					for (int k = 0; k < numBands; k++) {
						double value = GeneralizedImageOps.get(orig,j,i,k);
						if( k == band ) {
							assertEquals(10.0,value,1e-4);
						} else {
							if( 10.0 == value ) numMatch++;
						}
					}
				}
			}

			assertFalse(numMatch > width * height * (numBands - 1) / 5);
		}
	}

	private void testInsertBand(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray input = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		ImageInterleaved output = GeneralizedImageOps.createInterleaved(paramTypes[2], width, height, numBands);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for (int band = 0; band < numBands; band++) {
			GImageMiscOps.fillUniform(output, rand, 0, 20);

			m.invoke(null, input, band, output);

			int numMatch = 0;
			for( int i = 0; i < height; i++ ) {
				for( int j = 0; j < width; j++ ) {
					double valueIn = GeneralizedImageOps.get(input,j,i);

					for (int k = 0; k < numBands; k++) {
						double valueOut = GeneralizedImageOps.get(output,j,i,k);
						if( k == band ) {
							assertEquals(valueIn,valueOut,1e-4);
						} else {
							if( valueIn == valueOut ) numMatch++;
						}
					}
				}
			}

			assertFalse(numMatch > width * height * (numBands - 1) / 5);
		}
	}

	private void testFillBorder( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GImageMiscOps.fill(orig, 4);

		int r = 2;
		if( orig.getDataType().isInteger()) {
			m.invoke(null,orig,5,r);
		} else {
			m.invoke(null,orig,5,r);
		}

		GImageGray a = FactoryGImageGray.wrap(orig);
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
		if( ImageGray.class.isAssignableFrom(paramTypes[0])) {
			testFillRectangle_Single(m);
		} else {
			testFillRectangle_Interleaved(m);
		}
	}

	private void testFillRectangle_Single( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		int x0 = 2;
		int y0 = 3;
		int width = 5;
		int height = 6;

		if( orig.getDataType().isInteger() ) {
			m.invoke(null,orig,10,x0,y0,width,height);
		} else {
			m.invoke(null,orig,10.0f,x0,y0,width,height);
		}

		GImageGray a = FactoryGImageGray.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				if( j < x0 || i < y0 || i >= (x0+width) || j >= (y0+height ))
					assertEquals(j+" "+i,0.0,a.get(j,i).doubleValue(),1e-4);
				else
					assertEquals(10.0,a.get(j,i).doubleValue(),1e-4);
			}
		}
	}

	private void testFillRectangle_Interleaved( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, 2);

		int x0 = 2;
		int y0 = 3;
		int width = 5;
		int height = 6;

		Class numParam = paramTypes[1];
		if( numParam == byte.class ) {
			m.invoke(null, orig, (byte) 10, x0, y0, width, height);
		} else if( numParam == short.class ) {
			m.invoke(null,orig,(short)10,x0,y0,width,height);
		} else if( numParam == int.class ) {
			m.invoke(null,orig,10,x0,y0,width,height);
		} else if( numParam == long.class ) {
			m.invoke(null,orig,(long)10,x0,y0,width,height);
		} else {
			m.invoke(null,orig,10.0f,x0,y0,width,height);
		}

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int band = 0; band < orig.numBands; band++) {
					double value = GeneralizedImageOps.get(orig,j,i,band);

					if( j < x0 || i < y0 || i >= (x0+width) || j >= (y0+height ))
						assertEquals(j+" "+i,0.0,value,1e-4);
					else
						assertEquals(10.0,value,1e-4);
				}
			}
		}
	}

	private void testFillUniform( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		if( ImageGray.class.isAssignableFrom(paramTypes[0])) {
			testFillUniform_Single(m);
		} else {
			testFillUniform_Interleaved(m);
		}
	}

	private void testFillUniform_Single(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

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

		GImageGray a = FactoryGImageGray.wrap(orig);
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

	private void testFillGaussian( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		if( ImageGray.class.isAssignableFrom(paramTypes[0])) {
			testFillGaussian_Single(m);
		} else {
			testFillGaussian_Interleaved(m);
		}
	}

	private void testFillGaussian_Single(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		if( orig.getDataType().isSigned() )
			m.invoke(null,orig,rand,0,5,-2,2);
		else {
			m.invoke(null,orig,rand,5,7,0,12);
		}

		int numZero = 0;

		GImageGray a = FactoryGImageGray.wrap(orig);
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

	private void testFillGaussian_Interleaved(Method m) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, 2);

		if( orig.getDataType().isSigned() )
			m.invoke(null,orig,rand,0,5,-2,2);
		else {
			m.invoke(null,orig,rand,5,7,0,12);
		}

		int numZero = 0;

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int band = 0; band < orig.numBands; band++) {
					double value = GeneralizedImageOps.get(orig,j,i,band);

					if( orig.getDataType().isSigned() ) {
						assertTrue("value = "+value,value>=-2 && value <= 2);
					} else {
						assertTrue("value = "+value,value>=0 && value <= 12);
					}

					if( value == 0 )
						numZero++;
				}
			}
		}

		assertTrue( numZero < width*height*2 );
	}

	private void testAddUniform( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		if( ImageGray.class.isAssignableFrom(paramTypes[0])) {
			testAddUniform_Single(m);
		} else {
			testAddUniform_Interleaved(m);
		}
	}

	private void testAddUniform_Single( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);
		GImageMiscOps.fill(orig, 1);

		if( orig.getDataType().isInteger() ) {
			m.invoke(null,orig,rand,1,10);
		} else {
			m.invoke(null,orig,rand,1,10);
		}

		GImageGray a = FactoryGImageGray.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				assertTrue(value>=-2 && value <= 11);
			}
		}
	}

	private void testAddUniform_Interleaved( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, 2);
		GImageMiscOps.fill(orig, 1);

		if( orig.getDataType().isInteger() ) {
			m.invoke(null,orig,rand,1,10);
		} else {
			m.invoke(null,orig,rand,1,10);
		}

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int band = 0; band < orig.numBands; band++) {
					double value = GeneralizedImageOps.get(orig,j,i,band);
					assertTrue(value>=-2 && value <= 11);
				}
			}
		}
	}

	private void testAddGaussian( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		if( ImageGray.class.isAssignableFrom(paramTypes[0])) {
			testAddGaussian_Single(m);
		} else {
			testAddGaussian_Interleaved(m);
		}
	}

	private void testAddGaussian_Single( Method m ) throws InvocationTargetException, IllegalAccessException {

		double mean = 10;

		Class paramTypes[] = m.getParameterTypes();
		ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageMiscOps.fill(orig,mean);
		m.invoke(null,orig,rand,2.0,0,255);

		double stdev2 = 0;
		GImageGray a = FactoryGImageGray.wrap(orig);
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				double value = a.get(j,i).doubleValue();
				stdev2 += (value-mean)*(value-mean);
			}
		}

		GImageMiscOps.fill(orig,mean);
		m.invoke(null, orig, rand, 10.0, 0, 255);

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

	private void testAddGaussian_Interleaved( Method m ) throws InvocationTargetException, IllegalAccessException {

		double mean = 10;

		Class paramTypes[] = m.getParameterTypes();
		ImageInterleaved orig = GeneralizedImageOps.createInterleaved(paramTypes[0], width, height, 2);

		GImageMiscOps.fill(orig,mean);
		m.invoke(null,orig,rand,2.0,0,255);

		double stdev2 = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int band = 0; band < orig.numBands; band++) {
					double value = GeneralizedImageOps.get(orig,j,i,band);
					stdev2 += (value-mean)*(value-mean);
				}
			}
		}

		GImageMiscOps.fill(orig,mean);
		m.invoke(null, orig, rand, 10.0, 0, 255);

		double stdev10 = 0;
		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				for (int band = 0; band < orig.numBands; band++) {
					double value = GeneralizedImageOps.get(orig, j, i, band);
					stdev10 += (value - mean) * (value - mean);
				}
			}
		}

		// see if the gaussian with the larger variance creates a noisier image
		assertTrue(stdev2<stdev10);
	}

	private void testFlipVertical(Method m) throws InvocationTargetException, IllegalAccessException {

		// test with an even and odd height
		testFlipVertical(m, height);
		testFlipVertical(m, height + 1);
	}

	private void testFlipVertical(Method m, int height) throws IllegalAccessException, InvocationTargetException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray imgA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageMiscOps.fillUniform(imgA, rand, 0, 100);
		ImageGray imgB = (ImageGray)imgA.clone();

		m.invoke(null, imgB);

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
		ImageGray imgA = GeneralizedImageOps.createSingleBand(paramTypes[0], width, height);

		GImageMiscOps.fillUniform(imgA, rand, 0, 100);
		ImageGray imgB = (ImageGray)imgA.clone();

		m.invoke(null, imgB);

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
		if( paramTypes.length == 1 ) {
			testRotateCW_one(m);
		} else {
			testRotateCW_two(m);
		}
	}

	public void testRotateCW_one( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();

		// test even and odd width
		for (int i = 0; i < 2; i++) {
			int w = 6+i;
			ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], w, w);
			GImageMiscOps.fillUniform(orig, rand, 0, 10);
			ImageGray rotated = (ImageGray)orig.clone();

			m.invoke(null, rotated);

			for (int y = 0; y < w; y++) {
				for (int x = 0; x < w; x++) {
					double expected = GeneralizedImageOps.get(orig,x,y);
					double found = GeneralizedImageOps.get(rotated,w-y-1,x);

					assertEquals(x+" "+y,expected,found,1e-8);
				}
			}
		}
	}

	public void testRotateCW_two( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray a = GeneralizedImageOps.createSingleBand(paramTypes[0], 3, 2);
		ImageGray b = GeneralizedImageOps.createSingleBand(paramTypes[0], 2, 3);

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
		if( paramTypes.length == 1 ) {
			testRotateCCW_one(m);
		} else {
			testRotateCCW_two(m);
		}
	}

	public void testRotateCCW_one( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		// test even and odd width
		for (int i = 0; i < 2; i++) {
			int w = 6+i;
			ImageGray orig = GeneralizedImageOps.createSingleBand(paramTypes[0], w, w);
			GImageMiscOps.fillUniform(orig, rand, 0, 10);
			ImageGray rotated = (ImageGray)orig.clone();

			m.invoke(null,rotated);

			for (int y = 0; y < w; y++) {
				for (int x = 0; x < w; x++) {
					double expected = GeneralizedImageOps.get(orig,x,y);
					double found = GeneralizedImageOps.get(rotated,y,w-x-1);

					assertEquals(expected,found,1e-8);
				}
			}
		}
	}

	public void testRotateCCW_two( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramTypes[] = m.getParameterTypes();
		ImageGray a = GeneralizedImageOps.createSingleBand(paramTypes[0], 3, 2);
		ImageGray b = GeneralizedImageOps.createSingleBand(paramTypes[0], 2, 3);

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
