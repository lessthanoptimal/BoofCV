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

package boofcv.alg.filter.binary;

import boofcv.PerformerBase;
import boofcv.ProfileOperation;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkThresholding {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static ImageUInt8 input = new ImageUInt8(imgWidth, imgHeight);
	static ImageSInt32 output_S32 = new ImageSInt32(imgWidth, imgHeight);
	static ImageUInt8 output_U8 = new ImageUInt8(imgWidth, imgHeight);
	static ImageUInt8 work = new ImageUInt8(imgWidth, imgHeight);

	static int threshLower = 20;
	static int threshUpper = 30;


	public static class Threshold extends PerformerBase {
		@Override
		public void process() {
			ThresholdImageOps.threshold(input, output_U8 ,threshLower,true);
		}
	}

	public static class HysteresisLabel4 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageHighOps.hysteresisLabel4(input, output_S32,threshLower,threshUpper,true,work);
			BinaryImageOps.labelToBinary(output_S32,output_U8);
		}
	}

	public static class HysteresisLabel8 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageHighOps.hysteresisLabel8(input, output_S32,threshLower,threshUpper,true,work);
			BinaryImageOps.labelToBinary(output_S32,output_U8);
		}
	}

	public static void main(String args[]) {
		Random rand = new Random(234);
		ImageTestingOps.randomize(input, rand, 0, 100);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Threshold(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new HysteresisLabel4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new HysteresisLabel8(), TEST_TIME);
	}
}
