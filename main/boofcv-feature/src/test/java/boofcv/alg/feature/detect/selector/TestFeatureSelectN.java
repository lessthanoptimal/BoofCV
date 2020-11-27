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

package boofcv.alg.feature.detect.selector;

import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
class TestFeatureSelectN extends ChecksFeatureSelectLimit.I16 {
	@Override
	public FeatureSelectN<Point2D_I16> createAlgorithm() {
		return new FeatureSelectN<>();
	}

	/**
	 * The order should not change
	 */
	@Test
	void checkOrder() {
		DogArray<Point2D_I16> detected = createRandom(15);

		var found = new FastArray<>(Point2D_I16.class);
		FeatureSelectN<Point2D_I16> alg = createAlgorithm();
		alg.select(width, height, null, detected, 10, found);

		assertEquals(10, found.size);
		for (int i = 0; i < found.size; i++) {
			int matchIdx = detected.indexOf(found.get(i));
			assertSame(found.get(i), detected.get(matchIdx));
		}
	}
}
