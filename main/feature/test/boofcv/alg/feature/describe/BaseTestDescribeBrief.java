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

package boofcv.alg.feature.describe;


import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.brief.BriefDefinition_I32;
import boofcv.alg.feature.describe.brief.BriefFeature;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.FactoryGeneralizedSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class BaseTestDescribeBrief <T extends ImageSingleBand> {

	Random rand = new Random(234);
	int width = 30;
	int height = 40;
	Class<T> imageType;

	BriefDefinition_I32 def = FactoryBriefDefinition.gaussian2(rand, 5, 20);
	BlurFilter<T> filterBlur;

	public BaseTestDescribeBrief(Class<T> imageType) {
		this.imageType = imageType;
		filterBlur = FactoryBlurFilter.gaussian(imageType, -1, 1);
	}

	protected  T createImage( int width , int height ) {
		T ret = GeneralizedImageOps.createSingleBand(imageType, width, height);
		GeneralizedImageOps.randomize(ret,rand,0,50);
		return ret;
	}

	/**
	 * Have brief process a sub-image and see if it produces the same results.
	 */
	@Test
	public void testSubImage() {
		T input = createImage(width,height);

		DescribePointBrief<T> alg = FactoryDescribePointAlgs.brief(def, filterBlur);
		BriefFeature desc1 = alg.createFeature();
		BriefFeature desc2 = alg.createFeature();

		// resize the image and see if it computes the same output
		alg.setImage(input);
		assertTrue(alg.process(input.width / 2, input.height / 2, desc1));

		T sub = (T)BoofTesting.createSubImageOf(input);

		alg.setImage(sub);
		assertTrue(alg.process(input.width / 2, input.height / 2, desc2));

		for( int i = 0; i < desc1.data.length; i++ ) {
			assertEquals(desc1.data[i],desc2.data[i]);
		}
	}

	/**
	 * Change the input image size and see if it handles that case properly.
	 */
	@Test
	public void changeInInputSize() {
		T inputA = createImage(width,height);
		T inputB = createImage(width-5,height-5);

		DescribePointBrief<T> alg = FactoryDescribePointAlgs.brief(def, filterBlur);
		BriefFeature desc = alg.createFeature();

		alg.setImage(inputA);
		assertTrue(alg.process(inputA.width / 2, inputA.height / 2, desc));

		// just see if it blows up or not
		alg.setImage(inputB);
		assertTrue(alg.process(inputA.width / 2, inputA.height / 2, desc));
	}

	/**
	 * Vary the intensity of the input image and see if the description changes.
	 */
	@Test
	public void testIntensityInvariance() {
		T input = createImage(width,height);
		T mod = (T)input.clone();

		GPixelMath.multiply(input, mod, 2);

		DescribePointBrief<T> alg = FactoryDescribePointAlgs.brief(def, filterBlur);

		BriefFeature desc1 = alg.createFeature();
		BriefFeature desc2 = alg.createFeature();

		// compute the image from the same image but different intensities
		alg.setImage(input);
		assertTrue(alg.process(input.width / 2, input.height / 2, desc1));

		alg.setImage(mod);
		assertTrue(alg.process(input.width / 2, input.height / 2, desc2));

		// compare the descriptions
		int count = 0;
		for( int i = 0; i < desc1.numBits; i++ ) {
			count += desc1.isBitTrue(i) == desc2.isBitTrue(i) ? 1 : 0;
		}
		// blurring the image can cause some bits to switch in the description
		assertTrue( count > desc1.numBits-3);
	}

	/**
	 * Compute the BRIEF descriptor manually and see if it gets the same answer
	 */
	@Test
	public void testManualCheck() {
		T input = createImage(width,height);
		T blurred = (T)input._createNew(width, height);
		filterBlur.process(input,blurred);


		GImageSingleBand a = FactoryGeneralizedSingleBand.wrap(blurred);

		DescribePointBrief<T> alg = FactoryDescribePointAlgs.brief(def, filterBlur);

		alg.setImage(input);

		int c_x = input.width/2;
		int c_y = input.height/2;

		BriefFeature desc = alg.createFeature();
		assertTrue(alg.process(c_x, c_y, desc));

		for( int i = 0; i < def.compare.length; i++ ) {
			Point2D_I32 c = def.compare[i];
			Point2D_I32 p0 = def.samplePoints[c.x];
			Point2D_I32 p1 = def.samplePoints[c.y];

			boolean expected = a.get(c_x+p0.x,c_y+p0.y).doubleValue()
					< a.get(c_x+p1.x,c_y+p1.y).doubleValue();
			assertTrue(expected == desc.isBitTrue(i));
		}
	}
}
