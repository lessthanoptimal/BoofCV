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

package boofcv.abst.filter.convolve;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.convolve.ConvolveDownNoBorder;
import boofcv.alg.filter.convolve.ConvolveDownNormalized;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.convolve.FactoryConvolveDown;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestFactoryConvolveDown {

	int skip = 2;
	int kernelWidth = 5;
	int radius = kernelWidth/2;
	Random rand = new Random(2342);

	int width = 30;
	int height = 40;

	@Test
	public void convolve1D_F32() {
		Kernel1D_F32 kernel = FactoryKernel.random1D_F32(kernelWidth,radius,1,6,rand);

		FilterImageInterface<GrayF32,GrayF32> conv;

		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width/skip,height);
		GrayF32 expected = new GrayF32(width/skip,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.SKIP,true,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.horizontal(kernel,input,expected,skip);
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.EXTENDED,true);
//		conv.process(input,found);
//		ConvolveWithBorder.horizontal(kernel,input,expected);
//		BoofTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolveDown.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.NORMALIZED,true,skip);
		conv.process(input,found);
		ConvolveDownNormalized.horizontal(kernel,input,expected,skip);
		BoofTesting.assertEquals(expected,found,1e-4f);
	}

	@Test
	public void convolve1D_I32() {

		Kernel1D_I32 kernel = FactoryKernel.random1D_I32(kernelWidth,radius,1,6,rand);

		FilterImageInterface conv;

		GrayU8 input = new GrayU8(width,height);
		GrayS16 found = new GrayS16(width/skip,height);
		GrayS16 expected = new GrayS16(width/skip,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,GrayU8.class,GrayI16.class,BorderType.SKIP,true,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.horizontal(kernel,input,expected,skip);
		BoofTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,GrayU8.class, ImageInt16.class,BorderType.EXTENDED,true);
//		conv.process(input,found);
//		ConvolveWithBorder.horizontal(kernel,input,expected);
//		BoofTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		GrayU8 found8 = new GrayU8(width/skip,height);
		GrayU8 expected8 = new GrayU8(width/skip,height);
		conv = FactoryConvolveDown.convolve( kernel,GrayU8.class, GrayI8.class,BorderType.NORMALIZED,true,skip);
		conv.process(input,found8);
		ConvolveDownNormalized.horizontal(kernel,input,expected8,skip);
		BoofTesting.assertEquals(expected8,found8,0);
	}

	@Test
	public void convolve2D_F32() {
		Kernel2D_F32 kernel = FactoryKernel.random2D_F32(kernelWidth,radius,1,6,rand);

		FilterImageInterface<GrayF32,GrayF32> conv;

		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width/skip,height/skip);
		GrayF32 expected = new GrayF32(width/skip,height/skip);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.SKIP,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.convolve(kernel,input,expected,skip);
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.EXTENDED);
//		conv.process(input,found);
//		ConvolveWithBorder.convolve(kernel,input,expected);
//		BoofTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolveDown.convolve( kernel,GrayF32.class,GrayF32.class, BorderType.NORMALIZED,skip);
		conv.process(input,found);
		ConvolveDownNormalized.convolve(kernel,input,expected,skip);
		BoofTesting.assertEquals(expected,found,1e-4f);
	}

	@Test
	public void convolve2D_I32() {

		Kernel2D_I32 kernel = FactoryKernel.random2D_I32(kernelWidth,radius,1,6,rand);

		FilterImageInterface conv;

		GrayU8 input = new GrayU8(width,height);
		GrayS16 found = new GrayS16(width/skip,height/skip);
		GrayS16 expected = new GrayS16(width/skip,height/skip);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolveDown.convolve( kernel,GrayU8.class,GrayI16.class,BorderType.SKIP,skip);
		conv.process(input,found);
		ConvolveDownNoBorder.convolve(kernel,input,expected,skip);
		BoofTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
//		conv = FactoryConvolveDown.convolve( kernel,GrayU8.class,ImageInt16.class,BorderType.EXTENDED);
//		conv.process(input,found);
//		ConvolveWithBorder.convolve(kernel,input,expected);
//		BoofTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		GrayU8 found8 = new GrayU8(width/skip,height/skip);
		GrayU8 expected8 = new GrayU8(width/skip,height/skip);
		conv = FactoryConvolveDown.convolve( kernel,GrayU8.class,GrayU8.class,BorderType.NORMALIZED,skip);
		conv.process(input,found8);
		ConvolveDownNormalized.convolve(kernel,input,expected8,skip);
		BoofTesting.assertEquals(expected8,found8,0);
	}
}
