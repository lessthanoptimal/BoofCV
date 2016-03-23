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

package boofcv.alg.transform.fft;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedF32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkFastFourierTransform {

	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;

	static GrayF32 input = new GrayF32(width,height);
	static InterleavedF32 fourier = new InterleavedF32(width,height,2);
	static GrayF32 output = new GrayF32(width,height);


	public static class ComputeFFT extends PerformerBase {

		DiscreteFourierTransform dft = DiscreteFourierTransformOps.createTransformF32();

		@Override
		public void process() {
			dft.forward(input,fourier);
			dft.inverse(fourier,output);
		}
	}

	public static void main( String args[] ) {

		Random rand = new Random(234);
		ImageMiscOps.fillUniform(input, rand, 0, 100);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new ComputeFFT(), TEST_TIME);
	}
}
