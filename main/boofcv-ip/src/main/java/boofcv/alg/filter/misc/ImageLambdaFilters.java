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

import boofcv.struct.image.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Generated;

/**
 * Image filters which have been abstracted using lambdas. In most situations the 'src' image is assumed to be
 * passed in directory to the lambda, along with any other input parameters. What's given to the lambda
 * are parameters which define the local region. For inner functions, it can be assumed that all pixel values passed
 * in have a region contained entirely inside the region.
 *
 * <ol>
 *     <li>rectangle-center: filter that's applied to a local rectangular region centered on a pixel</li>
 * </ol>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImageLambdaFilters</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.misc.GenerateImageLambdaFilters")
public class ImageLambdaFilters {

	public static void filterRectCenterInner( GrayI8 src, int radiusX, int radiusY, GrayI8 dst,
											  @Nullable Object workspace, RectCenter_S32 filter ) {
		final int y0 = radiusY;
		final int y1 = src.height - radiusY;

		// Go through all inner pixels

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y -> {
		for (int y = y0; y < y1; y++) {
			int indexSrc = src.startIndex + y*src.stride + radiusX;
			int indexDst = dst.startIndex + y*dst.stride + radiusX;

			// index of last pixel along x-axis it should process
			int end = src.startIndex + y*src.stride + src.width - radiusX;
			while (indexSrc < end) {
				// Apply the transform. It's assumed that the src image has been passed into the lambda
				dst.data[indexDst++] = (byte)filter.apply(indexSrc++, workspace);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void filterRectCenterEdge( GrayI8 src, int radiusX, int radiusY, GrayI8 dst,
											 @Nullable Object workspace, Rect_S32 filter ) {
		// top edge
		for (int y = 0; y < radiusY; y++) {
			int y0 = 0;
			int y1 = Math.min(src.height, y + radiusY + 1);

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (byte)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		// bottom edge
		for (int y = Math.max(0, src.height - radiusY); y < src.height; y++) {
			int y0 = Math.max(0, y - radiusY);
			int y1 = src.height;

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (byte)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		for (int y = radiusY; y < Math.max(0, src.height - radiusY); y++) {
			int y0 = y - radiusY;
			int y1 = y + radiusY + 1;

			int indexDstRow = dst.startIndex + y*dst.stride;

			// left side
			for (int x = 0; x < radiusX; x++) {
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (byte)filter.apply(x, y, 0, y0, x1, y1, workspace);
			}

			// right side
			for (int x = Math.max(0, src.width - radiusX); x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (byte)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}
	}

	public static void filterRectCenterInner( GrayI16 src, int radiusX, int radiusY, GrayI16 dst,
											  @Nullable Object workspace, RectCenter_S32 filter ) {
		final int y0 = radiusY;
		final int y1 = src.height - radiusY;

		// Go through all inner pixels

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y -> {
		for (int y = y0; y < y1; y++) {
			int indexSrc = src.startIndex + y*src.stride + radiusX;
			int indexDst = dst.startIndex + y*dst.stride + radiusX;

			// index of last pixel along x-axis it should process
			int end = src.startIndex + y*src.stride + src.width - radiusX;
			while (indexSrc < end) {
				// Apply the transform. It's assumed that the src image has been passed into the lambda
				dst.data[indexDst++] = (short)filter.apply(indexSrc++, workspace);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void filterRectCenterEdge( GrayI16 src, int radiusX, int radiusY, GrayI16 dst,
											 @Nullable Object workspace, Rect_S32 filter ) {
		// top edge
		for (int y = 0; y < radiusY; y++) {
			int y0 = 0;
			int y1 = Math.min(src.height, y + radiusY + 1);

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (short)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		// bottom edge
		for (int y = Math.max(0, src.height - radiusY); y < src.height; y++) {
			int y0 = Math.max(0, y - radiusY);
			int y1 = src.height;

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (short)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		for (int y = radiusY; y < Math.max(0, src.height - radiusY); y++) {
			int y0 = y - radiusY;
			int y1 = y + radiusY + 1;

			int indexDstRow = dst.startIndex + y*dst.stride;

			// left side
			for (int x = 0; x < radiusX; x++) {
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (short)filter.apply(x, y, 0, y0, x1, y1, workspace);
			}

			// right side
			for (int x = Math.max(0, src.width - radiusX); x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (short)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}
	}

	public static void filterRectCenterInner( GrayS32 src, int radiusX, int radiusY, GrayS32 dst,
											  @Nullable Object workspace, RectCenter_S32 filter ) {
		final int y0 = radiusY;
		final int y1 = src.height - radiusY;

		// Go through all inner pixels

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y -> {
		for (int y = y0; y < y1; y++) {
			int indexSrc = src.startIndex + y*src.stride + radiusX;
			int indexDst = dst.startIndex + y*dst.stride + radiusX;

			// index of last pixel along x-axis it should process
			int end = src.startIndex + y*src.stride + src.width - radiusX;
			while (indexSrc < end) {
				// Apply the transform. It's assumed that the src image has been passed into the lambda
				dst.data[indexDst++] = (int)filter.apply(indexSrc++, workspace);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void filterRectCenterEdge( GrayS32 src, int radiusX, int radiusY, GrayS32 dst,
											 @Nullable Object workspace, Rect_S32 filter ) {
		// top edge
		for (int y = 0; y < radiusY; y++) {
			int y0 = 0;
			int y1 = Math.min(src.height, y + radiusY + 1);

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (int)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		// bottom edge
		for (int y = Math.max(0, src.height - radiusY); y < src.height; y++) {
			int y0 = Math.max(0, y - radiusY);
			int y1 = src.height;

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (int)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		for (int y = radiusY; y < Math.max(0, src.height - radiusY); y++) {
			int y0 = y - radiusY;
			int y1 = y + radiusY + 1;

			int indexDstRow = dst.startIndex + y*dst.stride;

			// left side
			for (int x = 0; x < radiusX; x++) {
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (int)filter.apply(x, y, 0, y0, x1, y1, workspace);
			}

			// right side
			for (int x = Math.max(0, src.width - radiusX); x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (int)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}
	}

	public static void filterRectCenterInner( GrayS64 src, int radiusX, int radiusY, GrayS64 dst,
											  @Nullable Object workspace, RectCenter_S64 filter ) {
		final int y0 = radiusY;
		final int y1 = src.height - radiusY;

		// Go through all inner pixels

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y -> {
		for (int y = y0; y < y1; y++) {
			int indexSrc = src.startIndex + y*src.stride + radiusX;
			int indexDst = dst.startIndex + y*dst.stride + radiusX;

			// index of last pixel along x-axis it should process
			int end = src.startIndex + y*src.stride + src.width - radiusX;
			while (indexSrc < end) {
				// Apply the transform. It's assumed that the src image has been passed into the lambda
				dst.data[indexDst++] = (long)filter.apply(indexSrc++, workspace);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void filterRectCenterEdge( GrayS64 src, int radiusX, int radiusY, GrayS64 dst,
											 @Nullable Object workspace, Rect_S64 filter ) {
		// top edge
		for (int y = 0; y < radiusY; y++) {
			int y0 = 0;
			int y1 = Math.min(src.height, y + radiusY + 1);

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (long)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		// bottom edge
		for (int y = Math.max(0, src.height - radiusY); y < src.height; y++) {
			int y0 = Math.max(0, y - radiusY);
			int y1 = src.height;

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (long)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		for (int y = radiusY; y < Math.max(0, src.height - radiusY); y++) {
			int y0 = y - radiusY;
			int y1 = y + radiusY + 1;

			int indexDstRow = dst.startIndex + y*dst.stride;

			// left side
			for (int x = 0; x < radiusX; x++) {
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (long)filter.apply(x, y, 0, y0, x1, y1, workspace);
			}

			// right side
			for (int x = Math.max(0, src.width - radiusX); x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (long)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}
	}

	public static void filterRectCenterInner( GrayF32 src, int radiusX, int radiusY, GrayF32 dst,
											  @Nullable Object workspace, RectCenter_F32 filter ) {
		final int y0 = radiusY;
		final int y1 = src.height - radiusY;

		// Go through all inner pixels

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y -> {
		for (int y = y0; y < y1; y++) {
			int indexSrc = src.startIndex + y*src.stride + radiusX;
			int indexDst = dst.startIndex + y*dst.stride + radiusX;

			// index of last pixel along x-axis it should process
			int end = src.startIndex + y*src.stride + src.width - radiusX;
			while (indexSrc < end) {
				// Apply the transform. It's assumed that the src image has been passed into the lambda
				dst.data[indexDst++] = (float)filter.apply(indexSrc++, workspace);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void filterRectCenterEdge( GrayF32 src, int radiusX, int radiusY, GrayF32 dst,
											 @Nullable Object workspace, Rect_F32 filter ) {
		// top edge
		for (int y = 0; y < radiusY; y++) {
			int y0 = 0;
			int y1 = Math.min(src.height, y + radiusY + 1);

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (float)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		// bottom edge
		for (int y = Math.max(0, src.height - radiusY); y < src.height; y++) {
			int y0 = Math.max(0, y - radiusY);
			int y1 = src.height;

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (float)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		for (int y = radiusY; y < Math.max(0, src.height - radiusY); y++) {
			int y0 = y - radiusY;
			int y1 = y + radiusY + 1;

			int indexDstRow = dst.startIndex + y*dst.stride;

			// left side
			for (int x = 0; x < radiusX; x++) {
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (float)filter.apply(x, y, 0, y0, x1, y1, workspace);
			}

			// right side
			for (int x = Math.max(0, src.width - radiusX); x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (float)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}
	}

	public static void filterRectCenterInner( GrayF64 src, int radiusX, int radiusY, GrayF64 dst,
											  @Nullable Object workspace, RectCenter_F64 filter ) {
		final int y0 = radiusY;
		final int y1 = src.height - radiusY;

		// Go through all inner pixels

		//CONCURRENT_BELOW BoofConcurrency.loopFor(y0, y1 , y -> {
		for (int y = y0; y < y1; y++) {
			int indexSrc = src.startIndex + y*src.stride + radiusX;
			int indexDst = dst.startIndex + y*dst.stride + radiusX;

			// index of last pixel along x-axis it should process
			int end = src.startIndex + y*src.stride + src.width - radiusX;
			while (indexSrc < end) {
				// Apply the transform. It's assumed that the src image has been passed into the lambda
				dst.data[indexDst++] = (double)filter.apply(indexSrc++, workspace);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void filterRectCenterEdge( GrayF64 src, int radiusX, int radiusY, GrayF64 dst,
											 @Nullable Object workspace, Rect_F64 filter ) {
		// top edge
		for (int y = 0; y < radiusY; y++) {
			int y0 = 0;
			int y1 = Math.min(src.height, y + radiusY + 1);

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (double)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		// bottom edge
		for (int y = Math.max(0, src.height - radiusY); y < src.height; y++) {
			int y0 = Math.max(0, y - radiusY);
			int y1 = src.height;

			int indexDstRow = dst.startIndex + y*dst.stride;
			for (int x = 0; x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (double)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}

		for (int y = radiusY; y < Math.max(0, src.height - radiusY); y++) {
			int y0 = y - radiusY;
			int y1 = y + radiusY + 1;

			int indexDstRow = dst.startIndex + y*dst.stride;

			// left side
			for (int x = 0; x < radiusX; x++) {
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (double)filter.apply(x, y, 0, y0, x1, y1, workspace);
			}

			// right side
			for (int x = Math.max(0, src.width - radiusX); x < src.width; x++) {
				int x0 = Math.max(0, x - radiusX);
				int x1 = Math.min(src.width, x + radiusX + 1);
				dst.data[indexDstRow + x] = (double)filter.apply(x, y, x0, y0, x1, y1, workspace);
			}
		}
	}

	// indexPixel = index of pixel in the src image. Pixel index is passed in to avoid extra math

	// @formatter:off
	public @FunctionalInterface interface RectCenter_S32 { int apply( int indexPixel, Object workspace ); }
	public @FunctionalInterface interface RectCenter_S64 { long apply( int indexPixel, Object workspace ); }
	public @FunctionalInterface interface RectCenter_F32 { float apply( int indexPixel, Object workspace ); }
	public @FunctionalInterface interface RectCenter_F64 { double apply( int indexPixel, Object workspace ); }
	// @formatter:on

	// (cx, cy) = center pixel. (x0,y0) = lower extent. (x1, y1) = upper extent

	// @formatter:off
	public @FunctionalInterface interface Rect_S32 { int apply( int cx, int cy, int x0, int y0, int x1, int y1, Object workspace ); }
	public @FunctionalInterface interface Rect_S64 { long apply( int cx, int cy, int x0, int y0, int x1, int y1, Object workspace ); }
	public @FunctionalInterface interface Rect_F32 { float apply( int cx, int cy, int x0, int y0, int x1, int y1, Object workspace ); }
	public @FunctionalInterface interface Rect_F64 { double apply(int cx, int cy,  int x0, int y0, int x1, int y1, Object workspace ); }
	// @formatter:on
}
