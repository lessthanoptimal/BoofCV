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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.*;
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
 * Benchmark for different convolution which renormalize along the image edges.
 *
 * @author Peter Abeles
 */
public class BenchmarkConvolveWithBorder {
	static private int imgWidth = 640;
	static private int imgHeight = 480;

	static private Kernel2D_F32 kernel2D_F32;
	static private Kernel1D_F32 kernelF32;
	static private GrayF32 imgFloat32;
	static private GrayF32 out_F32;
	static private Kernel1D_I32 kernelI32;
	static private Kernel2D_I32 kernel2D_I32;
	static private GrayU8 imgInt8;
	static private GrayS16 imgInt16;
	static private GrayU8 out_I8;
	static private GrayS16 out_I16;
	static private GrayS32 out_I32;
	static private ImageBorder_S32 border_I32 = new ImageBorder1D_S32(BorderIndex1D_Extend.class);
	static private ImageBorder_F32 border_F32 = new ImageBorder1D_F32(BorderIndex1D_Extend.class);

	// iterate through different sized kernel radius
//	@Param({"1", "2", "3", "5","10"})
	private int radius;

	public BenchmarkConvolveWithBorder() {
		imgInt8 = new GrayU8(imgWidth,imgHeight);
		imgInt16 = new GrayS16(imgWidth,imgHeight);
		out_I32 = new GrayS32(imgWidth,imgHeight);
		out_I16 = new GrayS16(imgWidth,imgHeight);
		out_I8 = new GrayU8(imgWidth,imgHeight);
		imgFloat32 = new GrayF32(imgWidth,imgHeight);
		out_F32 = new GrayF32(imgWidth,imgHeight);

		Random rand = new Random(234234);
		ImageMiscOps.fillUniform(imgInt8,rand, 0, 100);
		ImageMiscOps.fillUniform(imgInt16,rand,0,200);
		ImageMiscOps.fillUniform(imgFloat32,rand,0,200);
	}

	protected void setUp() throws Exception {
		kernelF32 = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,radius);
		kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,radius);
		kernel2D_F32 = FactoryKernelGaussian.gaussian(Kernel2D_F32.class,-1,radius);
		kernel2D_I32 = FactoryKernelGaussian.gaussian(Kernel2D_I32.class,-1,radius);
	}

	public int timeHorizontal_NoBorder_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.horizontal(kernelF32,imgFloat32,out_F32);
		return 0;
	}

	public int timeHorizontal_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.horizontal(kernelF32,imgFloat32,out_F32,border_F32);
		return 0;
	}

	public int timeHorizontal_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.horizontal(kernelI32,imgInt8,out_I16,border_I32);
		return 0;
	}

	public int timeHorizontal_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.horizontal(kernelI32,imgInt16,out_I16,border_I32);
		return 0;
	}

	public int timeVertical_NoBorder_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.vertical(kernelF32,imgFloat32,out_F32);
		return 0;
	}

	public int timeVertical_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.vertical(kernelF32,imgFloat32,out_F32,border_F32);
		return 0;
	}

	public int timeVertical_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.vertical(kernelI32,imgInt8,out_I16,border_I32);
		return 0;
	}

	public int timeVertical_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.vertical(kernelI32,imgInt16,out_I16,border_I32);
		return 0;
	}

	public int timeConvolve2D_NoBorder_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveImageNoBorder.convolve(kernel2D_F32,imgFloat32,out_F32);
		return 0;
	}

	public int timeConvolve2D_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.convolve(kernel2D_F32,imgFloat32,out_F32,border_F32);
		return 0;
	}

	public int timeConvolve2D_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.convolve(kernel2D_I32,imgInt8,out_I16,border_I32);
		return 0;
	}

	public int timeConvolve2D_I16(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveWithBorder.convolve(kernel2D_I32,imgInt16,out_I16,border_I32);
		return 0;
	}

	public static void main( String args[] ) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight +" ==========");

//		Runner.main(BenchmarkConvolveMean.class, args);
	}
}
