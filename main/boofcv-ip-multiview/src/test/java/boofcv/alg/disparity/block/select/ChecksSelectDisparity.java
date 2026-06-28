/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Basic tests for selecting disparity with a correlation score
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
	GrayF32 score = new GrayF32(1, 1);

	DisparitySelect<ArrayData, D> alg;

	protected ChecksSelectDisparity( Class<ArrayData> arrayType, Class<D> disparityType ) {

		this.arrayType = arrayType;
		disparity = GeneralizedImageOps.createSingleBand(disparityType, w, h);
		score.reshape(w, h);


		alg = createAlg();
	}

	public abstract DisparitySelect<ArrayData, D> createAlg();

	void init( int minDisparity, int maxDisparity, int radius ) {
		this.radius = radius;
		this.minDisparity = minDisparity;
		this.maxDisparity = maxDisparity;
		this.rangeDisparity = maxDisparity - minDisparity + 1;
	}

	/// Give it a handcrafted score with known results for WTA. See if it produces those results
	@Test void simpleTest() {
		simpleTest(0, 10, 2);
		simpleTest(2, 10, 2);
		simpleTest(4, 11, 3);

		simpleTest(-2, 10, 2);
		simpleTest(-4, 11, 3);
		simpleTest(-7, 3, 2);

		// it only considers negative disparities here
		simpleTest(-8, -2, 2);
	}

	/// See if it blows up if you feed it a null disparity
	@Test void checkExplodeNullDisparity() {
		init(2, 10, 2);

		alg.configure(disparity, null, minDisparity, maxDisparity, radius);

		int[] scores = new int[w*rangeDisparity];
		alg.process(3, copyToCorrectType(scores));
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

	/// @param disparityLow Lowest disparity it will consider, inclusive
	/// @param disparityUpp Upper most disparity it will consider, inclusive
	void simpleTest( int disparityLow, int disparityUpp, int radius ) {
		init(disparityLow, disparityUpp, radius);

		int y = 3;

		// TODO is radius tested? before range was being passed in instead of radius and everything passed.
		GImageMiscOps.fill(disparity, 0);
		alg.configure(disparity, score, disparityLow, disparityUpp, radius);

		int[] scores = new int[w*rangeDisparity];

		// scores are stored at the absolute column within each disparity's row
		for (int d = 0; d < rangeDisparity; d++) {
			for (int x = 0; x < w; x++) {
				scores[w*d + x] = convertErrorToScore(Math.abs(d - 5));
			}
		}

		alg.process(y, copyToCorrectType(scores));

		// Consider each column in the output disparity image
		for (int leftCol = 0; leftCol < w; leftCol++) {
			// upper and lowest column in right image it will consider
			int rightColUp = leftCol - disparityLow;  // inclusive
			int rightColLo = leftCol - disparityUpp;  // exclusive

			// If it's impossible to compute disparity at this location then it will be marked as invalid
			if (rightColUp < 0 || rightColLo >= w) {
				assertEquals(rangeDisparity, GeneralizedImageOps.get(disparity, leftCol, y), 1e-8);
				continue;
			}

			// number of disparity values it will consider, ignoring right clamp
			int ddHi = rightColUp - Math.max(0, rightColLo);
			int ddLo = Math.max(0, rightColUp - (w - 1));   // right-border clamp
			int expected = Math.max(ddLo, Math.min(ddHi, 5));       // index 5 clamped into [ddLo, ddHi]

			assertEquals(expected, GeneralizedImageOps.get(disparity, leftCol, y), 1e-8, "col=" + leftCol);
		}
	}

	public abstract int convertErrorToScore( int d );
}
