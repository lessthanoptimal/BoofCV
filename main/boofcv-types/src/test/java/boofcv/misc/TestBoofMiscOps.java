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

package boofcv.misc;

import boofcv.struct.ImageRectangle;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestBoofMiscOps extends BoofStandardJUnit {

	@Test
	void numDigits() {
		assertEquals(1, BoofMiscOps.numDigits(0));
		assertEquals(1, BoofMiscOps.numDigits(1));
		assertEquals(1, BoofMiscOps.numDigits(9));
		assertEquals(2, BoofMiscOps.numDigits(10));
		assertEquals(2, BoofMiscOps.numDigits(55));
		assertEquals(3, BoofMiscOps.numDigits(120));
		assertEquals(5, BoofMiscOps.numDigits(67890));
		assertEquals(2, BoofMiscOps.numDigits(-1));
		assertEquals(2, BoofMiscOps.numDigits(-9));
		assertEquals(3, BoofMiscOps.numDigits(-10));
		assertEquals(3, BoofMiscOps.numDigits(-55));
		assertEquals(4, BoofMiscOps.numDigits(-120));
		assertEquals(6, BoofMiscOps.numDigits(-67890));
	}

	@Test
	void boundRectangleInside() {
		GrayU8 image = new GrayU8(20, 25);

		checkBound(-2, -3, 5, 6, 0, 0, 5, 6, image);
		checkBound(16, 15, 22, 26, 16, 15, 20, 25, image);
		checkBound(0, 0, 20, 25, 0, 0, 20, 25, image);
		checkBound(-2, -3, 22, 26, 0, 0, 20, 25, image);
	}

	private void checkBound( int x0, int y0, int x1, int y1,
							 int ex0, int ey0, int ex1, int ey1,
							 ImageGray image ) {
		ImageRectangle a = new ImageRectangle(x0, y0, x1, y1);
		BoofMiscOps.boundRectangleInside(image, a);
		assertEquals(ex0, a.x0);
		assertEquals(ey0, a.y0);
		assertEquals(ex1, a.x1);
		assertEquals(ey1, a.y1);
	}

	@Test
	void checkInside_img_float() {
		GrayU8 image = new GrayU8(20, 25);

		assertTrue(BoofMiscOps.isInside(image, 0f, 0f));
		assertTrue(BoofMiscOps.isInside(image, 19.99f, 24.99f));
		assertTrue(BoofMiscOps.isInside(image, 5f, 10f));

		assertFalse(BoofMiscOps.isInside(image, -0.01f, 0));
		assertFalse(BoofMiscOps.isInside(image, 0, -0.01f));
		assertFalse(BoofMiscOps.isInside(image, 0, 25.01f));
		assertFalse(BoofMiscOps.isInside(image, 20.01f, 0));
	}

	@Test
	void checkInside_wh_float() {
		assertTrue(BoofMiscOps.isInside(20, 25, 0f, 0f));
		assertTrue(BoofMiscOps.isInside(20, 25, 19.99f, 24.99f));
		assertTrue(BoofMiscOps.isInside(20, 25, 5f, 10f));

		assertFalse(BoofMiscOps.isInside(20, 25, -0.01f, 0));
		assertFalse(BoofMiscOps.isInside(20, 25, 0, -0.01f));
		assertFalse(BoofMiscOps.isInside(20, 25, 0, 25.01f));
		assertFalse(BoofMiscOps.isInside(20, 25, 20.01f, 0));
	}

	@Test
	void checkInside_img_double() {
		GrayU8 image = new GrayU8(20, 25);

		assertTrue(BoofMiscOps.isInside(image, 0d, 0d));
		assertTrue(BoofMiscOps.isInside(image, 19.99d, 24.99d));
		assertTrue(BoofMiscOps.isInside(image, 5d, 10d));

		assertFalse(BoofMiscOps.isInside(image, -0.01d, 0));
		assertFalse(BoofMiscOps.isInside(image, 0, -0.01d));
		assertFalse(BoofMiscOps.isInside(image, 0, 25.01d));
		assertFalse(BoofMiscOps.isInside(image, 20.01d, 0));
	}

	@Test
	void checkInside_wh_double() {

		assertTrue(BoofMiscOps.isInside(20, 25, 0d, 0d));
		assertTrue(BoofMiscOps.isInside(20, 25, 19.99d, 24.99d));
		assertTrue(BoofMiscOps.isInside(20, 25, 5d, 10d));

		assertFalse(BoofMiscOps.isInside(20, 25, -0.01d, 0));
		assertFalse(BoofMiscOps.isInside(20, 25, 0, -0.01d));
		assertFalse(BoofMiscOps.isInside(20, 25, 0, 25.01d));
		assertFalse(BoofMiscOps.isInside(20, 25, 20.01d, 0));
	}

	@Test
	void checkInside_w_h() {
		GrayU8 image = new GrayU8(20, 25);

		assertTrue(BoofMiscOps.isInside(image, 2, 3, 2, 3));
		assertTrue(BoofMiscOps.isInside(image, 17, 21, 2, 3));
		assertTrue(BoofMiscOps.isInside(image, 6, 7, 2, 3));

		assertFalse(BoofMiscOps.isInside(image, 1, 3, 2, 3));
		assertFalse(BoofMiscOps.isInside(image, 2, 2, 2, 3));
		assertFalse(BoofMiscOps.isInside(image, 18, 21, 2, 3));
		assertFalse(BoofMiscOps.isInside(image, 17, 22, 2, 3));
	}

	@Test
	void checkInside_radius_int() {
		GrayU8 image = new GrayU8(20, 25);

		assertTrue(BoofMiscOps.isInside(image, 2, 2, 2));
		assertTrue(BoofMiscOps.isInside(image, 17, 22, 2));
		assertTrue(BoofMiscOps.isInside(image, 15, 20, 4));
		assertTrue(BoofMiscOps.isInside(image, 4, 4, 4));
		assertTrue(BoofMiscOps.isInside(image, 0, 0, 0));
		assertTrue(BoofMiscOps.isInside(image, 19, 24, 0));

		assertFalse(BoofMiscOps.isInside(image, 1, 2, 2));
		assertFalse(BoofMiscOps.isInside(image, 2, 1, 2));
		assertFalse(BoofMiscOps.isInside(image, 18, 22, 2));
		assertFalse(BoofMiscOps.isInside(image, 17, 23, 2));
		assertFalse(BoofMiscOps.isInside(image, -1, 0, 0));
		assertFalse(BoofMiscOps.isInside(image, 0, -1, 0));
	}

	@Test
	void checkInside_radius_F32() {
		GrayU8 image = new GrayU8(20, 25);

		assertTrue(BoofMiscOps.isInside(image, 2f, 2f, 2f));
		assertTrue(BoofMiscOps.isInside(image, 17f, 22f, 2f));
		assertTrue(BoofMiscOps.isInside(image, 15f, 20f, 4f));
		assertTrue(BoofMiscOps.isInside(image, 4f, 4f, 4f));
		assertTrue(BoofMiscOps.isInside(image, 0f, 0f, 0f));
		assertTrue(BoofMiscOps.isInside(image, 19f, 24f, 0f));

		assertFalse(BoofMiscOps.isInside(image, 1f, 2f, 2f));
		assertFalse(BoofMiscOps.isInside(image, 2f, 1f, 2f));
		assertFalse(BoofMiscOps.isInside(image, 18f, 22f, 2f));
		assertFalse(BoofMiscOps.isInside(image, 17f, 23f, 2f));
		assertFalse(BoofMiscOps.isInside(image, -1f, 0f, 0f));
		assertFalse(BoofMiscOps.isInside(image, 0f, -1f, 0f));

		assertFalse(BoofMiscOps.isInside(image, 0f, -0.4f, 0f));
		assertFalse(BoofMiscOps.isInside(image, -0.4f, 0f, 0f));
		assertFalse(BoofMiscOps.isInside(image, 19.1f, 24f, 0f));
		assertFalse(BoofMiscOps.isInside(image, 19.1f, 24.1f, 0f));

		assertTrue(BoofMiscOps.isInside(image, 0.1f, 0.1f, 0f));
		assertTrue(BoofMiscOps.isInside(image, 18.9f, 23.9f, 0f));
	}

	@Test
	void checkInside_radius_F64() {
		GrayU8 image = new GrayU8(20, 25);

		assertTrue(BoofMiscOps.isInside(image, 2d, 2d, 2d));
		assertTrue(BoofMiscOps.isInside(image, 17d, 22d, 2d));
		assertTrue(BoofMiscOps.isInside(image, 15d, 20d, 4d));
		assertTrue(BoofMiscOps.isInside(image, 4d, 4d, 4d));
		assertTrue(BoofMiscOps.isInside(image, 0d, 0d, 0d));
		assertTrue(BoofMiscOps.isInside(image, 19d, 24d, 0d));

		assertFalse(BoofMiscOps.isInside(image, 1d, 2d, 2d));
		assertFalse(BoofMiscOps.isInside(image, 2d, 1d, 2d));
		assertFalse(BoofMiscOps.isInside(image, 18d, 22d, 2d));
		assertFalse(BoofMiscOps.isInside(image, 17d, 23d, 2d));
		assertFalse(BoofMiscOps.isInside(image, -1d, 0d, 0d));
		assertFalse(BoofMiscOps.isInside(image, 0d, -1d, 0d));

		assertFalse(BoofMiscOps.isInside(image, 0d, -0.4d, 0d));
		assertFalse(BoofMiscOps.isInside(image, -0.4d, 0d, 0d));
		assertFalse(BoofMiscOps.isInside(image, 19.1d, 24d, 0d));
		assertFalse(BoofMiscOps.isInside(image, 19.1d, 24.1d, 0d));

		assertTrue(BoofMiscOps.isInside(image, 0.1d, 0.1d, 0d));
		assertTrue(BoofMiscOps.isInside(image, 18.9d, 23.9d, 0d));
	}

	@Test
	void checkInside_ImageRectangle() {
		GrayU8 image = new GrayU8(20, 25);

		assertTrue(BoofMiscOps.isInside(image, new ImageRectangle(0, 0, 20, 25)));
		assertTrue(BoofMiscOps.isInside(image, new ImageRectangle(2, 4, 15, 23)));
		assertFalse(BoofMiscOps.isInside(image, new ImageRectangle(-1, 0, 20, 25)));
		assertFalse(BoofMiscOps.isInside(image, new ImageRectangle(0, -1, 20, 25)));
		assertFalse(BoofMiscOps.isInside(image, new ImageRectangle(0, 0, 21, 25)));
		assertFalse(BoofMiscOps.isInside(image, new ImageRectangle(0, 0, 20, 26)));
	}

	@Test
	void containsDuplicates() {
		List<Integer> list = new ArrayList<>();
		assertFalse(BoofMiscOps.containsDuplicates(list));
		list.add(1);
		list.add(2);
		assertFalse(BoofMiscOps.containsDuplicates(list));
		list.add(2);
		assertTrue(BoofMiscOps.containsDuplicates(list));
	}

	@Test void similarity_string() {
		assertEquals(1.0, BoofMiscOps.similarity("", ""));
		assertEquals(1.0, BoofMiscOps.similarity("foo", "foo"));
		assertEquals(1.0, BoofMiscOps.similarity("FFO", "ffo"));
		assertEquals(1.0, BoofMiscOps.similarity("123A", "123a"));

		assertTrue(BoofMiscOps.similarity("foooo", "fooo") > BoofMiscOps.similarity("f", "fooo"));
		assertTrue(BoofMiscOps.similarity("asdf", "abdf") > BoofMiscOps.similarity("asdfasdf", "asdf"));
	}

	@Test void stringToByteArray() {
		int N = 400;
		var builder = new StringBuilder();
		for (int i = 0; i < N; i++) {
			builder.append((char)i);
		}

		byte[] found = BoofMiscOps.castStringToByteArray(builder.toString());
		for (int i = 0; i < N; i++) {
			assertEquals(i & 0xFF, found[i] & 0xFF);
		}
	}
}
