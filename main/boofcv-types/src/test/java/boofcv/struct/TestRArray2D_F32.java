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

package boofcv.struct;

import boofcv.testing.BoofStandardJUnit;
import georegression.misc.GrlConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRArray2D_F32 extends BoofStandardJUnit {
	@Test void resize() {
		RArray2D_F32 a = new RArray2D_F32(5, 4);

		assertEquals(5, a.rows);
		assertEquals(4, a.cols);
		assertEquals(5, a.data.length);
		assertEquals(4, a.data[0].length);

		a.reshape(10, 15);
		assertEquals(10, a.rows);
		assertEquals(15, a.cols);
		assertEquals(10, a.data.length);
		assertEquals(15, a.data[0].length);

		a.reshape(9, 12);
		assertEquals(9, a.rows);
		assertEquals(12, a.cols);
		assertTrue(9 <= a.data.length);
		assertTrue(12 <= a.data[0].length);
	}

	@Test void getRows_getCols() {
		RArray2D_F32 a = new RArray2D_F32(9, 12);

		assertEquals(9, a.getRows());
		assertEquals(12, a.getCols());
	}

	@Test void get() {
		RArray2D_F32 a = new RArray2D_F32(9, 12);

		a.data[4][5] = 3;
		assertEquals(3, a.get(4, 5), GrlConstants.TEST_F32);
	}
}