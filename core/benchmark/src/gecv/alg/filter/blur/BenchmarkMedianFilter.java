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
import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.alg.filter.blur.impl.MedianHistogramNaive_I8;
import gecv.alg.filter.blur.impl.MedianHistogram_I8;
import gecv.alg.filter.blur.impl.MedianSortNaive_F32;
import gecv.alg.filter.blur.impl.MedianSortNaive_I8;
import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;

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
	static ImageInt8 imgInt8;
	static ImageInt16 imgInt16;
	static ImageInt8 out_I8;
	static ImageInt16 out_I16;
	static ImageInt32 out_I32;

	public static class HistogramNaive_I8 extends PerformerBase
	{
		MedianHistogramNaive_I8 alg = new MedianHistogramNaive_I8(radius);
		@Override
		public void process() {
			alg.process(imgInt8,out_I8);
		}
	}

	public static class Histogram_I8 extends PerformerBase
	{
		MedianHistogram_I8 alg = new MedianHistogram_I8(radius);
		@Override
		public void process() {
			alg.process(imgInt8,out_I8);
		}
	}

	public static class SortNaive_I8 extends PerformerBase
	{
		MedianSortNaive_I8 alg = new MedianSortNaive_I8(radius);
		@Override
		public void process() {
			alg.process(imgInt8,out_I8);
		}
	}

	public static class SortNaive_F32 extends PerformerBase
	{
		MedianSortNaive_F32 alg = new MedianSortNaive_F32(radius);
		@Override
		public void process() {
			alg.process(imgFloat32,out_F32);
		}
	}

	public static void main( String args[] ) {
		imgInt8 = new ImageInt8(imgWidth,imgHeight);
		imgInt16 = new ImageInt16(imgWidth,imgHeight, true);
		out_I32 = new ImageInt32(imgWidth,imgHeight);
		out_I16 = new ImageInt16(imgWidth,imgHeight, true);
		out_I8 = new ImageInt8(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);
		out_F32 = new ImageFloat32(imgWidth,imgHeight);

		Random rand = new Random(234);
		BasicDrawing_I8.randomize(imgInt8,rand);
		UtilImageFloat32.randomize(imgFloat32,rand,0,200);

		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		for( int radius = 1; radius < 10; radius += 1 ) {
			System.out.println("Radius: "+radius);
			System.out.println();
			BenchmarkMedianFilter.radius = radius;
			

			ProfileOperation.printOpsPerSec(new HistogramNaive_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Histogram_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new SortNaive_I8(),TEST_TIME);
		    ProfileOperation.printOpsPerSec(new SortNaive_F32(),TEST_TIME);
		}


	}
}
