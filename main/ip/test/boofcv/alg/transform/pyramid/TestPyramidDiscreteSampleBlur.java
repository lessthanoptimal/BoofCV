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

package boofcv.alg.transform.pyramid;

import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPyramidDiscreteSampleBlur extends GenericPyramidTests<GrayF32> {

	public TestPyramidDiscreteSampleBlur() {
		super(GrayF32.class);
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void _update() {
		GrayF32 input = new GrayF32(width,height);

		BoofTesting.checkSubImage(this, "_update", true, input);
	}

	public void _update(GrayF32 input) {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,3);
		GrayF32 convImg = new GrayF32(width, height);
		GrayF32 convImg2 = new GrayF32(width/2, height/2);

		GrayF32 storage = new GrayF32(width, height);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,convImg);

		PyramidDiscreteSampleBlur<GrayF32> alg =
				new PyramidDiscreteSampleBlur<>(kernel,3,GrayF32.class,true,new int[]{1,2,4});

		alg.process(input);

		// top layer should be the same as the input layer
		BoofTesting.assertEquals(input, alg.getLayer(0), 1e-4f);

		// second layer should have the same values as the convolved image
		for (int i = 0; i < height; i += 2) {
			for (int j = 0; j < width; j += 2) {
				float a = convImg.get(j, i);
				float b = alg.getLayer(1).get(j / 2, i / 2);

				assertEquals(a, b, 1e-4);
			}
		}

		storage.reshape(width/2,height/2);
		ConvolveNormalized.horizontal(kernel,alg.getLayer(1),storage);
		ConvolveNormalized.vertical(kernel,storage,convImg2);
		// second layer should have the same values as the second convolved image
		for (int i = 0; i < height/2; i += 2) {
			for (int j = 0; j < width/2; j += 2) {
				float a = convImg2.get(j, i);
				float b = alg.getLayer(2).get(j / 2, i / 2);

				assertEquals(j+" "+j,a, b, 1e-4);
			}
		}
	}

	/**
	 * Makes sure the amount of Gaussian blur in each level is correctly computed
	 */
	@Test
	public void checkSigmas() {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,3);

		PyramidDiscreteSampleBlur<GrayF32> alg =
				new PyramidDiscreteSampleBlur<>(kernel,3,GrayF32.class,true,new int[]{1,2,4});

		assertEquals(0,alg.getSigma(0),1e-8);
		assertEquals(3,alg.getSigma(1),1e-8);
		assertEquals(6.7082,alg.getSigma(2),1e-3);

		alg = new PyramidDiscreteSampleBlur<>(kernel,3,GrayF32.class,true,new int[]{2,4,8});
		assertEquals(0,alg.getSigma(0),1e-8);
		assertEquals(6,alg.getSigma(1),1e-8);
	}

	@Override
	protected ImagePyramid<GrayF32> createPyramid(int... scales) {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,3);
		return new PyramidDiscreteSampleBlur<>(kernel,3,GrayF32.class,true,new int[]{1,2,4});
	}
}
