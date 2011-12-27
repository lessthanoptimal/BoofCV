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

import boofcv.alg.filter.binary.impl.ImplBinaryBlobLabeling;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkBinaryBlobLabeling {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static ImageUInt8 input;
	static ImageSInt32 output;


	public static class Normal8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryBlobLabeling.quickLabelBlobs8(input, output);
		}
	}

	public static class Normal4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryBlobLabeling.quickLabelBlobs4(input, output);
		}
	}

	public static class Naive8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryBlobLabeling.quickLabelBlobs8_Naive(input, output);
		}
	}

	public static class Naive4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryBlobLabeling.quickLabelBlobs4_Naive(input, output);
		}
	}

	public static class Full8 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.labelBlobs8(input, output);
		}
	}

	public static class Full4 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.labelBlobs4(input, output);
		}
	}

	public static void main(String args[]) {
		input = new ImageUInt8(imgWidth, imgHeight);
		output = new ImageSInt32(imgWidth, imgHeight);
		Random rand = new Random(234);
		ImageTestingOps.randomize(input, rand, 0, 1);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Normal8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Normal4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Naive8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Naive4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Full8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Full4(), TEST_TIME);
	}
}
