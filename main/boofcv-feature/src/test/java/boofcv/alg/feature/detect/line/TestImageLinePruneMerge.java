/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.line;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.line.LineParametric2D_F32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestImageLinePruneMerge extends BoofStandardJUnit {
	/**
	 * If two lines are parallel it should check to see if they are similar by looking
	 */
	@Test void pruneSimilar_parallel() {
		var alg = new ImageLinePruneMerge();

		// Add two parallel lines. The closest point on the left side of the image
		alg.add(new LineParametric2D_F32(0, 10, 1, 0), 10);
		alg.add(new LineParametric2D_F32(0, 16, 0.99f, 0.01f), 10);

		// Tell it to prune, but they are farther than the tolerance
		alg.pruneSimilar(0.1f, 5, 800, 600);
		assertEquals(2, alg.lines.size());

		// Tell it to prune, but they are not within tolerance
		alg.pruneSimilar(0.1f, 10, 800, 600);
		assertEquals(1, alg.lines.size());

		// Same deal, but with the closest point on the right side of the image
		alg.reset();
		alg.add(new LineParametric2D_F32(800, 10, -1, 0), 10);
		alg.add(new LineParametric2D_F32(800, 16, -0.99f, 0.01f), 10);

		alg.pruneSimilar(0.1f, 5, 800, 600);
		assertEquals(2, alg.lines.size());
		alg.pruneSimilar(0.1f, 10, 800, 600);
		assertEquals(1, alg.lines.size());
	}
}
