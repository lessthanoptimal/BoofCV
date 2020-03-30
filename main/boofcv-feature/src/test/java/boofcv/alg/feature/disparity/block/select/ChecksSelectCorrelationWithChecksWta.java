/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.block.select;

import boofcv.alg.feature.disparity.block.DisparitySelect;
import boofcv.alg.feature.disparity.block.SelectDisparityWithChecksWta;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for implementers of {@link SelectDisparityWithChecksWta}
 *
 * @author Peter Abeles
 */
@SuppressWarnings("WeakerAccess")
public abstract class ChecksSelectCorrelationWithChecksWta<ArrayData,T extends ImageGray<T>>
		extends ChecksBasicSelectDisparity.ScoreCorrelation<ArrayData,T>
{

	int w=20;
	int h=25;
	int minDisparity;
	int maxDisparity;
	int rangeDisparity;
	int reject;

	T disparity;

	public ChecksSelectCorrelationWithChecksWta(Class<ArrayData> arrayType, Class<T> disparityType) {
		super(arrayType, disparityType);
		this.arrayType = arrayType;
		disparity = GeneralizedImageOps.createSingleBand(disparityType,w,h);
	}

	void init( int min , int max ) {
		this.minDisparity = min;
		this.maxDisparity = max;
		this.rangeDisparity = max-min+1;
		this.reject = rangeDisparity;
		GImageMiscOps.fill(disparity, rangeDisparity);
	}

	public abstract SelectDisparityWithChecksWta<ArrayData,T> createSelector(int rightToLeftTolerance, double texture );

	@Override
	public DisparitySelect<ArrayData, T> createAlg() {
		return createSelector(-1,-1);
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
	void testRightToLeftValidation() {
		rightToLeftValidation(0);
		rightToLeftValidation(2);
	}

	private void rightToLeftValidation( int minDisparity ) {
		init( minDisparity , 10 );

		int y = 3;
		int r = 2;

		SelectDisparityWithChecksWta<ArrayData,T> alg = createSelector(1,-1);
		alg.configure(disparity,minDisparity,maxDisparity,r);

		int[] scores = new int[w*rangeDisparity];

		for( int d = 0; d < rangeDisparity; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = computeError(d);
			}
		}

		alg.process(y,copyToCorrectType(scores,arrayType));

		// Less than the minimum disparity should be reject
		for( int i = 0; i < minDisparity; i++ )
			assertEquals(reject, getDisparity(i + r, y), 1e-8);

		// These should all be zero since other pixels will have lower scores
		for( int i = minDisparity; i < 4+minDisparity; i++ )
			assertEquals(reject, getDisparity(i, y), 1e-8);

		// the tolerance is one, so this should be 4
		assertEquals(4, getDisparity(4 + minDisparity, y), 1e-8);
		// should be at 5 for the remainder
		for( int i = minDisparity+5; i < w-r; i++ )
			assertEquals(5, getDisparity(i, y), 1e-8);

		// sanity check, I now set the tolerance to zero
		alg = createSelector(0,-1);
		alg.configure(disparity,minDisparity,maxDisparity,2);
		alg.process(y,copyToCorrectType(scores,arrayType));
		assertEquals(reject, getDisparity(4 + minDisparity, y), 1e-8);
	}

	/**
	 * Test the confidence in a region with very similar cost score (little texture)
	 */
	@Test
	void confidenceFlatRegion() {
		init( 0,10 );
		int minValue = 3;
		int y = 3;

		SelectDisparityWithChecksWta<ArrayData,T> alg = createSelector(-1,0.2);
		alg.configure(disparity,minDisparity,maxDisparity,2);

		int[] scores = new int[w*rangeDisparity];

		for( int d = 0; d < rangeDisparity; d++ ) {
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
	void confidenceMultiplePeak() {
		confidenceMultiplePeak(3,0);
		confidenceMultiplePeak(0,0);
		confidenceMultiplePeak(3,2);
		confidenceMultiplePeak(0,2);
	}

	private void confidenceMultiplePeak(int minValue , int minDisparity) {
		init(minDisparity,10);
		int y = 3;
		int r = 2;

		SelectDisparityWithChecksWta<ArrayData,T> alg = createSelector(-1,0.2);
		alg.configure(disparity,minDisparity,maxDisparity,r);

		int[] scores = new int[w*rangeDisparity];

		for( int d = 0; d < rangeDisparity; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = minValue + (d % 3);
			}
		}

		alg.process(y,copyToCorrectType(scores,arrayType));

		// it should reject the solution
		for( int i = r+minDisparity+3; i < w-r; i++)
			assertEquals(reject, getDisparity(i, y), 1e-8);
	}

	static <ArrayData> ArrayData copyToCorrectType( int scores[] , Class<ArrayData> arrayType ) {

		if( arrayType == int[].class )
			return (ArrayData)scores;

		float[] ret = new float[ scores.length ];

		for( int i = 0; i < scores.length; i++ ) {
			ret[i] = scores[i];
		}

		return (ArrayData)ret;
	}
}
