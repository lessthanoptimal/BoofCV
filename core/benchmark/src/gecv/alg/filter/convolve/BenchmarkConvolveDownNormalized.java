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

package gecv.alg.filter.convolve;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.drawing.impl.ImageInitialization_F32;
import gecv.alg.drawing.impl.ImageInitialization_I16;
import gecv.alg.drawing.impl.ImageInitialization_I8;
import gecv.alg.filter.convolve.down.ConvolveDownNormalizedNaive;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkConvolveDownNormalized {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int radius;
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

	public static class Horizontal_Naive_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNormalizedNaive.horizontal(kernelF32,imgFloat32,out_F32,skip);
		}
	}

	public static class Horizontal_NoBorder_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNoBorder.horizontal(kernelF32,imgFloat32,out_F32,skip);
		}
	}

	public static class Horizontal_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNormalized.horizontal(kernelF32,imgFloat32,out_F32,skip);
		}
	}

	public static class Vertical_Naive_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNormalizedNaive.vertical(kernelF32,imgFloat32,out_F32,skip);
		}
	}

	public static class Vertical_NoBorder_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNoBorder.vertical(kernelF32,imgFloat32,out_F32,skip);
		}
	}

	public static class Vertical_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNormalized.vertical(kernelF32,imgFloat32,out_F32,skip);
		}
	}

	public static class Convolve_Naive_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNormalizedNaive.convolve(kernel2D_F32,imgFloat32,out_F32,skip);
		}
	}

	public static class Convolve_NoBorder_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNoBorder.convolve(kernel2D_F32,imgFloat32,out_F32,skip);
		}
	}

	public static class Convolve_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveDownNormalized.convolve(kernel2D_F32,imgFloat32,out_F32,skip);
		}
	}

	public static void main( String args[] ) {

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
		ImageInitialization_I8.randomize(imgInt8,rand);
		ImageInitialization_I16.randomize(imgInt16,rand,0,200);
		ImageInitialization_F32.randomize(imgFloat32,rand,0,200);


		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		for( int radius = 1; radius < 10; radius += 1 ) {
			System.out.println("Radius: "+radius);
			System.out.println();
			BenchmarkConvolveDownNormalized.radius = radius;
			kernelF32 = KernelFactory.gaussian1D_F32(radius,true);
			kernelI32 = KernelFactory.gaussian1D_I32(radius);
			kernel2D_F32 = KernelFactory.gaussian2D_F32(1.0,radius,true);
			kernel2D_I32 = KernelFactory.gaussian2D_I32(1.0,radius);
			
			ProfileOperation.printOpsPerSec(new Horizontal_Naive_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_NoBorder_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_Naive_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_NoBorder_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Naive_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_NoBorder_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_F32(),TEST_TIME);
		}
	}
}
