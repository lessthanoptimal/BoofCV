/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.edge;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.misc.ImageTestingOps;
import gecv.struct.image.ImageFloat32;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkGradientToEdge {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);

	final static int width = 640;
	final static int height = 480;

	static ImageFloat32 derivX_F32 = new ImageFloat32(width,height);
	static ImageFloat32 derivY_F32 = new ImageFloat32(width,height);

	static ImageFloat32 intensity_F32 = new ImageFloat32(width,height);
	static ImageFloat32 orientation_F32 = new ImageFloat32(width,height);


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

	public static void main(String args[]) {
		ImageTestingOps.randomize(derivX_F32, rand, 0, 255);
		ImageTestingOps.randomize(derivY_F32, rand, 0, 255);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Euclidian_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Abs_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Orientation_F32(), TEST_TIME);

	}
}
