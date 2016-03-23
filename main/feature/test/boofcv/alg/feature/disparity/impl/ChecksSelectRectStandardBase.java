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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.feature.disparity.SelectRectStandard;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for implementers of {@link SelectRectStandardBase_S32}
 *
 * @author Peter Abeles
 */
public abstract class ChecksSelectRectStandardBase<ArrayData,T extends ImageGray> {

	Class<ArrayData> arrayType;

	int w=20;
	int h=25;
	int minDisparity=0;
	int maxDisparity=10;
	int reject;

	T disparity;
	Class<T> disparityType;

	public ChecksSelectRectStandardBase(Class<ArrayData> arrayType, Class<T> disparityType) {
		this.disparityType = disparityType;
		this.arrayType = arrayType;
		disparity = GeneralizedImageOps.createSingleBand(disparityType,w,h);
	}

	void init( int min , int max ) {
		this.minDisparity = min;
		this.maxDisparity = max;
		this.reject = (max-min)+1;
		GImageMiscOps.fill(disparity, reject);
	}

	public abstract SelectRectStandard<ArrayData,T> createSelector( int maxError, int rightToLeftTolerance, double texture );

	@Test
	public void basic() {
		BasicDisparitySelectRectTests<ArrayData,T> test =
				new BasicDisparitySelectRectTests<ArrayData,T>(arrayType, disparityType) {
					@Override
					public DisparitySelect<ArrayData,T> createAlg() {
						return createSelector(-1, -1, -1);
					}
				};

		test.allTests();
	}

	@Test
	public void maxError() {
		init(0,10);

		int y = 3;

		SelectRectStandard<ArrayData,T> alg = createSelector(2,-1,-1);
		alg.configure(disparity,0,maxDisparity,2);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = d==0 ? 5 : x;
			}
		}

		alg.process(y, copyToCorrectType(scores,arrayType));

		// Below error threshold and disparity of 1 should be optimal
		assertEquals(1, getDisparity( 1 + 2, y), 1e-8);
		assertEquals(1, getDisparity(2 + 2, y), 1);
		// At this point the error should become too high
		assertEquals(reject, getDisparity(3 + 2, y), 1e-8);
		assertEquals(reject, getDisparity(4 + 2, y), 1e-8);

		// Sanity check, much higher error threshold
		alg = createSelector(20,-1,-1);
		alg.configure(disparity,0,maxDisparity,2);
		alg.process(y,copyToCorrectType(scores,arrayType));
		assertEquals(1, getDisparity( 3+2, y), 1);
		assertEquals(1, getDisparity(4 + 2, y), 1);
	}

	/**
	 * Could potentially return a sub-pixel accuracy but tests are only for pixel accuracy.
	 *
	 * Will not work in all situations since the movement could be farther than 0.5 from
	 * the "correct" value
	 */
	private int getDisparity( int x , int y ) {
		double value = GeneralizedImageOps.get(disparity, x, y);
		return (int)Math.round(value);
	}

	/**
	 * Similar to simpleTest but takes in account the effects of right to left validation
	 */
	@Test
	public void testRightToLeftValidation() {

		rightToLeftValidation(0);
		rightToLeftValidation(2);
	}

	private void rightToLeftValidation( int minDisparity ) {
		init( minDisparity , 10 );

		int y = 3;
		int r = 2;

		SelectRectStandard<ArrayData,T> alg = createSelector(-1,1,-1);
		alg.configure(disparity,minDisparity,maxDisparity,r);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = Math.abs(d-5);
			}
		}

		alg.process(y,copyToCorrectType(scores,arrayType));

		// outside the border should be maxDisparity+1
		for( int i = 0; i < r+minDisparity; i++ )
			assertEquals(reject, getDisparity(i + r, y), 1e-8);
		// These should all be zero since other pixels will have lower scores
		for( int i = r+minDisparity; i < r+4+minDisparity; i++ )
			assertEquals(reject, getDisparity(i, y), 1e-8);
		// the tolerance is one, so this should be 4
		assertEquals(4, getDisparity(4 + r + minDisparity, y), 1e-8);
		// should be at 5 for the remainder
		for( int i = r+minDisparity+5; i < w-r; i++ )
			assertEquals("i = "+i,5, getDisparity(i, y), 1e-8);

		// sanity check, I now set the tolerance to zero
		alg = createSelector(-1,0,-1);
		alg.configure(disparity,minDisparity,maxDisparity,2);
		alg.process(y,copyToCorrectType(scores,arrayType));
		assertEquals(reject, getDisparity(4 + r + minDisparity, y), 1e-8);
	}

	/**
	 * Test the confidence in a region with very similar cost score (little texture)
	 */
	@Test
	public void confidenceFlatRegion() {
		init( 0,10 );
		int minValue = 3;
		int y = 3;

		SelectRectStandard<ArrayData,T> alg = createSelector(-1,-1,3);
		alg.configure(disparity,0,maxDisparity,2);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = minValue + Math.abs(2-d);
			}
		}

		alg.process(y,copyToCorrectType(scores,arrayType));

		// it should reject the solution
		assertEquals(reject, getDisparity(4 + 2, y), 1e-8);
	}

	/**
	 * There are two similar peaks.  Repeated pattern
	 */
	@Test
	public void confidenceMultiplePeak() {
		confidenceMultiplePeak(3,0);
		confidenceMultiplePeak(0,0);
		confidenceMultiplePeak(3,2);
		confidenceMultiplePeak(0,2);
	}

	private void confidenceMultiplePeak(int minValue , int minDisparity) {
		init(minDisparity,10);
		int y = 3;
		int r = 2;

		SelectRectStandard<ArrayData,T> alg = createSelector(-1,-1,3);
		alg.configure(disparity,minDisparity,maxDisparity,r);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = minValue + (d % 3);
			}
		}

		alg.process(y,copyToCorrectType(scores,arrayType));

		// it should reject the solution
		for( int i = r+minDisparity+3; i < w-r; i++)
			assertEquals("i = "+i,reject, getDisparity(i, y), 1e-8);
	}

	public static <ArrayData> ArrayData copyToCorrectType( int scores[] , Class<ArrayData> arrayType ) {

		if( arrayType == int[].class )
			return (ArrayData)scores;

		float[] ret = new float[ scores.length ];

		for( int i = 0; i < scores.length; i++ ) {
			ret[i] = scores[i];
		}

		return (ArrayData)ret;
	}
}
