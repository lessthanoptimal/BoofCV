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

import gecv.alg.filter.convolve.ConvolveNormalized;
import gecv.alg.filter.kernel.FactoryKernelGaussian;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.DiscreteImagePyramid;
import gecv.struct.pyramid.ImagePyramid;
import gecv.testing.GecvTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPyramidUpdateIntegerDown extends BasePyramidTests {

	public TestPyramidUpdateIntegerDown() {
		super();
		this.width = 80;
		this.height = 80;
	}

	/**
	 * Make sure this flag is handled correctly on update
	 */
	@Test
	public void saveOriginalReference() {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(3, true);
		PyramidUpdateIntegerDown<ImageFloat32> alg = new PyramidUpdateIntegerDown<ImageFloat32>(kernel,ImageFloat32.class);

		ImagePyramid<ImageFloat32> pyramid = new DiscreteImagePyramid<ImageFloat32>(true,alg,1,2,4);
		pyramid.update(inputF32);

		assertTrue(inputF32 == pyramid.getLayer(0));

		pyramid = new DiscreteImagePyramid<ImageFloat32>(false,alg,1,2,4);
		pyramid.update(inputF32);

		assertTrue(inputF32 != pyramid.getLayer(0));

		pyramid = new DiscreteImagePyramid<ImageFloat32>(true,alg,2,4);
		pyramid.update(inputF32);

		assertTrue(inputF32 != pyramid.getLayer(0));
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void _update() {

		GecvTesting.checkSubImage(this, "_update", true, inputF32);
	}

	public void _update(ImageFloat32 img) {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian1D_F32(3, true);
		ImageFloat32 convImg = new ImageFloat32(width, height);
		ImageFloat32 convImg2 = new ImageFloat32(width/2, height/2);

		ImageFloat32 storage = new ImageFloat32(width, height);

		ConvolveNormalized.horizontal(kernel,img,storage);
		ConvolveNormalized.vertical(kernel,storage,convImg);

		PyramidUpdateIntegerDown<ImageFloat32> alg = new PyramidUpdateIntegerDown<ImageFloat32>(kernel,ImageFloat32.class);

		ImagePyramid<ImageFloat32> pyramid = new DiscreteImagePyramid<ImageFloat32>(true,alg,1,2,4);
		pyramid.update(img);

		// top layer should be the same as the input layer
		GecvTesting.assertEquals(img, pyramid.getLayer(0), 1, 1e-4f);

		// second layer should have the same values as the convolved image
		for (int i = 0; i < height; i += 2) {
			for (int j = 0; j < width; j += 2) {
				float a = convImg.get(j, i);
				float b = pyramid.getLayer(1).get(j / 2, i / 2);

				assertEquals(a, b, 1e-4);
			}
		}

		storage.reshape(width/2,height/2);
		ConvolveNormalized.horizontal(kernel,pyramid.getLayer(1),storage);
		ConvolveNormalized.vertical(kernel,storage,convImg2);
		// second layer should have the same values as the second convolved image
		for (int i = 0; i < height/2; i += 2) {
			for (int j = 0; j < width/2; j += 2) {
				float a = convImg2.get(j, i);
				float b = pyramid.getLayer(2).get(j / 2, i / 2);

				assertEquals(j+" "+j,a, b, 1e-4);
			}
		}
	}
}
