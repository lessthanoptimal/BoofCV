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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.*;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 *
 * @author Peter Abeles
 */
public class BenchmarkSsdCornerIntensity {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int windowRadius = 2;
	static long TEST_TIME = 1000;

	static GrayF32 intensity = new GrayF32(imgWidth,imgHeight);

	static GrayF32 derivX_F32;
	static GrayF32 derivY_F32;
	static GrayF32 derivXX_F32;
	static GrayF32 derivYY_F32;
	static GrayF32 derivXY_F32;
	static GrayS16 derivX_I16;
	static GrayS16 derivY_I16;
	static GrayS16 derivXX_I16;
	static GrayS16 derivYY_I16;
	static GrayS16 derivXY_I16;

	static Random rand = new Random(234);

	public static class KLT_F32 extends PerformerBase {
		ImplShiTomasiCorner_F32 corner = new ImplShiTomasiCorner_F32(windowRadius);

		@Override
		public void process() {
			corner.process(derivX_F32, derivY_F32,intensity);
		}
	}

	public static class KLT_WEIGHT_F32 extends PerformerBase {
		ImplShiTomasiCornerWeighted_F32 corner = new ImplShiTomasiCornerWeighted_F32(windowRadius);

		@Override
		public void process() {
			corner.process(derivX_F32, derivY_F32,intensity);
		}
	}

	public static class KLT_I16 extends PerformerBase {
		ImplShiTomasiCorner_S16 corner = new ImplShiTomasiCorner_S16(windowRadius);

		@Override
		public void process() {
			corner.process(derivX_I16, derivY_I16,intensity);
		}
	}
	public static class KLT_WEIGHT_I16 extends PerformerBase {
		ImplShiTomasiCornerWeighted_S16 corner = new ImplShiTomasiCornerWeighted_S16(windowRadius);

		@Override
		public void process() {
			corner.process(derivX_I16, derivY_I16,intensity);
		}
	}

	public static class KLT_Naive_I16 extends PerformerBase {
		ImplSsdCornerNaive corner = new ImplSsdCornerNaive(imgWidth, imgHeight, windowRadius,false);

		@Override
		public void process() {
			corner.process(derivX_I16, derivY_I16,intensity);
		}
	}

	public static class Harris_F32 extends PerformerBase {
		ImplHarrisCorner_F32 corner = new ImplHarrisCorner_F32(windowRadius, 0.04f);

		@Override
		public void process() {
			corner.process(derivX_F32, derivY_F32,intensity);
		}
	}

	public static class Harris_I16 extends PerformerBase {
		ImplHarrisCorner_S16 corner = new ImplHarrisCorner_S16( windowRadius, 0.04f);

		@Override
		public void process() {
			corner.process(derivX_I16, derivY_I16,intensity);
		}
	}

	public static class KitRos_F32 extends PerformerBase {

		@Override
		public void process() {
			KitRosCornerIntensity.process(intensity,derivX_F32, derivY_F32,derivXX_F32,derivYY_F32,derivXY_F32);
		}
	}

	public static class KitRos_I16 extends PerformerBase {
		@Override
		public void process() {
			KitRosCornerIntensity.process(intensity,derivX_I16, derivY_I16, derivXX_I16,derivYY_I16, derivXY_I16);
		}
	}


	public static void main(String args[]) {
		derivX_F32 = new GrayF32(imgWidth, imgHeight);
		derivY_F32 = new GrayF32(imgWidth, imgHeight);
		derivXX_F32 = new GrayF32(imgWidth, imgHeight);
		derivYY_F32 = new GrayF32(imgWidth, imgHeight);
		derivXY_F32 = new GrayF32(imgWidth, imgHeight);
		derivX_I16 = new GrayS16(imgWidth, imgHeight);
		derivY_I16 = new GrayS16(imgWidth, imgHeight);
		derivXX_I16 = new GrayS16(imgWidth, imgHeight);
		derivYY_I16 = new GrayS16(imgWidth, imgHeight);
		derivXY_I16 = new GrayS16(imgWidth, imgHeight);

		ImageMiscOps.fillUniform(derivX_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(derivY_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(derivXX_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(derivYY_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(derivXY_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(derivX_I16, rand, 0, 255);
		ImageMiscOps.fillUniform(derivY_I16, rand, 0, 255);
		ImageMiscOps.fillUniform(derivXX_I16, rand, 0, 255);
		ImageMiscOps.fillUniform(derivYY_I16, rand, 0, 255);
		ImageMiscOps.fillUniform(derivXY_I16, rand, 0, 255);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new KLT_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new KLT_WEIGHT_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Harris_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new KitRos_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new KLT_I16(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new KLT_WEIGHT_I16(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Harris_I16(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new KitRos_I16(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new KLT_Naive_I16(), TEST_TIME);

	}
}
