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

package boofcv.alg.feature.describe.llah;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestLlahFeature extends BoofStandardJUnit {
	@Test
	void reset() {
		var feat = new LlahFeature(5);
		feat.next = feat;
		feat.documentID = 10;
		feat.landmarkID = 11;
		for (int i = 0; i < 5; i++) {
			feat.invariants[i] = i;
		}

		feat.reset();

		assertNull(feat.next);
		assertEquals(-1,feat.documentID);
		assertEquals(-1,feat.landmarkID);
		for (int i = 0; i < 5; i++) {
			assertEquals(-1, feat.invariants[i]);
		}
	}

	@Test
	void doInvariantsMatch() {
		var featA = new LlahFeature(5);
		var featB = new LlahFeature(5);

		assertTrue(featA.doInvariantsMatch(featB));
		for (int i = 0; i < 5; i++) {
			featA.invariants[i] = i+1;
			featB.invariants[i] = i+1;
			assertTrue(featA.doInvariantsMatch(featB));
		}
		for (int i = 0; i < 5; i++) {
			featA.invariants[i] = -i-1;
			assertFalse(featA.doInvariantsMatch(featB));
			featA.invariants[i] = featB.invariants[i];
		}
	}
}
