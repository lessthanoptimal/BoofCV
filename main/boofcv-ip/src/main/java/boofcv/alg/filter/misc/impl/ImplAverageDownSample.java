/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc.impl;

import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p> * Overlays a rectangular grid on top of the src image and computes the average value within each cell
 * which is then written into the dst image. The dst image must be smaller than or equal to the src image.</p>
 *
 * <p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplAverageDownSample</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.misc.impl.GenerateImplAverageDownSample")
public class ImplAverageDownSample {

	/// Computes the border of the upper extent for the output image for regions that are entirely contained inside
	/// the input image
	protected static int upperBorder(int inputLength, int outputLength, float offset, float regionWidth) {
		return outputLength*regionWidth - offset > inputLength ? outputLength - 1 : outputLength;
	}

	public static float naivePixelHorizontal( GrayU8 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float x0 = outX*regionWidth + offset;
		float x1 = x0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(x0);
		int idx1 = (int)Math.min(input.width, Math.ceil(x1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			x0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		float sum = 0;
		for (int x = idx0; x < idx1; x++) {
			float intersection = Math.min(x + 1.0f, x1) - x0;
			area += intersection;
			sum += intersection*input.get(x, outY);
			x0 = x + 1.0f;
		}

		return sum/area;
	}

	public static float naivePixelHorizontal( GrayU16 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float x0 = outX*regionWidth + offset;
		float x1 = x0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(x0);
		int idx1 = (int)Math.min(input.width, Math.ceil(x1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			x0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		float sum = 0;
		for (int x = idx0; x < idx1; x++) {
			float intersection = Math.min(x + 1.0f, x1) - x0;
			area += intersection;
			sum += intersection*input.get(x, outY);
			x0 = x + 1.0f;
		}

		return sum/area;
	}

	public static float naivePixelHorizontal( GrayF32 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float x0 = outX*regionWidth + offset;
		float x1 = x0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(x0);
		int idx1 = (int)Math.min(input.width, Math.ceil(x1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			x0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		float sum = 0;
		for (int x = idx0; x < idx1; x++) {
			float intersection = Math.min(x + 1.0f, x1) - x0;
			area += intersection;
			sum += intersection*input.get(x, outY);
			x0 = x + 1.0f;
		}

		return sum/area;
	}

	public static double naivePixelHorizontal( GrayF64 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float x0 = outX*regionWidth + offset;
		float x1 = x0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(x0);
		int idx1 = (int)Math.min(input.width, Math.ceil(x1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			x0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		double sum = 0;
		for (int x = idx0; x < idx1; x++) {
			float intersection = Math.min(x + 1.0f, x1) - x0;
			area += intersection;
			sum += intersection*input.get(x, outY);
			x0 = x + 1.0f;
		}

		return sum/area;
	}

	public static float naivePixelVertical( GrayU8 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float y0 = outY*regionWidth + offset;
		float y1 = y0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(y0);
		int idx1 = (int)Math.min(input.height, Math.ceil(y1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			y0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		float sum = 0.0f;
		for (int y = idx0; y < idx1; y++) {
			float intersection = Math.min(y + 1.0f, y1) - y0;
			area += intersection;
			sum += intersection*input.get(outX, y);
			y0 = y + 1.0f;
		}

		return sum/area;
	}

	public static float naivePixelVertical( GrayU16 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float y0 = outY*regionWidth + offset;
		float y1 = y0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(y0);
		int idx1 = (int)Math.min(input.height, Math.ceil(y1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			y0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		float sum = 0.0f;
		for (int y = idx0; y < idx1; y++) {
			float intersection = Math.min(y + 1.0f, y1) - y0;
			area += intersection;
			sum += intersection*input.get(outX, y);
			y0 = y + 1.0f;
		}

		return sum/area;
	}

	public static float naivePixelVertical( GrayF32 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float y0 = outY*regionWidth + offset;
		float y1 = y0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(y0);
		int idx1 = (int)Math.min(input.height, Math.ceil(y1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			y0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		float sum = 0.0f;
		for (int y = idx0; y < idx1; y++) {
			float intersection = Math.min(y + 1.0f, y1) - y0;
			area += intersection;
			sum += intersection*input.get(outX, y);
			y0 = y + 1.0f;
		}

		return sum/area;
	}

	public static double naivePixelVertical( GrayF64 input, float offset, float regionWidth, int outX, int outY ) {
		// Convert to input pixel coordinates for the range of values it will sample
		float y0 = outY*regionWidth + offset;
		float y1 = y0 + regionWidth;

		// Discrete pixels it will sample
		int idx0 = (int)Math.floor(y0);
		int idx1 = (int)Math.min(input.height, Math.ceil(y1));

		// Can't sample outside the image
		if (idx0 < 0) {
			idx0 = 0;
			y0 = 0.0f;
		}

		// Sample values and compute overlap
		float area = 0.0f;
		double sum = 0.0f;
		for (int y = idx0; y < idx1; y++) {
			float intersection = Math.min(y + 1.0f, y1) - y0;
			area += intersection;
			sum += intersection*input.get(outX, y);
			y0 = y + 1.0f;
		}

		return sum/area;
	}

	/// Down samples the image along the x-axis only. Image height's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	public static void horizontal( GrayU8 src , boolean centered, GrayF32 dst ) {

		if (src.width < dst.width)
			throw new IllegalArgumentException("src width must be >= dst width");
		if (src.height != dst.height)
			throw new IllegalArgumentException("src height must equal dst height");

		float scale = src.width/(float)dst.width;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y*dst.stride;

			if (centered) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, 0, y);
			}

			for (int x = dstBorder0; x < dstBorder1; x++) {
				float srcX0 = x*scale + offset;
				float srcX1 = (x + 1)*scale + offset;

				int isrcX0 = (int)srcX0;
				int isrcX1 = (int)srcX1;

				int indexSrc = src.getIndex(isrcX0, y);

				// compute value of overlapped region
				float sum = (isrcX0 + 1 - srcX0)*(src.data[indexSrc++]& 0xFF);

				for (int i = isrcX0 + 1; i < isrcX1; i++) {
					sum += src.data[indexSrc++]& 0xFF;
				}

				if (isrcX1 < srcX1) {
					sum += (srcX1 - isrcX1)*(src.data[indexSrc]& 0xFF);
				}

				dst.data[indexDst++] = sum/scale;
			}

			if (dstBorder1 != dst.width) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, dstBorder1, y);
			}
		}
		//CONCURRENT_ABOVE });
	}

	/// Same as vertical but workspace is null by default
	public static void vertical( GrayF32 src, boolean centered, GrayI8 dst) {
		vertical(src, centered, dst, null);
	}

	/// Down samples the image along the y-axis only. Image width's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	/// @param workspaces (Optional) Storage for workspace used to store intermediate results
	public static void vertical( GrayF32 src, boolean centered, GrayI8 dst, @Nullable GrowArray<DogArray_F32> workspaces ) {
		if (src.height < dst.height)
			throw new IllegalArgumentException("src height must be >= dst height");
		if (src.width != dst.width)
			throw new IllegalArgumentException("src width must equal dst width");

		float scale = src.height/(float)dst.height;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		// If workspace was not provided declare it
		if (workspaces == null) {
			workspaces = new GrowArray<>(DogArray_F32::new);
		}

		workspaces.reset();
		DogArray_F32 workspace = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		int idx0 = 0, idx1 = dst.height; //CONCURRENT_REMOVE_LINE

		//CONCURRENT_INLINE BoofConcurrency.loopBlocks(0, dst.height, workspaces, (workspace, idx0, idx1) -> {
		float[] workArray = workspace.resize(dst.width).data;
		for (int y = idx0; y < idx1; y++) {
			if (y < dstBorder0) {
				for (int x = 0; x < dst.width; x++) {
					workArray[x] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else if (y >= dstBorder1) {
				for (int x = 0; x < dst.width; x++) {
					workArray[x] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else {
				// process incrementally along the columns to reduce CPU cache misses
				float y0 = y*scale + offset;
				float y1 = (y + 1)*scale + offset;

				// Convert to integer values
				int isrcY0 = (int)y0;
				int isrcY1 = (int)y1;

				int srcIndex = src.getIndex(0, isrcY0);
				float area = isrcY0 + 1 - y0;
				for (int x = 0; x < dst.width; x++) {
					workArray[x] = area*src.data[srcIndex++];
				}
				isrcY0++;

				area += isrcY1 - isrcY0;
				for (int innerY = isrcY0; innerY < isrcY1; innerY++) {
					srcIndex = src.getIndex(0, innerY);
					for (int x = 0; x < dst.width; x++) {
						workArray[x] += src.data[srcIndex++];
					}
				}

				if (isrcY1 < y1) {
					float intersection = y1 - isrcY1;
					area += intersection;
					srcIndex = src.getIndex(0, isrcY1);
					for (int x = 0; x < dst.width; x++) {
						workArray[x] += intersection*src.data[srcIndex++];
					}
				}

				for (int x = 0; x < dst.width; x++) {
					workArray[x] /= area;
				}
			}

			int dstIndex = dst.getIndex(0, y);
			for (int x = 0; x < dst.width; x++) {
				dst.data[dstIndex++] = (byte)(workArray[x] + + 0.5f);
			}
		}
		//CONCURRENT_INLINE });
	}

	/// Down samples the image along the x-axis only. Image height's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	public static void horizontal( GrayU16 src , boolean centered, GrayF32 dst ) {

		if (src.width < dst.width)
			throw new IllegalArgumentException("src width must be >= dst width");
		if (src.height != dst.height)
			throw new IllegalArgumentException("src height must equal dst height");

		float scale = src.width/(float)dst.width;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y*dst.stride;

			if (centered) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, 0, y);
			}

			for (int x = dstBorder0; x < dstBorder1; x++) {
				float srcX0 = x*scale + offset;
				float srcX1 = (x + 1)*scale + offset;

				int isrcX0 = (int)srcX0;
				int isrcX1 = (int)srcX1;

				int indexSrc = src.getIndex(isrcX0, y);

				// compute value of overlapped region
				float sum = (isrcX0 + 1 - srcX0)*(src.data[indexSrc++]& 0xFFFF);

				for (int i = isrcX0 + 1; i < isrcX1; i++) {
					sum += src.data[indexSrc++]& 0xFFFF;
				}

				if (isrcX1 < srcX1) {
					sum += (srcX1 - isrcX1)*(src.data[indexSrc]& 0xFFFF);
				}

				dst.data[indexDst++] = sum/scale;
			}

			if (dstBorder1 != dst.width) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, dstBorder1, y);
			}
		}
		//CONCURRENT_ABOVE });
	}

	/// Same as vertical but workspace is null by default
	public static void vertical( GrayF32 src, boolean centered, GrayI16 dst) {
		vertical(src, centered, dst, null);
	}

	/// Down samples the image along the y-axis only. Image width's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	/// @param workspaces (Optional) Storage for workspace used to store intermediate results
	public static void vertical( GrayF32 src, boolean centered, GrayI16 dst, @Nullable GrowArray<DogArray_F32> workspaces ) {
		if (src.height < dst.height)
			throw new IllegalArgumentException("src height must be >= dst height");
		if (src.width != dst.width)
			throw new IllegalArgumentException("src width must equal dst width");

		float scale = src.height/(float)dst.height;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		// If workspace was not provided declare it
		if (workspaces == null) {
			workspaces = new GrowArray<>(DogArray_F32::new);
		}

		workspaces.reset();
		DogArray_F32 workspace = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		int idx0 = 0, idx1 = dst.height; //CONCURRENT_REMOVE_LINE

		//CONCURRENT_INLINE BoofConcurrency.loopBlocks(0, dst.height, workspaces, (workspace, idx0, idx1) -> {
		float[] workArray = workspace.resize(dst.width).data;
		for (int y = idx0; y < idx1; y++) {
			if (y < dstBorder0) {
				for (int x = 0; x < dst.width; x++) {
					workArray[x] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else if (y >= dstBorder1) {
				for (int x = 0; x < dst.width; x++) {
					workArray[x] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else {
				// process incrementally along the columns to reduce CPU cache misses
				float y0 = y*scale + offset;
				float y1 = (y + 1)*scale + offset;

				// Convert to integer values
				int isrcY0 = (int)y0;
				int isrcY1 = (int)y1;

				int srcIndex = src.getIndex(0, isrcY0);
				float area = isrcY0 + 1 - y0;
				for (int x = 0; x < dst.width; x++) {
					workArray[x] = area*src.data[srcIndex++];
				}
				isrcY0++;

				area += isrcY1 - isrcY0;
				for (int innerY = isrcY0; innerY < isrcY1; innerY++) {
					srcIndex = src.getIndex(0, innerY);
					for (int x = 0; x < dst.width; x++) {
						workArray[x] += src.data[srcIndex++];
					}
				}

				if (isrcY1 < y1) {
					float intersection = y1 - isrcY1;
					area += intersection;
					srcIndex = src.getIndex(0, isrcY1);
					for (int x = 0; x < dst.width; x++) {
						workArray[x] += intersection*src.data[srcIndex++];
					}
				}

				for (int x = 0; x < dst.width; x++) {
					workArray[x] /= area;
				}
			}

			int dstIndex = dst.getIndex(0, y);
			for (int x = 0; x < dst.width; x++) {
				dst.data[dstIndex++] = (short)(workArray[x] + + 0.5f);
			}
		}
		//CONCURRENT_INLINE });
	}

	/// Down samples the image along the x-axis only. Image height's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	public static void horizontal( GrayF32 src , boolean centered, GrayF32 dst ) {

		if (src.width < dst.width)
			throw new IllegalArgumentException("src width must be >= dst width");
		if (src.height != dst.height)
			throw new IllegalArgumentException("src height must equal dst height");

		float scale = src.width/(float)dst.width;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y*dst.stride;

			if (centered) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, 0, y);
			}

			for (int x = dstBorder0; x < dstBorder1; x++) {
				float srcX0 = x*scale + offset;
				float srcX1 = (x + 1)*scale + offset;

				int isrcX0 = (int)srcX0;
				int isrcX1 = (int)srcX1;

				int indexSrc = src.getIndex(isrcX0, y);

				// compute value of overlapped region
				float sum = (isrcX0 + 1 - srcX0)*(src.data[indexSrc++]);

				for (int i = isrcX0 + 1; i < isrcX1; i++) {
					sum += src.data[indexSrc++];
				}

				if (isrcX1 < srcX1) {
					sum += (srcX1 - isrcX1)*(src.data[indexSrc]);
				}

				dst.data[indexDst++] = sum/scale;
			}

			if (dstBorder1 != dst.width) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, dstBorder1, y);
			}
		}
		//CONCURRENT_ABOVE });
	}

	/// Down samples the image along the y-axis only. Image width's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	public static void vertical( GrayF32 src, boolean centered, GrayF32 dst ) {
		if (src.height < dst.height)
			throw new IllegalArgumentException("src height must be >= dst height");
		if (src.width != dst.width)
			throw new IllegalArgumentException("src width must equal dst width");

		float scale = src.height/(float)dst.height;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int dstIndex = dst.getIndex(0, y);
			if (y < dstBorder0) {
				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex++] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else if (y >= dstBorder1) {
				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex++] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else {
				// process incrementally along the columns to reduce CPU cache misses
				float y0 = y*scale + offset;
				float y1 = (y + 1)*scale + offset;

				// Convert to integer values
				int isrcY0 = (int)y0;
				int isrcY1 = (int)y1;

				int srcIndex = src.getIndex(0, isrcY0);
				float area = isrcY0 + 1 - y0;
				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex + x] = area*src.data[srcIndex++];
				}
				isrcY0++;

				area += isrcY1 - isrcY0;
				for (int innerY = isrcY0; innerY < isrcY1; innerY++) {
					srcIndex = src.getIndex(0, innerY);
					for (int x = 0; x < dst.width; x++) {
						dst.data[dstIndex + x] += src.data[srcIndex++];
					}
				}

				if (isrcY1 < y1) {
					float intersection = y1 - isrcY1;
					area += intersection;
					srcIndex = src.getIndex(0, isrcY1);
					for (int x = 0; x < dst.width; x++) {
						dst.data[dstIndex + x] += intersection*src.data[srcIndex++];
					}
				}

				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex + x] /= area;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	/// Down samples the image along the x-axis only. Image height's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	public static void horizontal( GrayF64 src , boolean centered, GrayF64 dst ) {

		if (src.width < dst.width)
			throw new IllegalArgumentException("src width must be >= dst width");
		if (src.height != dst.height)
			throw new IllegalArgumentException("src height must equal dst height");

		float scale = src.width/(float)dst.width;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y*dst.stride;

			if (centered) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, 0, y);
			}

			for (int x = dstBorder0; x < dstBorder1; x++) {
				float srcX0 = x*scale + offset;
				float srcX1 = (x + 1)*scale + offset;

				int isrcX0 = (int)srcX0;
				int isrcX1 = (int)srcX1;

				int indexSrc = src.getIndex(isrcX0, y);

				// compute value of overlapped region
				double sum = (isrcX0 + 1 - srcX0)*(src.data[indexSrc++]);

				for (int i = isrcX0 + 1; i < isrcX1; i++) {
					sum += src.data[indexSrc++];
				}

				if (isrcX1 < srcX1) {
					sum += (srcX1 - isrcX1)*(src.data[indexSrc]);
				}

				dst.data[indexDst++] = sum/scale;
			}

			if (dstBorder1 != dst.width) {
				dst.data[indexDst++] = naivePixelHorizontal(src, offset, scale, dstBorder1, y);
			}
		}
		//CONCURRENT_ABOVE });
	}

	/// Down samples the image along the y-axis only. Image width's must be the same.
	///
	/// @param src Input image. Not modified.
	/// @param centered The kernel will be centered to avoid shifting pixel values.
	/// @param dst Output image. Modified.
	public static void vertical( GrayF64 src, boolean centered, GrayF64 dst ) {
		if (src.height < dst.height)
			throw new IllegalArgumentException("src height must be >= dst height");
		if (src.width != dst.width)
			throw new IllegalArgumentException("src width must equal dst width");

		float scale = src.height/(float)dst.height;
		float offset = centered ? -(scale - 1.0f)/2.0f : 0.0f;
		int dstBorder0 = centered ? 1 : 0;
		int dstBorder1 = upperBorder(src.width, dst.width, offset, scale);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int dstIndex = dst.getIndex(0, y);
			if (y < dstBorder0) {
				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex++] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else if (y >= dstBorder1) {
				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex++] = naivePixelVertical(src, offset, scale, x, y);
				}
			} else {
				// process incrementally along the columns to reduce CPU cache misses
				float y0 = y*scale + offset;
				float y1 = (y + 1)*scale + offset;

				// Convert to integer values
				int isrcY0 = (int)y0;
				int isrcY1 = (int)y1;

				int srcIndex = src.getIndex(0, isrcY0);
				float area = isrcY0 + 1 - y0;
				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex + x] = area*src.data[srcIndex++];
				}
				isrcY0++;

				area += isrcY1 - isrcY0;
				for (int innerY = isrcY0; innerY < isrcY1; innerY++) {
					srcIndex = src.getIndex(0, innerY);
					for (int x = 0; x < dst.width; x++) {
						dst.data[dstIndex + x] += src.data[srcIndex++];
					}
				}

				if (isrcY1 < y1) {
					float intersection = y1 - isrcY1;
					area += intersection;
					srcIndex = src.getIndex(0, isrcY1);
					for (int x = 0; x < dst.width; x++) {
						dst.data[dstIndex + x] += intersection*src.data[srcIndex++];
					}
				}

				for (int x = 0; x < dst.width; x++) {
					dst.data[dstIndex + x] /= area;
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

}
