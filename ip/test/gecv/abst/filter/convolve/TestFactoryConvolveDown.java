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

package gecv.abst.filter.convolve;

import gecv.abst.filter.FilterImageInterface;
import gecv.alg.filter.convolve.ConvolveDownNoBorder;
import gecv.alg.filter.convolve.ConvolveDownNormalized;
import gecv.alg.misc.ImageTestingOps;
import gecv.core.image.border.BorderType;
import gecv.factory.filter.convolve.FactoryConvolveDown;
import gecv.factory.filter.kernel.FactoryKernel;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.*;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestFactoryConvolveDown {

	int skip = 2;
	int radius = 2;
	Random rand = new Random(2342);

	int width = 30;
	int height = 40;

	@Test
	public void convolve1D_F32() {
		Kernel1D_F32 kernel = FactoryKernel.random1D_F32(radius,1,6,rand);

		FilterImageInterface<ImageFloat32,ImageFloat32> conv;

		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width/skip,height);
		ImageFloat32 expected = new ImageFloat32(width/skip,height);

		ImageTestingOps.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.SKIP,true,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.horizontal(kernel,input,expected,skip);
		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.EXTENDED,true);
//		conv.process(input,found);
//		ConvolveWithBorder.horizontal(kernel,input,expected);
//		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolveDown.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.NORMALIZED,true,skip);
		conv.process(input,found);
		ConvolveDownNormalized.horizontal(kernel,input,expected,skip);
		GecvTesting.assertEquals(expected,found,0,1e-4f);
	}

	@Test
	public void convolve1D_I32() {

		Kernel1D_I32 kernel = FactoryKernel.random1D_I32(radius,1,6,rand);

		FilterImageInterface conv;

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageSInt16 found = new ImageSInt16(width/skip,height);
		ImageSInt16 expected = new ImageSInt16(width/skip,height);

		ImageTestingOps.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,ImageUInt8.class,ImageInt16.class,BorderType.SKIP,true,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.horizontal(kernel,input,expected,skip);
		GecvTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,ImageUInt8.class, ImageInt16.class,BorderType.EXTENDED,true);
//		conv.process(input,found);
//		ConvolveWithBorder.horizontal(kernel,input,expected);
//		GecvTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		ImageUInt8 found8 = new ImageUInt8(width/skip,height);
		ImageUInt8 expected8 = new ImageUInt8(width/skip,height);
		conv = FactoryConvolveDown.convolve( kernel,ImageUInt8.class, ImageInt8.class,BorderType.NORMALIZED,true,skip);
		conv.process(input,found8);
		ConvolveDownNormalized.horizontal(kernel,input,expected8,skip);
		GecvTesting.assertEquals(expected8,found8,0);
	}

	@Test
	public void convolve2D_F32() {
		Kernel2D_F32 kernel = FactoryKernel.random2D_F32(radius,1,6,rand);

		FilterImageInterface<ImageFloat32,ImageFloat32> conv;

		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width/skip,height/skip);
		ImageFloat32 expected = new ImageFloat32(width/skip,height/skip);

		ImageTestingOps.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.SKIP,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.convolve(kernel,input,expected,skip);
		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.EXTENDED);
//		conv.process(input,found);
//		ConvolveWithBorder.convolve(kernel,input,expected);
//		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolveDown.convolve( kernel,ImageFloat32.class,ImageFloat32.class, BorderType.NORMALIZED,skip);
		conv.process(input,found);
		ConvolveDownNormalized.convolve(kernel,input,expected,skip);
		GecvTesting.assertEquals(expected,found,0,1e-4f);
	}

	@Test
	public void convolve2D_I32() {

		Kernel2D_I32 kernel = FactoryKernel.random2D_I32(radius,1,6,rand);

		FilterImageInterface conv;

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageSInt16 found = new ImageSInt16(width/skip,height/skip);
		ImageSInt16 expected = new ImageSInt16(width/skip,height/skip);

		ImageTestingOps.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,ImageUInt8.class,ImageInt16.class,BorderType.SKIP,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.convolve(kernel,input,expected,skip);
		GecvTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,ImageUInt8.class,ImageInt16.class,BorderType.EXTENDED);
//		conv.process(input,found);
//		ConvolveWithBorder.convolve(kernel,input,expected);
//		GecvTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		ImageUInt8 found8 = new ImageUInt8(width/skip,height/skip);
		ImageUInt8 expected8 = new ImageUInt8(width/skip,height/skip);
		conv = FactoryConvolveDown.convolve( kernel,ImageUInt8.class,ImageUInt8.class,BorderType.NORMALIZED,skip);
		conv.process(input,found8);
		ConvolveDownNormalized.convolve(kernel,input,expected8,skip);
		GecvTesting.assertEquals(expected8,found8,0);
	}
}
