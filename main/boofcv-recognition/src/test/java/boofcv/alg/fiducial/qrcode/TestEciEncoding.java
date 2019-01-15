/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEciEncoding {
	@Test
	public void isValidUTF8() {
		for (int i = 0; i <= 0xFF; i++) {
			if( i <= 0xBF )
				assertTrue(EciEncoding.isValidUTF8(i));
			else if( i >= 0xC3 && i <= 0xF4 )
				assertTrue(EciEncoding.isValidUTF8(i));
			else
				assertFalse(EciEncoding.isValidUTF8(i));
		}
	}

	@Test
	public void isValidJIS() {
		for (int i = 0; i <= 0xFF; i++) {
			if( i >= 0x20 && i <= 0x7E )
				assertTrue(EciEncoding.isValidJIS(i));
			else if( i >= 0xA1 && i <= 0xDF )
				assertTrue(EciEncoding.isValidJIS(i));
			else
				assertFalse(EciEncoding.isValidJIS(i));
		}
	}

	@Test
	public void isValidIso8869_1() {
		for (int i = 0; i <= 0xFF; i++) {
			if( i >= 0x20 && i <= 0x7E )
				assertTrue(EciEncoding.isValidIso8869_1(i));
			else if( i >= 0xA0 )
				assertTrue(EciEncoding.isValidIso8869_1(i));
			else
				assertFalse(EciEncoding.isValidIso8869_1(i));
		}
	}
}