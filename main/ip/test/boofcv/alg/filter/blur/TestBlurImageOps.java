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

package boofcv.alg.filter.blur;

import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.alg.filter.convolve.normalized.ConvolveNormalizedNaive;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_F64;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestBlurImageOps {

	Random rand = new Random(234);

	int width = 15;
	int height = 20;

	@Test
	public void mean_U8() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 found = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			int w = radius*2+1;

			Kernel2D_I32 kernel = new Kernel2D_I32(w);
			Arrays.fill(kernel.data,1);

			ConvolveNormalizedNaive.convolve(kernel, input, expected);

			BlurImageOps.mean(input,found, radius, null);

			BoofTesting.assertEquals(expected,found,2);
		}
	}

	@Test
	public void mean_F32() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			int w = radius*2+1;

			Kernel2D_F32 kernel = new Kernel2D_F32(w);
			Arrays.fill(kernel.data,1f/(w*w));

			ConvolveNormalized.convolve(kernel,input,expected);
			BlurImageOps.mean(input,found, radius, null);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test
	public void mean_F64() {
		GrayF64 input = new GrayF64(width,height);
		GrayF64 found = new GrayF64(width,height);
		GrayF64 expected = new GrayF64(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			int w = radius*2+1;

			Kernel2D_F64 kernel = new Kernel2D_F64(w);
			Arrays.fill(kernel.data,1.0/(w*w));

			ConvolveNormalized.convolve(kernel,input,expected);
			BlurImageOps.mean(input,found, radius, null);

			BoofTesting.assertEquals(expected,found,1e-8);
		}
	}

	@Test
	public void median_U8() {

		GrayU8 input = new GrayU8(width,height);
		GrayU8 found = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			BoofTesting.assertEquals(expected,found,0);
		}
	}

	@Test
	public void median_F32() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImplMedianSortNaive.process(input,expected,radius,null);
			BlurImageOps.median(input,found,radius);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test
	public void gaussian_U8() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 found = new GrayU8(width,height);
		GrayU8 expected = new GrayU8(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 200);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			// make sure the kernels are equivalent
			Kernel1D_I32 ker1 = FactoryKernelGaussian.gaussian(1, false, 32, -1, radius);
			Kernel2D_I32 kernel = KernelMath.convolve2D(ker1, ker1);
			ConvolveNormalizedNaive.convolve(kernel, input, expected);

			BlurImageOps.gaussian(input,found,-1,radius,null);
			BoofTesting.assertEquals(expected,found,2);
		}
	}

	@Test
	public void gaussian_F32() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			Kernel2D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel2D_F32.class,-1,radius);
			ConvolveNormalized.convolve(kernel,input,expected);

			double sigma = FactoryKernelGaussian.sigmaForRadius(radius,0);

			BlurImageOps.gaussian(input,found,sigma,radius,null);

			BoofTesting.assertEquals(expected,found,1e-4);
		}
	}

	@Test
	public void gaussian_F64() {
		GrayF64 input = new GrayF64(width,height);
		GrayF64 found = new GrayF64(width,height);
		GrayF64 expected = new GrayF64(width,height);

		GImageMiscOps.fillUniform(input, rand, 0, 20);

		for( int radius = 1; radius <= 4; radius++ ) {
			ImageMiscOps.fill(expected,0);
			ImageMiscOps.fill(found,0);

			Kernel2D_F64 kernel = FactoryKernelGaussian.gaussian(Kernel2D_F64.class,-1,radius);
			ConvolveNormalized.convolve(kernel,input,expected);

			double sigma = FactoryKernelGaussian.sigmaForRadius(radius,0);

			BlurImageOps.gaussian(input,found,sigma,radius,null);

			BoofTesting.assertEquals(expected,found,1e-8);
		}
	}
}
