/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkThresholding extends SimpleBenchmark {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static ImageUInt8 input = new ImageUInt8(imgWidth, imgHeight);
	static ImageSInt32 output_S32 = new ImageSInt32(imgWidth, imgHeight);
	static ImageUInt8 output_U8 = new ImageUInt8(imgWidth, imgHeight);
	static ImageUInt8 work = new ImageUInt8(imgWidth, imgHeight);

	static int threshLower = 20;
	static int threshUpper = 30;


	public BenchmarkThresholding() {
		Random rand = new Random(234);
		ImageTestingOps.randomize(input, rand, 0, 100);
	}

	public int timeThreshold(int reps) {
		for( int i = 0; i < reps; i++ )
			ThresholdImageOps.threshold(input, output_U8, threshLower, true);
		return 0;
	}

	public int timeHysteresisLabel4(int reps) {
		for( int i = 0; i < reps; i++ ) {
			ThresholdImageOps.hysteresisLabel4(input, output_S32,threshLower,threshUpper,true,work);
			BinaryImageOps.labelToBinary(output_S32,output_U8);
		}
		return 0;
	}

	public int timeHysteresisLabel8(int reps) {
		for( int i = 0; i < reps; i++ ) {
			ThresholdImageOps.hysteresisLabel8(input, output_S32,threshLower,threshUpper,true,work);
			BinaryImageOps.labelToBinary(output_S32,output_U8);
		}
		return 0;
	}

	public static void main(String args[]) {

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		Runner.main(BenchmarkThresholding.class, args);
	}
}
