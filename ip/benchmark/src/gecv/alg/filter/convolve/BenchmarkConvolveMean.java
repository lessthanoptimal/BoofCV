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
import gecv.alg.filter.convolve.noborder.ImplConvolveMean;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkConvolveMean {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int radius;
	static long TEST_TIME = 1000;

	static Kernel1D_F32 kernelF32;
	static ImageFloat32 imgFloat32;
	static ImageFloat32 out_F32;
	static Kernel1D_I32 kernelI32;
	static ImageUInt8 imgInt8;
	static ImageSInt16 imgInt16;
	static ImageUInt8 out_I8;
	static ImageSInt16 out_I16;
	static ImageSInt32 out_I32;

	public static class Convolve_Vertical_U8_I8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.vertical(kernelI32,imgInt8,out_I8,radius*2+1,false);
		}
	}

	public static class Convolve_Horizontal_U8_I8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.horizontal(kernelI32,imgInt8,out_I8,radius*2+1,false);
		}
	}

	public static class Mean_U8_I8_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.vertical(imgInt8,out_I8,radius,false);
		}
	}

	public static class Mean_U8_I8_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.horizontal(imgInt8,out_I8,radius,false);
		}
	}

	public static class Mean_F32_F32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.vertical(imgFloat32,out_F32,radius,false);
		}
	}

	public static class Mean_F32_F32_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveMean.horizontal(imgFloat32,out_F32,radius,false);
		}
	}


	public static void main( String args[] ) {
		imgInt8 = new ImageUInt8(imgWidth,imgHeight);
		imgInt16 = new ImageSInt16(imgWidth,imgHeight);
		out_I32 = new ImageSInt32(imgWidth,imgHeight);
		out_I16 = new ImageSInt16(imgWidth,imgHeight);
		out_I8 = new ImageUInt8(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);
		out_F32 = new ImageFloat32(imgWidth,imgHeight);


		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
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
			ProfileOperation.printOpsPerSec(new Convolve_Horizontal_U8_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Vertical_U8_I8(),TEST_TIME);

		}


	}
}
