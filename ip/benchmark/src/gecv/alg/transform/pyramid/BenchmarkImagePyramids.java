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

package gecv.alg.transform.pyramid;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.filter.convolve.FactoryKernelGaussian;
import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.transform.gss.PyramidUpdateGaussianScale;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.gss.ScaleSpacePyramid;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.DiscreteImagePyramid;
import gecv.struct.pyramid.PyramidUpdater;

import java.util.Random;


/**
 * Shows runtime performance difference of different types of image pyramids.  Each pyramid has
 * been configured to produce similar outputs.
 *
 * @author Peter Abeles
 */
public class BenchmarkImagePyramids {
	static int width = 640;
	static int height = 480;
	static long TEST_TIME = 1000;

	static ImageFloat32 input = new ImageFloat32(width,height);

	static int scalesI[] = new int[]{1,2,2,2};
	static double scalesF[] = new double[]{1,2,2,2};

	static PyramidUpdater<ImageFloat32> updaterI;
	static PyramidUpdater<ImageFloat32> updaterF;


	public static class ScaleSpace_F32 extends PerformerBase {

		ScaleSpacePyramid<ImageFloat32> pyramid =
				new ScaleSpacePyramid<ImageFloat32>(updaterF,scalesF);

		@Override
		public void process() {
			pyramid.update(input);
		}
	}

	public static class Discrete_F32 extends PerformerBase {

		DiscreteImagePyramid<ImageFloat32> pyramid =
				new DiscreteImagePyramid<ImageFloat32>(false,updaterI,scalesI);

		@Override
		public void process() {
			pyramid.update(input);
		}
	}

	private static void createUpdate() {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(2,true);
		updaterI = new PyramidUpdateIntegerDown<ImageFloat32>(kernel,ImageFloat32.class);

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		updaterF = new PyramidUpdateGaussianScale<ImageFloat32>(interp);

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
