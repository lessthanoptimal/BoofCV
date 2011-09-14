/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.alg.filter.convolve.noborder.ImplConvolveBox;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkConvolveBox {
	static int width = 640;
	static int height = 480;
	static int radius;
	static long TEST_TIME = 1000;
	static Random rand = new Random(234);

	static Kernel1D_I32 kernelI32;
	static Kernel1D_F32 kernelF32;
	static ImageFloat32 input_F32 = new ImageFloat32(width,height);
	static ImageFloat32 out_F32 = new ImageFloat32(width,height);
	static ImageFloat32 storageF32 = new ImageFloat32(width,height);
	static ImageUInt8 input_I8 = new ImageUInt8(width,height);
	static ImageSInt16 input_I16 = new ImageSInt16(width,height);
	static ImageUInt8 out_I8 = new ImageUInt8(width,height);
	static ImageSInt16 out_I16 = new ImageSInt16(width,height);
	static ImageSInt32 out_I32 = new ImageSInt32(width,height);

	public static class Convolve_Vertical_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.vertical(kernelI32, input_I8,out_I16,false);
		}
	}

	public static class Convolve_Vertical_I8_I32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.vertical(kernelI32, input_I8,out_I32,false);
		}
	}

	public static class Box_U8_I16_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.vertical(input_I8,out_I16,radius,false);
		}
	}

	public static class Box_U8_I32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.vertical(input_I8,out_I32,radius,false);
		}
	}

	public static class Box_S16_I16_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.vertical(input_I16,out_I16,radius,false);
		}
	}

	public static class Box_F32_F32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.vertical(input_F32,out_F32,radius,false);
		}
	}

	public static class BoxAlt_F32_F32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBoxAlt.vertical(input_F32,out_F32,radius,false);
		}
	}

	public static class Box_U8_I16_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.horizontal(input_I8,out_I16,radius,false);
		}
	}

	public static class Box_U8_I32_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.horizontal(input_I8,out_I32,radius,false);
		}
	}

	public static class Box_S16_I16_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.horizontal(input_I16,out_I16,radius,false);
		}
	}

	public static class Box_F32_F32_Horizontal extends PerformerBase
	{
		@Override
		public void process() {
			ImplConvolveBox.horizontal(input_F32,out_F32,radius,false);
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
			BenchmarkConvolveBox.radius = radius;
			kernelF32 = FactoryKernel.table1D_F32(radius,false);
			kernelI32 = FactoryKernel.table1D_I32(radius);

			ProfileOperation.printOpsPerSec(new Box_U8_I16_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_U8_I32_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_S16_I16_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_F32_F32_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new BoxAlt_F32_F32_Vertical(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_U8_I16_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_U8_I32_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_S16_I16_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_F32_F32_Horizontal(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Vertical_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve_Vertical_I8_I32(),TEST_TIME);

		}


	}
}
