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

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.alg.filter.convolve.ConvolveWithBorder;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.ImageBorder1D_F32;
import boofcv.core.image.border.ImageBorder1D_S32;
import boofcv.factory.filter.convolve.FactoryConvolve;
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
public class TestFactoryConvolve {

	int kernelWidth = 5;
	int radius = kernelWidth/2;
	Random rand = new Random(2342);

	int width = 30;
	int height = 40;

	@Test
	public void convolve1D_F32() {
		Kernel1D_F32 kernel = FactoryKernel.random1D_F32(kernelWidth,radius,1,6,rand);

		ConvolveInterface<GrayF32,GrayF32> conv;

		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.SKIP,true);
		conv.process(input,found);
		ConvolveImageNoBorder.horizontal(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.EXTENDED,true);
		conv.process(input,found);
		ConvolveWithBorder.horizontal(kernel,input,expected,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolve.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.NORMALIZED,true);
		conv.process(input,found);
		ConvolveNormalized.horizontal(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);
	}

	@Test
	public void convolve1D_I32() {

		Kernel1D_I32 kernel = FactoryKernel.random1D_I32(kernelWidth,radius,1,6,rand);

		ConvolveInterface conv;

		GrayU8 input = new GrayU8(width,height);
		GrayS16 found = new GrayS16(width,height);
		GrayS16 expected = new GrayS16(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,GrayU8.class,GrayI16.class, BorderType.SKIP,true);
		conv.process(input,found);
		ConvolveImageNoBorder.horizontal(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,GrayU8.class, GrayI16.class,BorderType.EXTENDED,true);
		conv.process(input,found);
		ConvolveWithBorder.horizontal(kernel,input,expected,new ImageBorder1D_S32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		GrayU8 found8 = new GrayU8(width,height);
		GrayU8 expected8 = new GrayU8(width,height);
		conv = FactoryConvolve.convolve( kernel,GrayU8.class, GrayI8.class,BorderType.NORMALIZED,true);
		conv.process(input,found8);
		ConvolveNormalized.horizontal(kernel,input,expected8);
		BoofTesting.assertEquals(expected8,found8,0);
	}

	@Test
	public void convolve2D_F32() {
		Kernel2D_F32 kernel = FactoryKernel.random2D_F32(kernelWidth,radius,1,6,rand);

		ConvolveInterface<GrayF32,GrayF32> conv;

		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.SKIP);
		conv.process(input,found);
		ConvolveImageNoBorder.convolve(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.EXTENDED);
		conv.process(input,found);
		ConvolveWithBorder.convolve(kernel,input,expected,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolve.convolve( kernel,GrayF32.class,GrayF32.class,BorderType.NORMALIZED);
		conv.process(input,found);
		ConvolveNormalized.convolve(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);
	}

	@Test
	public void convolve2D_I32() {

		Kernel2D_I32 kernel = FactoryKernel.random2D_I32(kernelWidth,radius,1,6,rand);

		ConvolveInterface conv;

		GrayU8 input = new GrayU8(width,height);
		GrayS16 found = new GrayS16(width,height);
		GrayS16 expected = new GrayS16(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,GrayU8.class,GrayI16.class,BorderType.SKIP);
		conv.process(input,found);
		ConvolveImageNoBorder.convolve(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,GrayU8.class,GrayI16.class,BorderType.EXTENDED);
		conv.process(input,found);
		ConvolveWithBorder.convolve(kernel,input,expected,new ImageBorder1D_S32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		GrayU8 found8 = new GrayU8(width,height);
		GrayU8 expected8 = new GrayU8(width,height);
		conv = FactoryConvolve.convolve( kernel,GrayU8.class,GrayU8.class,BorderType.NORMALIZED);
		conv.process(input,found8);
		ConvolveNormalized.convolve(kernel,input,expected8);
		BoofTesting.assertEquals(expected8,found8,0);
	}
}
