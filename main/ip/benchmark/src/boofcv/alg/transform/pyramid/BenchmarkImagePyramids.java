/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.transform.pyramid;

import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidFloat;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterFloat;

import java.util.Random;


/**
 * Shows runtime performance difference of each type of image pyramid given similar configurations.
 *
 * @author Peter Abeles
 */
public class BenchmarkImagePyramids {
	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;

	static ImageFloat32 input = new ImageFloat32(width,height);

	static int scalesD[] = new int[]{1,2,4,8};
	static double scalesF[] = new double[]{1,2,4,8};

	static PyramidUpdaterDiscrete<ImageFloat32> updaterD;
	static PyramidUpdaterFloat<ImageFloat32> updaterF;

	static Class<ImageFloat32> imageType = ImageFloat32.class;

	public static class ScaleSpace_F32 extends PerformerBase {

		PyramidFloat<ImageFloat32> pyramid =
				new PyramidFloat<ImageFloat32>(imageType,scalesF);

		@Override
		public void process() {
			updaterF.update(input,pyramid);
		}
	}

	public static class Discrete_F32 extends PerformerBase {

		PyramidDiscrete<ImageFloat32> pyramid =
				new PyramidDiscrete<ImageFloat32>(imageType,false,scalesD);

		@Override
		public void process() {
			updaterD.update(input,pyramid);
		}
	}

	private static void createUpdate() {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1.0,2);
		updaterD = new PyramidUpdateIntegerDown<ImageFloat32>(kernel,ImageFloat32.class);

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		updaterF = new PyramidUpdateGaussianScale<ImageFloat32>(interp,scalesF);

	}

	public static void main(String args[]) {

		Random rand = new Random(234);
		ImageTestingOps.randomize(input, rand, 0, 100);
		createUpdate();

		System.out.println("=========  Profile Image Size " + width + " x " + height + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new ScaleSpace_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Discrete_F32(), TEST_TIME);
	}
}
