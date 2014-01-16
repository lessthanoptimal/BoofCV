/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.weights.WeightDistanceUniform_F32;
import boofcv.alg.weights.WeightDistance_F32;
import boofcv.alg.weights.WeightPixelGaussian_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestSegmentMeanShiftColor {

	Random rand = new Random(234);

	ImageType<MultiSpectral<ImageFloat32>> imageType = ImageType.ms(3,ImageFloat32.class);

	InterpolatePixelMB<MultiSpectral<ImageFloat32>> interp =
			FactoryInterpolation.createPixelMB(0,256,TypeInterpolate.BILINEAR,imageType);
	// use a gaussian distribution by default so that the indexes matter
	WeightPixel_F32 weightSpacial = new WeightPixelGaussian_F32();
	WeightDistance_F32 weightDist = new WeightDistanceUniform_F32(200);

	@Before
	public void before() {
		weightSpacial.setRadius(2,2);
	}

	/**
	 * Process a random image and do a basic sanity check on the output
	 */
	@Test
	public void simpleTest() {
		fail("implement");
	}

	@Test
	public void compareToGray() {
		fail("implement");
	}

	@Test
	public void findPeak_inside() {
		fail("implement");
	}

	@Test
	public void findPeak_border() {
		findPeak_border(2, 2, 0, 0);
		findPeak_border(17, 22, 19, 24);
	}

	private void findPeak_border(int cx, int cy, int startX, int startY) {
		fail("implement");
	}

	@Test
	public void meanColor() {
		fail("implement");
	}

	@Test
	public void sumColor() {
		fail("implement");
	}

	@Test
	public void savePeakColor() {
		fail("implement");
	}

	@Test
	public void distanceSq() {
		fail("implement");
	}
}
