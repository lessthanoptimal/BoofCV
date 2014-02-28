/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.flow;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImageFlow {

	@Test
	public void constructor() {
		ImageFlow a = new ImageFlow(3,4);

		assertEquals(3,a.getWidth());
		assertEquals(4,a.getHeight());
		assertEquals(3*4,a.data.length);
	}

	@Test
	public void reshape() {
		ImageFlow a = new ImageFlow(3,4);

		a.reshape(2,3);
		assertEquals(2, a.getWidth());
		assertEquals(3,a.getHeight());
		assertEquals(3 * 4, a.data.length);

		a.reshape(4,5);
		assertEquals(4,a.getWidth());
		assertEquals(5,a.getHeight());
		assertEquals(4*5,a.data.length);
	}

	@Test
	public void invalidateAll() {
		ImageFlow a = new ImageFlow(3,4);
		for( int i = 0; i < a.data.length; i++ )
			a.data[i].markInvalid();

		a.invalidateAll();

		for( int i = 0; i < a.data.length; i++ )
			assertFalse(a.data[i].isValid());
	}

	@Test
	public void get() {
		ImageFlow a = new ImageFlow(3,4);

		assertTrue(a.data[0] == a.get(0,0));
		assertTrue(a.data[1] == a.get(1,0));
		assertTrue(a.data[3] == a.get(0,1));
	}

	@Test
	public void isInBounds() {
		ImageFlow a = new ImageFlow(10,11);

		assertTrue(a.isInBounds(0,0));
		assertTrue(a.isInBounds(9, 10));
		assertFalse(a.isInBounds(-1, 0));
		assertFalse(a.isInBounds(10, 0));
		assertFalse(a.isInBounds(0, -1));
		assertFalse(a.isInBounds(0, 11));
	}

}
