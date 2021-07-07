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

package boofcv.alg.disparity.sgm;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericSgmStereoDisparityChecks<T extends ImageGray<T>, C extends ImageBase<C>>
		extends CommonSgmChecks<T> {
	// test using a random image
	boolean useRandomImage = true;
	double acceptTol = 0.01;

	protected GenericSgmStereoDisparityChecks( ImageType<T> imageType ) {
		super(80, 60, imageType);
	}

	public abstract SgmStereoDisparity<T, C> createAlgorithm();

	/**
	 * Input images are identical. Disparity should be zero
	 */
	@Test void identicalImages() {
		int rangeD = 10;
		renderStereoRandom(0, 255, 0, rangeD);

		SgmStereoDisparity<T, C> alg = createAlgorithm();

		alg.setDisparityMin(0);
		alg.setDisparityRange(rangeD);
		alg.process(left, right);
		GrayU8 disparity = alg.getDisparity();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(0, disparity.get(x, y));
			}
		}
	}

	/**
	 * Adjust the disparity search and see if it succeeds and fails when it should
	 */
	@Test void disparitySearch() {
		SgmStereoDisparity<T, C> alg = createAlgorithm();
		// When you include the diagonal paths it's harder to predict what's going on
		alg.getAggregation().setPathsConsidered(4);
		alg.getSelector().setRightToLeftTolerance(-1);

		disparitySearch(0, 20, 6, alg);
		disparitySearch(0, 15, 19, alg);
		disparitySearch(6, 15, 6, alg);
		disparitySearch(7, 15, 6, alg);
		disparitySearch(8, 15, 6, alg);
	}

	public void disparitySearch( int disparityMin, int disparityRange, int disparityActual, SgmStereoDisparity<T, C> alg ) {

		SgmHelper helper = new SgmHelper();
		helper.width = width;
		helper.disparityMin = disparityMin;
		helper.disparityRange = disparityRange;

		if (useRandomImage)
			renderStereoRandom(0, 255, disparityActual, disparityRange);
		else
			renderStereoStep(disparityActual, disparityRange);

		alg.setDisparityMin(disparityMin);
		alg.setDisparityRange(disparityRange);

		alg.process(left, right);
		GrayU8 found = alg.getDisparity();

		int correct = 0;
		int total = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < disparityMin; x++) {
				if (found.get(x, y) == disparityRange)
					correct++;
			}
			total += disparityMin;
			for (int x = disparityMin; x < width; x++) {
				int localRangeD = helper.localDisparityRangeLeft(x);
				if (localRangeD > disparityActual) {
					if (disparityTruth.get(x, y) == found.get(x, y) + disparityMin) {
						correct++;
					}
					total++;
				}
			}
		}

		boolean solvable = disparityActual >= disparityMin && disparityActual < disparityMin + disparityRange;
		double fractionCorrect = correct/(double)(total + 1E-8);
		if (solvable)
			assertTrue(fractionCorrect > 1.0 - acceptTol, "diff " + fractionCorrect);
		else
			assertTrue(fractionCorrect < 0.15, "diff " + fractionCorrect);
	}
}
