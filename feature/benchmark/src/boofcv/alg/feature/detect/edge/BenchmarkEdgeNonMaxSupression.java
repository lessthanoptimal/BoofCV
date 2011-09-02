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

package boofcv.alg.feature.detect.edge;

import boofcv.PerformerBase;
import boofcv.ProfileOperation;
import boofcv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppression;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkEdgeNonMaxSupression {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);

	final static int width = 640;
	final static int height = 480;

	static ImageFloat32 intensity = new ImageFloat32(width,height);
	static ImageFloat32 output = new ImageFloat32(width,height);
	static ImageUInt8 direction = new ImageUInt8(width,height);


	public static class Naive_F32 extends PerformerBase {

		@Override
		public void process() {
			ImplEdgeNonMaxSuppression.naive(intensity,direction,output);
		}
	}

	public static class Main_F32 extends PerformerBase {

		@Override
		public void process() {
			GradientToEdgeFeatures.nonMaxSuppression(intensity,direction,output);
		}
	}


	public static void main(String args[]) {
		ImageTestingOps.randomize(intensity, rand, 0, 100);
		ImageTestingOps.randomize(direction, rand, 0, 4);

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Naive_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Main_F32(), TEST_TIME);

	}
}
