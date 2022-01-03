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

package boofcv.alg.fiducial.microqr;

import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.microqr.MicroQrCodeMaskPattern.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMicroQrCodeMaskPattern {
	/**
	 * Exhaustively check to make sure the output is always 0 or 1 when given an input of 0 or 1
	 */
	@Test void ensureOutputIs0or1() {
		ensureOutputIs0or1(M00);
		ensureOutputIs0or1(M01);
		ensureOutputIs0or1(M10);
		ensureOutputIs0or1(M11);
	}

	@Test void checkM00() {
		assertEquals(1, M00.apply(0, 0, 0));
		assertEquals(1, M00.apply(0, 1, 0));
		assertEquals(0, M00.apply(1, 0, 0));
		assertEquals(0, M00.apply(1, 1, 0));

		assertEquals(0, M00.apply(0, 0, 1));
	}

	@Test void checkM01() {
		assertEquals(1, M01.apply(0, 0, 0));
		assertEquals(1, M01.apply(0, 1, 0));
		assertEquals(1, M01.apply(0, 2, 0));
		assertEquals(0, M01.apply(0, 3, 0));
		assertEquals(1, M01.apply(1, 0, 0));
		assertEquals(0, M01.apply(2, 0, 0));

		assertEquals(0, M01.apply(0, 0, 1));
	}

	@Test void checkM10() {
		assertEquals(1, M10.apply(0, 0, 0));
		assertEquals(1, M10.apply(0, 1, 0));
		assertEquals(1, M10.apply(0, 2, 0));
		assertEquals(1, M10.apply(0, 3, 0));
		assertEquals(1, M10.apply(1, 0, 0));
		assertEquals(1, M10.apply(2, 0, 0));
		assertEquals(1, M10.apply(1, 1, 0));
		assertEquals(0, M10.apply(2, 2, 0));
		assertEquals(1, M10.apply(1, 2, 0));

		assertEquals(0, M10.apply(0, 0, 1));
	}

	@Test void checkM11() {
		assertEquals(1, M11.apply(0, 0, 0));
		assertEquals(0, M11.apply(0, 1, 0));
		assertEquals(1, M11.apply(0, 2, 0));
		assertEquals(0, M11.apply(0, 3, 0));
		assertEquals(0, M11.apply(1, 0, 0));
		assertEquals(1, M11.apply(2, 0, 0));
		assertEquals(0, M11.apply(1, 1, 0));
		assertEquals(0, M11.apply(2, 2, 0));
		assertEquals(0, M11.apply(1, 2, 0));
		assertEquals(1, M11.apply(1, 3, 0));

		assertEquals(0, M11.apply(0, 0, 1));
	}

	public void ensureOutputIs0or1( MicroQrCodeMaskPattern pattern ) {
		int found;
		for (int i = 0; i < 30; i++) {
			for (int j = 0; j < 30; j++) {
				found = pattern.apply(i, j, 1);
				assertTrue(found == 0 || found == 1);
				found = pattern.apply(i, j, 0);
				assertTrue(found == 0 || found == 1);
			}
		}
	}
}
