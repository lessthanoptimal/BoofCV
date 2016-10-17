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

import boofcv.abst.distort.FDistort;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestPyramidFloatGaussianScale extends GenericPyramidTests<GrayF32> {

	double sigmas[] = new double[]{1,2};
	double scales[] = new double[]{3,5};

	public TestPyramidFloatGaussianScale() {
		super(GrayF32.class);
	}

	/**
	 * Compares update to a convolution and sub-sampling of upper layers.
	 */
	@Test
	public void update() {

		GrayF32 img = new GrayF32(width,height);

		BoofTesting.checkSubImage(this, "_update", true, img);
	}

	public void _update(GrayF32 img) {

		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(img, BorderType.EXTENDED);

		PyramidFloatGaussianScale<GrayF32> alg = new PyramidFloatGaussianScale<>(interp,scales,sigmas,imageType);

		alg.process(img);

		// test the first layer
		BlurFilter<GrayF32> blur = FactoryBlurFilter.gaussian(GrayF32.class,3,-1);
		GrayF32 blurrImg = new GrayF32(width, height);
		blur.process(img, blurrImg);
		GrayF32 expected = new GrayF32((int)Math.ceil(width/3.0),(int)Math.ceil(height/3.0));
		new FDistort(blurrImg,expected).scaleExt().apply();
		GrayF32 found = alg.getLayer(0);

		BoofTesting.assertEquals(expected,found,1e-4);

		// test the second layer
		blur = FactoryBlurFilter.gaussian(GrayF32.class,sigmas[0],-1);
		blurrImg = new GrayF32(expected.width,expected.height);
		blur.process(expected,blurrImg);
		expected = new GrayF32((int)Math.ceil(width/5.0),(int)Math.ceil(height/5.0));
		new FDistort(blurrImg,expected).scaleExt().apply();
		found = alg.getLayer(1);

		BoofTesting.assertEquals(expected,found, 1e-4);
	}

	@Override
	protected ImagePyramid<GrayF32> createPyramid(int... scales) {
		double a[] = BoofMiscOps.convertTo_F64(scales);
		double sigmas[] = new double[ scales.length ];
		for( int i = 0; i < sigmas.length; i++ )
			sigmas[i] = i+1;
		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.bilinearPixelS(imageType,BorderType.EXTENDED);
		return new PyramidFloatGaussianScale<>(interp,a,sigmas,imageType);
	}

	/**
	 * Makes sure the amount of Gaussian blur in each level is correctly computed.  Test against hand computed
	 * numbers
	 */
	@Test
	public void checkSigmas() {
		InterpolatePixelS<GrayF32> interp = FactoryInterpolation.
				bilinearPixelS(GrayF32.class,BorderType.EXTENDED);
		double scales[] = new double[]{1,1};
		PyramidFloatGaussianScale<GrayF32> alg = new PyramidFloatGaussianScale<>(interp,scales,sigmas,imageType);

		// easy case with no adjustment to the scales
		assertEquals(1,alg.getSigma(0),1e-6);
		assertEquals(2.23606797749979,alg.getSigma(1),0.001);

		// now the input image is being scaled
		scales = new double[]{2,3};
		alg = new PyramidFloatGaussianScale<>(interp,scales,sigmas,imageType);
		assertEquals(1,alg.getSigma(0),1e-6);
		assertEquals(4.123105625617661,alg.getSigma(1),0.001);
	}
}
