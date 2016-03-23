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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderStandard;
import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderUnrolled_F32_F32;
import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderUnrolled_U8_I16;
import boofcv.alg.filter.convolve.down.ConvolveDownNoBorderUnrolled_U8_I8_Div;
import boofcv.alg.misc.ImageMiscOps;
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
public class BenchmarkConvolveDown {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int skip = 2;

	static Kernel2D_F32 kernel2D_F32;
	static Kernel1D_F32 kernelF32;
	static GrayF32 imgFloat32;
	static GrayF32 out_F32_D;
	static GrayF32 out_F32;
	static Kernel1D_I32 kernelI32;
	static Kernel2D_I32 kernel2D_I32;
	static GrayU8 imgInt8;
	static GrayS16 imgInt16;
	static GrayU8 out_I8;
	static GrayS16 out_I16;
	static GrayS32 out_I32;

	// iterate through different sized kernel radius
//	@Param({"1", "2", "3", "5","10"})
	private int radius;

	public BenchmarkConvolveDown() {
		int outWidth = imgWidth/skip;
		int outHeight = imgHeight/skip;

		imgInt8 = new GrayU8(imgWidth,imgHeight);
		imgInt16 = new GrayS16(imgWidth,imgHeight);
		out_I32 = new GrayS32(imgWidth,imgHeight);
		out_I16 = new GrayS16(imgWidth,imgHeight);
		out_I8 = new GrayU8(imgWidth,imgHeight);
		imgFloat32 = new GrayF32(imgWidth,imgHeight);
		out_F32_D = new GrayF32(outWidth,outHeight);
		out_F32 = new GrayF32(imgWidth,imgHeight);

		Random rand = new Random(234234);
		ImageMiscOps.fillUniform(imgInt8,rand, 0, 100);
		ImageMiscOps.fillUniform(imgInt16,rand,0,200);
		ImageMiscOps.fillUniform(imgFloat32,rand,0,200);
	}

	protected void setUp() throws Exception {
		kernelF32 = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, -1, radius);
		kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,radius);
		kernel2D_F32 = FactoryKernelGaussian.gaussian(Kernel2D_F32.class,-1,radius);
		kernel2D_I32 = FactoryKernelGaussian.gaussian(Kernel2D_I32.class, -1, radius);
	}

	public int timeHorizontalStandard_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorderStandard.horizontal(kernelF32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeHorizontalUnrolled_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveDownNoBorderUnrolled_F32_F32.horizontal(kernelF32,imgFloat32,out_F32,skip) )
				throw new RuntimeException();
		return 0;
	}

	public int timeVerticalStandard_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorderStandard.vertical(kernelF32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeVerticalUnrolled_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveDownNoBorderUnrolled_F32_F32.vertical(kernelF32,imgFloat32,out_F32,skip) )
				throw new RuntimeException();
		return 0;
	}

	public int timeConvolve2DStandard_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorderStandard.convolve(kernel2D_F32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeConvolve2DUnrolled_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveDownNoBorderUnrolled_F32_F32.convolve(kernel2D_F32,imgFloat32,out_F32,skip) )
				throw new RuntimeException();
		return 0;
	}

	public int timeVerticalStandard_U8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorderStandard.vertical(kernelI32,imgInt8,out_I16,skip);
		return 0;
	}

	public int timeVerticalUnrolled_U8_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveDownNoBorderUnrolled_U8_I16.vertical(kernelI32,imgInt8,out_I16,skip) )
				throw new RuntimeException();
		return 0;
	}

	public int timeVerticalStandard_U8_I8_Div(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorderStandard.vertical(kernelI32,imgInt8,out_I8,skip,10);
		return 0;
	}

	public int timeVerticalUnrolled_U8_I8_Div(int reps) {
		for( int i = 0; i < reps; i++ )
			if( !ConvolveDownNoBorderUnrolled_U8_I8_Div.vertical(kernelI32,imgInt8,out_I8,skip,10) )
				throw new RuntimeException();
		return 0;
	}

	public static void main( String args[] ) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight +" ==========");

//		Runner.main(BenchmarkConvolveDown.class, args);
	}
}
