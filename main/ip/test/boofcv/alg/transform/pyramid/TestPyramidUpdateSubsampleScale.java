/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidFloat;
import boofcv.struct.pyramid.PyramidUpdater;
import boofcv.testing.BoofTesting;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestPyramidUpdateSubsampleScale extends GenericPyramidUpdateTests<ImageFloat32> {

	public TestPyramidUpdateSubsampleScale() {
		super(ImageFloat32.class);
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void update() {

		ImageFloat32 input = new ImageFloat32(width,height);
		BoofTesting.checkSubImage(this, "_update", true, input);
	}

	public void _update(ImageFloat32 input) {

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(input);
		PyramidUpdateSubsampleScale<ImageFloat32> alg = new PyramidUpdateSubsampleScale<ImageFloat32>(interp);

		PyramidFloat<ImageFloat32> pyramid = new PyramidFloat<ImageFloat32>(imageType,3,5);
		alg.update(input,pyramid);

		// test the first layer
		ImageFloat32 expected = new ImageFloat32((int)Math.ceil(width/3.0),(int)Math.ceil(height/3.0));
		DistortImageOps.scale(input, expected, TypeInterpolate.BILINEAR);
		ImageFloat32 found = pyramid.getLayer(0);

		BoofTesting.assertEquals(expected,found);

		// test the second layer
		ImageFloat32 next = new ImageFloat32((int)Math.ceil(width/5.0),(int)Math.ceil(height/5.0));
		DistortImageOps.scale(expected, next, TypeInterpolate.BILINEAR);
		found = pyramid.getLayer(1);

		BoofTesting.assertEquals(next,found,0,1e-4);
	}

	@Override
	protected PyramidUpdater createUpdater() {
		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(imageType);
		return new PyramidUpdateSubsampleScale(interp);
	}

	@Override
	protected ImagePyramid<ImageFloat32> createPyramid(int... scales) {
		double a[] = BoofMiscOps.convertTo_F64(scales);
		return new PyramidFloat<ImageFloat32>(imageType,a);
	}
}
