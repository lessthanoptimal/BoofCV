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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

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
	static GrayF32 imgFloat32;
	static GrayF32 out_F32;
	static Kernel1D_I32 kernelI32;
	static GrayU8 imgInt8;
	static GrayS16 imgInt16;
	static GrayU8 out_I8;
	static GrayS16 out_I16;
	static GrayS32 out_I32;

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
			FilterImageInterface<GrayU8,GrayS16> filter =
			FactoryConvolve.convolve(kernelI32,GrayU8.class,GrayS16.class, BorderType.SKIP,false);
			filter.process(imgInt8,out_I16);
		}
	}

	public static class Pre_Vertical_I8_I16 extends PerformerBase
	{
		FilterImageInterface<GrayU8,GrayS16> filter =
					FactoryConvolve.convolve(kernelI32,GrayU8.class,GrayS16.class,BorderType.SKIP,false);

		@Override
		public void process() {
			filter.process(imgInt8,out_I16);
		}
	}


	public static void main( String args[] ) {
		imgInt8 = new GrayU8(imgWidth,imgHeight);
		imgInt16 = new GrayS16(imgWidth,imgHeight);
		out_I32 = new GrayS32(imgWidth,imgHeight);
		out_I16 = new GrayS16(imgWidth,imgHeight);
		out_I8 = new GrayU8(imgWidth,imgHeight);
		imgFloat32 = new GrayF32(imgWidth,imgHeight);
		out_F32 = new GrayF32(imgWidth,imgHeight);


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
