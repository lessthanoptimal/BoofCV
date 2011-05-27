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

package gecv.alg.pyramid;

import gecv.alg.filter.blur.BlurImageOps;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.alg.misc.ImageTestingOps;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid_F32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConvolutionPyramid {
	Random rand = new Random(234);
	int width = 80;
	int height = 160;

	/**
	 * Make sure this flag is handled correctly on update
	 */
	@Test
	public void saveOriginalReference() {
		ImageFloat32 img = new ImageFloat32(width, height);
		ImageTestingOps.randomize(img, rand, 0, 100);

		Kernel1D_F32 kernel = KernelFactory.gaussian1D_F32(3, true);
		ImagePyramid_F32 pyramid = new ImagePyramid_F32(width, height, true);
		pyramid.setScaling(1, 2, 2);

		ConvolutionPyramid<ImageFloat32> alg = new ConvolutionPyramid<ImageFloat32>(kernel,ImageFloat32.class);
		alg.setPyramid(pyramid);
		alg.update(img);

		assertTrue(img == pyramid.getLayer(0));

		pyramid = new ImagePyramid_F32(width, height, false);
		pyramid.setScaling(1, 2, 2);
		alg.setPyramid(pyramid);
		alg.update(img);

		assertTrue(img != pyramid.getLayer(0));

		pyramid = new ImagePyramid_F32(width, height, true);
		pyramid.setScaling(2, 2);
		alg.setPyramid(pyramid);
		alg.update(img);

		assertTrue(img != pyramid.getLayer(0));
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void _update() {
		ImageFloat32 img = new ImageFloat32(width, height);
		ImageTestingOps.randomize(img, rand, 0, 100);

		GecvTesting.checkSubImage(this, "_update", true, img);
	}

	public void _update(ImageFloat32 img) {
		Kernel1D_F32 kernel = KernelFactory.gaussian1D_F32(3, true);
		ImageFloat32 convImg = new ImageFloat32(width, height);

		BlurImageOps.kernel(img, convImg, kernel, new ImageFloat32(width, height));

		ImagePyramid_F32 pyramid = new ImagePyramid_F32(width, height, false);
		pyramid.setScaling(1, 2, 2);
		ConvolutionPyramid<ImageFloat32> alg = new ConvolutionPyramid<ImageFloat32>(kernel,ImageFloat32.class);
		alg.setPyramid(pyramid);
		alg.update(img);

		// top layer should be the same as the input layer
		GecvTesting.assertEquals(img, pyramid.getLayer(0), 1, 1e-4f);

		for (int i = 0; i < height; i += 2) {
			for (int j = 0; j < width; j += 2) {
				float a = convImg.get(j, i);
				float b = pyramid.getLayer(1).get(j / 2, i / 2);

				assertEquals(a, b, 1e-4);
			}
		}
	}
}
