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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppression;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkEdgeNonMaxSupression {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);

	final static int width = 640;
	final static int height = 480;

	static GrayF32 intensity = new GrayF32(width,height);
	static GrayF32 output = new GrayF32(width,height);
	static GrayS8 direction4 = new GrayS8(width,height);
	static GrayS8 direction8 = new GrayS8(width,height);


	public static class Naive4_F32 extends PerformerBase {

		@Override
		public void process() {
			ImplEdgeNonMaxSuppression.naive4(intensity, direction4,output);
		}
	}

	public static class Main4_F32 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.nonMaxSuppression4(intensity, direction4,output);
		}
	}

	public static class Naive8_F32 extends PerformerBase {

		@Override
		public void process() {
			ImplEdgeNonMaxSuppression.naive8(intensity,direction8,output);
		}
	}

	public static class Main8_F32 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.nonMaxSuppression8(intensity,direction8,output);
		}
	}

	public static void main(String args[]) {
		ImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction4, rand, -1, 3);
		ImageMiscOps.fillUniform(direction8, rand, -3, 5);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Naive4_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Main4_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Naive8_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Main8_F32(), TEST_TIME);

	}
}
