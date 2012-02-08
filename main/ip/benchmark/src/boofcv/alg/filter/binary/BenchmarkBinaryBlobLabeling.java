/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.alg.filter.binary.impl.ImplBinaryBlobLabeling;
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
public class BenchmarkBinaryBlobLabeling extends SimpleBenchmark {
	static int imgWidth = 640;
	static int imgHeight = 480;

	static ImageUInt8 input = new ImageUInt8(imgWidth, imgHeight);
	static ImageSInt32 output = new ImageSInt32(imgWidth, imgHeight);

	public BenchmarkBinaryBlobLabeling() {
		Random rand = new Random(234);
		ImageTestingOps.randomize(input, rand, 0, 1);
	}

	public int timeNormal8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryBlobLabeling.quickLabelBlobs8(input, output);
		return 0;
	}

	public int timeNormal4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryBlobLabeling.quickLabelBlobs4(input, output);
		return 0;
	}

	public int timeNaive8(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryBlobLabeling.quickLabelBlobs8_Naive(input, output);
		return 0;
	}

	public int timeNaive4(int reps) {
		for( int i = 0; i < reps; i++ )
			ImplBinaryBlobLabeling.quickLabelBlobs4_Naive(input, output);
		return 0;
	}

	public int timeFull8(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.labelBlobs8(input, output);
		return 0;
	}

	public int timeFull4(int reps) {
		for( int i = 0; i < reps; i++ )
			BinaryImageOps.labelBlobs4(input, output);
		return 0;
	}

	public static void main(String args[]) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight  +" ==========");

		Runner.main(BenchmarkBinaryBlobLabeling.class, args);
	}
}
