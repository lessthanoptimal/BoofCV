/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Peter Abeles
 */
public class TestPackedBits32 extends BoofStandardJUnit {
	@Test void set_get() {
		PackedBits32 values = new PackedBits32(60);

		values.set(2, 1);
		assertEquals(values.get(2), 1);
		values.set(2, 0);
		assertNotEquals(values.get(2), 1);
		isZeros(values);

		values.set(33, 1);
		assertEquals(values.get(33), 1);
		values.set(33, 0);
		assertNotEquals(values.get(33), 1);
		isZeros(values);
	}

	private void isZeros( PackedBits32 values ) {
		int N = values.size/32;
		for (int i = 0; i < N; i++) {
			assertEquals(0, values.data[i]);
		}
	}

	/**
	 * See if negative internal integers are handled correctly. This was a bug and get() return -1
	 */
	@Test void negativeInteger() {
		PackedBits32 values = new PackedBits32(60);
		values.data[0] = -Integer.MAX_VALUE;
		assertEquals(1, values.get(31));
	}

	@Test void resize() {
		PackedBits32 values = new PackedBits32(60);
		assertEquals(60, values.size);
		assertEquals(2, values.data.length);
		values.resize(20);
		assertEquals(20, values.size);
		assertEquals(2, values.data.length);
		values.resize(100);
		assertEquals(100, values.size);
		assertEquals(100/32 + 1, values.data.length);
	}
}
