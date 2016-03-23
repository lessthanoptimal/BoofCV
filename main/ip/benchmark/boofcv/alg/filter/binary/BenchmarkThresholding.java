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

package boofcv.alg.filter.binary;

import boofcv.alg.filter.binary.impl.ThresholdSauvola;
import boofcv.alg.filter.binary.impl.ThresholdSquareBlockMinMax_F32;
import boofcv.alg.filter.binary.impl.ThresholdSquareBlockMinMax_U8;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.ConvertImage;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

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

	static GrayU8 input = new GrayU8(imgWidth, imgHeight);
	static GrayF32 inputF32 = new GrayF32(imgWidth, imgHeight);
	static GrayS32 output_S32 = new GrayS32(imgWidth, imgHeight);
	static GrayU8 output_U8 = new GrayU8(imgWidth, imgHeight);
	static GrayU8 work = new GrayU8(imgWidth, imgHeight);
	static GrayU8 work2 = new GrayU8(imgWidth, imgHeight);

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

	public static class LocalSquare extends PerformerBase {
		@Override
		public void process() {
			ThresholdImageOps.localSquare(input, output_U8, adaptiveRadius, 0, true, work, work2);
		}
	}

	public static class LocalGaussian extends PerformerBase {
		@Override
		public void process() {
			ThresholdImageOps.localGaussian(input, output_U8, adaptiveRadius, 0, true, work, work2);
		}
	}

	public static class LocalSauvola extends PerformerBase {
		@Override
		public void process() {
			GThresholdImageOps.localSauvola(input, output_U8, adaptiveRadius, 0.3f, true);
		}
	}

	public static class LocalSauvola2 extends PerformerBase {
		ThresholdSauvola alg = new ThresholdSauvola(adaptiveRadius,0.3f, true);
		@Override
		public void process() {
			alg.process(inputF32,output_U8);
		}
	}

	public static class SquareBlockMinMax_F32 extends PerformerBase {
		ThresholdSquareBlockMinMax_F32 alg = new ThresholdSquareBlockMinMax_F32(2*adaptiveRadius+1,20,0.95f,true);
		@Override
		public void process() {
			alg.process(inputF32,output_U8);
		}
	}

	public static class SquareBlockMinMax_U8 extends PerformerBase {
		ThresholdSquareBlockMinMax_U8 alg = new ThresholdSquareBlockMinMax_U8(2*adaptiveRadius+1,20,0.95,true);
		@Override
		public void process() {
			alg.process(input,output_U8);
		}
	}


	public static void main(String args[]) {

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Threshold(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new LocalSquare(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new LocalGaussian(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new LocalSauvola(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new LocalSauvola2(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new SquareBlockMinMax_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new SquareBlockMinMax_U8(), TEST_TIME);

	}
}
