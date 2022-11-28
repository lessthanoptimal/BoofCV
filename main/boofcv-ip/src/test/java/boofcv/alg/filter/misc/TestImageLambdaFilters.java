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

package boofcv.alg.filter.misc;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static boofcv.alg.filter.misc.ImageLambdaFilters.filterRectCenterInner;
import static boofcv.misc.BoofMiscOps.isInside;
import static org.junit.jupiter.api.Assertions.*;

class TestImageLambdaFilters extends BoofStandardJUnit {
	int width = 30;
	int height = 25;
	int radiusX = 2;
	int radiusY = 3;

	/**
	 * Checks that it's passed in all the inner pixels and not the edge
	 */
	@Test void filterRectCenterInner_Bounds() {
		var src = new GrayU8(width, height);
		var dst = new GrayU8(width, height);
		var filter = new MockRectCenter(src, radiusX, radiusY);

		// Image large enough that there is an inner region
		filterRectCenterInner(src, radiusX, radiusY, dst, null, filter);
		assertEquals((width - 2*radiusX)*(height - 2*radiusY), filter.callCount);

		filter.callCount = 0;
		// image that is too narrow along x-axis
		src.reshape(radiusX*2, height);
		filterRectCenterInner(src, radiusX, radiusY, dst, null, filter);
		assertEquals(0, filter.callCount);

		// image that is too narrow along y-axis
		src.reshape(width, radiusY*2);
		filterRectCenterInner(src, radiusX, radiusY, dst, null, filter);
		assertEquals(0, filter.callCount);
	}

	/**
	 * Checks the value of the dst image
	 */
	@Test void filterRectCenterInner_dstValue() {
		var src = new GrayU8(width, height);
		var dst = new GrayU8(width, height);
		var filter = new MockRectCenter(src, radiusX, radiusY);

		filterRectCenterInner(src, radiusX, radiusY, dst, null, filter);

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				if (isInside(src, x, y, radiusX, radiusY)) {
					assertEquals(x%100, dst.get(x, y));
				} else {
					assertEquals(0, dst.get(x, y));
				}
			}
		}
	}

	/**
	 * Checks that it's passing in only border pixels to the filter
	 */
	@Test void filterRectCenterEdge_Bounds() {
		var src = new GrayU8(width, height);
		var dst = new GrayU8(width, height);
		var filter = new MockRect(src, radiusX, radiusY);

		// Image large enough that there is an inner region
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, filter);
		assertEquals(width*height - (width-radiusX*2)*(height-radiusY*2), filter.callCount);

		// image that is too narrow along x-axis
		src.reshape(radiusX*2, height);
		filter.callCount = 0;
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, filter);
		assertEquals(src.width*src.height, filter.callCount);

		// image that is too narrow along y-axis
		src.reshape(width, radiusY*2);
		filter.callCount = 0;
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, filter);
		assertEquals(src.width*src.height, filter.callCount);

		// image that's (0,0) pixels
		src.reshape(0, 0);
		filter.callCount = 0;
		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, filter);
		assertEquals(0, filter.callCount);
	}

	/**
	 * Checks the value of the dst image
	 */
	@Test void filterRectCenterEdge_dstValue() {
		var src = new GrayU8(width, height);
		var dst = new GrayU8(width, height);
		var filter = new MockRect(src, radiusX, radiusY);

		ImageLambdaFilters.filterRectCenterEdge(src, radiusX, radiusY, dst, null, filter);

		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				if (isInside(src, x, y, radiusX, radiusY)) {
					assertEquals(0, dst.get(x, y));
				} else {
					assertEquals(1 + x%50 + y%50, dst.get(x, y));
				}
			}
		}
	}

	static class MockRectCenter implements ImageLambdaFilters.RectCenter_S32 {
		ImageBase<?> image;
		int radiusX, radiusY;
		int callCount = 0;

		public MockRectCenter( ImageBase<?> image, int radiusX, int radiusY ) {
			this.image = image;
			this.radiusX = radiusX;
			this.radiusY = radiusY;
		}

		@Override public int apply( int indexPixel, Object workspace ) {
			callCount++;

			// Sanity check the passed in point
			indexPixel -= image.startIndex;
			int cy = indexPixel/image.stride;
			int cx = indexPixel%image.stride;

			assertFalse(cx < radiusX && cx >= image.width - radiusX);
			assertFalse(cy < radiusY && cy >= image.height - radiusY);

			return cx%100;
		}
	}

	static class MockRect implements ImageLambdaFilters.Rect_S32 {
		ImageBase<?> image;
		int radiusX, radiusY;
		int callCount = 0;

		public MockRect( ImageBase<?> image, int radiusX, int radiusY ) {
			this.image = image;
			this.radiusX = radiusX;
			this.radiusY = radiusY;
		}

		@Override public int apply( int cx, int cy, int x0, int y0, int x1, int y1, Object workspace ) {
			// The center should be inside the image border
			assertFalse(isInside(image, cx, cy, radiusX, radiusY));

			// Make sure these pixels are inside the image
			assertTrue(image.isInBounds(x0, y0));
			assertTrue(image.isInBounds(x1-1, y1-1)); // -1 because it's exclusive

			callCount++;
			return 1 + cx%50 + cy%50;
		}
	}
}