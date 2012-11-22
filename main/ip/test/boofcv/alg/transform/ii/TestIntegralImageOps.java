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

package boofcv.alg.transform.ii;

import boofcv.alg.filter.convolve.ConvolveWithBorder;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.ImageRectangle;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestIntegralImageOps {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;


	@Test
	public void transform() {
		int numFound = BoofTesting.findMethodThenCall(this,"transform",IntegralImageOps.class,"transform");
		Assert.assertEquals(3, numFound);
	}

	public void transform( Method m ) {
		Class paramType[] = m.getParameterTypes();
		Class inputType = paramType[0];
		Class outputType = paramType[1];

		ImageSingleBand input = GeneralizedImageOps.createSingleBand(inputType, width, height);
		ImageSingleBand integral = GeneralizedImageOps.createSingleBand(outputType, width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		BoofTesting.checkSubImage(this,"checkTransformResults",true,m,input,integral);
	}

	public void checkTransformResults(Method m , ImageSingleBand a, ImageSingleBand b) throws InvocationTargetException, IllegalAccessException {

		m.invoke(null,a,b);

		GImageSingleBand aa = FactoryGImageSingleBand.wrap(a);
		GImageSingleBand bb = FactoryGImageSingleBand.wrap(b);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double total = 0;

				for( int i = 0; i <= y; i++ ) {
					for( int j = 0; j <= x; j++ ) {
						total += aa.get(j,i).doubleValue();
					}
				}

				Assert.assertEquals(x+" "+y,total,bb.get(x,y).doubleValue(),1e-1);
			}
		}
	}

	@Test
	public void convolve() {
		int numFound = BoofTesting.findMethodThenCall(this,"convolve",IntegralImageOps.class,"convolve");
		assertEquals(2,numFound);
	}

	public void convolve( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramType[] = m.getParameterTypes();
		Class inputType = paramType[0];
		Class outputType = paramType[2];
		Class origType = GeneralizedImageOps.isFloatingPoint(inputType) ? ImageFloat32.class : ImageUInt8.class;

		ImageSingleBand input = GeneralizedImageOps.createSingleBand(origType, width, height);
		ImageSingleBand integral = GeneralizedImageOps.createSingleBand(outputType, width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 10);
		GIntegralImageOps.transform(input,integral);

		ImageSingleBand expected = GeneralizedImageOps.createSingleBand(outputType, width, height);
		ImageSingleBand found = GeneralizedImageOps.createSingleBand(outputType, width, height);

		if( paramType[0] == ImageFloat32.class ) {
			Kernel2D_F32 kernel = new Kernel2D_F32(3, new float[]{1,1,1,2,2,2,1,1,1});
			ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value((ImageFloat32) input, 0);
			ConvolveWithBorder.convolve(kernel,(ImageFloat32)input,(ImageFloat32)expected,border);
		} else {
			Kernel2D_I32 kernel = new Kernel2D_I32(3, new int[]{1,1,1,2,2,2,1,1,1});
			ImageBorder_I32 border = (ImageBorder_I32)FactoryImageBorderAlgs.value((ImageInteger) input, 0);
			ConvolveWithBorder.convolve(kernel,(ImageUInt8)input,(ImageSInt32)expected,border);
		}

		IntegralKernel kernel = new IntegralKernel(2);
		kernel.blocks[0] = new ImageRectangle(-2,-2,1,1);
		kernel.blocks[1] = new ImageRectangle(-2,-1,1,0);
		kernel.scales = new int[]{1,1};

		m.invoke(null,integral,kernel,found);

		BoofTesting.assertEqualsGeneric(expected,found,0,1e-4f);
	}


	@Test
	public void convolveBorder() {
		int numFound = BoofTesting.findMethodThenCall(this,"convolveBorder",IntegralImageOps.class,"convolveBorder");
		assertEquals(2,numFound);
	}

	public void convolveBorder( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramType[] = m.getParameterTypes();
		Class inputType = paramType[0];
		Class outputType = paramType[2];
		Class origType = GeneralizedImageOps.isFloatingPoint(inputType) ? ImageFloat32.class : ImageUInt8.class;

		ImageSingleBand input = GeneralizedImageOps.createSingleBand(origType, width, height);
		ImageSingleBand integral = GeneralizedImageOps.createSingleBand(outputType, width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 10);
		GIntegralImageOps.transform(input,integral);

		ImageSingleBand expected = GeneralizedImageOps.createSingleBand(outputType, width, height);
		ImageSingleBand found = GeneralizedImageOps.createSingleBand(outputType, width, height);

		if( paramType[0] == ImageFloat32.class ) {
			Kernel2D_F32 kernel = new Kernel2D_F32(3, new float[]{1,1,1,2,2,2,1,1,1});
			ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorderAlgs.value((ImageFloat32) input, 0);
			ConvolveWithBorder.convolve(kernel,(ImageFloat32)input,(ImageFloat32)expected,border);
		} else {
			Kernel2D_I32 kernel = new Kernel2D_I32(3, new int[]{1,1,1,2,2,2,1,1,1});
			ImageBorder_I32 border = (ImageBorder_I32)FactoryImageBorderAlgs.value((ImageInteger) input, 0);
			ConvolveWithBorder.convolve(kernel,(ImageUInt8)input,(ImageSInt32)expected,border);
		}

		IntegralKernel kernel = new IntegralKernel(2);
		kernel.blocks[0] = new ImageRectangle(-2,-2,1,1);
		kernel.blocks[1] = new ImageRectangle(-2,-1,1,0);
		kernel.scales = new int[]{1,1};

		m.invoke(null,integral,kernel,found,4,5);

		BoofTesting.assertEqualsBorder(expected,found,1e-4f,4,5);
	}

	@Test
	public void convolveSparse() {
		int numFound = BoofTesting.findMethodThenCall(this,"convolveSparse",IntegralImageOps.class,"convolveSparse");
		assertEquals(2,numFound);
	}

	public void convolveSparse( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramType[] = m.getParameterTypes();
		Class inputType = paramType[0];

		ImageSingleBand integral = GeneralizedImageOps.createSingleBand(inputType, width, height);

		GImageMiscOps.fillUniform(integral, rand, 0, 1000);

		ImageSingleBand expected = GeneralizedImageOps.createSingleBand(inputType, width, height);

		IntegralKernel kernel = new IntegralKernel(2);
		kernel.blocks[0] = new ImageRectangle(-2,-2,1,1);
		kernel.blocks[1] = new ImageRectangle(-2,-1,1,0);
		kernel.scales =  new int[]{1,2};

		GIntegralImageOps.convolve(integral,kernel,expected);

		GImageSingleBand e = FactoryGImageSingleBand.wrap(expected);

		double found0 = ((Number)m.invoke(null,integral,kernel,0,0)).doubleValue();
		double found1 = ((Number)m.invoke(null,integral,kernel,10,12)).doubleValue();
		double found2 = ((Number)m.invoke(null,integral,kernel,19,29)).doubleValue();

		assertEquals(e.get(0,0).doubleValue(),found0,1e-4f);
		assertEquals(e.get(10,12).doubleValue(),found1,1e-4f);
		assertEquals(e.get(19,29).doubleValue(),found2,1e-4f);
	}

	@Test
	public void block_unsafe() {
		int numFound = BoofTesting.findMethodThenCall(this,"block_unsafe",IntegralImageOps.class,"block_unsafe");
		assertEquals(2,numFound);
	}
	
	public void block_unsafe( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramType[] = m.getParameterTypes();
		Class inputType = paramType[0];
		Class origType = GeneralizedImageOps.isFloatingPoint(inputType) ? ImageFloat32.class : ImageUInt8.class;

		ImageSingleBand input = GeneralizedImageOps.createSingleBand(origType, width, height);
		ImageSingleBand integral = GeneralizedImageOps.createSingleBand(inputType, width, height);

		GImageMiscOps.fill(input,1);
		GIntegralImageOps.transform(input,integral);

		double found0 = ((Number)m.invoke(null,integral,4,5,8,8)).doubleValue();

		assertEquals(12,found0,1e-4f);
	}

	@Test
	public void block_zero() {
		int numFound = BoofTesting.findMethodThenCall(this,"block_zero",IntegralImageOps.class,"block_zero");
		assertEquals(2,numFound);
	}

	public void block_zero( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class paramType[] = m.getParameterTypes();
		Class inputType = paramType[0];
		Class origType = GeneralizedImageOps.isFloatingPoint(inputType) ? ImageFloat32.class : ImageUInt8.class;

		ImageSingleBand input = GeneralizedImageOps.createSingleBand(origType, width, height);
		ImageSingleBand integral = GeneralizedImageOps.createSingleBand(inputType, width, height);

		GImageMiscOps.fill(input,1);
		GIntegralImageOps.transform(input,integral);

		double found = ((Number)m.invoke(null,integral,4,5,8,8)).doubleValue();
		assertEquals(12,found,1e-4f);

		found = ((Number)m.invoke(null,integral,-1,-2,2,3)).doubleValue();
		assertEquals(12,found,1e-4f);

		found = ((Number)m.invoke(null,integral,width-2,height-3,width+1,height+3)).doubleValue();
		assertEquals(2,found,1e-4f);

		found = ((Number)m.invoke(null,integral,3,-4,-1,-1)).doubleValue();
		assertEquals(0,found,1e-4f);

		found = ((Number)m.invoke(null,integral,width+1,height+2,width+6,height+8)).doubleValue();
		assertEquals(0,found,1e-4f);
	}
	
	@Test
	public void isInBounds() {
		IntegralKernel kernel = new IntegralKernel(2);
		kernel.blocks[0] = new ImageRectangle(-1,-2,1,2);
		kernel.blocks[1] = new ImageRectangle(-3,-2,1,2);

		// obvious cases
		assertTrue(IntegralImageOps.isInBounds(30,30,kernel,100,100));
		assertFalse(IntegralImageOps.isInBounds(-10, -20, kernel, 100, 100));
		// positive border cases
		assertTrue(IntegralImageOps.isInBounds(3,30,kernel,100,100));
		assertTrue(IntegralImageOps.isInBounds(98,30,kernel,100,100));
		assertTrue(IntegralImageOps.isInBounds(30,2,kernel,100,100));
		assertTrue(IntegralImageOps.isInBounds(30,97,kernel,100,100));
		// negative border cases
		assertFalse(IntegralImageOps.isInBounds(2, 30, kernel, 100, 100));
		assertFalse(IntegralImageOps.isInBounds(99, 30, kernel, 100, 100));
		assertFalse(IntegralImageOps.isInBounds(30, 1, kernel, 100, 100));
		assertFalse(IntegralImageOps.isInBounds(30, 98, kernel, 100, 100));

	}
}
