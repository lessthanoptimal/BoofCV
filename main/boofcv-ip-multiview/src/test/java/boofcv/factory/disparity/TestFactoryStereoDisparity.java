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

package boofcv.factory.disparity;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.abst.disparity.StereoDisparitySparse;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Checks that configuration reaches the constructed algorithm intact.
class TestFactoryStereoDisparity extends BoofStandardJUnit {
	int width = 60, height = 50;

	/// A fractional maxPerPixelError, which is what you get with images normalized to have a maximum pixel
	/// value of 1, has to survive the trip to the selector. It used to be truncated to an int on the way,
	/// which silently rounded any limit below 1.0 down to zero and disabled the check entirely.
	@Test void blockMatch_fractionalMaxPerPixelError() {
		var left = new GrayF32(width, height);
		var right = new GrayF32(width, height);

		// Normalized images. The right image is unrelated to the left, so every block is a bad match and
		// a tight error limit has to reject it.
		GImageMiscOps.fillUniform(left, rand, 0.0, 1.0);
		GImageMiscOps.fillUniform(right, rand, 0.0, 1.0);

		// Region is 5x5, so the summed error limit is 25*0.02 = 0.5. Truncating that to an int gives 0,
		// which the selector reads as "disabled".
		int invalidTight = countInvalid(left, right, 0.02);

		// Same configuration with the check explicitly disabled
		int invalidDisabled = countInvalid(left, right, -1);

		assertEquals(0, invalidDisabled);
		assertTrue(invalidTight > 0, "Fractional maxPerPixelError was ignored");
	}

	private int countInvalid( GrayF32 left, GrayF32 right, double maxPerPixelError ) {
		var config = new ConfigDisparityBM();
		config.errorType = DisparityError.SAD;
		config.disparityMin = 0;
		config.disparityRange = 5;
		config.regionRadiusX = config.regionRadiusY = 2;
		config.texture = 0;
		config.validateRtoL = -1;
		config.subpixel = false;
		config.maxPerPixelError = maxPerPixelError;

		StereoDisparity<GrayF32, GrayU8> alg =
				FactoryStereoDisparity.blockMatch(config, GrayF32.class, GrayU8.class);
		alg.process(left, right);

		GrayU8 disparity = alg.getDisparity();
		int invalid = alg.getInvalidValue();

		int total = 0;
		for (int y = 0; y < disparity.height; y++) {
			for (int x = 0; x < disparity.width; x++) {
				if (disparity.get(x, y) >= invalid)
					total++;
			}
		}
		return total;
	}

	/// Same idea for the sparse path, which builds its selector through a different branch of the factory.
	@Test void sparseRectifiedBM_fractionalMaxPerPixelError() {
		var left = new GrayF32(width, height);
		var right = new GrayF32(width, height);

		GImageMiscOps.fillUniform(left, rand, 0.0, 1.0);
		GImageMiscOps.fillUniform(right, rand, 0.0, 1.0);

		assertTrue(countSparseInvalid(left, right, 0.02) > 0, "Fractional maxPerPixelError was ignored");
		assertEquals(0, countSparseInvalid(left, right, -1));
	}

	private int countSparseInvalid( GrayF32 left, GrayF32 right, double maxPerPixelError ) {
		var config = new ConfigDisparityBM();
		config.errorType = DisparityError.SAD;
		config.disparityMin = 0;
		config.disparityRange = 5;
		config.regionRadiusX = config.regionRadiusY = 2;
		config.texture = 0;
		config.validateRtoL = -1;
		config.subpixel = false;
		config.maxPerPixelError = maxPerPixelError;

		StereoDisparitySparse<GrayF32> alg = FactoryStereoDisparity.sparseRectifiedBM(config, GrayF32.class);
		alg.setImages(left, right);

		// Only sample well inside the image so that the block and the full disparity range are available
		int total = 0;
		for (int y = 10; y < height - 10; y++) {
			for (int x = 10; x < width - 10; x++) {
				if (!alg.process(x, y))
					total++;
			}
		}
		return total;
	}
}
