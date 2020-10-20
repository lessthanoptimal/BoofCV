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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestDetectChessboardCornersX extends BoofStandardJUnit {
	int width = 20;
	int height = 15;

	@Test
	void checkPositiveInside() {
		DetectChessboardCornersX alg = new DetectChessboardCornersX();
		GrayF32 intensity = alg._intensity = alg.intensityRaw;
		intensity.reshape(width,height);
		alg.nonmaxThreshold = 1e-6f;
		ImageMiscOps.fill(intensity,-1);

		int cx = 7,cy = 8;

		ImageMiscOps.fillRectangle(intensity,1,cx,cy,2,2);
		assertTrue(alg.checkPositiveInside(cx,cy,4));

		intensity.set(cx,cy,0);
		assertFalse(alg.checkPositiveInside(cx,cy,4));
	}

	@Test
	void checkNegativeInside() {
		DetectChessboardCornersX alg = new DetectChessboardCornersX();
		GrayF32 intensity = alg._intensity = alg.intensityRaw;
		intensity.reshape(width,height);
		alg.nonmaxThreshold = 1e-6f;
		ImageMiscOps.fill(intensity,-1);

		int cx = 7,cy = 8;

		ImageMiscOps.fillRectangle(intensity,1,cx,cy,2,2);
		assertTrue(alg.checkNegativeInside(cx,cy,6));

		// fill everything with positive values
		ImageMiscOps.fillRectangle(intensity,1,cx-3,cy-3,7,7);
		assertFalse(alg.checkNegativeInside(cx,cy,6));
	}

	// There are 3 other inner functions that should be explicitly tested. Being lazy since the detector
	// as a whole works well.
	//
	// computeFeatures()
	// checkCorner()
	// checkChessboardCircle()
}
