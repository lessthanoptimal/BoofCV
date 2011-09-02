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

package boofcv.alg.filter.convolve;

import boofcv.PerformerBase;
import boofcv.ProfileOperation;
import boofcv.alg.filter.convolve.noborder.*;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.ImageBorder1D_I32;
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
public class BenchmarkConvolve {
	static int width = 640;
	static int height = 480;
	static int radius;
	static long TEST_TIME = 1000;

	static Kernel2D_F32 kernel2D_F32;
	static Kernel1D_F32 kernelF32;
	static Kernel1D_I32 kernelI32;
	static Kernel2D_I32 kernel2D_I32;
	static ImageFloat32 input_F32 = new ImageFloat32(width,height);
	static ImageFloat32 out_F32 = new ImageFloat32(width,height);
	static ImageUInt8 input_I8 = new ImageUInt8(width,height);
	static ImageSInt16 input_I16 = new ImageSInt16(width,height);
	static ImageUInt8 out_I8 = new ImageUInt8(width,height);
	static ImageSInt16 out_I16 = new ImageSInt16(width,height);
	static ImageSInt32 out_I32 = new ImageSInt32(width,height);

	public static class Horizontal_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelF32, input_F32,out_F32,false);
		}
	}

	public static class Horizontal_I8_I8_div extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelI32, input_I8,out_I8,10,false);
		}
	}

	public static class HorizontalUnroll_I8_I8_div extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I8_I8_Div.horizontal(kernelI32, input_I8,out_I8,10,false) )
				throw new RuntimeException();
		}
	}

	public static class Horizontal_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelI32, input_I8,out_I16,false);
		}

	}

	public static class Horizontal_I16_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelI32, input_I8,out_I16,false);
		}
	}

	public static class Vertical_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.vertical(kernelF32, input_F32,out_F32,false);
		}
	}

	public static class Vertical_I8_I8_div extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.vertical(kernelI32, input_I8,out_I8,10,false);
		}
	}

	public static class VerticalUnrolled_I8_I8_div extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I8_I8_Div.vertical(kernelI32, input_I8,out_I8,10,false) )
				throw new RuntimeException();
		}
	}

	public static class Vertical_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.vertical(kernelI32, input_I8,out_I16,false);
		}
	}

	public static class Vertical_I16_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.vertical(kernelI32, input_I16,out_I16,false);
		}
	}

	public static class Convolve2D_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.convolve(kernel2D_F32, input_F32,out_F32);
		}
	}

	public static class Convolve2D_Std_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.convolve(kernel2D_F32, input_F32,out_F32);
		}
	}

	public static class Convolve2D_Unrolled_F32 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_F32_F32.convolve(kernel2D_F32, input_F32,out_F32) )
				throw new RuntimeException();
		}
	}

	public static class Convolve2D_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.convolve(kernel2D_I32, input_I8,out_I16);
		}
	}

	public static class Convolve2D_Extend_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveWithBorder.convolve(kernel2D_I32, input_I8,out_I16,new ImageBorder1D_I32(BorderIndex1D_Extend.class));
		}
	}

	public static class Convolve2D_Std_I8_I8_DIV extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.convolve(kernel2D_I32, input_I8,out_I8,10);
		}
	}

	public static class Convolve2D_I8_I8_DIV extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.convolve(kernel2D_I32, input_I8,out_I8,10);
		}
	}

	public static class Convolve2D_Std_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.convolve(kernel2D_I32, input_I8,out_I16);
		}
	}

	public static class HorizontalUnrolled_F32 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_F32_F32.horizontal(kernelF32, input_F32,out_F32,false) )
				throw new RuntimeException();
		}
	}

	public static class VerticalUnrolled_F32 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_F32_F32.vertical(kernelF32, input_F32,out_F32,false) )
				throw new RuntimeException();
		}
	}

	public static class HorizontalUnrolled_I8 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I8_I16.horizontal(kernelI32, input_I8,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class VerticalUnrolled_I8 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I8_I16.vertical(kernelI32, input_I8,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class HorizontalUnrolled_I16 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I16_I16.horizontal(kernelI32, input_I16,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class VerticalUnrolled_I16 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I16_I16.vertical(kernelI32, input_I16,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class Box_U8_S32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.vertical(input_I8,out_I32,radius,false);
		}
	}

	public static void main( String args[] ) {

		Random rand = new Random(234234);
		ImageTestingOps.randomize(input_I8,rand, 0, 100);
		ImageTestingOps.randomize(input_I16,rand,0,200);
		ImageTestingOps.randomize(input_F32,rand,0,200);


		System.out.println("=========  Profile Image Size "+ width +" x "+ height +" ==========");
		System.out.println();

		for( int radius = 1; radius < 10; radius += 1 ) {
			System.out.println("Radius: "+radius);
			System.out.println();
			BenchmarkConvolve.radius = radius;
			kernelF32 = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,radius);
			kernelI32 = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,radius);
			kernel2D_F32 = FactoryKernelGaussian.gaussian(Kernel2D_F32.class,-1,radius);
			kernel2D_I32 = FactoryKernelGaussian.gaussian(Kernel2D_I32.class,-1,radius);
			
			ProfileOperation.printOpsPerSec(new Horizontal_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_I8_I8_div(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_I16_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HorizontalUnrolled_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HorizontalUnrolled_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HorizontalUnroll_I8_I8_div(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HorizontalUnrolled_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_I8_I8_div(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_I16_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new VerticalUnrolled_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new VerticalUnrolled_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new VerticalUnrolled_I8_I8_div(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new VerticalUnrolled_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_U8_S32_Vertical(),TEST_TIME);

			ProfileOperation.printOpsPerSec(new Convolve2D_Std_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_Unrolled_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_Std_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_Extend_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_Std_I8_I8_DIV(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_I8_I8_DIV(),TEST_TIME);
		}


	}
}
