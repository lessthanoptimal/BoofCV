/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block.select;

import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Basic tests for selecting disparity with a correlation score
 *
 * @author Peter Abeles
 */
@SuppressWarnings("WeakerAccess")
public abstract class ChecksSelectDisparity<ArrayData, D extends ImageGray<D>> extends BoofStandardJUnit {

	Class<ArrayData> arrayType;

	int w = 20;
	int h = 25;
	int radius;
	int minDisparity;
	int maxDisparity;
	int rangeDisparity;

	D disparity;

	DisparitySelect<ArrayData, D> alg;

	protected ChecksSelectDisparity( Class<ArrayData> arrayType, Class<D> disparityType ) {

		this.arrayType = arrayType;
		disparity = GeneralizedImageOps.createSingleBand(disparityType, w, h);

		alg = createAlg();
	}

	public abstract DisparitySelect<ArrayData, D> createAlg();

	void init( int minDisparity, int maxDisparity, int radius ) {
		this.radius = radius;
		this.minDisparity = minDisparity;
		this.maxDisparity = maxDisparity;
		this.rangeDisparity = maxDisparity - minDisparity + 1;
	}

	/**
	 * Give it a hand crafted score with known results for WTA. See if it produces those results
	 */
	@Test
	void simpleTest() {
		simpleTest(0, 10, 2);
		simpleTest(2, 10, 2);
		simpleTest(4, 11, 3);
	}

	protected ArrayData copyToCorrectType( int[] scores ) {
		return copyToCorrectType(scores, arrayType);
	}

	static <ArrayData> ArrayData copyToCorrectType( int[] scores, Class<ArrayData> arrayType ) {

		if (arrayType == int[].class)
			return (ArrayData)scores;

		float[] ret = new float[scores.length];

		for (int i = 0; i < scores.length; i++) {
			ret[i] = scores[i];
		}

		return (ArrayData)ret;
	}

	void simpleTest( int minDisparity, int maxDisparity, int radius ) {
		init(minDisparity, maxDisparity, radius);

		int y = 3;

		GImageMiscOps.fill(disparity, 0);
		alg.configure(disparity, minDisparity, maxDisparity, rangeDisparity);

		int[] scores = new int[w*rangeDisparity];

		for (int d = 0; d < rangeDisparity; d++) {
			for (int x = 0; x < w - minDisparity; x++) {
				scores[w*d + x] = convertErrorToScore(Math.abs(d - 5));
			}
		}

		alg.process(y, copyToCorrectType(scores));

		// When x is less than min disparity it should be zero
		for (int i = 0; i < minDisparity; i++)
			assertEquals(rangeDisparity, GeneralizedImageOps.get(disparity, i, y), 1e-8);

		// should ramp up to 5
		for (int i = minDisparity; i < minDisparity + 5; i++)
			assertEquals(i - minDisparity, GeneralizedImageOps.get(disparity, i, y), 0.99);
		// should be at 5 for the remainder
		for (int i = minDisparity + 5; i < w; i++)
			assertEquals(5, GeneralizedImageOps.get(disparity, i, y), 0.99);
	}

	public abstract int convertErrorToScore( int d );
}
