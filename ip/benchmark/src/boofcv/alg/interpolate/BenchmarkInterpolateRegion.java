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

package boofcv.alg.interpolate;

import boofcv.PerformerBase;
import boofcv.ProfileOperation;
import boofcv.alg.interpolate.impl.BilinearRectangle_F32;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;

import java.util.Random;

/**
 * Benchmark interpolating rectangular regions
 *
 * @author Peter Abeles
 */
public class BenchmarkInterpolateRegion {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static ImageFloat32 imgFloat32;
	static ImageUInt8 imgInt8;
	static ImageFloat32 outputImage;

	// defines the region its interpolation
	static float start = 10.1f;
	static int regionSize = 300;

	public static class Bilinear_F32 extends PerformerBase {
		BilinearRectangle_F32 alg = new BilinearRectangle_F32(imgFloat32);

		@Override
		public void process() {
			alg.region(start, start, outputImage);
		}
	}

	public static void main(String args[]) {
		imgInt8 = new ImageUInt8(imgWidth, imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth, imgHeight);

		outputImage = new ImageFloat32(regionSize,regionSize);

		Random rand = new Random(234);
		ImageTestingOps.randomize(imgInt8, rand, 0, 100);
		ImageTestingOps.randomize(imgFloat32, rand, 0, 200);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new Bilinear_F32(), TEST_TIME);

		System.out.println("   ---- Sub-Image ----");
		outputImage = BoofTesting.createSubImageOf(outputImage);
		ProfileOperation.printOpsPerSec(new Bilinear_F32(), TEST_TIME);

	}
}
