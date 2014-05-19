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

package boofcv.abst.filter.convolve;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.convolve.FactoryConvolve;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkConvolveAbstracted {
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

	public static class Convolve_Vertical_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageNoBorder.vertical(kernelI32,imgInt8,out_I16);
		}
	}

	public static class Abstracted_Vertical_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			FilterImageInterface<ImageUInt8,ImageSInt16> filter =
			FactoryConvolve.convolve(kernelI32,ImageUInt8.class,ImageSInt16.class, BorderType.SKIP,false);
			filter.process(imgInt8,out_I16);
		}
	}

	public static class Pre_Vertical_I8_I16 extends PerformerBase
	{
		FilterImageInterface<ImageUInt8,ImageSInt16> filter =
					FactoryConvolve.convolve(kernelI32,ImageUInt8.class,ImageSInt16.class,BorderType.SKIP,false);

		@Override
		public void process() {
			filter.process(imgInt8,out_I16);
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
			BenchmarkConvolveAbstracted.radius = radius;
			kernelF32 = FactoryKernel.table1D_F32(radius,true);
			kernelI32 = FactoryKernel.table1D_I32(radius);
			

			ProfileOperation.printOpsPerSec(new Convolve_Vertical_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Abstracted_Vertical_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Pre_Vertical_I8_I16(),TEST_TIME);
		}


	}
}
