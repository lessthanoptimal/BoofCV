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

package boofcv.alg.transform.pyramid;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestPyramidFloatGaussianScale extends GenericPyramidTests<ImageFloat32> {

	double sigmas[] = new double[]{1,2};
	double scales[] = new double[]{3,5};

	public TestPyramidFloatGaussianScale() {
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

		InterpolatePixelS<ImageFloat32> interp = FactoryInterpolation.bilinearPixelS(img);

		PyramidFloatGaussianScale<ImageFloat32> alg = new PyramidFloatGaussianScale<ImageFloat32>(interp,scales,sigmas,imageType);

		alg.process(img);

		// test the first layer
		BlurFilter<ImageFloat32> blur = FactoryBlurFilter.gaussian(ImageFloat32.class,3,-1);
		ImageFloat32 blurrImg = new ImageFloat32(width, height);
		blur.process(img,blurrImg);
		ImageFloat32 expected = new ImageFloat32((int)Math.ceil(width/3.0),(int)Math.ceil(height/3.0));
		DistortImageOps.scale(blurrImg, expected, TypeInterpolate.BILINEAR);
		ImageFloat32 found = alg.getLayer(0);

		BoofTesting.assertEquals(expected,found,1e-4);

		// test the second layer
		blur = FactoryBlurFilter.gaussian(ImageFloat32.class,sigmas[0],-1);
		blurrImg = new ImageFloat32(expected.width,expected.height);
		blur.process(expected,blurrImg);
		expected = new ImageFloat32((int)Math.ceil(width/5.0),(int)Math.ceil(height/5.0));
		DistortImageOps.scale(blurrImg, expected, TypeInterpolate.BILINEAR);
		found = alg.getLayer(1);

		BoofTesting.assertEquals(expected,found, 1e-4);
	}

	@Override
	protected ImagePyramid<ImageFloat32> createPyramid(int... scales) {
		double a[] = BoofMiscOps.convertTo_F64(scales);
		double sigmas[] = new double[ scales.length ];
		for( int i = 0; i < sigmas.length; i++ )
			sigmas[i] = i+1;
		InterpolatePixelS<ImageFloat32> interp = FactoryInterpolation.bilinearPixelS(imageType);
		return new PyramidFloatGaussianScale<ImageFloat32>(interp,a,sigmas,imageType);
	}

	/**
	 * Makes sure the amount of Gaussian blur in each level is correctly computed.  Test against hand computed
	 * numbers
	 */
	@Test
	public void checkSigmas() {
		InterpolatePixelS<ImageFloat32> interp = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);
		double scales[] = new double[]{1,1};
		PyramidFloatGaussianScale<ImageFloat32> alg = new PyramidFloatGaussianScale<ImageFloat32>(interp,scales,sigmas,imageType);

		// easy case with no adjustment to the scales
		assertEquals(1,alg.getSigma(0),1e-6);
		assertEquals(2.23606797749979,alg.getSigma(1),0.001);

		// now the input image is being scaled
		scales = new double[]{2,3};
		alg = new PyramidFloatGaussianScale<ImageFloat32>(interp,scales,sigmas,imageType);
		assertEquals(1,alg.getSigma(0),1e-6);
		assertEquals(4.123105625617661,alg.getSigma(1),0.001);
	}
}
