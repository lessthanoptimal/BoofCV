/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner12_U8;
import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner9_U8;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkFastIntensity<T extends ImageGray<T>> {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	T input;
	GrayF32 intensity;

	public BenchmarkFastIntensity(Class<T> imageType) {
		input = GeneralizedImageOps.createSingleBand(imageType,imgWidth,imgHeight);
		intensity = new GrayF32(input.width,input.height);

		Random rand = new Random(234);
		GImageMiscOps.fillUniform(input, rand, 0, 255);
	}

	public class FAST_NAIVE_9 extends PerformerBase {
		DetectorFastNaive corner = new DetectorFastNaive(3,9,60);

		@Override
		public void process() {
			corner.process((GrayU8)input);
		}
	}

	public class FAST9 extends PerformerBase {
		FastCornerDetector<GrayU8> corner = new FastCornerDetector<>(new ImplFastCorner9_U8(60));

		@Override
		public void process() {
			corner.process((GrayU8)input,intensity);
		}
	}

	public class FAST12 extends PerformerBase {
		FastCornerDetector<GrayU8> corner = new FastCornerDetector<>(new ImplFastCorner12_U8(60));

		@Override
		public void process() {
			corner.process((GrayU8)input,intensity);
		}
	}

	public void evaluate() {
		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new FAST_NAIVE_9(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new FAST9(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new FAST12(), TEST_TIME);

	}

	public static void main( String args[] ) {
		BenchmarkFastIntensity benchmark = new BenchmarkFastIntensity(GrayU8.class);

		benchmark.evaluate();
	}
}
