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

package boofcv.alg.scene;

import boofcv.alg.similar.ImageSimilarityAssociatedRatio;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestImageSimilarityAssociatedRatio extends BoofStandardJUnit {

	/**
	 * Test the threshold to make sure it's properly enforced
	 */
	@Test void threshold_relative() {
		var srcPixels = new DogArray<>(Point2D_F64::new);
		var dstPixels = new DogArray<>(Point2D_F64::new);
		var matches = new DogArray<>(AssociatedIndex::new);

		var alg = new ImageSimilarityAssociatedRatio();
		alg.minimum.setRelative(0.75, 50);

		// test to see if ratio is <= or <
		srcPixels.resize(100);
		dstPixels.resize(100);
		matches.resize(75);
		assertTrue(alg.isSimilar(srcPixels, dstPixels, matches));

		// Make sure just the src or dst needs to be below to fail
		srcPixels.resize(101);
		assertFalse(alg.isSimilar(srcPixels, dstPixels, matches));
		srcPixels.resize(100);
		dstPixels.resize(101);
		assertFalse(alg.isSimilar(srcPixels, dstPixels, matches));
	}

	/**
	 * Test the threshold to make sure it's properly enforced
	 */
	@Test void threshold_absolute() {
		var srcPixels = new DogArray<>(Point2D_F64::new);
		var dstPixels = new DogArray<>(Point2D_F64::new);
		var matches = new DogArray<>(AssociatedIndex::new);

		var alg = new ImageSimilarityAssociatedRatio();
		alg.minimum.setFixed(50);

		// test to see if ratio is <= or <
		srcPixels.resize(100);
		dstPixels.resize(100);
		matches.resize(50);
		assertTrue(alg.isSimilar(srcPixels, dstPixels, matches));

		// Make sure just the src or dst needs to be below to fail
		matches.resize(49);
		assertFalse(alg.isSimilar(srcPixels, dstPixels, matches));
		matches.resize(50);
		srcPixels.resize(200);
		dstPixels.resize(200);
		assertTrue(alg.isSimilar(srcPixels, dstPixels, matches));
	}

	/**
	 * Make sure it doesn't blow up if zeros are passed in
	 */
	@Test void divideByZero() {
		var srcPixels = new DogArray<>(Point2D_F64::new);
		var dstPixels = new DogArray<>(Point2D_F64::new);
		var matches = new DogArray<>(AssociatedIndex::new);

		var alg = new ImageSimilarityAssociatedRatio();
		alg.minimum.setRelative(0.5, 0);

		// This should be true by the algorithm because of how naive it is
		assertTrue(alg.isSimilar(srcPixels, dstPixels, matches));
	}
}
