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

package boofcv.alg.feature.describe;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.alg.feature.describe.brief.BinaryCompareDefinition_I32;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.factory.feature.describe.FactoryDescribePointAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestDescribePointBriefSO {
	Random rand = new Random(234);
	int width = 30;
	int height = 40;
	Class<GrayF32> imageType = GrayF32.class;

	int briefRadius = 5;
	BinaryCompareDefinition_I32 def = FactoryBriefDefinition.gaussian2(rand, briefRadius, 20);
	BlurFilter<GrayF32> filterBlur;

	public TestDescribePointBriefSO() {
		filterBlur = FactoryBlurFilter.gaussian(imageType, -1, 1);
	}

	protected GrayF32 createImage(int width , int height ) {
		GrayF32 ret = new GrayF32(width,height);
		GImageMiscOps.fillUniform(ret, rand, 0, 50);
		return ret;
	}

	protected DescribePointBriefSO<GrayF32> createAlg() {
		return FactoryDescribePointAlgs.briefso(def,filterBlur);
	}

	/**
	 * Checks to see if changing orientation changes the description
	 */
	@Test
	public void testOrientation() {
		GrayF32 input = createImage(width,height);

		DescribePointBriefSO<GrayF32> alg = createAlg();
		TupleDesc_B desc1 = alg.createFeature();
		TupleDesc_B desc2 = alg.createFeature();

		alg.setImage(input);
		alg.process(input.width/2,input.height/2,0,briefRadius,desc1);
		alg.process(input.width/2,input.height/2,1,briefRadius,desc2);

		boolean identical = true;
		for( int i = 0; i < desc1.data.length; i++ ) {
			if( desc1.data[i] != desc2.data[i] )
				identical = false;
		}

		assertFalse(identical);
	}

	/**
	 * Checks to see if changing orientation changes the description
	 */
	@Test
	public void testScale() {
		GrayF32 input = createImage(width,height);

		DescribePointBriefSO<GrayF32> alg = createAlg();
		TupleDesc_B desc1 = alg.createFeature();
		TupleDesc_B desc2 = alg.createFeature();

		alg.setImage(input);
		alg.process(input.width/2,input.height/2,0,briefRadius,desc1);
		alg.process(input.width/2,input.height/2,0,2*briefRadius,desc2);

		boolean identical = true;
		for( int i = 0; i < desc1.data.length; i++ ) {
			if( desc1.data[i] != desc2.data[i] )
				identical = false;
		}

		assertFalse(identical);
	}

	/**
	 * Have brief process a sub-image and see if it produces the same results.
	 */
	@Test
	public void testSubImage() {
		GrayF32 input = createImage(width,height);

		DescribePointBriefSO<GrayF32> alg = createAlg();
		TupleDesc_B desc1 = alg.createFeature();
		TupleDesc_B desc2 = alg.createFeature();

		// resize the image and see if it computes the same output
		alg.setImage(input);
		alg.process(input.width/2,input.height/2,0,briefRadius,desc1);

		GrayF32 sub = BoofTesting.createSubImageOf(input);

		alg.setImage(sub);
		alg.process(input.width/2,input.height/2,0,briefRadius,desc2);

		for( int i = 0; i < desc1.data.length; i++ ) {
			assertEquals(desc1.data[i],desc2.data[i]);
		}
	}

	/**
	 * Change the input image size and see if it handles that case properly.
	 */
	@Test
	public void changeInInputSize() {
		GrayF32 inputA = createImage(width,height);
		GrayF32 inputB = createImage(width-5,height-5);

		DescribePointBriefSO<GrayF32> alg = createAlg();
		TupleDesc_B desc = alg.createFeature();

		alg.setImage(inputA);
		alg.process(inputA.width/2,inputA.height/2,0,briefRadius,desc);

		// just see if it blows up or not
		alg.setImage(inputB);
		alg.process(inputA.width/2,inputA.height/2,0,briefRadius,desc);
	}

	/**
	 * Vary the intensity of the input image and see if the description changes.
	 */
	@Test
	public void testIntensityInvariance() {
		GrayF32 input = createImage(width,height);
		GrayF32 mod = input.clone();

		GPixelMath.multiply(input, 2, mod);

		DescribePointBriefSO<GrayF32> alg = createAlg();

		TupleDesc_B desc1 = alg.createFeature();
		TupleDesc_B desc2 = alg.createFeature();

		// compute the image from the same image but different intensities
		alg.setImage(input);
		alg.process(input.width/2,input.height/2,0,briefRadius,desc1);

		alg.setImage(mod);
		alg.process(input.width/2,input.height/2,0,briefRadius,desc2);

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
		GrayF32 input = createImage(width,height);
		GrayF32 blurred = input.createNew(width, height);
		filterBlur.process(input,blurred);

		GImageGray a = FactoryGImageGray.wrap(blurred);

		DescribePointBriefSO<GrayF32> alg = createAlg();

		alg.setImage(input);

		int c_x = input.width/2;
		int c_y = input.height/2;

		TupleDesc_B desc = alg.createFeature();
		alg.process(c_x,c_y,0,briefRadius,desc);

		double s = briefRadius/BoofDefaults.BRIEF_SCALE_TO_RADIUS;

		for( int i = 0; i < def.compare.length; i++ ) {
			Point2D_I32 c = def.compare[i];
			Point2D_I32 p0 = def.samplePoints[c.x];
			Point2D_I32 p1 = def.samplePoints[c.y];

			boolean expected = a.get((int)Math.round(c_x+p0.x*s),(int)Math.round(c_y+p0.y*s)).doubleValue()
					< a.get((int)Math.round(c_x+p1.x*s),(int)Math.round(c_y+p1.y*s)).doubleValue();
			assertTrue(expected == desc.isBitTrue(i));
		}
	}

	/**
	 * See if it handles the image border correctly.
	 */
	@Test
	public void checkBorder() {
		GrayF32 input = createImage(width,height);

		DescribePointBriefSO<GrayF32> alg = createAlg();

		alg.setImage(input);

		TupleDesc_B desc = alg.createFeature();

		// part of sanity check
		assertTrue(desc.data[0] == 0 );

		// just see if it blows up for now.  a more rigorous test would be better
		alg.process(0, 0, 0.1f , briefRadius*1.2f,desc);
		alg.process(width - 1, height - 1, 0.1f, briefRadius*1.2f, desc);

		// sanity check.  the description should not be zero
		assertTrue(desc.data[0] != 0 );
	}
}
