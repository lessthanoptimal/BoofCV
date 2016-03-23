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
import boofcv.alg.filter.convolve.ConvolveUnsafe_U8;
import boofcv.alg.filter.convolve.ConvolveWithBorder;
import boofcv.alg.filter.convolve.noborder.*;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.ImageBorder1D_S32;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
@SuppressWarnings({"UnusedDeclaration"})
public class BenchmarkConvolve  {
	static int width = 640;
	static int height = 480;

	Random rand = new Random(234);

	static Kernel2D_F32 kernel2D_F32;
	static Kernel1D_F32 kernelF32;
	static Kernel1D_I32 kernelI32;
	static Kernel2D_I32 kernel2D_I32;
	static GrayF32 input_F32 = new GrayF32(width,height);
	static GrayF32 out_F32 = new GrayF32(width,height);
	static GrayU8 input_U8 = new GrayU8(width,height);
	static GrayS16 input_S16 = new GrayS16(width,height);
	static GrayU8 out_U8 = new GrayU8(width,height);
	static GrayS16 out_S16 = new GrayS16(width,height);
	static GrayS32 out_S32 = new GrayS32(width,height);

	// iterate through different sized kernel radius
//	@Param({"1", "2"})
	private int radius;

	public BenchmarkConvolve() {
		ImageMiscOps.fillUniform(input_U8,rand,0,20);
		ImageMiscOps.fillUniform(input_S16,rand,0,20);
		ImageMiscOps.fillUniform(input_F32,rand,0,20);
	}

	protected void setUp() throws Exception {
		kernelF32 = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, -1, radius);
		kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,radius);
		kernel2D_F32 = FactoryKernelGaussian.gaussian(Kernel2D_F32.class,-1,radius);
		kernel2D_I32 = FactoryKernelGaussian.gaussian(Kernel2D_I32.class, -1, radius);
	}

	public int timeHorizontal_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.horizontal(kernelF32, input_F32,out_F32);
		return 0;
	}

	public int timeHorizontal_I8_I8_div2(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.horizontal(kernelI32, input_U8, out_U8, 10);
		return 0;
	}

	public int timeHorizontalUnroll_I8_I8_div(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_U8_I8_Div.horizontal(kernelI32, input_U8, out_U8,10) )
				throw new RuntimeException();
		return 0;
	}

	public int timeHorizontal_I8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.horizontal(kernelI32, input_U8, out_S16);
		return 0;
	}

	public int timeHorizontal_I16_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.horizontal(kernelI32, input_S16, out_S16);
		return 0;
	}

	public int timeVertical_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.vertical(kernelF32, input_F32, out_F32);
		return 0;
	}

	public int timeVertical_I8_I8_div(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.vertical(kernelI32, input_U8, out_U8,10);
		return 0;
	}

	public int timeVerticalUnrolled_U8_I8_div(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_U8_I8_Div.vertical(kernelI32, input_U8, out_U8,10) )
				throw new RuntimeException();
		return 0;
	}

	public int timeVertical_I8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.vertical(kernelI32, input_U8, out_S16);
		return 0;
	}

	public int timeVertical_I16_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.vertical(kernelI32, input_S16, out_S16);
		return 0;
	}

	public int timeConvolve2D_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.convolve(kernel2D_F32, input_F32, out_F32);
		return 0;
	}

	public int timeConvolve2D_Std_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.convolve(kernel2D_F32, input_F32,out_F32);
		return 0;
	}

	public int timeConvolve2D_Unrolled_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_F32_F32.convolve(kernel2D_F32, input_F32,out_F32) )
				throw new RuntimeException();
		return 0;
	}

	public int timeConvolve2D_I8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.convolve(kernel2D_I32, input_U8, out_S16);
		return 0;
	}

	public int timeConvolve2D_Extend_I8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.convolve(kernel2D_I32, input_U8, out_S16, new ImageBorder1D_S32(BorderIndex1D_Extend.class));
		return 0;
	}

	public int timeConvolve2D_Std_I8_I8_DIV(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageStandard.convolve(kernel2D_I32, input_U8, out_U8,10);
		return 0;
	}

	public int timeConvolve2D_I8_I8_DIV(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.convolve(kernel2D_I32, input_U8, out_U8,10);
		return 0;
	}

	public int timeConvolveUnsafe2D_I8_I8_DIV(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveUnsafe_U8.convolve(kernel2D_I32, input_U8, out_U8, 10);
		return 0;
	}

	public int timeConvolve2D_Std_I8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.convolve(kernel2D_I32, input_U8, out_U8,10);
		return 0;
	}

	public int timeHorizontalUnrolled_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_F32_F32.horizontal(kernelF32, input_F32,out_F32) )
				throw new RuntimeException();
		return 0;
	}

	public int timeVerticalUnrolled_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_F32_F32.vertical(kernelF32, input_F32, out_F32) )
				throw new RuntimeException();
		return 0;
	}

	public int timeHorizontalUnrolled_U8(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_U8_I16.horizontal(kernelI32, input_U8, out_S16) )
				throw new RuntimeException();
		return 0;
	}

	public int timeVerticalUnrolled_U8(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_U8_I16.vertical(kernelI32, input_U8, out_S16) )
				throw new RuntimeException();
		return 0;
	}

	public int timeHorizontalUnrolled_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_S16_I16.horizontal(kernelI32, input_S16, out_S16) )
				throw new RuntimeException();
		return 0;
	}

	public int timeVerticalUnrolled_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveImageUnrolled_S16_I16.vertical(kernelI32, input_S16, out_S16) )
				throw new RuntimeException();
		return 0;
	}

	public int timeBox_U8_S32_Vertical6(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplConvolveBox.vertical(input_U8, out_S32,radius);
		return 0;
	}

//	public static void main( String args[] ) {
//		System.out.println("=========  Profile Image Size "+ width +" x "+ height +" ==========");
//
//		Runner.main(BenchmarkConvolve.class, args);
//	}
}
