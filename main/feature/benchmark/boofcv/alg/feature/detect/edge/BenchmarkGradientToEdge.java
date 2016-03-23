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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkGradientToEdge {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);

	final static int width = 640;
	final static int height = 480;

	static GrayF32 derivX_F32 = new GrayF32(width,height);
	static GrayF32 derivY_F32 = new GrayF32(width,height);

	static GrayF32 intensity_F32 = new GrayF32(width,height);
	static GrayF32 orientation_F32 = new GrayF32(width,height);

	static GrayS8 direction = new GrayS8(width,height);

	public static class Euclidian_F32 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.intensityE(derivX_F32,derivY_F32,intensity_F32);
		}
	}

	public static class Abs_F32 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.intensityAbs(derivX_F32,derivY_F32,intensity_F32);
		}
	}

	public static class Orientation_F32 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.direction(derivX_F32,derivY_F32,orientation_F32);
		}
	}

	public static class Orientation2_F32 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.direction2(derivX_F32,derivY_F32,orientation_F32);
		}
	}

	public static class Discretize4 extends PerformerBase {
		@Override
		public void process() {
			GradientToEdgeFeatures.discretizeDirection4(intensity_F32,direction);
		}
	}

	public static class Discretize8 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.discretizeDirection8(intensity_F32,direction);
		}
	}

	public static void main(String args[]) {
		ImageMiscOps.fillUniform(derivX_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(derivY_F32, rand, 0, 255);
		ImageMiscOps.fillUniform(orientation_F32, rand, (float)(-Math.PI/2.0), (float)(Math.PI/2.0));

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Euclidian_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Abs_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Orientation_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Orientation2_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Discretize4(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Discretize8(), TEST_TIME);
	}
}
