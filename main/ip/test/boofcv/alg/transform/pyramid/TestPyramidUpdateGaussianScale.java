/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.factory.filter.blur.FactoryBlurFilter;
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
public class TestPyramidUpdateGaussianScale extends GenericPyramidUpdateTests<ImageFloat32> {

	double sigmas[] = new double[]{1,2};

	public TestPyramidUpdateGaussianScale() {
		super(ImageFloat32.class);
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void update() {

		ImageFloat32 img = new ImageFloat32(width,height);

		BoofTesting.checkSubImage(this, "_update", true, img);
	}

	public void _update(ImageFloat32 img) {

		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(img);
		PyramidUpdateGaussianScale<ImageFloat32> alg = new PyramidUpdateGaussianScale <ImageFloat32>(interp,sigmas);

		PyramidFloat<ImageFloat32> pyramid = new PyramidFloat<ImageFloat32>(imageType,3,5);
		alg.update(img,pyramid);

		// test the first layer
		BlurStorageFilter<ImageFloat32> blur = FactoryBlurFilter.gaussian(ImageFloat32.class,3,-1);
		ImageFloat32 blurrImg = new ImageFloat32(width, height);
		blur.process(img,blurrImg);
		ImageFloat32 expected = new ImageFloat32((int)Math.ceil(width/3.0),(int)Math.ceil(height/3.0));
		DistortImageOps.scale(blurrImg, expected, TypeInterpolate.BILINEAR);
		ImageFloat32 found = pyramid.getLayer(0);

		BoofTesting.assertEquals(expected,found);

		// test the second layer
		blur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigmas[0],-1);
		blurrImg = new ImageFloat32(expected.width,expected.height);
		blur.process(expected,blurrImg);
		expected = new ImageFloat32((int)Math.ceil(width/5.0),(int)Math.ceil(height/5.0));
		DistortImageOps.scale(blurrImg, expected, TypeInterpolate.BILINEAR);
		found = pyramid.getLayer(1);

		BoofTesting.assertEquals(expected,found,0,1e-4);
	}

	@Override
	protected PyramidUpdater createUpdater() {
		InterpolatePixel<ImageFloat32> interp = FactoryInterpolation.bilinearPixel(imageType);
		return new PyramidUpdateGaussianScale(interp,sigmas);
	}

	@Override
	protected ImagePyramid<ImageFloat32> createPyramid(int... scales) {
		double a[] = BoofMiscOps.convertTo_F64(scales);
		return new PyramidFloat<ImageFloat32>(imageType,a);
	}
}
