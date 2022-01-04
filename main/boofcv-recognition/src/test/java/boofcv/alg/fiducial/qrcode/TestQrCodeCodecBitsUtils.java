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

package boofcv.alg.fiducial.qrcode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestQrCodeCodecBitsUtils {

	@Test void checkAlphaNumericLookUpTable() {
		assertEquals(45, QrCodeCodecBitsUtils.ALPHANUMERIC.length());
	}

	@Test void alphanumericToValues() {
		byte[] found = QrCodeCodecBitsUtils.alphanumericToValues("14AE%*+-./:");
		byte[] expected = new byte[]{1, 4, 10, 14, 38, 39, 40, 41, 42, 43, 44};

		assertArrayEquals(expected, found);
	}

	@Test void valueToAlphanumeric() {
		byte[] input = new byte[]{1, 4, 10, 14, 38, 39, 40, 41, 42, 43, 44};
		String expected = "14AE%*+-./:";
		for (int i = 0; i < input.length; i++) {
			char c = QrCodeCodecBitsUtils.valueToAlphanumeric(input[i]);
			assertEquals(expected.charAt(i), c);
		}
	}
}
