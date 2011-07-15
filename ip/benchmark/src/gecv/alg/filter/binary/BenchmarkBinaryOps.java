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

package gecv.alg.filter.binary;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.filter.binary.impl.ImplBinaryInnerOps;
import gecv.alg.filter.binary.impl.ImplBinaryNaiveOps;
import gecv.alg.misc.ImageTestingOps;
import gecv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkBinaryOps {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static ImageUInt8 input;
	static ImageUInt8 output;

	public static class NaiveErode4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryNaiveOps.erode4(input, output);
		}
	}

	public static class NaiveErode8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryNaiveOps.erode8(input, output);
		}
	}

	public static class NaiveDilate4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryNaiveOps.dilate4(input, output);
		}
	}

	public static class NaiveDilate8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryNaiveOps.dilate8(input, output);
		}
	}

	public static class NaiveEdge4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryNaiveOps.edge4(input, output);
		}
	}

	public static class NaiveEdge8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryNaiveOps.edge8(input, output);
		}
	}

	public static class NaiveRemovePointNoise extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryNaiveOps.removePointNoise(input, output);
		}
	}

	public static class InnerErode4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryInnerOps.erode4(input, output);
		}
	}

	public static class InnerErode8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryInnerOps.erode8(input, output);
		}
	}

	public static class InnerDilate4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryInnerOps.dilate4(input, output);
		}
	}

	public static class InnerDilate8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryInnerOps.dilate8(input, output);
		}
	}

	public static class InnerEdge4 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryInnerOps.edge4(input, output);
		}
	}

	public static class InnerEdge8 extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryInnerOps.edge8(input, output);
		}
	}

	public static class InnerRemovePointNoise extends PerformerBase {
		@Override
		public void process() {
			ImplBinaryInnerOps.edge8(input, output);
		}
	}

	public static class Erode4 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.erode4(input, output);
		}
	}

	public static class Dilate4 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.dilate4(input, output);
		}
	}

	public static class Edge4 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.edge4(input, output);
		}
	}

	public static class Erode8 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.erode8(input, output);
		}
	}

	public static class Dilate8 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.dilate8(input, output);
		}
	}

	public static class Edge8 extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.edge8(input, output);
		}
	}

	public static class RemovePointNoise extends PerformerBase {
		@Override
		public void process() {
			BinaryImageOps.removePointNoise(input, output);
		}
	}

	public static void main(String args[]) {
		input = new ImageUInt8(imgWidth, imgHeight);
		output = new ImageUInt8(imgWidth, imgHeight);
		Random rand = new Random(234);
		ImageTestingOps.randomize(input, rand, 0, 1);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Erode4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NaiveErode4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new InnerErode4(), TEST_TIME);
		System.out.println();
		ProfileOperation.printOpsPerSec(new Erode8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NaiveErode8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new InnerErode8(), TEST_TIME);
		System.out.println();
		ProfileOperation.printOpsPerSec(new Dilate4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NaiveDilate4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new InnerDilate4(), TEST_TIME);
		System.out.println();
		ProfileOperation.printOpsPerSec(new Dilate8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NaiveDilate8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new InnerDilate8(), TEST_TIME);
		System.out.println();
		ProfileOperation.printOpsPerSec(new Edge4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NaiveEdge4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new InnerEdge4(), TEST_TIME);
		System.out.println();
		ProfileOperation.printOpsPerSec(new Edge8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NaiveEdge8(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new InnerEdge8(), TEST_TIME);
		System.out.println();
		ProfileOperation.printOpsPerSec(new RemovePointNoise(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new NaiveRemovePointNoise(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new InnerRemovePointNoise(), TEST_TIME);

	}
}
