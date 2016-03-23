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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.feature.detect.intensity.IntegralImageFeatureIntensity;
import boofcv.alg.feature.detect.intensity.impl.ImplIntegralImageFeatureIntensity;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkFastHessianFeatureIntensity {
	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;

	static int skip = 1;
	static int size = 15;

	static Random rand = new Random(234);

	static GrayF32 original = new GrayF32(width,height);
	static GrayF32 integral = new GrayF32(width,height);
	static GrayF32 intensity = new GrayF32(width,height);

	public static class Naive extends PerformerBase {

		@Override
		public void process() {
			ImplIntegralImageFeatureIntensity.hessianNaive(integral,skip,size,intensity);
		}
	}

	public static class Standard extends PerformerBase {

		@Override
		public void process() {
			IntegralImageFeatureIntensity.hessian(integral,skip,size,intensity);
		}
	}

	public static void main(String args[]) {
		ImageMiscOps.fillUniform(original,rand,0,200);
		IntegralImageOps.transform(original,integral);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println("     skip = "+skip+" size = "+size);
		System.out.println();

		ProfileOperation.printOpsPerSec(new Naive(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Standard(), TEST_TIME);
	}
}
