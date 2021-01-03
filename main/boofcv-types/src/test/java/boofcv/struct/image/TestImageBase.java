/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestImageBase extends BoofStandardJUnit {

	@Test void isInBounds() {
		Dummy a = new Dummy();
		a.width = 10;
		a.height = 11;

		assertTrue(a.isInBounds(0, 0));
		assertTrue(a.isInBounds(9, 10));
		assertFalse(a.isInBounds(-1, 0));
		assertFalse(a.isInBounds(10, 0));
		assertFalse(a.isInBounds(0, -1));
		assertFalse(a.isInBounds(0, 11));
	}

	@Test void indexToPixel() {
		Dummy a = new Dummy();

		a.startIndex = 7;
		a.stride = 5;
		a.width = 4;
		a.height = 11;

		int index = 7 + 6*5 + 2;

		assertEquals(2, a.indexToPixelX(index));
		assertEquals(6, a.indexToPixelY(index));
	}

	@Test void isSubimage() {
		Dummy a = new Dummy();
		a.subImage = false;

		assertFalse(a.isSubimage());

		a.subImage = true;
		assertTrue(a.isSubimage());
	}

	@Test void totalPixels() {
		Dummy a = new Dummy();
		a.width = 10;
		a.height = 22;
		assertEquals(10*22, a.totalPixels());
	}

	@Test void forEachXY() {
		var helper = new ForEachHelper();
		var a = new Dummy();
		a.width = 10;
		a.height = 22;

		a.forEachXY(( x, y ) -> {
			helper.count++;
			helper.maxX = Math.max(x, helper.maxX);
			helper.maxY = Math.max(y, helper.maxY);
		});
	}

	private static class ForEachHelper {
		int count = 0;
		int maxX = 0;
		int maxY = 0;
	}

	private static class Dummy extends ImageBase {

		@Override
		public ImageBase subimage( int x0, int y0, int x1, int y1, ImageBase subimage ) {return null;}

		@Override
		public void reshape( int width, int height ) {}

		@Override
		public void setTo( ImageBase orig ) {}

		@Override
		public ImageBase createNew( int imgWidth, int imgHeight ) {
			return null;
		}

		@Override
		public void copyRow( int row, int col0, int col1, int offset, Object array ) {}

		@Override
		public void copyCol( int col, int row0, int row1, int offset, Object array ) {}
	}
}
