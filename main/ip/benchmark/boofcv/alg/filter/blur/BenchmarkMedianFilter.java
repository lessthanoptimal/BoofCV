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

package boofcv.alg.filter.blur;

import boofcv.alg.filter.blur.impl.ImplMedianHistogramInner;
import boofcv.alg.filter.blur.impl.ImplMedianHistogramInnerNaive;
import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkMedianFilter  {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static GrayF32 imgFloat32 = new GrayF32(imgWidth,imgHeight);
	static GrayF32 out_F32 = new GrayF32(imgWidth,imgHeight);
	static GrayU8 imgInt8 = new GrayU8(imgWidth,imgHeight);
	static GrayS16 imgInt16 = new GrayS16(imgWidth,imgHeight);
	static GrayU8 out_I8 = new GrayU8(imgWidth,imgHeight);
	static GrayS16 out_I16 = new GrayS16(imgWidth,imgHeight);
	static GrayS32 out_I32 = new GrayS32(imgWidth,imgHeight);

	// iterate through different sized kernel radius
	private int radius;

	public BenchmarkMedianFilter() {
		Random rand = new Random(234);
		ImageMiscOps.fillUniform(imgInt8,rand, 0, 100);
		ImageMiscOps.fillUniform(imgFloat32,rand,0,200);
	}

	public int timeBlurImageOps_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			BlurImageOps.median(imgInt8, out_I8, radius);
		return 0;
	}

	public int timeBlurImageOps_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			BlurImageOps.median(imgFloat32,out_F32,radius);
		return 0;
	}

	public int timeHistogramNaive_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplMedianHistogramInnerNaive.process(imgInt8, out_I8, radius, null, null);
		return 0;
	}

	public int timeHistogram_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplMedianHistogramInner.process(imgInt8,out_I8,radius,null,null);
		return 0;
	}

	public int timeSortNaive_I8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplMedianSortNaive.process(imgInt8,out_I8,radius,null);
		return 0;
	}

	public int timeSortNaive_F32(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplMedianSortNaive.process(imgFloat32,out_F32,radius,null);
		return 0;
	}

	public static void main( String args[] ) {
		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

//		Runner.main(BenchmarkMedianFilter.class, args);
	}
}
