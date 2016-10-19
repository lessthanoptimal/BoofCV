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

package boofcv.alg.distort;

import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import boofcv.struct.distort.PixelTransform2_F32;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.homography.Homography2D_F32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkPixelTransform {


	public static final int imgWidth = 640;
	public static final int imgHeight = 480;
	
	public static final int TEST_TIME = 1000;

	public static class TestPixelTransform_F32 implements Performer {
		PixelTransform2_F32 alg;
		String name;

		public TestPixelTransform_F32(PixelTransform2_F32 alg, String name ) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			for (int y = 0; y < imgHeight; y++ )
				for (int x = 0; x < imgWidth; x++)
					alg.compute(x, y);
		}

		@Override
		public String getName() {
			return name;
		}
	}

	
	private static void benchmark(PixelTransform2_F32 alg , String name ) {
		ProfileOperation.printOpsPerSec(new TestPixelTransform_F32(alg,name), TEST_TIME);
	}
	
	public static void main( String args[] ) {
		Random rand = new Random(234);

		Affine2D_F32 affine = new Affine2D_F32((float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian(),(float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian());

		Homography2D_F32 homography = new Homography2D_F32((float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian(),(float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian(),(float)rand.nextGaussian(),(float)rand.nextGaussian(),
				(float)rand.nextGaussian());

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		benchmark(new PixelTransformHomography_F32(homography), "Homography");
		benchmark(new PixelTransformAffine_F32(affine), "Affine");

	}
}
