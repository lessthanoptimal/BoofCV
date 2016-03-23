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

package boofcv.alg.transform.ii;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;

import java.util.Random;


/**
 *
 * @author Peter Abeles
 */
public class BenchmarkIntegralImage {
	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;

	static GrayF32 input = new GrayF32(width,height);
	static GrayF32 integral = new GrayF32(width,height);

	static GrayF32 output = new GrayF32(width,height);


	public static class ComputeIntegral extends PerformerBase {
		@Override
		public void process() {
			IntegralImageOps.transform(input,integral);
		}
	}

	public static class DerivXX extends PerformerBase {

		IntegralKernel kernel = DerivativeIntegralImage.kernelDerivXX(9,null);

		@Override
		public void process() {
			DerivativeIntegralImage.derivXX(integral,output,9);
			IntegralImageOps.convolveBorder(integral,kernel,output,4,4);
		}
	}

	public static class GenericDerivXX extends PerformerBase {

		IntegralKernel kernel = DerivativeIntegralImage.kernelDerivXX(9,null);

		@Override
		public void process() {
			IntegralImageOps.convolve(integral,kernel,output);
		}
	}

	public static void main(String args[]) {

		Random rand = new Random(234);
		ImageMiscOps.fillUniform(input, rand, 0, 100);
		IntegralImageOps.transform(input,integral);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new ComputeIntegral(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new DerivXX(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new GenericDerivXX(), TEST_TIME);
	}
}
