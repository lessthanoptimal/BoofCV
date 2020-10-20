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

package boofcv.alg.disparity.sgm;

import boofcv.alg.disparity.sgm.cost.StereoMutualInformation;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestSgmStereoDisparityHmi extends GenericSgmStereoDisparityChecks<GrayU8, GrayU8> {

	protected TestSgmStereoDisparityHmi() {
		super(ImageType.SB_U8);
		// HMI doesn't excel in these scenarios as much as the others
		this.acceptTol = 0.15;
		// HMI needs more structure to work well
		this.useRandomImage = false;
	}

	@Override
	public SgmStereoDisparity<GrayU8, GrayU8> createAlgorithm() {
		return create();
	}

	/**
	 * No pyramid is required where. MI is initialized with perfect disparity
	 */
	@Test
	void perfect_GivenTrueDisparity() {
		int rangeD = 12;
		int d = 7;

		// Render an image with a smooth gradient. If given a perfect initial disparity
		// it should produce a perfect output. If given a random disparity there's a good chance
		// it would converge to an incorrect solution
		renderStereoStep(d, rangeD);

		SgmStereoDisparityHmi alg = create();
		alg.setDisparityMin(0);
		alg.setDisparityRange(rangeD);
		alg.process(left, right, disparityTruth, rangeD);

		// disparity should be 5 everywhere
		GrayU8 found = alg.getDisparity();
		evaluateFound(rangeD, d, found);
	}

	private void evaluateFound( int rangeD, int d, GrayU8 found ) {
		int errorSum = 0;
		int errorMax = 0;
		int totalFailed = 0;
		int totalSuccess = 0;
		for (int y = 0; y < height; y++) {
			// only check where the actual solution can be found
			for (int x = d; x < width; x++) {
				int f = found.get(x, y);
				if (f >= rangeD) {
					totalFailed++;
					continue;
				} else {
					totalSuccess++;
				}
				int errorAbs = Math.abs(d - f);
				errorSum += errorAbs;
				errorMax = Math.max(errorMax, errorAbs);
//				assertEquals(d,found.get(x,y), x+" "+y);
			}
		}
		assertEquals(0, totalFailed);
		double errorAve = errorSum/(double)totalSuccess;
		assertTrue(errorAve < 0.2, "ave error " + errorAve);
//		assertTrue(errorMax<2,"max error "+errorMax);
	}

	SgmStereoDisparityHmi create() {
		StereoMutualInformation stereoMI = new StereoMutualInformation();
		stereoMI.configureSmoothing(3);
		stereoMI.configureHistogram(256);
		SgmDisparitySelector selector = new SgmDisparitySelector();
		selector.setRightToLeftTolerance(-1);
		return new SgmStereoDisparityHmi(new ConfigDiscreteLevels(-1, 20, 20), stereoMI, selector);
	}
}
