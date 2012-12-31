/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.image;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImageInterleaved {

	@Test
	public void setTo() {
		DummyImage a = new DummyImage(10, 20, 3);

		// test it against a regular matrix
		DummyImage b = new DummyImage(10, 20, 3);
		assertEquals(0, b.data[5]);

		a.data[5] = 6;
		b.setTo(a);
		assertEquals(6, b.data[5]);

		// test it against a submatrix
		DummyImage c = new DummyImage(20, 20, 3);
		c = c.subimage(10, 0, 20, 20);
		c.setTo(a);
		assertEquals(0, c.data[5]);
		assertEquals(6, c.data[35]);
	}

	/**
	 * The two matrices do not have the same shape
	 */
	@Test(expected = IllegalArgumentException.class)
	public void setTo_mismatch_Shape() {
		DummyImage a = new DummyImage(10, 20, 3);
		DummyImage b = new DummyImage(11, 20, 3);

		a.setTo(b);
	}

	/**
	 * The two matrices do not have the same shape
	 */
	@Test(expected = IllegalArgumentException.class)
	public void setTo_mismatch_bands() {
		DummyImage a = new DummyImage(10, 20, 3);
		DummyImage b = new DummyImage(10, 20, 4);

		a.setTo(b);
	}

	/**
	 * Test the constructor where the width,height and number of bands is specified.
	 */
	@Test
	public void constructor_w_h_n() {
		DummyImage a = new DummyImage(10, 20, 3);

		assertEquals(10 * 20 * 3, a.data.length);
		assertEquals(10, a.getWidth());
		assertEquals(20, a.getHeight());
		assertEquals(3, a.getNumBands());
		assertEquals(30, a.getStride());
		assertEquals(0, a.getStartIndex());
	}

	@Test
	public void createSubImage() {
		DummyImage a = new DummyImage(10, 20, 3).subimage(2, 3, 8, 10);

		assertEquals(10 * 20 * 3, a.data.length);
		assertEquals(6, a.getWidth());
		assertEquals(7, a.getHeight());
		assertEquals(3, a.getNumBands());
		assertEquals(30, a.getStride());
		assertEquals(3 * 30 + 2 * 3, a.getStartIndex());
	}

	@Test
	public void isInBounds() {
		DummyImage a = new DummyImage(10, 20, 3);

		assertTrue(a.isInBounds(0, 0));
		assertTrue(a.isInBounds(9, 19));

		assertFalse(a.isInBounds(-1, 0));
		assertFalse(a.isInBounds(0, -1));
		assertFalse(a.isInBounds(10, 0));
		assertFalse(a.isInBounds(0, 20));
	}

	@Test
	public void getIndex() {
		DummyImage a = new DummyImage(10, 20, 3);

		assertEquals(4 * 30 + 3 * 3 + 1, a.getIndex(3, 4, 1));
	}

	private static class DummyImage extends ImageInterleaved<DummyImage> {
		int data[];

		private DummyImage(int width, int height, int numBands) {
			super(width, height, numBands);
		}

		private DummyImage() {
		}

		@Override
		protected Object _getData() {
			return data;
		}

		@Override
		protected Class getDataType() {
			return int.class;
		}


		@Override
		protected void _setData(Object data) {
			this.data = (int[]) data;
		}

		@Override
		public DummyImage _createNew(int imgWidth, int imgHeight) {
			return new DummyImage();
		}
	}
}
