/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.impl.ThresholdSauvola;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.ConvertImage;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;
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
	static ImageFloat32 inputF32 = new ImageFloat32(imgWidth, imgHeight);
	static ImageSInt32 output_S32 = new ImageSInt32(imgWidth, imgHeight);
	static ImageUInt8 output_U8 = new ImageUInt8(imgWidth, imgHeight);
	static ImageUInt8 work = new ImageUInt8(imgWidth, imgHeight);
	static ImageUInt8 work2 = new ImageUInt8(imgWidth, imgHeight);

	static int threshLower = 20;
	static int threshUpper = 30;

	static int adaptiveRadius = 6;

	public BenchmarkThresholding() {
		Random rand = new Random(234);
		ImageMiscOps.fillUniform(input, rand, 0, 100);
		ConvertImage.convert(input,inputF32);
	}

	public static class Threshold extends PerformerBase {
		@Override
		public void process() {
			ThresholdImageOps.threshold(input, output_U8, threshLower, true);
		}
	}

	public static class AdaptiveSquare extends PerformerBase {
		@Override
		public void process() {
			ThresholdImageOps.adaptiveSquare(input, output_U8, adaptiveRadius, 0, true, work, work2);
		}
	}

	public static class AdaptiveGaussian extends PerformerBase {
		@Override
		public void process() {
			ThresholdImageOps.adaptiveGaussian(input, output_U8, adaptiveRadius,0, true,work,work2);
		}
	}

	public static class AdaptiveSauvola extends PerformerBase {
		@Override
		public void process() {
			GThresholdImageOps.adaptiveSauvola(input, output_U8, adaptiveRadius, 0.3f, true);
		}
	}

	public static class AdaptiveSauvola2 extends PerformerBase {
		ThresholdSauvola alg = new ThresholdSauvola(adaptiveRadius,0.3f, true);
		@Override
		public void process() {
			alg.process(inputF32,output_U8);
		}
	}

	public static void main(String args[]) {

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Threshold(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new AdaptiveSquare(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new AdaptiveGaussian(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new AdaptiveSauvola(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new AdaptiveSauvola2(), TEST_TIME);
	}
}
