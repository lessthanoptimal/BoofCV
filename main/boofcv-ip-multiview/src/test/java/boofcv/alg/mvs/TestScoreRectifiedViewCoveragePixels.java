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

package boofcv.alg.mvs;

import boofcv.alg.distort.DoNothingPixelTransform_F64;
import boofcv.alg.distort.PixelTransformAffine_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestScoreRectifiedViewCoveragePixels extends BoofStandardJUnit {
	// input default image shape
	int width = 1000;
	int height = 1500;

	/** Put it all together and test using simple examples */
	@Test void everything() {
		var alg = new ScoreRectifiedViewCoveragePixels();
		alg.maxSide = 30;
		// do nothing to make the math easier to hand compute
		alg.initialize(width, height, new DoNothingPixelTransform_F64());

		// add a view that's half the size and rectification is simply identity (no change in pixels)
		alg.addView(width/2, height, CommonOps_DDRM.diag(1, 1, 1), 1.0f, Float::max);

		// compute for just this one view
		alg.process();
		// coverage should be 0.5 and average 1.0
		double expected0 = 0.5*(alg.scoreAverageOffset + 300.0/(1.0 + 300.0));
		assertEquals(expected0, alg.getScore(), UtilEjml.TEST_F64);
		assertEquals(0.5, alg.getCovered(), UtilEjml.TEST_F64);

		// Add another image which will cover everything
		alg.addView(width, height, CommonOps_DDRM.diag(1, 1, 1), 1.0f, Float::sum);
		alg.process();
		double expected1 = 1.0*(alg.scoreAverageOffset + 900/(1.0 + 600.0));
		assertEquals(expected1, alg.getScore(), UtilEjml.TEST_F64);
	}

	@Test void initialize() {
		var pixel_to_undst = new PixelTransformAffine_F64(new Affine2D_F64(1, 0, 0, 1, -2, 0));

		var alg = new ScoreRectifiedViewCoveragePixels();
		alg.maxSide = 30;
		alg.initialize(width, height, pixel_to_undst);

		assertEquals(30.0/height, alg.scale, UtilEjml.TEST_F64);
		assertEquals(20, alg.viewed.width);
		assertEquals(30, alg.viewed.height);
		assertEquals(20*30, alg.pixel_to_undist.size);

		for (int y = 0; y < 30; y++) {
			int scaledY = (int)(y*height/30.0);
			for (int x = 0; x < 20; x++) {
				int scaledX = (int)(x*height/30.0);
				Point2D_F64 found = alg.pixel_to_undist.get(y*20 + x);
				assertEquals(scaledX - 2, found.x, UtilEjml.TEST_F64);
				assertEquals(scaledY, found.y, UtilEjml.TEST_F64);
			}
		}
	}

	@Test void addView() {
		var alg = new ScoreRectifiedViewCoveragePixels();
		alg.maxSide = 30;
		alg.initialize(width, height, new DoNothingPixelTransform_F64());
		// set one pixels to -1 and it should not be updated
		alg.viewed.set(0, 0, -1);

		// This will scale x-coordinate by a factor of two and leave y as is
		DMatrixRMaj rect = CommonOps_DDRM.diag(2.0, 1.0, 1.0);

		// Call the function being tested. Note that the height is smaller
		alg.addView(width, 1000, rect, 0.5f, Float::sum);

		for (int y = 0; y < 30; y++) {
			for (int x = 0; x < 20; x++) {
				float found = alg.viewed.unsafe_get(x, y);
				if (x == 0 && y == 0) {
					assertEquals(-1, found);
				} else if (x < 10 && y < 20) {
					// These should all be updated since they are within the small view AND 1/2 the x-axis resolution
					assertEquals(0.5f, found);
				} else {
					assertEquals(0, found);
				}
			}
		}
	}

	@Test void fractionIntersection() {
		// simplify the math
		width = 30;
		height = 30;
		var pixel_to_undist = new PixelTransformAffine_F64(new Affine2D_F64(1, 0, 0, 1, 0, 0));

		var alg = new ScoreRectifiedViewCoveragePixels();
		alg.maxSide = 30;
		alg.initialize(width, height, pixel_to_undist);

		DMatrixRMaj rectification = new DMatrixRMaj(3,3,true,1,0,-2,0,1,1,0,0,1);

		// Fraction that was moved out of the image due to rectification
		double expected = ((alg.maxSide-2)*(alg.maxSide-1))/(double)(alg.maxSide*alg.maxSide);
		assertEquals(expected,alg.fractionIntersection(width,height,rectification), UtilEjml.TEST_F64);
	}
}
