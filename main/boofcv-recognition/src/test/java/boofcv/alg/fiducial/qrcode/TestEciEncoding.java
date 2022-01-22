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

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static boofcv.alg.fiducial.qrcode.EciEncoding.ISO8859_1;
import static boofcv.alg.fiducial.qrcode.EciEncoding.UTF8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestEciEncoding extends BoofStandardJUnit {
	@Test void isValidUTF8_value() {
		for (int i = 0; i <= 0xFF; i++) {
			if (i <= 0xBF)
				assertTrue(EciEncoding.isValidUTF8(i));
			else if (i >= 0xC2 && i <= 0xF4)
				assertTrue(EciEncoding.isValidUTF8(i));
			else
				assertFalse(EciEncoding.isValidUTF8(i));
		}
	}

	@Test void isValidUTF8_string() {
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

	@Test void isValidJIS() {
		for (int i = 0; i <= 0xFF; i++) {
			if (i >= 0x20 && i <= 0x7E)
				assertTrue(EciEncoding.isValidJIS(i));
			else if (i >= 0xA1 && i <= 0xDF)
				assertTrue(EciEncoding.isValidJIS(i));
			else
				assertFalse(EciEncoding.isValidJIS(i));
		}
	}

	@Test void isValidIso8869_1() {
		for (int i = 0; i <= 0xFF; i++) {
			if (i >= 0x20 && i <= 0x7E)
				assertTrue(EciEncoding.isValidIso8869_1(i));
			else if (i >= 0xA0)
				assertTrue(EciEncoding.isValidIso8869_1(i));
			else
				assertFalse(EciEncoding.isValidIso8869_1(i));
		}
	}

	@Test void guessEncoding_ISO88591() {
		byte[] message = "0123456789abcdefgABCDEFG*#$!zZyYxX<¥Àý".getBytes(StandardCharsets.ISO_8859_1);
		assertSame(ISO8859_1, EciEncoding.guessEncoding(message));
	}

	@Test void guessEncoding_JIF() {
		// This is intentionally blank as a reinder that we can't tell a valid JIF apart from ISO-8859-1. It should
		// probably be removed or an option added to default to one of these two when it's not UTF-8
	}

	@Test void guessEncoding_Bug_01() {
		byte[] message = "§".getBytes(StandardCharsets.UTF_8);
		assertSame(UTF8, EciEncoding.guessEncoding(message));
	}
}
