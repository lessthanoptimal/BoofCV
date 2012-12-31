/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.blur;

import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
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

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			BlurImageOps.mean(input,found, radius, null);

			BoofTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void mean_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		ImageFloat32 storage = new ImageFloat32(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius,true);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			BlurImageOps.mean(input,found, radius, null);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test
	public void median_U8() {

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			BoofTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void median_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test
	public void gaussian_U8() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);

		ImageUInt8 storage = new ImageUInt8(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			Kernel1D_I32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,-1,radius);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			double sigma = FactoryKernelGaussian.sigmaForRadius(radius,0);

			BlurImageOps.gaussian(input,found,sigma,radius,null);

			BoofTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void gaussian_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		ImageFloat32 storage = new ImageFloat32(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,radius);
			ConvolveNormalized.horizontal(kernel,input,storage);
			ConvolveNormalized.vertical(kernel,storage,expected);

			double sigma = FactoryKernelGaussian.sigmaForRadius(radius,0);

			BlurImageOps.gaussian(input,found,sigma,radius,null);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}
}
