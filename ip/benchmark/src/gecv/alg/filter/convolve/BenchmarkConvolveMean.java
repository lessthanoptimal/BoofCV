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
import gecv.abst.filter.blur.FactoryBlurFilter;
import gecv.abst.filter.blur.impl.BlurStorageFilter;
import gecv.alg.filter.blur.BlurImageOps;
import gecv.alg.filter.convolve.noborder.ImplConvolveMean;
import gecv.alg.misc.ImageTestingOps;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkConvolveMean {
	static int width = 640;
	static int height = 480;
	static int radius;
	static long TEST_TIME = 1000;
	static Random rand = new Random(234);

	static Kernel1D_F32 kernelF32;
	static ImageFloat32 input_F32 = new ImageFloat32(width,height);
	static ImageFloat32 out_F32 = new ImageFloat32(width,height);
	static ImageFloat32 storageF32 = new ImageFloat32(width,height);
	static Kernel1D_I32 kernelI32;
	static ImageUInt8 input_I8 = new ImageUInt8(width,height);
	static ImageSInt16 input_I16 = new ImageSInt16(width,height);
	static ImageUInt8 out_I8 = new ImageUInt8(width,height);
	static ImageSInt16 out_I16 = new ImageSInt16(width,height);
	static ImageSInt32 out_I32 = new ImageSInt32(width,height);

	public static class Convolve_Vertical_U8_I8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.vertical(kernelI32, input_I8,out_I8,radius*2+1,false);
		}
	}

	public static class Convolve_Horizontal_U8_I8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.horizontal(kernelI32, input_I8,out_I8,radius*2+1,false);
		}
	}

	public static class Mean_U8_I8_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.vertical(input_I8,out_I8,radius,false);
		}
	}

	public static class Mean_U8_I8_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.horizontal(input_I8,out_I8,radius,false);
		}
	}

	public static class Mean_F32_F32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.vertical(input_F32,out_F32,radius,false);
		}
	}

	public static class Mean_F32_F32_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.horizontal(input_F32,out_F32,radius,false);
		}
	}

	public static class Mean_F32_F32_Blur extends PerformerBase
	{
		@Override
		public void process() {
			BlurImageOps.mean(input_F32,out_F32,radius,storageF32);
		}
	}

	public static class Mean_F32_F32_BlurAbst extends PerformerBase
	{
		BlurStorageFilter<ImageFloat32> filter = FactoryBlurFilter.mean(ImageFloat32.class,radius);

		@Override
		public void process() {
			filter.process(input_F32,out_F32);
		}
	}


	public static void main( String args[] ) {
		ImageTestingOps.randomize(input_I8,rand,0,20);
		ImageTestingOps.randomize(input_I16,rand,0,20);
		ImageTestingOps.randomize(input_F32,rand,0,20);

		System.out.println("=========  Profile Image Size "+ width +" x "+ height +" ==========");
		System.out.println();

		for( int radius = 1; radius < 10; radius += 1 ) {
			System.out.println("Radius: "+radius);
			System.out.println();
			BenchmarkConvolveMean.radius = radius;
			kernelF32 = KernelFactory.table1D_F32(radius,true);
			kernelI32 = KernelFactory.table1D_I32(radius);

			ProfileOperation.printOpsPerSec(new Mean_U8_I8_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Mean_F32_F32_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Mean_U8_I8_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Mean_F32_F32_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Mean_F32_F32_Blur(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Mean_F32_F32_BlurAbst(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Horizontal_U8_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Vertical_U8_I8(),TEST_TIME);

		}


	}
}
