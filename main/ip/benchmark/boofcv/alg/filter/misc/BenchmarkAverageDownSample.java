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

package boofcv.alg.filter.misc;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkAverageDownSample {
	static final long TEST_TIME = 1000;
	static final Random rand = new Random(234234);
	static final int width = 640;
	static final int height = 480;

	static GrayU8 inputU8 = new GrayU8(width,height);
	static GrayS8 inputS8 = new GrayS8(width,height);
	static GrayU8 out8 = new GrayU8(1,1);

	static int square = 4;

	public static class General2 extends PerformerBase {

		@Override
		public void process() {
			ImplAverageDownSampleN.down(inputU8, 2, out8);
		}
	}

	public static class General2S extends PerformerBase {

		@Override
		public void process() {
			ImplAverageDownSampleN.down(inputS8, 2, out8);
		}
	}

	public static class GeneralN extends PerformerBase {

		@Override
		public void process() {
			ImplAverageDownSampleN.down(inputU8, square, out8);
		}
	}



	public static class Special2 extends PerformerBase {

		@Override
		public void process() {
			ImplAverageDownSample2.down(inputU8, out8);
		}
	}


	public static void main( String argsp[ ] ) {
		System.out.println("=========  "+width+"  "+height);
		System.out.println();

		ImageMiscOps.fillUniform(inputU8,rand,0,100);
		ImageMiscOps.fillUniform(inputS8,rand,-50,50);

		AverageDownSampleOps.reshapeDown(out8,width,height,2);

		ProfileOperation.printOpsPerSec(new General2(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Special2(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new General2S(), TEST_TIME);

		AverageDownSampleOps.reshapeDown(out8,width,height,square);
		ProfileOperation.printOpsPerSec(new GeneralN(), TEST_TIME);

	}
}
