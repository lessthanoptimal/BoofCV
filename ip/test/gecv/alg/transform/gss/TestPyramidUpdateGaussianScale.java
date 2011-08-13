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

package gecv.alg.transform.gss;

import gecv.abst.filter.blur.FactoryBlurFilter;
import gecv.abst.filter.blur.impl.BlurStorageFilter;
import gecv.alg.distort.DistortImageOps;
import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.interpolate.TypeInterpolate;
import gecv.alg.transform.pyramid.BasePyramidTests;
import gecv.struct.gss.ScaleSpacePyramid;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.testing.GecvTesting;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestPyramidUpdateGaussianScale extends BasePyramidTests {

	public TestPyramidUpdateGaussianScale() {
		super();
		this.width = 80;
		this.height = 80;
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void update() {

		GecvTesting.checkSubImage(this, "_update", true, inputF32);
	}

	public void _update(ImageFloat32 img) {

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(inputF32);
		PyramidUpdateGaussianScale <ImageFloat32> alg = new PyramidUpdateGaussianScale <ImageFloat32>(interp);

		ImagePyramid<ImageFloat32> pyramid = new ScaleSpacePyramid<ImageFloat32>(alg,3,5);
		pyramid.update(img);

		// test the first layer
		BlurStorageFilter<ImageFloat32> blur = FactoryBlurFilter.gaussian(ImageFloat32.class,3,-1);
		ImageFloat32 blurrImg = new ImageFloat32(width, height);
		blur.process(img,blurrImg);
		ImageFloat32 expected = new ImageFloat32((int)Math.ceil(width/3.0),(int)Math.ceil(height/3.0));
		DistortImageOps.scale(blurrImg, expected, TypeInterpolate.BILINEAR);
		ImageFloat32 found = pyramid.getLayer(0);

		GecvTesting.assertEquals(expected,found);

		// test the second layer
		blur = FactoryBlurFilter.gaussian(ImageFloat32.class,5.0/3.0,-1);
		blurrImg = new ImageFloat32(expected.width,expected.height);
		blur.process(expected,blurrImg);
		expected = new ImageFloat32((int)Math.ceil(width/5.0),(int)Math.ceil(height/5.0));
		DistortImageOps.scale(blurrImg, expected, TypeInterpolate.BILINEAR);
		found = pyramid.getLayer(1);

		GecvTesting.assertEquals(expected,found,0,1e-4);
	}
}
