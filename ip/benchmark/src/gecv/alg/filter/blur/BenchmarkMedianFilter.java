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

package gecv.alg.filter.blur;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.filter.blur.impl.ImplMedianHistogramInner;
import gecv.alg.filter.blur.impl.ImplMedianHistogramInnerNaive;
import gecv.alg.filter.blur.impl.ImplMedianSortNaive;
import gecv.alg.misc.ImageTestingOps;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkMedianFilter {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int radius;
	static long TEST_TIME = 1000;

	static ImageFloat32 imgFloat32;
	static ImageFloat32 out_F32;
	static ImageUInt8 imgInt8;
	static ImageSInt16 imgInt16;
	static ImageUInt8 out_I8;
	static ImageSInt16 out_I16;
	static ImageSInt32 out_I32;

	public static class BlurOps_I8 extends PerformerBase
	{
		@Override
		public void process() {
			BlurImageOps.median(imgInt8,out_I8,radius);
		}
	}

	public static class BlurOps_F32 extends PerformerBase
	{
		@Override
		public void process() {
			BlurImageOps.median(imgFloat32,out_F32,radius);
		}
	}

	public static class HistogramNaive_I8 extends PerformerBase
	{
		@Override
		public void process() {
			ImplMedianHistogramInnerNaive.process(imgInt8,out_I8,radius,null,null);
		}
	}

	public static class Histogram_I8 extends PerformerBase
	{
		@Override
		public void process() {
			ImplMedianHistogramInner.process(imgInt8,out_I8,radius,null,null);
		}
	}

	public static class SortNaive_I8 extends PerformerBase
	{
		@Override
		public void process() {
			ImplMedianSortNaive.process(imgInt8,out_I8,radius,null);
		}
	}

	public static class SortNaive_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ImplMedianSortNaive.process(imgFloat32,out_F32,radius,null);
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

		Random rand = new Random(234);
		ImageTestingOps.randomize(imgInt8,rand, 0, 100);
		ImageTestingOps.randomize(imgFloat32,rand,0,200);

		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		for( int radius = 1; radius < 10; radius += 1 ) {
			System.out.println("Radius: "+radius);
			System.out.println();
			BenchmarkMedianFilter.radius = radius;
			
			ProfileOperation.printOpsPerSec(new BlurOps_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new BlurOps_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HistogramNaive_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Histogram_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new SortNaive_I8(),TEST_TIME);
		    ProfileOperation.printOpsPerSec(new SortNaive_F32(),TEST_TIME);
		}


	}
}
