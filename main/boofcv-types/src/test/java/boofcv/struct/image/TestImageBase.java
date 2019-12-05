/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestImageBase {

	@Test
	void isInBounds() {
		Dummy a = new Dummy();
		a.width = 10;
		a.height = 11;
		
		assertTrue(a.isInBounds(0,0));
		assertTrue(a.isInBounds(9, 10));
		assertFalse(a.isInBounds(-1, 0));
		assertFalse(a.isInBounds(10, 0));
		assertFalse(a.isInBounds(0, -1));
		assertFalse(a.isInBounds(0, 11));
	}

	@Test
	void indexToPixel() {
		Dummy a = new Dummy();
		
		a.startIndex = 7;
		a.stride = 5;
		a.width = 4;
		a.height = 11;

		Point2D_I32 p = a.indexToPixel(7+6*5+2);
		
		assertEquals(2,p.x);
		assertEquals(6,p.y);
	}

	@Test
	void isSubimage() {
		Dummy a = new Dummy();
		a.subImage = false;

		assertFalse(a.isSubimage());

		a.subImage = true;
		assertTrue(a.isSubimage());
	}

	@Test
	void totalPixels() {
		Dummy a = new Dummy();
		a.width = 10;
		a.height = 22;
		assertEquals(10*22, a.totalPixels());
	}
	
	private static class Dummy extends ImageBase
	{

		@Override
		public ImageBase subimage(int x0, int y0, int x1, int y1, ImageBase subimage) {return null;}

		@Override
		public void reshape(int width, int height) {}

		@Override
		public void setTo(ImageBase orig) {}

		@Override
		public ImageBase createNew(int imgWidth, int imgHeight) {
			return null;
		}

		@Override
		public void copyRow(int row, int col0, int col1, int offset, Object array) {}

		@Override
		public void copyCol(int col, int row0, int row1, int offset, Object array) {}
	}
}
