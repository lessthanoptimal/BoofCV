/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCircularIndex {
	@Test
	public void plusPOffset() {
		assertEquals(3, CircularIndex.plusPOffset(2, 1, 5));
		assertEquals(4, CircularIndex.plusPOffset(2, 2, 5));
		assertEquals(0, CircularIndex.plusPOffset(2, 3, 5));
		assertEquals(1, CircularIndex.plusPOffset(2, 4, 5));
	}

	@Test
	public void addOffset() {
		assertEquals(3, CircularIndex.addOffset(2, 1, 5));
		assertEquals(4, CircularIndex.addOffset(2, 2, 5));
		assertEquals(0, CircularIndex.addOffset(2, 3, 5));
		assertEquals(1, CircularIndex.addOffset(2, 4, 5));
		assertEquals(1, CircularIndex.addOffset(2, -1, 5));
		assertEquals(0, CircularIndex.addOffset(2, -2, 5));
		assertEquals(4, CircularIndex.addOffset(2, -3, 5));
		assertEquals(3, CircularIndex.addOffset(2, -4, 5));
	}

	@Test
	public void minusPOffset() {
		assertEquals(1, CircularIndex.minusPOffset(2, 1, 5));
		assertEquals(0, CircularIndex.minusPOffset(2, 2, 5));
		assertEquals(4, CircularIndex.minusPOffset(2, 3, 5));
		assertEquals(3, CircularIndex.minusPOffset(2, 4, 5));
	}

	@Test
	public void distanceP() {
		assertEquals(0, CircularIndex.distanceP(2, 2, 4));
		assertEquals(1, CircularIndex.distanceP(2, 3, 4));
		assertEquals(3, CircularIndex.distanceP(2, 1, 4));
		assertEquals(2, CircularIndex.distanceP(2, 0, 4));
	}

	@Test
	public void distance() {
		assertEquals(0, CircularIndex.distance(2, 2, 4));
		assertEquals(1, CircularIndex.distance(2, 3, 4));
		assertEquals(1, CircularIndex.distance(2, 1, 4));
		assertEquals(2, CircularIndex.distance(2, 0, 4));
	}

	@Test
	public void subtract() {
		assertEquals( 0, CircularIndex.subtract(2, 2, 4));
		assertEquals( 1, CircularIndex.subtract(2, 3, 4));
		assertEquals(-1, CircularIndex.subtract(2, 1, 4));
		assertEquals(-2, CircularIndex.subtract(2, 0, 4));
		assertEquals(-2, CircularIndex.subtract(2, 0, 5));
		assertEquals(2, CircularIndex.subtract(2, 4, 5));
	}
}