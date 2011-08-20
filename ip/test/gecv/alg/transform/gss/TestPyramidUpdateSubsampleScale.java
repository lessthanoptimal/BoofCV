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

import gecv.alg.distort.DistortImageOps;
import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.interpolate.TypeInterpolate;
import gecv.alg.transform.pyramid.BasePyramidTests;
import gecv.alg.transform.pyramid.PyramidUpdateSubsampleScale;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.SubsamplePyramid;
import gecv.testing.GecvTesting;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestPyramidUpdateSubsampleScale extends BasePyramidTests {

	public TestPyramidUpdateSubsampleScale() {
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
		PyramidUpdateSubsampleScale<ImageFloat32> alg = new PyramidUpdateSubsampleScale <ImageFloat32>(interp);

		ImagePyramid<ImageFloat32> pyramid = new SubsamplePyramid<ImageFloat32>(alg,3,5);
		pyramid.update(img);

		// test the first layer
		ImageFloat32 expected = new ImageFloat32((int)Math.ceil(width/3.0),(int)Math.ceil(height/3.0));
		DistortImageOps.scale(img, expected, TypeInterpolate.BILINEAR);
		ImageFloat32 found = pyramid.getLayer(0);

		GecvTesting.assertEquals(expected,found);

		// test the second layer
		ImageFloat32 next = new ImageFloat32((int)Math.ceil(width/5.0),(int)Math.ceil(height/5.0));
		DistortImageOps.scale(expected, next, TypeInterpolate.BILINEAR);
		found = pyramid.getLayer(1);

		GecvTesting.assertEquals(next,found,0,1e-4);
	}
}
