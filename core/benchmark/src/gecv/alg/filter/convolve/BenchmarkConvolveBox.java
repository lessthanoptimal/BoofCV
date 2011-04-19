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
import gecv.alg.filter.convolve.impl.*;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkConvolveBox {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int radius;
	static long TEST_TIME = 1000;

	static Kernel1D_F32 kernelF32;
	static ImageFloat32 imgFloat32;
	static ImageFloat32 out_F32;
	static Kernel1D_I32 kernelI32;
	static ImageInt8 imgInt8;
	static ImageInt16 imgInt16;
	static ImageInt8 out_I8;
	static ImageInt16 out_I16;
	static ImageInt32 out_I32;

	public static class Convolve_Vertical_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImage.vertical(kernelI32,imgInt8,out_I16,false);
		}
	}

	public static class Convolve_Vertical_I8_I32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImage.vertical(kernelI32,imgInt8,out_I32,false);
		}
	}

	public static class Box_I8_I16_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBox_I8_I16.vertical(imgInt8,out_I16,radius,false);
		}
	}

	public static class Box_I8_I32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBox_I8_I32.vertical(imgInt8,out_I32,radius,false);
		}
	}

	public static class Box_F32_F32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBox_F32_F32.vertical(imgFloat32,out_F32,radius,false);
		}
	}

	public static class Box_I8_I16_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBox_I8_I16.horizontal(imgInt8,out_I16,radius,false);
		}
	}

	public static class Box_I8_I32_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBox_I8_I32.horizontal(imgInt8,out_I32,radius,false);
		}
	}

	public static class Box_F32_F32_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBox_F32_F32.horizontal(imgFloat32,out_F32,radius,false);
		}
	}

	public static void main( String args[] ) {
		imgInt8 = new ImageInt8(imgWidth,imgHeight);
		imgInt16 = new ImageInt16(imgWidth,imgHeight);
		out_I32 = new ImageInt32(imgWidth,imgHeight);
		out_I16 = new ImageInt16(imgWidth,imgHeight);
		out_I8 = new ImageInt8(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);
		out_F32 = new ImageFloat32(imgWidth,imgHeight);


		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		for( int radius = 1; radius < 10; radius += 1 ) {
			System.out.println("Radius: "+radius);
			System.out.println();
			BenchmarkConvolveBox.radius = radius;
			kernelF32 = KernelFactory.table1D_F32(radius,true);
			kernelI32 = KernelFactory.table1D_I32(radius);
			

			ProfileOperation.printOpsPerSec(new Box_I8_I16_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_I8_I32_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_F32_F32_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_I8_I16_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_I8_I32_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_F32_F32_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Vertical_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Vertical_I8_I32(),TEST_TIME);

		}


	}
}
