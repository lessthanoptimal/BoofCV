/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block.score;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Provides a series of simple tests that check basic functionality at computing image disparity
 *
 * @author Peter Abeles
 */
public abstract class BasicDisparityTests<I extends ImageGray<I>, DI extends ImageGray<DI>>
		extends BoofStandardJUnit {
	I left;
	I right;

	// image size
	int w = 50;
	int h = 60;

	// minimum and maximum pixel intensity
	double minVal, maxVal;

	int maxDisparity = 40;

	Random rand = new Random();

	BasicDisparityTests( double minVal, double maxVal, Class<I> imageType ) {
		this.minVal = minVal;
		this.maxVal = maxVal;
		left = GeneralizedImageOps.createSingleBand(imageType, w, h);
		right = GeneralizedImageOps.createSingleBand(imageType, w, h);
	}

	public abstract void initialize( int minDisparity, int maxDisparity );

	public abstract DI computeDisparity( I left, I right );

	/**
	 * A random image is generated then it's shifted by a fixed amount into the right image
	 */
	@Test
	void checkShifted() {
		initialize(0, maxDisparity);

		int disparity = 5;

		GImageMiscOps.fill(right, 0);
		GImageMiscOps.fillUniform(left, rand, minVal, maxVal);
		GImageMiscOps.copy(disparity, 0, 0, 0, left.width - disparity, left.height, left, right);

		DI output = computeDisparity(left, right);

		assertTrue(checkSolution(disparity, 0, output, 0.12));
	}

	/**
	 * Set the minimum disparity to a non-zero value and see if it has the expected results
	 */
	@Test
	void checkMinimumDisparity() {
		int disparity = 4;
		int minDisparity = disparity + 2;
		initialize(disparity + 2, maxDisparity);

		GImageMiscOps.fillUniform(left, rand, minVal, maxVal);
		GImageMiscOps.copy(disparity, 0, 0, 0, left.width - disparity, left.height, left, right);

		DI output = computeDisparity(left, right);

		// this should fail because the motion is less than the minimum disparity
		assertFalse(checkSolution(disparity, minDisparity, output, 0.8));
	}

	private boolean checkSolution( int expected, int minDisparity, DI output, double fraction ) {
		int total = 0;
		int errors = 0;
		for (int y = 0; y < h; y++) {
			// pixel's less than min disparity should be marked as invalid since there's nothing to compare against
			for (int x = 0; x < minDisparity; x++) {
				double found = GeneralizedImageOps.get(output, x, y);
				assertEquals(maxDisparity - minDisparity + 1, found, 1e-8);
			}
			for (int x = minDisparity; x < w; x++) {
				double found = GeneralizedImageOps.get(output, x, y) + minDisparity;
				// the minimum disparity should  be the closest match
				if (Math.abs(expected - found) > 1e-8)
					errors++;
				total++;
			}
		}

//		System.out.println(errors+" vs "+total);
		// Inputs are random so the match might not be perfect, especially around border regions
		return (errors/(double)total <= fraction);
	}
}
