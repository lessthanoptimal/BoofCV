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

package boofcv.abst.filter.convolve;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.alg.filter.convolve.ConvolveWithBorder;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.ImageBorder1D_F32;
import boofcv.core.image.border.ImageBorder1D_I32;
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

	int radius = 2;
	Random rand = new Random(2342);

	int width = 30;
	int height = 40;

	@Test
	public void convolve1D_F32() {
		Kernel1D_F32 kernel = FactoryKernel.random1D_F32(radius,1,6,rand);

		ConvolveInterface<ImageFloat32,ImageFloat32> conv;

		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.SKIP,true);
		conv.process(input,found);
		ConvolveImageNoBorder.horizontal(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.EXTENDED,true);
		conv.process(input,found);
		ConvolveWithBorder.horizontal(kernel,input,expected,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolve.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.NORMALIZED,true);
		conv.process(input,found);
		ConvolveNormalized.horizontal(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);
	}

	@Test
	public void convolve1D_I32() {

		Kernel1D_I32 kernel = FactoryKernel.random1D_I32(radius,1,6,rand);

		ConvolveInterface conv;

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageSInt16 found = new ImageSInt16(width,height);
		ImageSInt16 expected = new ImageSInt16(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,ImageUInt8.class,ImageInt16.class, BorderType.SKIP,true);
		conv.process(input,found);
		ConvolveImageNoBorder.horizontal(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,ImageUInt8.class, ImageInt16.class,BorderType.EXTENDED,true);
		conv.process(input,found);
		ConvolveWithBorder.horizontal(kernel,input,expected,new ImageBorder1D_I32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		ImageUInt8 found8 = new ImageUInt8(width,height);
		ImageUInt8 expected8 = new ImageUInt8(width,height);
		conv = FactoryConvolve.convolve( kernel,ImageUInt8.class, ImageInt8.class,BorderType.NORMALIZED,true);
		conv.process(input,found8);
		ConvolveNormalized.horizontal(kernel,input,expected8);
		BoofTesting.assertEquals(expected8,found8,0);
	}

	@Test
	public void convolve2D_F32() {
		Kernel2D_F32 kernel = FactoryKernel.random2D_F32(radius,1,6,rand);

		ConvolveInterface<ImageFloat32,ImageFloat32> conv;

		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.SKIP);
		conv.process(input,found);
		ConvolveImageNoBorder.convolve(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.EXTENDED);
		conv.process(input,found);
		ConvolveWithBorder.convolve(kernel,input,expected,new ImageBorder1D_F32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolve.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.NORMALIZED);
		conv.process(input,found);
		ConvolveNormalized.convolve(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,1e-4f);
	}

	@Test
	public void convolve2D_I32() {

		Kernel2D_I32 kernel = FactoryKernel.random2D_I32(radius,1,6,rand);

		ConvolveInterface conv;

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageSInt16 found = new ImageSInt16(width,height);
		ImageSInt16 expected = new ImageSInt16(width,height);

		ImageMiscOps.fillUniform(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolve.convolve( kernel,ImageUInt8.class,ImageInt16.class,BorderType.SKIP);
		conv.process(input,found);
		ConvolveImageNoBorder.convolve(kernel,input,expected);
		BoofTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
		conv = FactoryConvolve.convolve( kernel,ImageUInt8.class,ImageInt16.class,BorderType.EXTENDED);
		conv.process(input,found);
		ConvolveWithBorder.convolve(kernel,input,expected,new ImageBorder1D_I32(BorderIndex1D_Extend.class));
		BoofTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		ImageUInt8 found8 = new ImageUInt8(width,height);
		ImageUInt8 expected8 = new ImageUInt8(width,height);
		conv = FactoryConvolve.convolve( kernel,ImageUInt8.class,ImageUInt8.class,BorderType.NORMALIZED);
		conv.process(input,found8);
		ConvolveNormalized.convolve(kernel,input,expected8);
		BoofTesting.assertEquals(expected8,found8,0);
	}
}
