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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.down.ConvolveDownNormalizedNaive;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
@SuppressWarnings({"UnusedDeclaration"})
public class BenchmarkConvolveDownNormalized {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int skip = 2;
	static long TEST_TIME = 1000;

	static Kernel2D_F32 kernel2D_F32;
	static Kernel1D_F32 kernelF32;
	static ImageFloat32 imgFloat32;
	static ImageFloat32 out_F32_D;
	static ImageFloat32 out_F32;
	static Kernel1D_I32 kernelI32;
	static Kernel2D_I32 kernel2D_I32;
	static ImageUInt8 imgInt8;
	static ImageSInt16 imgInt16;
	static ImageUInt8 out_I8;
	static ImageSInt16 out_I16;
	static ImageSInt32 out_I32;

	// iterate through different sized kernel radius
//	@Param({"1", "2", "3", "5","10"})
	private int radius;

	public BenchmarkConvolveDownNormalized() {
		int outWidth = imgWidth/skip;
		int outHeight = imgHeight/skip;

		imgInt8 = new ImageUInt8(imgWidth,imgHeight);
		imgInt16 = new ImageSInt16(imgWidth,imgHeight);
		out_I32 = new ImageSInt32(imgWidth,imgHeight);
		out_I16 = new ImageSInt16(imgWidth,imgHeight);
		out_I8 = new ImageUInt8(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);
		out_F32_D = new ImageFloat32(outWidth,outHeight);
		out_F32 = new ImageFloat32(imgWidth,imgHeight);

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

	public int timeHorizontal_Naive_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNormalizedNaive.horizontal(kernelF32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeHorizontal_NoBorder_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorder.horizontal(kernelF32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeHorizontal_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNormalized.horizontal(kernelF32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeVertical_Naive_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNormalizedNaive.vertical(kernelF32, imgFloat32, out_F32, skip);
		return 0;
	}

	public int timeVertical_NoBorder_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorder.vertical(kernelF32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeVertical_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNormalized.vertical(kernelF32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeConvolve_Naive_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNormalizedNaive.convolve(kernel2D_F32, imgFloat32, out_F32, skip);
		return 0;
	}

	public int timeConvolve_NoBorder_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNoBorder.convolve(kernel2D_F32,imgFloat32,out_F32,skip);
		return 0;
	}

	public int timeConvolve_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ConvolveDownNormalized.convolve(kernel2D_F32,imgFloat32,out_F32,skip);
		return 0;
	}

	public static void main( String args[] ) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight +" ==========");
	}
}
