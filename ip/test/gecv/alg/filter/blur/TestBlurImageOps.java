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

package gecv.alg.filter.blur;

import gecv.alg.filter.blur.impl.ImplMedianSortNaive;
import gecv.alg.filter.convolve.ConvolveNormalized;
import gecv.alg.filter.kernel.FactoryKernel;
import gecv.alg.filter.kernel.FactoryKernelGaussian;
import gecv.alg.misc.ImageTestingOps;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestBlurImageOps {

	Random rand = new Random(234);

	int width = 10;
	int height = 12;

	@Test
	public void mean_U8() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);

		ImageUInt8 storage = new ImageUInt8(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageTestingOps.fill(expected,0);
			ImageTestingOps.fill(found,0);

			Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			BlurImageOps.mean(input,found, radius, null);

			GecvTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void mean_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		ImageFloat32 storage = new ImageFloat32(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageTestingOps.fill(expected,0);
			ImageTestingOps.fill(found,0);

			Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius,true);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			BlurImageOps.mean(input,found, radius, null);

			GecvTesting.assertEquals(expected,found,0,1e-4);
		}
	}

	@Test
	public void median_U8() {

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			GecvTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void median_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			GecvTesting.assertEquals(expected,found,0,1e-4);
		}
	}

	@Test
	public void gaussian_U8() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);

		ImageUInt8 storage = new ImageUInt8(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageTestingOps.fill(expected,0);
			ImageTestingOps.fill(found,0);

			Kernel1D_I32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,radius);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			double sigma = FactoryKernelGaussian.sigmaForRadius(radius,0);

			BlurImageOps.gaussian(input,found,sigma,radius,null);

			GecvTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void gaussian_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		ImageFloat32 storage = new ImageFloat32(width,height);

		GeneralizedImageOps.randomize(input,rand,0,20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageTestingOps.fill(expected,0);
			ImageTestingOps.fill(found,0);

			Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,radius);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			double sigma = FactoryKernelGaussian.sigmaForRadius(radius,0);

			BlurImageOps.gaussian(input,found,sigma,radius,null);

			GecvTesting.assertEquals(expected,found,0,1e-4);
		}
	}
}
