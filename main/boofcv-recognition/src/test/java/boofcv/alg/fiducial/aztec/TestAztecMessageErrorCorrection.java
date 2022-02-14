/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestAztecMessageErrorCorrection {
	/** Construct a message and corrupt a single word. See if it fixes it */
	@Test void correctSingleWordErrors() {
		var alg = new Helper();

		int capacity = 18;
		for (int i = 0; i < 6; i++) {
			alg.storageDataWords.add(i + 2);
		}

		for (int wordBits : new int[]{6, 8, 10, 12}) {
			// Compute error correction words
			alg.computeEccWords(wordBits, capacity);
			assertEquals(18 - alg.storageDataWords.size, alg.storageEccWords.size);

			// correct data with a single bad word
			alg.storageDataWords.data[2] += 3;
			assertEquals(1, alg.applyEcc(capacity, wordBits));

			for (int i = 0; i < 6; i++) {
				assertEquals(i + 2, alg.storageDataWords.get(i));
			}
		}
	}

	private static class Helper extends AztecMessageErrorCorrection {}
}
