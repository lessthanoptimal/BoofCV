/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
		c = c.subimage(10, 0, 20, 20, null);
		c.setTo(a);
		assertEquals(0, c.data[5]);
		assertEquals(6, c.data[35]);
	}

	/**
	 * The two matrices do not have the same shape
	 */
	@Test
	public void setTo_mismatch() {
		DummyImage a = new DummyImage(10, 20 , 3);
		DummyImage b = new DummyImage(11, 21 , 3);

		a.setTo(b);

		assertEquals(a.width, 11);
		assertEquals(b.height, 21);
	}

	@Test
	public void reshape() {
		DummyImage a = new DummyImage(10, 20, 3);

		a.reshape(5,10);
		assertTrue(50 <= a.data.length);
		assertEquals(5,a.width);
		assertEquals(10,a.height);
		assertEquals(3,a.numBands);
		assertEquals(5*3,a.stride);

		a.reshape(30,25);
		assertTrue(30*25 <= a.data.length);
		assertEquals(30,a.width);
		assertEquals(25,a.height);
		assertEquals(3,a.numBands);
		assertEquals(30*3,a.stride);
	}

	@Test
	public void reshape_subimage() {
		DummyImage img = new DummyImage(10, 20, 3);
		img = img.subimage(0,0,2,2, null);

		try {
			img.reshape(10,20);
			fail("Should have thrown exception");
		} catch( IllegalArgumentException ignore ) {}
	}

	/**
	 * The two matrices do not have the same shape
	 */
	@Test
	public void setTo_mismatch_Shape() {
		DummyImage a = new DummyImage(10, 20, 3);
		DummyImage b = new DummyImage(11, 21, 3);

		a.setTo(b);

		assertEquals(a.width, 11);
		assertEquals(b.height, 21);
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
		DummyImage a = new DummyImage(10, 20, 3);
		assertFalse(a.isSubimage());

		DummyImage b = a.subimage(2, 3, 8, 10, null);

		assertTrue(b.isSubimage());
		assertTrue(a.getImageType()==b.getImageType());
		assertTrue(a._getData()==b._getData());
		assertEquals(10 * 20 * 3, b.data.length);
		assertEquals(6, b.getWidth());
		assertEquals(7, b.getHeight());
		assertEquals(3, b.getNumBands());
		assertEquals(30, b.getStride());
		assertEquals(3 * 30 + 2 * 3, b.getStartIndex());
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
		public ImageDataType getDataType() {
			return ImageDataType.S32;
		}

		@Override
		protected Object _getData() {
			return data;
		}

		@Override
		protected Class getPrimitiveDataType() {
			return int.class;
		}


		@Override
		protected void _setData(Object data) {
			this.data = (int[]) data;
		}

		@Override
		public DummyImage createNew(int imgWidth, int imgHeight) {
			return new DummyImage(imgWidth,imgHeight,numBands);
		}
	}
}
