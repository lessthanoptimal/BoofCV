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

package boofcv.alg.fiducial.qrcode;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEciEncoding extends BoofStandardJUnit {
	@Test void isValidUTF8() {
		assertTrue(EciEncoding.isValidUTF8("asdfafd".getBytes(StandardCharsets.UTF_8)));
		assertTrue(EciEncoding.isValidUTF8("目asdfafd木要₹".getBytes(StandardCharsets.UTF_8)));

		byte[] damaged = "a目sdfafd木".getBytes(StandardCharsets.UTF_8);
		damaged[1] = (byte)(0b0110_0000);
		assertFalse(EciEncoding.isValidUTF8(damaged));

		// mess up one of the extended byte characters by changing the first two bits
		damaged = "a目sdfafd木".getBytes(StandardCharsets.UTF_8);
		damaged[2] |= 0b1100_000;
		assertFalse(EciEncoding.isValidUTF8(damaged));
	}

	/** Give it a byte string of ascii characters and see if it says it's not UTF-8 */
	@Test void isValidUTF8_ISO_8859_1() {
		assertFalse(EciEncoding.isValidUTF8("asdfafdÿ¡£".getBytes(StandardCharsets.ISO_8859_1)));
	}
}
