/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.noborder;

import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_F64;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArrayPrimitive;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Generated;
import java.util.Arrays;

/**
 * Convolves a mean filter across the image. Each output pixel is the mean of 'length' pixels, starting 'offset'
 * pixels before it. Near the image border the mean is of the pixels inside the image, unless an
 * {@link boofcv.struct.border.ImageBorder} gives the pixels outside the image. Each function also has a version for
 * a range of rows, which {@link ImplConvolveMean_MT} calls on each block.
 *
 * <p>The vertical pass keeps the last 'length' input rows in a ring of row buffers, so every array in the inner loop
 * has the same index and the loop can be vectorized. S16 and U16 row buffers are double so the division can be
 * vectorized, since x86-64 has no vector integer division. Their sums are integers below 2^53, so double arithmetic
 * is exact in any order and (int)(sum/length) equals integer division. U8 divides with an exact multiply and
 * shift.</p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplConvolveMean</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.noborder.GenerateImplConvolveMean")
@SuppressWarnings({"ForLoopReplaceableByForEach", "Duplicates"})
public class ImplConvolveMean {

	/// Resizes the vertical pass's buffers: length + 1 ring rows, totals and the row being written
	private static void declareRows( DogArray<? extends DogArrayPrimitive<?>> rows, int length, int width ) {
		rows.resize(length + 3);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(width);
		}
	}

	/// Mean along x. See the class description for the border
	public static void horizontal( GrayU8 input, GrayI8 output, int offset, int length,
								   @Nullable ImageBorder_S32<GrayU8> border ) {
		horizontal(input, output, offset, length, border, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayU8 input, GrayI8 output, int offset, int length,
								   @Nullable ImageBorder_S32<GrayU8> border, int y0, int y1 ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		// pixels outside the image to the left and right of a row
		final int[] outside = border == null ? null : new int[offset + offsetR];

		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*input.stride;
			final int indexOut = output.startIndex + y*output.stride;
			if (border != null) {
				for (int i = 0; i < offset; i++) {
					outside[i] = border.get(i - offset, y);
				}
				for (int i = 0; i < offsetR; i++) {
					outside[offset + i] = border.get(width + i, y);
				}
			}

			if (border == null && length <= width) {
				borderInside(input, output, offset, length, indexIn, indexOut);
				continue;
			}

			// Running sums along the left and then the right border. Without a border only pixels inside
			// the image are counted
			for (int side = 0; side < 2; side++) {
				final int xStart = side == 0 ? 0 : Math.max(Math.min(offset, width), width - offsetR);
				final int xEnd = side == 0 ? Math.min(offset, width) : width;
				int total = 0;
				int count = 0;
				for (int x = xStart; x < xEnd; x++) {
					if (x == xStart) {
						for (int i = x - offset; i < x - offset + length; i++) {
							total += pixel(input, indexIn, i, outside, offset);
							count += i >= 0 && i < width ? 1 : 0;
						}
					} else {
						final int removed = x - offset - 1, added = x - offset + length - 1;
						total -= pixel(input, indexIn, removed, outside, offset);
						total += pixel(input, indexIn, added, outside, offset);
						count += (added < width ? 1 : 0) - (removed >= 0 ? 1 : 0);
					}
					final int divisor = border == null ? count : length;
					output.data[indexOut + x] = (byte)((total + divisor/2)/divisor);
				}
			}
		}
		horizontalInner(input, output, offset, length, y0, y1);
	}

	private static void borderInside( GrayU8 input, GrayI8 output, int offset, int length, int indexIn,
									  int indexOut ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		if (offset > 0) {
			int total = 0;
			int count = length - offset;
			for (int i = 0; i < count; i++) {
				total += input.data[indexIn + i]& 0xFF;
			}
			output.data[indexOut] = (byte)((total + count/2)/count);
			for (int x = 1; x < offset; x++) {
				total += input.data[indexIn + x + offsetR]& 0xFF;
				count++;
				output.data[indexOut + x] = (byte)((total + count/2)/count);
			}
		}
		if (offsetR > 0) {
			final int x0 = width - offsetR;
			int total = 0;
			int count = offsetR + offset;
			for (int i = x0 - offset; i < width; i++) {
				total += input.data[indexIn + i]& 0xFF;
			}
			output.data[indexOut + x0] = (byte)((total + count/2)/count);
			for (int x = x0 + 1; x < width; x++) {
				total -= input.data[indexIn + x - offset - 1]& 0xFF;
				count--;
				output.data[indexOut + x] = (byte)((total + count/2)/count);
			}
		}
	}

	/// horizontal() without the border
	public static void horizontalInner( GrayU8 input, GrayI8 output, int offset, int length ) {
		horizontalInner(input, output, offset, length, 0, input.height);
	}

	/// horizontalInner() of the rows y0 to y1
	public static void horizontalInner( GrayU8 input, GrayI8 output, int offset, int length, int y0, int y1 ) {
		final int width = input.width;
		if (length > width)
			return;
		final int half = length/2;
		// (n*multiplier) >>> 23 equals n/length while n*error < 2^23
		final int shift = 23;
		final long multiplier = ((1L << shift) + length - 1)/length;
		final long maxNumerator = 255L*length + length/2;
		final boolean exact = maxNumerator*(multiplier*length - (1L << shift)) < (1L << shift) &&
				maxNumerator*multiplier < (1L << 32);
		final int m = (int)multiplier;
		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			int total = 0;
			int indexEnd = indexIn + length;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn]& 0xFF;
			}
			output.data[indexOut++] = (byte)((total + half)/length);

			indexEnd = indexIn + width - length;
			if (exact) {
				for (; indexIn < indexEnd; indexIn++) {
					total -= input.data[indexIn - length]& 0xFF;
					total += input.data[indexIn]& 0xFF;
					output.data[indexOut++] = (byte)(((total + half)*m) >>> shift);
				}
				continue;
			}
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length]& 0xFF;
				total += input.data[indexIn]& 0xFF;
				output.data[indexOut++] = (byte)((total + half)/length);
			}
		}
	}

	/// Mean along y. See the class description for the border
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayU8 input, GrayI8 output, int offset, int length,
								 @Nullable ImageBorder_S32<GrayU8> border, DogArray<DogArray_I32> rows ) {
		vertical(input, output, offset, length, border, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the block's first inner row
	public static void vertical( GrayU8 input, GrayI8 output, int offset, int length,
								 @Nullable ImageBorder_S32<GrayU8> border, int y0, int y1, DogArray<DogArray_I32> rows ) {
		final int width = input.width, height = input.height;
		final int offsetR = length - offset - 1;

		declareRows(rows, length, width);
		final int[] totals = rows.get(length + 1).data;
		final int[] row = rows.get(length + 2).data;

		// Border rows. Pixels outside the image are fetched once into the ring rows
		if (border != null) {
			if (y0 < offset) {
				for (int j = -offset; j < 0; j++) {
					fetchRow(border, width, j, rows.get(j + offset).data);
				}
			}
			if (y1 > height - offsetR) {
				for (int j = height; j < height + offsetR; j++) {
					fetchRow(border, width, j, rows.get(offset + j - height).data);
				}
			}
		}
		for (int y = y0; y < y1; y++) {
			if (y >= offset && y < height - offsetR) {
				y = height - offsetR - 1;
				continue;
			}
			Arrays.fill(totals, 0, width, 0);
			int count = 0;
			for (int k = 0; k < length; k++) {
				final int j = y - offset + k;
				final int[] values;
				if (j >= 0 && j < height) {
					copyRow(input, width, j, row);
					values = row;
					count++;
				} else if (border != null) {
					values = rows.get(j < 0 ? j + offset : offset + j - height).data;
				} else {
					continue;
				}
				for (int i = 0; i < width; i++) {
					totals[i] += values[i];
				}
			}
			final int divisor = border == null ? count : length;
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				output.data[indexOut + i] = (byte)((int)(totals[i] + divisor/2)/divisor);
			}
		}

		verticalInner(input, output, offset, length, y0, y1, rows);
	}

	/// vertical() without the border
	public static void verticalInner( GrayU8 input, GrayI8 output, int offset, int length,
									  DogArray<DogArray_I32> rows ) {
		verticalInner(input, output, offset, length, 0, input.height, rows);
	}

	/// verticalInner() of the output rows y0 to y1
	public static void verticalInner( GrayU8 input, GrayI8 output, int offset, int length, int y0, int y1,
									  DogArray<DogArray_I32> rows ) {
		final int offsetR = length - offset - 1;
		final int inner0 = Math.max(y0, offset), inner1 = Math.min(y1, input.height - offsetR);
		if (inner0 >= inner1)
			return;
		declareRows(rows, length, input.width);
		verticalBlock(input, output, offset, length, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0
	private static void verticalBlock( GrayU8 input, GrayI8 output, int offset, int length, int y0, int y1,
									   DogArray<DogArray_I32> rows ) {
		final int n = input.width;
		final int[] totals = rows.get(length + 1).data;
		final int[] quotients = rows.get(length + 2).data;
		final int half = length/2;
		// (n*multiplier) >>> 23 equals n/length while n*error < 2^23
		final int shift = 23;
		final long multiplier = ((1L << shift) + length - 1)/length;
		final long maxNumerator = 255L*length + length/2;
		final boolean exact = maxNumerator*(multiplier*length - (1L << shift)) < (1L << shift) &&
				maxNumerator*multiplier < (1L << 32);
		final int m = (int)multiplier;

		// Input row k is in buffer (k - first)%ringSize
		final int first = y0 - offset;
		final int ringSize = length + 1;
		for (int k = 0; k < length; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < length; k++) {
			final int[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] += row[i];
			}
		}

		for (int y = y0; y < y1; y++) {
			if (y > y0) {
				final int[] removed = rows.get((y - offset - 1 - first)%ringSize).data;
				final int[] added = rows.get((y - offset + length - 1 - first)%ringSize).data;
				copyRow(input, n, y - offset + length - 1, added);
				for (int i = 0; i < n; i++) {
					totals[i] = totals[i] - removed[i] + added[i];
				}
			}
			final int indexOut = output.startIndex + y*output.stride;
			if (exact) {
				for (int i = 0; i < n; i++) {
					quotients[i] = ((totals[i] + half)*m) >>> shift;
				}
			} else {
				for (int i = 0; i < n; i++) {
					quotients[i] = (totals[i] + half)/length;
				}
			}
			for (int i = 0; i < n; i++) {
				output.data[indexOut + i] = (byte)quotients[i];
			}
		}
	}

	/// Pixels of row j, which is outside the image, from the border
	private static void fetchRow( ImageBorder_S32<?> border, int width, int j, int[] row ) {
		for (int i = 0; i < width; i++) {
			row[i] = border.get(i, j);
		}
	}

	/// Pixel i of the row starting at indexIn. Outside the image it's from 'outside', or 0 if that's null
	private static int pixel( GrayU8 input, int indexIn, int i, @Nullable int[] outside, int offset ) {
		final int width = input.width;
		if (i >= 0 && i < width)
			return input.data[indexIn + i]& 0xFF;
		if (outside == null)
			return 0;
		return outside[i < 0 ? i + offset : offset + i - width];
	}

	private static void copyRow( GrayU8 input, int n, int y, int[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		for (int i = 0; i < n; i++) {
			row[i] = input.data[indexIn + i]& 0xFF;
		}
	}

	/// Mean along x. See the class description for the border
	public static void horizontal( GrayS16 input, GrayI16 output, int offset, int length,
								   @Nullable ImageBorder_S32<GrayS16> border ) {
		horizontal(input, output, offset, length, border, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayS16 input, GrayI16 output, int offset, int length,
								   @Nullable ImageBorder_S32<GrayS16> border, int y0, int y1 ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		// pixels outside the image to the left and right of a row
		final int[] outside = border == null ? null : new int[offset + offsetR];

		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*input.stride;
			final int indexOut = output.startIndex + y*output.stride;
			if (border != null) {
				for (int i = 0; i < offset; i++) {
					outside[i] = border.get(i - offset, y);
				}
				for (int i = 0; i < offsetR; i++) {
					outside[offset + i] = border.get(width + i, y);
				}
			}

			if (border == null && length <= width) {
				borderInside(input, output, offset, length, indexIn, indexOut);
				continue;
			}

			// Running sums along the left and then the right border. Without a border only pixels inside
			// the image are counted
			for (int side = 0; side < 2; side++) {
				final int xStart = side == 0 ? 0 : Math.max(Math.min(offset, width), width - offsetR);
				final int xEnd = side == 0 ? Math.min(offset, width) : width;
				int total = 0;
				int count = 0;
				for (int x = xStart; x < xEnd; x++) {
					if (x == xStart) {
						for (int i = x - offset; i < x - offset + length; i++) {
							total += pixel(input, indexIn, i, outside, offset);
							count += i >= 0 && i < width ? 1 : 0;
						}
					} else {
						final int removed = x - offset - 1, added = x - offset + length - 1;
						total -= pixel(input, indexIn, removed, outside, offset);
						total += pixel(input, indexIn, added, outside, offset);
						count += (added < width ? 1 : 0) - (removed >= 0 ? 1 : 0);
					}
					final int divisor = border == null ? count : length;
					output.data[indexOut + x] = (short)((total + divisor/2)/divisor);
				}
			}
		}
		horizontalInner(input, output, offset, length, y0, y1);
	}

	private static void borderInside( GrayS16 input, GrayI16 output, int offset, int length, int indexIn,
									  int indexOut ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		if (offset > 0) {
			int total = 0;
			int count = length - offset;
			for (int i = 0; i < count; i++) {
				total += input.data[indexIn + i];
			}
			output.data[indexOut] = (short)((total + count/2)/count);
			for (int x = 1; x < offset; x++) {
				total += input.data[indexIn + x + offsetR];
				count++;
				output.data[indexOut + x] = (short)((total + count/2)/count);
			}
		}
		if (offsetR > 0) {
			final int x0 = width - offsetR;
			int total = 0;
			int count = offsetR + offset;
			for (int i = x0 - offset; i < width; i++) {
				total += input.data[indexIn + i];
			}
			output.data[indexOut + x0] = (short)((total + count/2)/count);
			for (int x = x0 + 1; x < width; x++) {
				total -= input.data[indexIn + x - offset - 1];
				count--;
				output.data[indexOut + x] = (short)((total + count/2)/count);
			}
		}
	}

	/// horizontal() without the border
	public static void horizontalInner( GrayS16 input, GrayI16 output, int offset, int length ) {
		horizontalInner(input, output, offset, length, 0, input.height);
	}

	/// horizontalInner() of the rows y0 to y1
	public static void horizontalInner( GrayS16 input, GrayI16 output, int offset, int length, int y0, int y1 ) {
		final int width = input.width;
		if (length > width)
			return;
		final int half = length/2;
		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			int total = 0;
			int indexEnd = indexIn + length;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn];
			}
			output.data[indexOut++] = (short)((total + half)/length);

			indexEnd = indexIn + width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length];
				total += input.data[indexIn];
				output.data[indexOut++] = (short)((total + half)/length);
			}
		}
	}

	/// Mean along y. See the class description for the border
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayS16 input, GrayI16 output, int offset, int length,
								 @Nullable ImageBorder_S32<GrayS16> border, DogArray<DogArray_F64> rows ) {
		vertical(input, output, offset, length, border, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the block's first inner row
	public static void vertical( GrayS16 input, GrayI16 output, int offset, int length,
								 @Nullable ImageBorder_S32<GrayS16> border, int y0, int y1, DogArray<DogArray_F64> rows ) {
		final int width = input.width, height = input.height;
		final int offsetR = length - offset - 1;

		declareRows(rows, length, width);
		final double[] totals = rows.get(length + 1).data;
		final double[] row = rows.get(length + 2).data;

		// Border rows. Pixels outside the image are fetched once into the ring rows
		if (border != null) {
			if (y0 < offset) {
				for (int j = -offset; j < 0; j++) {
					fetchRow(border, width, j, rows.get(j + offset).data);
				}
			}
			if (y1 > height - offsetR) {
				for (int j = height; j < height + offsetR; j++) {
					fetchRow(border, width, j, rows.get(offset + j - height).data);
				}
			}
		}
		for (int y = y0; y < y1; y++) {
			if (y >= offset && y < height - offsetR) {
				y = height - offsetR - 1;
				continue;
			}
			Arrays.fill(totals, 0, width, 0);
			int count = 0;
			for (int k = 0; k < length; k++) {
				final int j = y - offset + k;
				final double[] values;
				if (j >= 0 && j < height) {
					copyRow(input, width, j, row);
					values = row;
					count++;
				} else if (border != null) {
					values = rows.get(j < 0 ? j + offset : offset + j - height).data;
				} else {
					continue;
				}
				for (int i = 0; i < width; i++) {
					totals[i] += values[i];
				}
			}
			final int divisor = border == null ? count : length;
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				output.data[indexOut + i] = (short)((int)(totals[i] + divisor/2)/divisor);
			}
		}

		verticalInner(input, output, offset, length, y0, y1, rows);
	}

	/// vertical() without the border
	public static void verticalInner( GrayS16 input, GrayI16 output, int offset, int length,
									  DogArray<DogArray_F64> rows ) {
		verticalInner(input, output, offset, length, 0, input.height, rows);
	}

	/// verticalInner() of the output rows y0 to y1
	public static void verticalInner( GrayS16 input, GrayI16 output, int offset, int length, int y0, int y1,
									  DogArray<DogArray_F64> rows ) {
		final int offsetR = length - offset - 1;
		final int inner0 = Math.max(y0, offset), inner1 = Math.min(y1, input.height - offsetR);
		if (inner0 >= inner1)
			return;
		declareRows(rows, length, input.width);
		verticalBlock(input, output, offset, length, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0
	private static void verticalBlock( GrayS16 input, GrayI16 output, int offset, int length, int y0, int y1,
									   DogArray<DogArray_F64> rows ) {
		final int n = input.width;
		final double[] totals = rows.get(length + 1).data;
		final double[] quotients = rows.get(length + 2).data;
		final int half = length/2;
		final double divisor = length;

		// Input row k is in buffer (k - first)%ringSize
		final int first = y0 - offset;
		final int ringSize = length + 1;
		for (int k = 0; k < length; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < length; k++) {
			final double[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] += row[i];
			}
		}

		for (int y = y0; y < y1; y++) {
			if (y > y0) {
				final double[] removed = rows.get((y - offset - 1 - first)%ringSize).data;
				final double[] added = rows.get((y - offset + length - 1 - first)%ringSize).data;
				copyRow(input, n, y - offset + length - 1, added);
				for (int i = 0; i < n; i++) {
					totals[i] = totals[i] - removed[i] + added[i];
				}
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < n; i++) {
				quotients[i] = (totals[i] + half)/divisor;
			}
			for (int i = 0; i < n; i++) {
				output.data[indexOut + i] = (short)(int)quotients[i];
			}
		}
	}

	/// Pixels of row j, which is outside the image, from the border
	private static void fetchRow( ImageBorder_S32<?> border, int width, int j, double[] row ) {
		for (int i = 0; i < width; i++) {
			row[i] = border.get(i, j);
		}
	}

	/// Pixel i of the row starting at indexIn. Outside the image it's from 'outside', or 0 if that's null
	private static int pixel( GrayS16 input, int indexIn, int i, @Nullable int[] outside, int offset ) {
		final int width = input.width;
		if (i >= 0 && i < width)
			return input.data[indexIn + i];
		if (outside == null)
			return 0;
		return outside[i < 0 ? i + offset : offset + i - width];
	}

	private static void copyRow( GrayS16 input, int n, int y, double[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		for (int i = 0; i < n; i++) {
			row[i] = input.data[indexIn + i];
		}
	}

	/// Mean along x. See the class description for the border
	public static void horizontal( GrayU16 input, GrayI16 output, int offset, int length,
								   @Nullable ImageBorder_S32<GrayU16> border ) {
		horizontal(input, output, offset, length, border, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayU16 input, GrayI16 output, int offset, int length,
								   @Nullable ImageBorder_S32<GrayU16> border, int y0, int y1 ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		// pixels outside the image to the left and right of a row
		final int[] outside = border == null ? null : new int[offset + offsetR];

		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*input.stride;
			final int indexOut = output.startIndex + y*output.stride;
			if (border != null) {
				for (int i = 0; i < offset; i++) {
					outside[i] = border.get(i - offset, y);
				}
				for (int i = 0; i < offsetR; i++) {
					outside[offset + i] = border.get(width + i, y);
				}
			}

			if (border == null && length <= width) {
				borderInside(input, output, offset, length, indexIn, indexOut);
				continue;
			}

			// Running sums along the left and then the right border. Without a border only pixels inside
			// the image are counted
			for (int side = 0; side < 2; side++) {
				final int xStart = side == 0 ? 0 : Math.max(Math.min(offset, width), width - offsetR);
				final int xEnd = side == 0 ? Math.min(offset, width) : width;
				int total = 0;
				int count = 0;
				for (int x = xStart; x < xEnd; x++) {
					if (x == xStart) {
						for (int i = x - offset; i < x - offset + length; i++) {
							total += pixel(input, indexIn, i, outside, offset);
							count += i >= 0 && i < width ? 1 : 0;
						}
					} else {
						final int removed = x - offset - 1, added = x - offset + length - 1;
						total -= pixel(input, indexIn, removed, outside, offset);
						total += pixel(input, indexIn, added, outside, offset);
						count += (added < width ? 1 : 0) - (removed >= 0 ? 1 : 0);
					}
					final int divisor = border == null ? count : length;
					output.data[indexOut + x] = (short)((total + divisor/2)/divisor);
				}
			}
		}
		horizontalInner(input, output, offset, length, y0, y1);
	}

	private static void borderInside( GrayU16 input, GrayI16 output, int offset, int length, int indexIn,
									  int indexOut ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		if (offset > 0) {
			int total = 0;
			int count = length - offset;
			for (int i = 0; i < count; i++) {
				total += input.data[indexIn + i]& 0xFFFF;
			}
			output.data[indexOut] = (short)((total + count/2)/count);
			for (int x = 1; x < offset; x++) {
				total += input.data[indexIn + x + offsetR]& 0xFFFF;
				count++;
				output.data[indexOut + x] = (short)((total + count/2)/count);
			}
		}
		if (offsetR > 0) {
			final int x0 = width - offsetR;
			int total = 0;
			int count = offsetR + offset;
			for (int i = x0 - offset; i < width; i++) {
				total += input.data[indexIn + i]& 0xFFFF;
			}
			output.data[indexOut + x0] = (short)((total + count/2)/count);
			for (int x = x0 + 1; x < width; x++) {
				total -= input.data[indexIn + x - offset - 1]& 0xFFFF;
				count--;
				output.data[indexOut + x] = (short)((total + count/2)/count);
			}
		}
	}

	/// horizontal() without the border
	public static void horizontalInner( GrayU16 input, GrayI16 output, int offset, int length ) {
		horizontalInner(input, output, offset, length, 0, input.height);
	}

	/// horizontalInner() of the rows y0 to y1
	public static void horizontalInner( GrayU16 input, GrayI16 output, int offset, int length, int y0, int y1 ) {
		final int width = input.width;
		if (length > width)
			return;
		final int half = length/2;
		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			int total = 0;
			int indexEnd = indexIn + length;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn]& 0xFFFF;
			}
			output.data[indexOut++] = (short)((total + half)/length);

			indexEnd = indexIn + width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length]& 0xFFFF;
				total += input.data[indexIn]& 0xFFFF;
				output.data[indexOut++] = (short)((total + half)/length);
			}
		}
	}

	/// Mean along y. See the class description for the border
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayU16 input, GrayI16 output, int offset, int length,
								 @Nullable ImageBorder_S32<GrayU16> border, DogArray<DogArray_F64> rows ) {
		vertical(input, output, offset, length, border, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the block's first inner row
	public static void vertical( GrayU16 input, GrayI16 output, int offset, int length,
								 @Nullable ImageBorder_S32<GrayU16> border, int y0, int y1, DogArray<DogArray_F64> rows ) {
		final int width = input.width, height = input.height;
		final int offsetR = length - offset - 1;

		declareRows(rows, length, width);
		final double[] totals = rows.get(length + 1).data;
		final double[] row = rows.get(length + 2).data;

		// Border rows. Pixels outside the image are fetched once into the ring rows
		if (border != null) {
			if (y0 < offset) {
				for (int j = -offset; j < 0; j++) {
					fetchRow(border, width, j, rows.get(j + offset).data);
				}
			}
			if (y1 > height - offsetR) {
				for (int j = height; j < height + offsetR; j++) {
					fetchRow(border, width, j, rows.get(offset + j - height).data);
				}
			}
		}
		for (int y = y0; y < y1; y++) {
			if (y >= offset && y < height - offsetR) {
				y = height - offsetR - 1;
				continue;
			}
			Arrays.fill(totals, 0, width, 0);
			int count = 0;
			for (int k = 0; k < length; k++) {
				final int j = y - offset + k;
				final double[] values;
				if (j >= 0 && j < height) {
					copyRow(input, width, j, row);
					values = row;
					count++;
				} else if (border != null) {
					values = rows.get(j < 0 ? j + offset : offset + j - height).data;
				} else {
					continue;
				}
				for (int i = 0; i < width; i++) {
					totals[i] += values[i];
				}
			}
			final int divisor = border == null ? count : length;
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				output.data[indexOut + i] = (short)((int)(totals[i] + divisor/2)/divisor);
			}
		}

		verticalInner(input, output, offset, length, y0, y1, rows);
	}

	/// vertical() without the border
	public static void verticalInner( GrayU16 input, GrayI16 output, int offset, int length,
									  DogArray<DogArray_F64> rows ) {
		verticalInner(input, output, offset, length, 0, input.height, rows);
	}

	/// verticalInner() of the output rows y0 to y1
	public static void verticalInner( GrayU16 input, GrayI16 output, int offset, int length, int y0, int y1,
									  DogArray<DogArray_F64> rows ) {
		final int offsetR = length - offset - 1;
		final int inner0 = Math.max(y0, offset), inner1 = Math.min(y1, input.height - offsetR);
		if (inner0 >= inner1)
			return;
		declareRows(rows, length, input.width);
		verticalBlock(input, output, offset, length, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0
	private static void verticalBlock( GrayU16 input, GrayI16 output, int offset, int length, int y0, int y1,
									   DogArray<DogArray_F64> rows ) {
		final int n = input.width;
		final double[] totals = rows.get(length + 1).data;
		final double[] quotients = rows.get(length + 2).data;
		final int half = length/2;
		final double divisor = length;

		// Input row k is in buffer (k - first)%ringSize
		final int first = y0 - offset;
		final int ringSize = length + 1;
		for (int k = 0; k < length; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < length; k++) {
			final double[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] += row[i];
			}
		}

		for (int y = y0; y < y1; y++) {
			if (y > y0) {
				final double[] removed = rows.get((y - offset - 1 - first)%ringSize).data;
				final double[] added = rows.get((y - offset + length - 1 - first)%ringSize).data;
				copyRow(input, n, y - offset + length - 1, added);
				for (int i = 0; i < n; i++) {
					totals[i] = totals[i] - removed[i] + added[i];
				}
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < n; i++) {
				quotients[i] = (totals[i] + half)/divisor;
			}
			for (int i = 0; i < n; i++) {
				output.data[indexOut + i] = (short)(int)quotients[i];
			}
		}
	}

	/// Pixel i of the row starting at indexIn. Outside the image it's from 'outside', or 0 if that's null
	private static int pixel( GrayU16 input, int indexIn, int i, @Nullable int[] outside, int offset ) {
		final int width = input.width;
		if (i >= 0 && i < width)
			return input.data[indexIn + i]& 0xFFFF;
		if (outside == null)
			return 0;
		return outside[i < 0 ? i + offset : offset + i - width];
	}

	private static void copyRow( GrayU16 input, int n, int y, double[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		for (int i = 0; i < n; i++) {
			row[i] = input.data[indexIn + i]& 0xFFFF;
		}
	}

	/// Mean along x. See the class description for the border
	public static void horizontal( GrayF32 input, GrayF32 output, int offset, int length,
								   @Nullable ImageBorder_F32 border ) {
		horizontal(input, output, offset, length, border, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayF32 input, GrayF32 output, int offset, int length,
								   @Nullable ImageBorder_F32 border, int y0, int y1 ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		// pixels outside the image to the left and right of a row
		final float[] outside = border == null ? null : new float[offset + offsetR];

		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*input.stride;
			final int indexOut = output.startIndex + y*output.stride;
			if (border != null) {
				for (int i = 0; i < offset; i++) {
					outside[i] = border.get(i - offset, y);
				}
				for (int i = 0; i < offsetR; i++) {
					outside[offset + i] = border.get(width + i, y);
				}
			}

			if (border == null && length <= width) {
				borderInside(input, output, offset, length, indexIn, indexOut);
				continue;
			}

			// Running sums along the left and then the right border. Without a border only pixels inside
			// the image are counted
			for (int side = 0; side < 2; side++) {
				final int xStart = side == 0 ? 0 : Math.max(Math.min(offset, width), width - offsetR);
				final int xEnd = side == 0 ? Math.min(offset, width) : width;
				float total = 0;
				int count = 0;
				for (int x = xStart; x < xEnd; x++) {
					if (x == xStart) {
						for (int i = x - offset; i < x - offset + length; i++) {
							total += pixel(input, indexIn, i, outside, offset);
							count += i >= 0 && i < width ? 1 : 0;
						}
					} else {
						final int removed = x - offset - 1, added = x - offset + length - 1;
						total -= pixel(input, indexIn, removed, outside, offset);
						total += pixel(input, indexIn, added, outside, offset);
						count += (added < width ? 1 : 0) - (removed >= 0 ? 1 : 0);
					}
					output.data[indexOut + x] = total/(border == null ? count : length);
				}
			}
		}
		horizontalInner(input, output, offset, length, y0, y1);
	}

	private static void borderInside( GrayF32 input, GrayF32 output, int offset, int length, int indexIn,
									  int indexOut ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		if (offset > 0) {
			float total = 0;
			int count = length - offset;
			for (int i = 0; i < count; i++) {
				total += input.data[indexIn + i];
			}
			output.data[indexOut] = total/count;
			for (int x = 1; x < offset; x++) {
				total += input.data[indexIn + x + offsetR];
				count++;
				output.data[indexOut + x] = total/count;
			}
		}
		if (offsetR > 0) {
			final int x0 = width - offsetR;
			float total = 0;
			int count = offsetR + offset;
			for (int i = x0 - offset; i < width; i++) {
				total += input.data[indexIn + i];
			}
			output.data[indexOut + x0] = total/count;
			for (int x = x0 + 1; x < width; x++) {
				total -= input.data[indexIn + x - offset - 1];
				count--;
				output.data[indexOut + x] = total/count;
			}
		}
	}

	/// horizontal() without the border
	public static void horizontalInner( GrayF32 input, GrayF32 output, int offset, int length ) {
		horizontalInner(input, output, offset, length, 0, input.height);
	}

	/// horizontalInner() of the rows y0 to y1
	public static void horizontalInner( GrayF32 input, GrayF32 output, int offset, int length, int y0, int y1 ) {
		final int width = input.width;
		if (length > width)
			return;
		final float divisor = length;
		final int strideIn = input.stride, strideOut = output.stride;
		final int n = width - length + 1;
		for (int y = y0; y < y1; y += 2) {
			// the second row, or the same row again if only one is left
			final boolean pair = y + 1 < y1;
			final int nextIn = pair ? strideIn : 0, nextOut = pair ? strideOut : 0;
			int indexIn = input.startIndex + strideIn*y;
			final int indexOut0 = output.startIndex + strideOut*y + offset;
			int indexOut = indexOut0;

			float total0 = 0, total1 = 0;
			for (int i = indexIn; i < indexIn + length; i++) {
				total0 += input.data[i];
				total1 += input.data[i + nextIn];
			}
			output.data[indexOut] = total0;
			output.data[indexOut + nextOut] = total1;

			final int indexEnd = indexIn + width;
			for (int i = indexIn + length; i < indexEnd; i++) {
				indexOut++;
				total0 += input.data[i] - input.data[i - length];
				total1 += input.data[i + nextIn] - input.data[i + nextIn - length];
				output.data[indexOut] = total0;
				output.data[indexOut + nextOut] = total1;
			}
			divide(output.data, indexOut0, n, divisor);
			if (pair)
				divide(output.data, indexOut0 + strideOut, n, divisor);
		}
	}

	/// Mean along y. See the class description for the border
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayF32 input, GrayF32 output, int offset, int length,
								 @Nullable ImageBorder_F32 border, DogArray<DogArray_F32> rows ) {
		vertical(input, output, offset, length, border, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at y0 and every
	/// ImplConvolveBox.VERTICAL_RESTART rows, see ImplConvolveBox.restartRow()
	public static void vertical( GrayF32 input, GrayF32 output, int offset, int length,
								 @Nullable ImageBorder_F32 border, int y0, int y1, DogArray<DogArray_F32> rows ) {
		final int width = input.width, height = input.height;
		final int offsetR = length - offset - 1;

		declareRows(rows, length, width);
		final float[] totals = rows.get(length + 1).data;
		final float[] row = rows.get(length + 2).data;

		// Border rows. Pixels outside the image are fetched once into the ring rows
		if (border != null) {
			if (y0 < offset) {
				for (int j = -offset; j < 0; j++) {
					fetchRow(border, width, j, rows.get(j + offset).data);
				}
			}
			if (y1 > height - offsetR) {
				for (int j = height; j < height + offsetR; j++) {
					fetchRow(border, width, j, rows.get(offset + j - height).data);
				}
			}
		}
		for (int y = y0; y < y1; y++) {
			if (y >= offset && y < height - offsetR) {
				y = height - offsetR - 1;
				continue;
			}
			Arrays.fill(totals, 0, width, 0);
			int count = 0;
			for (int k = 0; k < length; k++) {
				final int j = y - offset + k;
				final float[] values;
				if (j >= 0 && j < height) {
					copyRow(input, width, j, row);
					values = row;
					count++;
				} else if (border != null) {
					values = rows.get(j < 0 ? j + offset : offset + j - height).data;
				} else {
					continue;
				}
				for (int i = 0; i < width; i++) {
					totals[i] += values[i];
				}
			}
			final int divisor = border == null ? count : length;
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				output.data[indexOut + i] = totals[i]/divisor;
			}
		}

		verticalInner(input, output, offset, length, y0, y1, rows);
	}

	/// vertical() without the border
	public static void verticalInner( GrayF32 input, GrayF32 output, int offset, int length,
									  DogArray<DogArray_F32> rows ) {
		verticalInner(input, output, offset, length, 0, input.height, rows);
	}

	/// verticalInner() of the output rows y0 to y1
	public static void verticalInner( GrayF32 input, GrayF32 output, int offset, int length, int y0, int y1,
									  DogArray<DogArray_F32> rows ) {
		final int offsetR = length - offset - 1;
		final int inner0 = Math.max(y0, offset), inner1 = Math.min(y1, input.height - offsetR);
		if (inner0 >= inner1)
			return;
		declareRows(rows, length, input.width);
		final int restart = ImplConvolveBox.VERTICAL_RESTART;
		for (int start = inner0; start < inner1; ) {
			// next multiple of 'restart' rows after the first inner row
			int end = offset + ((start - offset)/restart + 1)*restart;
			end = Math.min(end, inner1);
			verticalBlock(input, output, offset, length, start, end, rows);
			start = end;
		}
	}

	/// Inner output rows y0 to y1 with running sums that start at y0
	private static void verticalBlock( GrayF32 input, GrayF32 output, int offset, int length, int y0, int y1,
									   DogArray<DogArray_F32> rows ) {
		final int n = input.width;
		final float[] totals = rows.get(length + 1).data;
		final float[] quotients = rows.get(length + 2).data;
		final float divisor = length;

		// Input row k is in buffer (k - first)%ringSize
		final int first = y0 - offset;
		final int ringSize = length + 1;
		for (int k = 0; k < length; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < length; k++) {
			final float[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] += row[i];
			}
		}

		for (int y = y0; y < y1; y++) {
			if (y > y0) {
				final float[] removed = rows.get((y - offset - 1 - first)%ringSize).data;
				final float[] added = rows.get((y - offset + length - 1 - first)%ringSize).data;
				copyRow(input, n, y - offset + length - 1, added);
				for (int i = 0; i < n; i++) {
					float total = totals[i] - removed[i];
					totals[i] = total + added[i];
				}
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < n; i++) {
				quotients[i] = totals[i]/divisor;
			}
			System.arraycopy(quotients, 0, output.data, indexOut, n);
		}
	}

	/// Pixels of row j, which is outside the image, from the border
	private static void fetchRow( ImageBorder_F32 border, int width, int j, float[] row ) {
		for (int i = 0; i < width; i++) {
			row[i] = border.get(i, j);
		}
	}

	/// Pixel i of the row starting at indexIn. Outside the image it's from 'outside', or 0 if that's null
	private static float pixel( GrayF32 input, int indexIn, int i, @Nullable float[] outside, int offset ) {
		final int width = input.width;
		if (i >= 0 && i < width)
			return input.data[indexIn + i];
		if (outside == null)
			return 0;
		return outside[i < 0 ? i + offset : offset + i - width];
	}

	private static void copyRow( GrayF32 input, int n, int y, float[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		System.arraycopy(input.data, indexIn, row, 0, n);
	}

	private static void divide( float[] data, int index, int n, float divisor ) {
		final int end = index + n;
		for (int i = index; i < end; i++) {
			data[i] = data[i]/divisor;
		}
	}

	/// Mean along x. See the class description for the border
	public static void horizontal( GrayF64 input, GrayF64 output, int offset, int length,
								   @Nullable ImageBorder_F64 border ) {
		horizontal(input, output, offset, length, border, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayF64 input, GrayF64 output, int offset, int length,
								   @Nullable ImageBorder_F64 border, int y0, int y1 ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		// pixels outside the image to the left and right of a row
		final double[] outside = border == null ? null : new double[offset + offsetR];

		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*input.stride;
			final int indexOut = output.startIndex + y*output.stride;
			if (border != null) {
				for (int i = 0; i < offset; i++) {
					outside[i] = border.get(i - offset, y);
				}
				for (int i = 0; i < offsetR; i++) {
					outside[offset + i] = border.get(width + i, y);
				}
			}

			if (border == null && length <= width) {
				borderInside(input, output, offset, length, indexIn, indexOut);
				continue;
			}

			// Running sums along the left and then the right border. Without a border only pixels inside
			// the image are counted
			for (int side = 0; side < 2; side++) {
				final int xStart = side == 0 ? 0 : Math.max(Math.min(offset, width), width - offsetR);
				final int xEnd = side == 0 ? Math.min(offset, width) : width;
				double total = 0;
				int count = 0;
				for (int x = xStart; x < xEnd; x++) {
					if (x == xStart) {
						for (int i = x - offset; i < x - offset + length; i++) {
							total += pixel(input, indexIn, i, outside, offset);
							count += i >= 0 && i < width ? 1 : 0;
						}
					} else {
						final int removed = x - offset - 1, added = x - offset + length - 1;
						total -= pixel(input, indexIn, removed, outside, offset);
						total += pixel(input, indexIn, added, outside, offset);
						count += (added < width ? 1 : 0) - (removed >= 0 ? 1 : 0);
					}
					output.data[indexOut + x] = total/(border == null ? count : length);
				}
			}
		}
		horizontalInner(input, output, offset, length, y0, y1);
	}

	private static void borderInside( GrayF64 input, GrayF64 output, int offset, int length, int indexIn,
									  int indexOut ) {
		final int width = input.width;
		final int offsetR = length - offset - 1;
		if (offset > 0) {
			double total = 0;
			int count = length - offset;
			for (int i = 0; i < count; i++) {
				total += input.data[indexIn + i];
			}
			output.data[indexOut] = total/count;
			for (int x = 1; x < offset; x++) {
				total += input.data[indexIn + x + offsetR];
				count++;
				output.data[indexOut + x] = total/count;
			}
		}
		if (offsetR > 0) {
			final int x0 = width - offsetR;
			double total = 0;
			int count = offsetR + offset;
			for (int i = x0 - offset; i < width; i++) {
				total += input.data[indexIn + i];
			}
			output.data[indexOut + x0] = total/count;
			for (int x = x0 + 1; x < width; x++) {
				total -= input.data[indexIn + x - offset - 1];
				count--;
				output.data[indexOut + x] = total/count;
			}
		}
	}

	/// horizontal() without the border
	public static void horizontalInner( GrayF64 input, GrayF64 output, int offset, int length ) {
		horizontalInner(input, output, offset, length, 0, input.height);
	}

	/// horizontalInner() of the rows y0 to y1
	public static void horizontalInner( GrayF64 input, GrayF64 output, int offset, int length, int y0, int y1 ) {
		final int width = input.width;
		if (length > width)
			return;
		final double divisor = length;
		final int strideIn = input.stride, strideOut = output.stride;
		final int n = width - length + 1;
		for (int y = y0; y < y1; y += 2) {
			// the second row, or the same row again if only one is left
			final boolean pair = y + 1 < y1;
			final int nextIn = pair ? strideIn : 0, nextOut = pair ? strideOut : 0;
			int indexIn = input.startIndex + strideIn*y;
			final int indexOut0 = output.startIndex + strideOut*y + offset;
			int indexOut = indexOut0;

			double total0 = 0, total1 = 0;
			for (int i = indexIn; i < indexIn + length; i++) {
				total0 += input.data[i];
				total1 += input.data[i + nextIn];
			}
			output.data[indexOut] = total0;
			output.data[indexOut + nextOut] = total1;

			final int indexEnd = indexIn + width;
			for (int i = indexIn + length; i < indexEnd; i++) {
				indexOut++;
				total0 += input.data[i] - input.data[i - length];
				total1 += input.data[i + nextIn] - input.data[i + nextIn - length];
				output.data[indexOut] = total0;
				output.data[indexOut + nextOut] = total1;
			}
			divide(output.data, indexOut0, n, divisor);
			if (pair)
				divide(output.data, indexOut0 + strideOut, n, divisor);
		}
	}

	/// Mean along y. See the class description for the border
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayF64 input, GrayF64 output, int offset, int length,
								 @Nullable ImageBorder_F64 border, DogArray<DogArray_F64> rows ) {
		vertical(input, output, offset, length, border, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at y0 and every
	/// ImplConvolveBox.VERTICAL_RESTART rows, see ImplConvolveBox.restartRow()
	public static void vertical( GrayF64 input, GrayF64 output, int offset, int length,
								 @Nullable ImageBorder_F64 border, int y0, int y1, DogArray<DogArray_F64> rows ) {
		final int width = input.width, height = input.height;
		final int offsetR = length - offset - 1;

		declareRows(rows, length, width);
		final double[] totals = rows.get(length + 1).data;
		final double[] row = rows.get(length + 2).data;

		// Border rows. Pixels outside the image are fetched once into the ring rows
		if (border != null) {
			if (y0 < offset) {
				for (int j = -offset; j < 0; j++) {
					fetchRow(border, width, j, rows.get(j + offset).data);
				}
			}
			if (y1 > height - offsetR) {
				for (int j = height; j < height + offsetR; j++) {
					fetchRow(border, width, j, rows.get(offset + j - height).data);
				}
			}
		}
		for (int y = y0; y < y1; y++) {
			if (y >= offset && y < height - offsetR) {
				y = height - offsetR - 1;
				continue;
			}
			Arrays.fill(totals, 0, width, 0);
			int count = 0;
			for (int k = 0; k < length; k++) {
				final int j = y - offset + k;
				final double[] values;
				if (j >= 0 && j < height) {
					copyRow(input, width, j, row);
					values = row;
					count++;
				} else if (border != null) {
					values = rows.get(j < 0 ? j + offset : offset + j - height).data;
				} else {
					continue;
				}
				for (int i = 0; i < width; i++) {
					totals[i] += values[i];
				}
			}
			final int divisor = border == null ? count : length;
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				output.data[indexOut + i] = totals[i]/divisor;
			}
		}

		verticalInner(input, output, offset, length, y0, y1, rows);
	}

	/// vertical() without the border
	public static void verticalInner( GrayF64 input, GrayF64 output, int offset, int length,
									  DogArray<DogArray_F64> rows ) {
		verticalInner(input, output, offset, length, 0, input.height, rows);
	}

	/// verticalInner() of the output rows y0 to y1
	public static void verticalInner( GrayF64 input, GrayF64 output, int offset, int length, int y0, int y1,
									  DogArray<DogArray_F64> rows ) {
		final int offsetR = length - offset - 1;
		final int inner0 = Math.max(y0, offset), inner1 = Math.min(y1, input.height - offsetR);
		if (inner0 >= inner1)
			return;
		declareRows(rows, length, input.width);
		final int restart = ImplConvolveBox.VERTICAL_RESTART;
		for (int start = inner0; start < inner1; ) {
			// next multiple of 'restart' rows after the first inner row
			int end = offset + ((start - offset)/restart + 1)*restart;
			end = Math.min(end, inner1);
			verticalBlock(input, output, offset, length, start, end, rows);
			start = end;
		}
	}

	/// Inner output rows y0 to y1 with running sums that start at y0
	private static void verticalBlock( GrayF64 input, GrayF64 output, int offset, int length, int y0, int y1,
									   DogArray<DogArray_F64> rows ) {
		final int n = input.width;
		final double[] totals = rows.get(length + 1).data;
		final double[] quotients = rows.get(length + 2).data;
		final double divisor = length;

		// Input row k is in buffer (k - first)%ringSize
		final int first = y0 - offset;
		final int ringSize = length + 1;
		for (int k = 0; k < length; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < length; k++) {
			final double[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] += row[i];
			}
		}

		for (int y = y0; y < y1; y++) {
			if (y > y0) {
				final double[] removed = rows.get((y - offset - 1 - first)%ringSize).data;
				final double[] added = rows.get((y - offset + length - 1 - first)%ringSize).data;
				copyRow(input, n, y - offset + length - 1, added);
				for (int i = 0; i < n; i++) {
					double total = totals[i] - removed[i];
					totals[i] = total + added[i];
				}
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < n; i++) {
				quotients[i] = totals[i]/divisor;
			}
			System.arraycopy(quotients, 0, output.data, indexOut, n);
		}
	}

	/// Pixels of row j, which is outside the image, from the border
	private static void fetchRow( ImageBorder_F64 border, int width, int j, double[] row ) {
		for (int i = 0; i < width; i++) {
			row[i] = border.get(i, j);
		}
	}

	/// Pixel i of the row starting at indexIn. Outside the image it's from 'outside', or 0 if that's null
	private static double pixel( GrayF64 input, int indexIn, int i, @Nullable double[] outside, int offset ) {
		final int width = input.width;
		if (i >= 0 && i < width)
			return input.data[indexIn + i];
		if (outside == null)
			return 0;
		return outside[i < 0 ? i + offset : offset + i - width];
	}

	private static void copyRow( GrayF64 input, int n, int y, double[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		System.arraycopy(input.data, indexIn, row, 0, n);
	}

	private static void divide( double[] data, int index, int n, double divisor ) {
		final int end = index + n;
		for (int i = index; i < end; i++) {
			data[i] = data[i]/divisor;
		}
	}

}
