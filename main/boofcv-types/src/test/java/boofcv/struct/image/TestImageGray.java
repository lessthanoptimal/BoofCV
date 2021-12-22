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

package boofcv.struct.image;

import boofcv.testing.BoofStandardJUnit;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestImageGray extends BoofStandardJUnit {

	@Test void reshape() {
		DummyImage a = new DummyImage(10, 30);
		// b has the expected values
		DummyImage b = new DummyImage(11, 12);

		a.reshape(11, 12);

		assertFalse(a.subImage);
		assertEquals(b.stride, a.stride);
		assertEquals(b.width, a.width);
		assertEquals(b.height, a.height);

		// see if it will grow
		b = new DummyImage(100, 120);
		a.reshape(100, 120);

		assertEquals(b.stride, a.stride);
		assertEquals(b.width, a.width);
		assertEquals(b.height, a.height);


		// should throw an exception if a sub-image is reshaped
		try {
			a.subimage(1, 2, 1, 2, null).reshape(100, 200);

			fail("should have thrown an exception");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test void setTo() {
		DummyImage a = new DummyImage(10, 20);

		// test it against a regular matrix
		DummyImage b = new DummyImage(10, 20);
		assertEquals(0, b.data[5]);

		a.data[5] = 6;
		b.setTo(a);
		assertFalse(b.subImage);
		assertEquals(6, b.data[5]);

		// test it against a submatrix
		DummyImage c = new DummyImage(20, 20);
		c = c.subimage(10, 0, 20, 20, null);
		c.setTo(a);
		assertEquals(0, c.data[5]);
		assertEquals(6, c.data[15]);
	}

	/**
	 * The two matrices do not have the same shape
	 */
	@Test void setTo_mismatch() {
		DummyImage a = new DummyImage(10, 20);
		DummyImage b = new DummyImage(11, 21);

		a.setTo(b);

		assertEquals(a.width, 11);
		assertEquals(b.height, 21);
	}

	/**
	 * Test the constructor where the width,height and number of bands is specified.
	 */
	@Test void constructor_w_h_n() {
		DummyImage a = new DummyImage(10, 20);

		assertEquals(10*20, a.data.length);
		assertEquals(10, a.getWidth());
		assertEquals(20, a.getHeight());
		assertEquals(10, a.getStride());
		assertEquals(0, a.getStartIndex());
	}

	@Test void subimage() {
		DummyImage a = new DummyImage(10, 20).subimage(2, 3, 8, 10, null);

		assertTrue(a.subImage);
		assertEquals(10*20, a.data.length);
		assertEquals(6, a.getWidth());
		assertEquals(7, a.getHeight());
		assertEquals(10, a.getStride());
		assertEquals(3*10 + 2, a.getStartIndex());
	}

	@Test void isInBounds() {
		DummyImage a = new DummyImage(10, 20);

		assertTrue(a.isInBounds(0, 0));
		assertTrue(a.isInBounds(9, 19));

		assertFalse(a.isInBounds(-1, 0));
		assertFalse(a.isInBounds(0, -1));
		assertFalse(a.isInBounds(10, 0));
		assertFalse(a.isInBounds(0, 20));
	}

	@Test void getIndex() {
		DummyImage a = new DummyImage(10, 20);

		assertEquals(4*10 + 3, a.getIndex(3, 4));
	}

	private static class DummyImage extends GrayS32 {
		int[] data;

		private DummyImage( int width, int height ) { super(width, height); }

		private DummyImage() {}

		@Override public void print() {}

		@Override protected Object _getData() { return data; }

		@Override public ImageDataType getDataType() { return ImageDataType.S32; }

		@Override public void _setData( Object data ) { this.data = (int[])data; }

		@Override
		public DummyImage createNew( int imgWidth, int imgHeight ) { return new DummyImage(imgWidth, imgHeight); }

		@Override public void copyCol( int col, int row0, int row1, int offset, Object array ) {}

		@Override public DummyImage subimage( int x0, int y0, int x1, int y1, @Nullable GrayS32 subimage ) {
			return (DummyImage)super.subimage(x0, y0, x1, y1, subimage);
		}
	}
}
