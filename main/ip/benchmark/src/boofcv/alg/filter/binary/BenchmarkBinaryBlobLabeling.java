/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkBinaryBlobLabeling {

	static final long TEST_TIME = 1000;

	static int imgWidth = 640;
	static int imgHeight = 480;

	static ImageUInt8 original = new ImageUInt8(imgWidth, imgHeight);
	static ImageUInt8 input = new ImageUInt8(imgWidth, imgHeight);
	static ImageSInt32 output = new ImageSInt32(imgWidth, imgHeight);

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

		FastQueue<Point2D_I32> queuePts = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

		@Override
		public void process() {
			int numFound = BinaryImageOps.labelBlobs8(input, output);
			BinaryImageOps.labelToClusters(output,numFound,queuePts);
			System.out.println("Full8 total = "+numFound);
		}
	}

	public static class Full4 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.labelBlobs4(input, output);
		}
	}

	public static class NewAlg extends PerformerBase {

		LinearContourLabelChang2004 alg = new LinearContourLabelChang2004();

		@Override
		public void process() {
			input.setTo(original);
			alg.process(input,output);
			System.out.println("Chang total = "+alg.getContours().size);
		}
	}

	public static void main(String args[]) {
		System.out.println("=========  Profile Image Size "+ imgWidth +" x "+ imgHeight  +" ==========");

		Random rand = new Random(234);
		ImageMiscOps.fillUniform(original, rand, 0, 1);
		input.setTo(original);

//		ProfileOperation.printOpsPerSec(new Normal8(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Normal4(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Naive8(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Naive4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Full8(), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Full4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NewAlg(), TEST_TIME);

	}
}
