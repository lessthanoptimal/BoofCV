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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestPackedBits8 extends BoofStandardJUnit {
	@Test void set_get() {
		PackedBits8 values = new PackedBits8(60);

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

	private void isZeros( PackedBits8 values ) {
		int N = values.arrayLength();
		for (int i = 0; i < N; i++) {
			assertEquals(0, values.data[i]);
		}
	}

	@Test void resize() {
		PackedBits8 values = new PackedBits8(60);
		assertEquals(60, values.size);
		assertEquals(60/8 + 1, values.data.length);
		values.resize(20);
		assertEquals(20, values.size);
		assertEquals(60/8 + 1, values.data.length);
		values.resize(100);
		assertEquals(100, values.size);
		assertEquals(100/8 + 1, values.data.length);
	}

	@Test void growArray() {
		PackedBits8 values = new PackedBits8(8);
		assertEquals(8, values.size);
		assertEquals(1, values.data.length);

		values.growArray(2, false);
		assertTrue(2 <= values.data.length);
		assertEquals(10, values.size);

		values.growArray(7, false);
		assertTrue(3 <= values.data.length);
		assertEquals(17, values.size);

		// see if save value works
		values.set(10, 1);
		values.growArray(1, true);
		assertEquals(1, values.get(10));
		assertTrue(3 <= values.data.length);
		assertEquals(18, values.size);
		values.growArray(7, true);
		assertEquals(1, values.get(10));
		assertTrue(4 <= values.data.length);
		assertEquals(25, values.size);

		values = new PackedBits8(8);
		values.set(2, 1);
		assertEquals(1, values.get(2));
		values.growArray(8, false);
		assertEquals(0, values.get(2));
	}

	@Test void append() {
		PackedBits8 values = new PackedBits8(0);

		values.append(0b1101, 4, true);
		assertEquals(4, values.size);
		assertEquals(1, values.get(0));
		assertEquals(0, values.get(1));
		assertEquals(1, values.get(2));
		assertEquals(1, values.get(3));

		values.append(0b1101, 4, false);
		assertEquals(8, values.size);
		assertEquals(1, values.get(4));
		assertEquals(1, values.get(5));
		assertEquals(0, values.get(6));
		assertEquals(1, values.get(7));
	}

	@Test void append_array() {
		PackedBits8 src = new PackedBits8(0);
		for (int i = 0; i < 2; i++) {
			src.append(rand.nextInt(), 8, false);
		}
		src.append(0b101, 3, false);

		PackedBits8 dst = new PackedBits8(0);
		dst.append(src, src.size);

		assertEquals(src.size, dst.size);
		for (int i = 0; i < src.size; i++) {
			assertEquals(src.get(i), dst.get(i), "i=" + i);
		}
	}

	@Test void read() {
		PackedBits8 values = new PackedBits8(0);

		values.append(0b1101, 4, true);
		values.append(0b00011, 5, true);

		assertEquals(0b1101, values.read(0, 4, false));
		assertEquals(0b00011, values.read(4, 5, false));

		assertEquals(0b1011, values.read(0, 4, true));
		assertEquals(0b11000, values.read(4, 5, true));
	}
}
