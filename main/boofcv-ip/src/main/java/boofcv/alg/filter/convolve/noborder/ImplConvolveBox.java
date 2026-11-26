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

import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I16;
import org.ddogleg.struct.DogArray_I32;

import javax.annotation.Generated;

/**
 * Convolves a box filter across an image. A box filter is equivalent to convolving a kernel with all 1's.
 * Pixels outside the image are zero. Each function also has a version for a range of rows, which
 * {@link ImplConvolveBox_MT} calls on each block.
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplConvolveBox</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.noborder.GenerateImplConvolveBox")
@SuppressWarnings({"ForLoopReplaceableByForEach", "Duplicates"})
public class ImplConvolveBox {

	/// Floating point running sums along y start over every VERTICAL_RESTART rows, so the results don't depend
	/// on how rows are split between threads. Fewer rows bound the error of the running sums more tightly but
	/// recompute more sums. ImplConvolveMean uses it and the functions below too
	public static int VERTICAL_RESTART = 64;

	/// Number of blocks of VERTICAL_RESTART inner rows for a kernel 'length' rows long
	public static int restartBlocks( int height, int length ) {
		return Math.max(1, (height - length + VERTICAL_RESTART)/VERTICAL_RESTART);
	}

	/// First output row of the block, for a kernel that starts 'offset' rows before the output row. Block 0
	/// includes the top border and the last block the bottom border
	public static int restartRow( int block, int blocks, int height, int offset ) {
		if (block == 0)
			return 0;
		if (block >= blocks)
			return height;
		return offset + block*VERTICAL_RESTART;
	}

	/// Box sum along x. Pixels outside the image are zero
	public static void horizontal( GrayU8 input, GrayI16 output, int radius ) {
		horizontal(input, output, radius, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayU8 input, GrayI16 output, int radius, int y0, int y1 ) {
		final int kernelWidth = radius*2 + 1;
		final int width = input.width;
		final int strideIn = input.stride, strideOut = output.stride;

		// Border: sum of the pixels inside the image
		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*strideIn;
			final int indexOut = output.startIndex + y*strideOut;
			for (int x = 0; x < width; x++) {
				if (x >= radius && x < width - radius) {
					x = width - radius - 1;
					continue;
				}
				int total = 0;
				final int end = Math.min(width, x + radius + 1);
				for (int i = Math.max(0, x - radius); i < end; i++) {
					total += input.data[indexIn + i]& 0xFF;
				}
				output.data[indexOut + x] = (short)total;
			}
		}
		if (kernelWidth > width)
			return;

		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + strideIn*y;
			int indexOut = output.startIndex + strideOut*y + radius;

			int total = 0;
			int indexEnd = indexIn + kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn]& 0xFF;
			}
			output.data[indexOut++] = (short)total;

			indexEnd = indexIn + width - kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - kernelWidth]& 0xFF;
				total += input.data[indexIn]& 0xFF;
				output.data[indexOut++] = (short)total;
			}
		}
	}

	/// Box sum along y. Pixels outside the image are zero
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayU8 input, GrayI16 output, int radius, DogArray<DogArray_I16> rows ) {
		vertical(input, output, radius, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the first inner row.
	public static void vertical( GrayU8 input, GrayI16 output, int radius, int y0, int y1,
								 DogArray<DogArray_I16> rows ) {
		final int width = input.width, height = input.height;
		final short[] dataOut = output.data;

		// Border: sum of the rows inside the image
		for (int y = y0; y < y1; y++) {
			if (y >= radius && y < height - radius) {
				y = height - radius - 1;
				continue;
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				dataOut[indexOut + i] = 0;
			}
			final int end = Math.min(height, y + radius + 1);
			for (int k = Math.max(0, y - radius); k < end; k++) {
				final int indexIn = input.startIndex + k*input.stride;
				for (int i = 0; i < width; i++) {
					dataOut[indexOut + i] = (short)(dataOut[indexOut + i] + (input.data[indexIn + i]& 0xFF));
				}
			}
		}

		final int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);
		if (inner0 < inner1)
			verticalInner(input, output, radius, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a
	/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized
	private static void verticalInner( GrayU8 input, GrayI16 output, int radius, int y0, int y1,
									   DogArray<DogArray_I16> rows ) {
		final int kernelWidth = radius*2 + 1;
		final int n = input.width;

		// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals
		final int first = y0 - radius;
		final int ringSize = kernelWidth + 1;
		rows.resize(ringSize + 1);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(n);
		}
		final short[] totals = rows.get(ringSize).data;

		for (int k = 0; k < kernelWidth; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < kernelWidth; k++) {
			final short[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] = (short)(totals[i] + row[i]);
			}
		}
		System.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);

		for (int y = y0 + 1; y < y1; y++) {
			final short[] removed = rows.get((y - radius - 1 - first)%ringSize).data;
			final short[] added = rows.get((y + radius - first)%ringSize).data;
			copyRow(input, n, y + radius, added);
			for (int i = 0; i < n; i++) {
				totals[i] = (short)(totals[i] - removed[i] + added[i]);
			}
			System.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);
		}
	}

	/// Copies the first n pixels of row y into 'row'
	private static void copyRow( GrayU8 input, int n, int y, short[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		for (int i = 0; i < n; i++) {
			row[i] = (short)(input.data[indexIn + i]& 0xFF);
		}
	}

	/// Box sum along x. Pixels outside the image are zero
	public static void horizontal( GrayU8 input, GrayS32 output, int radius ) {
		horizontal(input, output, radius, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayU8 input, GrayS32 output, int radius, int y0, int y1 ) {
		final int kernelWidth = radius*2 + 1;
		final int width = input.width;
		final int strideIn = input.stride, strideOut = output.stride;

		// Border: sum of the pixels inside the image
		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*strideIn;
			final int indexOut = output.startIndex + y*strideOut;
			for (int x = 0; x < width; x++) {
				if (x >= radius && x < width - radius) {
					x = width - radius - 1;
					continue;
				}
				int total = 0;
				final int end = Math.min(width, x + radius + 1);
				for (int i = Math.max(0, x - radius); i < end; i++) {
					total += input.data[indexIn + i]& 0xFF;
				}
				output.data[indexOut + x] = total;
			}
		}
		if (kernelWidth > width)
			return;

		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + strideIn*y;
			int indexOut = output.startIndex + strideOut*y + radius;

			int total = 0;
			int indexEnd = indexIn + kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn]& 0xFF;
			}
			output.data[indexOut++] = total;

			indexEnd = indexIn + width - kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - kernelWidth]& 0xFF;
				total += input.data[indexIn]& 0xFF;
				output.data[indexOut++] = total;
			}
		}
	}

	/// Box sum along y. Pixels outside the image are zero
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayU8 input, GrayS32 output, int radius, DogArray<DogArray_I32> rows ) {
		vertical(input, output, radius, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the first inner row.
	public static void vertical( GrayU8 input, GrayS32 output, int radius, int y0, int y1,
								 DogArray<DogArray_I32> rows ) {
		final int width = input.width, height = input.height;
		final int[] dataOut = output.data;

		// Border: sum of the rows inside the image
		for (int y = y0; y < y1; y++) {
			if (y >= radius && y < height - radius) {
				y = height - radius - 1;
				continue;
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				dataOut[indexOut + i] = 0;
			}
			final int end = Math.min(height, y + radius + 1);
			for (int k = Math.max(0, y - radius); k < end; k++) {
				final int indexIn = input.startIndex + k*input.stride;
				for (int i = 0; i < width; i++) {
					dataOut[indexOut + i] = (dataOut[indexOut + i] + (input.data[indexIn + i]& 0xFF));
				}
			}
		}

		final int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);
		if (inner0 < inner1)
			verticalInner(input, output, radius, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a
	/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized
	private static void verticalInner( GrayU8 input, GrayS32 output, int radius, int y0, int y1,
									   DogArray<DogArray_I32> rows ) {
		final int kernelWidth = radius*2 + 1;
		final int n = input.width;

		// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals
		final int first = y0 - radius;
		final int ringSize = kernelWidth + 1;
		rows.resize(ringSize + 1);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(n);
		}
		final int[] totals = rows.get(ringSize).data;

		for (int k = 0; k < kernelWidth; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < kernelWidth; k++) {
			final int[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] = (totals[i] + row[i]);
			}
		}
		System.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);

		for (int y = y0 + 1; y < y1; y++) {
			final int[] removed = rows.get((y - radius - 1 - first)%ringSize).data;
			final int[] added = rows.get((y + radius - first)%ringSize).data;
			copyRow(input, n, y + radius, added);
			for (int i = 0; i < n; i++) {
				totals[i] = totals[i] - removed[i] + added[i];
			}
			System.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);
		}
	}

	/// Copies the first n pixels of row y into 'row'
	private static void copyRow( GrayU8 input, int n, int y, int[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		for (int i = 0; i < n; i++) {
			row[i] = input.data[indexIn + i]& 0xFF;
		}
	}

	/// Box sum along x. Pixels outside the image are zero
	public static void horizontal( GrayS16 input, GrayI16 output, int radius ) {
		horizontal(input, output, radius, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayS16 input, GrayI16 output, int radius, int y0, int y1 ) {
		final int kernelWidth = radius*2 + 1;
		final int width = input.width;
		final int strideIn = input.stride, strideOut = output.stride;

		// Border: sum of the pixels inside the image
		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*strideIn;
			final int indexOut = output.startIndex + y*strideOut;
			for (int x = 0; x < width; x++) {
				if (x >= radius && x < width - radius) {
					x = width - radius - 1;
					continue;
				}
				int total = 0;
				final int end = Math.min(width, x + radius + 1);
				for (int i = Math.max(0, x - radius); i < end; i++) {
					total += input.data[indexIn + i];
				}
				output.data[indexOut + x] = (short)total;
			}
		}
		if (kernelWidth > width)
			return;

		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + strideIn*y;
			int indexOut = output.startIndex + strideOut*y + radius;

			int total = 0;
			int indexEnd = indexIn + kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn];
			}
			output.data[indexOut++] = (short)total;

			indexEnd = indexIn + width - kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - kernelWidth];
				total += input.data[indexIn];
				output.data[indexOut++] = (short)total;
			}
		}
	}

	/// Box sum along y. Pixels outside the image are zero
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayS16 input, GrayI16 output, int radius, DogArray<DogArray_I16> rows ) {
		vertical(input, output, radius, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the first inner row.
	public static void vertical( GrayS16 input, GrayI16 output, int radius, int y0, int y1,
								 DogArray<DogArray_I16> rows ) {
		final int width = input.width, height = input.height;
		final short[] dataOut = output.data;

		// Border: sum of the rows inside the image
		for (int y = y0; y < y1; y++) {
			if (y >= radius && y < height - radius) {
				y = height - radius - 1;
				continue;
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				dataOut[indexOut + i] = 0;
			}
			final int end = Math.min(height, y + radius + 1);
			for (int k = Math.max(0, y - radius); k < end; k++) {
				final int indexIn = input.startIndex + k*input.stride;
				for (int i = 0; i < width; i++) {
					dataOut[indexOut + i] = (short)(dataOut[indexOut + i] + (input.data[indexIn + i]));
				}
			}
		}

		final int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);
		if (inner0 < inner1)
			verticalInner(input, output, radius, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a
	/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized
	private static void verticalInner( GrayS16 input, GrayI16 output, int radius, int y0, int y1,
									   DogArray<DogArray_I16> rows ) {
		final int kernelWidth = radius*2 + 1;
		final int n = input.width;

		// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals
		final int first = y0 - radius;
		final int ringSize = kernelWidth + 1;
		rows.resize(ringSize + 1);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(n);
		}
		final short[] totals = rows.get(ringSize).data;

		for (int k = 0; k < kernelWidth; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < kernelWidth; k++) {
			final short[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] = (short)(totals[i] + row[i]);
			}
		}
		System.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);

		for (int y = y0 + 1; y < y1; y++) {
			final short[] removed = rows.get((y - radius - 1 - first)%ringSize).data;
			final short[] added = rows.get((y + radius - first)%ringSize).data;
			copyRow(input, n, y + radius, added);
			for (int i = 0; i < n; i++) {
				totals[i] = (short)(totals[i] - removed[i] + added[i]);
			}
			System.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);
		}
	}

	/// Copies the first n pixels of row y into 'row'
	private static void copyRow( GrayS16 input, int n, int y, short[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		System.arraycopy(input.data, indexIn, row, 0, n);
	}

	/// Box sum along x. Pixels outside the image are zero
	public static void horizontal( GrayU16 input, GrayI16 output, int radius ) {
		horizontal(input, output, radius, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayU16 input, GrayI16 output, int radius, int y0, int y1 ) {
		final int kernelWidth = radius*2 + 1;
		final int width = input.width;
		final int strideIn = input.stride, strideOut = output.stride;

		// Border: sum of the pixels inside the image
		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*strideIn;
			final int indexOut = output.startIndex + y*strideOut;
			for (int x = 0; x < width; x++) {
				if (x >= radius && x < width - radius) {
					x = width - radius - 1;
					continue;
				}
				int total = 0;
				final int end = Math.min(width, x + radius + 1);
				for (int i = Math.max(0, x - radius); i < end; i++) {
					total += input.data[indexIn + i]& 0xFFFF;
				}
				output.data[indexOut + x] = (short)total;
			}
		}
		if (kernelWidth > width)
			return;

		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + strideIn*y;
			int indexOut = output.startIndex + strideOut*y + radius;

			int total = 0;
			int indexEnd = indexIn + kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn]& 0xFFFF;
			}
			output.data[indexOut++] = (short)total;

			indexEnd = indexIn + width - kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - kernelWidth]& 0xFFFF;
				total += input.data[indexIn]& 0xFFFF;
				output.data[indexOut++] = (short)total;
			}
		}
	}

	/// Box sum along y. Pixels outside the image are zero
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayU16 input, GrayI16 output, int radius, DogArray<DogArray_I16> rows ) {
		vertical(input, output, radius, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the first inner row.
	public static void vertical( GrayU16 input, GrayI16 output, int radius, int y0, int y1,
								 DogArray<DogArray_I16> rows ) {
		final int width = input.width, height = input.height;
		final short[] dataOut = output.data;

		// Border: sum of the rows inside the image
		for (int y = y0; y < y1; y++) {
			if (y >= radius && y < height - radius) {
				y = height - radius - 1;
				continue;
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				dataOut[indexOut + i] = 0;
			}
			final int end = Math.min(height, y + radius + 1);
			for (int k = Math.max(0, y - radius); k < end; k++) {
				final int indexIn = input.startIndex + k*input.stride;
				for (int i = 0; i < width; i++) {
					dataOut[indexOut + i] = (short)(dataOut[indexOut + i] + (input.data[indexIn + i]& 0xFFFF));
				}
			}
		}

		final int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);
		if (inner0 < inner1)
			verticalInner(input, output, radius, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a
	/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized
	private static void verticalInner( GrayU16 input, GrayI16 output, int radius, int y0, int y1,
									   DogArray<DogArray_I16> rows ) {
		final int kernelWidth = radius*2 + 1;
		final int n = input.width;

		// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals
		final int first = y0 - radius;
		final int ringSize = kernelWidth + 1;
		rows.resize(ringSize + 1);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(n);
		}
		final short[] totals = rows.get(ringSize).data;

		for (int k = 0; k < kernelWidth; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < kernelWidth; k++) {
			final short[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] = (short)(totals[i] + row[i]);
			}
		}
		System.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);

		for (int y = y0 + 1; y < y1; y++) {
			final short[] removed = rows.get((y - radius - 1 - first)%ringSize).data;
			final short[] added = rows.get((y + radius - first)%ringSize).data;
			copyRow(input, n, y + radius, added);
			for (int i = 0; i < n; i++) {
				totals[i] = (short)(totals[i] - removed[i] + added[i]);
			}
			System.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);
		}
	}

	/// Copies the first n pixels of row y into 'row'
	private static void copyRow( GrayU16 input, int n, int y, short[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		System.arraycopy(input.data, indexIn, row, 0, n);
	}

	/// Box sum along x. Pixels outside the image are zero
	public static void horizontal( GrayS32 input, GrayS32 output, int radius ) {
		horizontal(input, output, radius, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayS32 input, GrayS32 output, int radius, int y0, int y1 ) {
		final int kernelWidth = radius*2 + 1;
		final int width = input.width;
		final int strideIn = input.stride, strideOut = output.stride;

		// Border: sum of the pixels inside the image
		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*strideIn;
			final int indexOut = output.startIndex + y*strideOut;
			for (int x = 0; x < width; x++) {
				if (x >= radius && x < width - radius) {
					x = width - radius - 1;
					continue;
				}
				int total = 0;
				final int end = Math.min(width, x + radius + 1);
				for (int i = Math.max(0, x - radius); i < end; i++) {
					total += input.data[indexIn + i];
				}
				output.data[indexOut + x] = total;
			}
		}
		if (kernelWidth > width)
			return;

		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + strideIn*y;
			int indexOut = output.startIndex + strideOut*y + radius;

			int total = 0;
			int indexEnd = indexIn + kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn];
			}
			output.data[indexOut++] = total;

			indexEnd = indexIn + width - kernelWidth;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - kernelWidth];
				total += input.data[indexIn];
				output.data[indexOut++] = total;
			}
		}
	}

	/// Box sum along y. Pixels outside the image are zero
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayS32 input, GrayS32 output, int radius, DogArray<DogArray_I32> rows ) {
		vertical(input, output, radius, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at the first inner row.
	public static void vertical( GrayS32 input, GrayS32 output, int radius, int y0, int y1,
								 DogArray<DogArray_I32> rows ) {
		final int width = input.width, height = input.height;
		final int[] dataOut = output.data;

		// Border: sum of the rows inside the image
		for (int y = y0; y < y1; y++) {
			if (y >= radius && y < height - radius) {
				y = height - radius - 1;
				continue;
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				dataOut[indexOut + i] = 0;
			}
			final int end = Math.min(height, y + radius + 1);
			for (int k = Math.max(0, y - radius); k < end; k++) {
				final int indexIn = input.startIndex + k*input.stride;
				for (int i = 0; i < width; i++) {
					dataOut[indexOut + i] = (dataOut[indexOut + i] + (input.data[indexIn + i]));
				}
			}
		}

		final int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);
		if (inner0 < inner1)
			verticalInner(input, output, radius, inner0, inner1, rows);
	}

	/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a
	/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized
	private static void verticalInner( GrayS32 input, GrayS32 output, int radius, int y0, int y1,
									   DogArray<DogArray_I32> rows ) {
		final int kernelWidth = radius*2 + 1;
		final int n = input.width;

		// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals
		final int first = y0 - radius;
		final int ringSize = kernelWidth + 1;
		rows.resize(ringSize + 1);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(n);
		}
		final int[] totals = rows.get(ringSize).data;

		for (int k = 0; k < kernelWidth; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < kernelWidth; k++) {
			final int[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] = (totals[i] + row[i]);
			}
		}
		System.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);

		for (int y = y0 + 1; y < y1; y++) {
			final int[] removed = rows.get((y - radius - 1 - first)%ringSize).data;
			final int[] added = rows.get((y + radius - first)%ringSize).data;
			copyRow(input, n, y + radius, added);
			for (int i = 0; i < n; i++) {
				totals[i] = totals[i] - removed[i] + added[i];
			}
			System.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);
		}
	}

	/// Copies the first n pixels of row y into 'row'
	private static void copyRow( GrayS32 input, int n, int y, int[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		System.arraycopy(input.data, indexIn, row, 0, n);
	}

	/// Box sum along x. Pixels outside the image are zero
	public static void horizontal( GrayF32 input, GrayF32 output, int radius ) {
		horizontal(input, output, radius, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayF32 input, GrayF32 output, int radius, int y0, int y1 ) {
		final int kernelWidth = radius*2 + 1;
		final int width = input.width;
		final int strideIn = input.stride, strideOut = output.stride;

		// Border: sum of the pixels inside the image
		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*strideIn;
			final int indexOut = output.startIndex + y*strideOut;
			for (int x = 0; x < width; x++) {
				if (x >= radius && x < width - radius) {
					x = width - radius - 1;
					continue;
				}
				float total = 0;
				final int end = Math.min(width, x + radius + 1);
				for (int i = Math.max(0, x - radius); i < end; i++) {
					total += input.data[indexIn + i];
				}
				output.data[indexOut + x] = total;
			}
		}
		if (kernelWidth > width)
			return;

		for (int y = y0; y < y1; y += 2) {
			// the second row, or the same row again if only one is left
			final boolean pair = y + 1 < y1;
			final int nextIn = pair ? strideIn : 0, nextOut = pair ? strideOut : 0;
			int indexIn = input.startIndex + strideIn*y;
			int indexOut = output.startIndex + strideOut*y + radius;

			float total0 = 0, total1 = 0;
			for (int i = indexIn; i < indexIn + kernelWidth; i++) {
				total0 += input.data[i];
				total1 += input.data[i + nextIn];
			}
			output.data[indexOut] = total0;
			output.data[indexOut + nextOut] = total1;

			final int indexEnd = indexIn + width;
			for (int i = indexIn + kernelWidth; i < indexEnd; i++) {
				indexOut++;
				total0 += input.data[i] - input.data[i - kernelWidth];
				total1 += input.data[i + nextIn] - input.data[i + nextIn - kernelWidth];
				output.data[indexOut] = total0;
				output.data[indexOut + nextOut] = total1;
			}
		}
	}

	/// Box sum along y. Pixels outside the image are zero
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayF32 input, GrayF32 output, int radius, DogArray<DogArray_F32> rows ) {
		vertical(input, output, radius, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at y0 and every VERTICAL_RESTART rows, see restartRow()
	public static void vertical( GrayF32 input, GrayF32 output, int radius, int y0, int y1,
								 DogArray<DogArray_F32> rows ) {
		final int width = input.width, height = input.height;
		final float[] dataOut = output.data;

		// Border: sum of the rows inside the image
		for (int y = y0; y < y1; y++) {
			if (y >= radius && y < height - radius) {
				y = height - radius - 1;
				continue;
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				dataOut[indexOut + i] = 0;
			}
			final int end = Math.min(height, y + radius + 1);
			for (int k = Math.max(0, y - radius); k < end; k++) {
				final int indexIn = input.startIndex + k*input.stride;
				for (int i = 0; i < width; i++) {
					dataOut[indexOut + i] = (dataOut[indexOut + i] + (input.data[indexIn + i]));
				}
			}
		}

		final int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);
		for (int start = inner0; start < inner1; ) {
			// next multiple of VERTICAL_RESTART rows after the first inner row
			int end = radius + ((start - radius)/VERTICAL_RESTART + 1)*VERTICAL_RESTART;
			end = Math.min(end, inner1);
			verticalInner(input, output, radius, start, end, rows);
			start = end;
		}
	}

	/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a
	/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized
	private static void verticalInner( GrayF32 input, GrayF32 output, int radius, int y0, int y1,
									   DogArray<DogArray_F32> rows ) {
		final int kernelWidth = radius*2 + 1;
		final int n = input.width;

		// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals
		final int first = y0 - radius;
		final int ringSize = kernelWidth + 1;
		rows.resize(ringSize + 1);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(n);
		}
		final float[] totals = rows.get(ringSize).data;

		for (int k = 0; k < kernelWidth; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < kernelWidth; k++) {
			final float[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] = (totals[i] + row[i]);
			}
		}
		System.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);

		for (int y = y0 + 1; y < y1; y++) {
			final float[] removed = rows.get((y - radius - 1 - first)%ringSize).data;
			final float[] added = rows.get((y + radius - first)%ringSize).data;
			copyRow(input, n, y + radius, added);
			for (int i = 0; i < n; i++) {
				float total = totals[i] - removed[i];
				totals[i] = total + added[i];
			}
			System.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);
		}
	}

	/// Copies the first n pixels of row y into 'row'
	private static void copyRow( GrayF32 input, int n, int y, float[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		System.arraycopy(input.data, indexIn, row, 0, n);
	}

	/// Box sum along x. Pixels outside the image are zero
	public static void horizontal( GrayF64 input, GrayF64 output, int radius ) {
		horizontal(input, output, radius, 0, input.height);
	}

	/// horizontal() of the rows y0 to y1
	public static void horizontal( GrayF64 input, GrayF64 output, int radius, int y0, int y1 ) {
		final int kernelWidth = radius*2 + 1;
		final int width = input.width;
		final int strideIn = input.stride, strideOut = output.stride;

		// Border: sum of the pixels inside the image
		for (int y = y0; y < y1; y++) {
			final int indexIn = input.startIndex + y*strideIn;
			final int indexOut = output.startIndex + y*strideOut;
			for (int x = 0; x < width; x++) {
				if (x >= radius && x < width - radius) {
					x = width - radius - 1;
					continue;
				}
				double total = 0;
				final int end = Math.min(width, x + radius + 1);
				for (int i = Math.max(0, x - radius); i < end; i++) {
					total += input.data[indexIn + i];
				}
				output.data[indexOut + x] = total;
			}
		}
		if (kernelWidth > width)
			return;

		for (int y = y0; y < y1; y += 2) {
			// the second row, or the same row again if only one is left
			final boolean pair = y + 1 < y1;
			final int nextIn = pair ? strideIn : 0, nextOut = pair ? strideOut : 0;
			int indexIn = input.startIndex + strideIn*y;
			int indexOut = output.startIndex + strideOut*y + radius;

			double total0 = 0, total1 = 0;
			for (int i = indexIn; i < indexIn + kernelWidth; i++) {
				total0 += input.data[i];
				total1 += input.data[i + nextIn];
			}
			output.data[indexOut] = total0;
			output.data[indexOut + nextOut] = total1;

			final int indexEnd = indexIn + width;
			for (int i = indexIn + kernelWidth; i < indexEnd; i++) {
				indexOut++;
				total0 += input.data[i] - input.data[i - kernelWidth];
				total1 += input.data[i + nextIn] - input.data[i + nextIn - kernelWidth];
				output.data[indexOut] = total0;
				output.data[indexOut + nextOut] = total1;
			}
		}
	}

	/// Box sum along y. Pixels outside the image are zero
	///
	/// @param rows Work space for the row buffers
	public static void vertical( GrayF64 input, GrayF64 output, int radius, DogArray<DogArray_F64> rows ) {
		vertical(input, output, radius, 0, input.height, rows);
	}

	/// vertical() of the output rows y0 to y1. The running sums start at y0 and every VERTICAL_RESTART rows, see restartRow()
	public static void vertical( GrayF64 input, GrayF64 output, int radius, int y0, int y1,
								 DogArray<DogArray_F64> rows ) {
		final int width = input.width, height = input.height;
		final double[] dataOut = output.data;

		// Border: sum of the rows inside the image
		for (int y = y0; y < y1; y++) {
			if (y >= radius && y < height - radius) {
				y = height - radius - 1;
				continue;
			}
			final int indexOut = output.startIndex + y*output.stride;
			for (int i = 0; i < width; i++) {
				dataOut[indexOut + i] = 0;
			}
			final int end = Math.min(height, y + radius + 1);
			for (int k = Math.max(0, y - radius); k < end; k++) {
				final int indexIn = input.startIndex + k*input.stride;
				for (int i = 0; i < width; i++) {
					dataOut[indexOut + i] = (dataOut[indexOut + i] + (input.data[indexIn + i]));
				}
			}
		}

		final int inner0 = Math.max(y0, radius), inner1 = Math.min(y1, height - radius);
		for (int start = inner0; start < inner1; ) {
			// next multiple of VERTICAL_RESTART rows after the first inner row
			int end = radius + ((start - radius)/VERTICAL_RESTART + 1)*VERTICAL_RESTART;
			end = Math.min(end, inner1);
			verticalInner(input, output, radius, start, end, rows);
			start = end;
		}
	}

	/// Inner output rows y0 to y1 with running sums that start at y0. The last 2*radius + 1 input rows are in a
	/// ring of row buffers, so every array in the inner loop has the same index and the loop can be vectorized
	private static void verticalInner( GrayF64 input, GrayF64 output, int radius, int y0, int y1,
									   DogArray<DogArray_F64> rows ) {
		final int kernelWidth = radius*2 + 1;
		final int n = input.width;

		// Input row k is in buffer (k - first)%ringSize. The last buffer has the totals
		final int first = y0 - radius;
		final int ringSize = kernelWidth + 1;
		rows.resize(ringSize + 1);
		for (int k = 0; k < rows.size; k++) {
			rows.get(k).resize(n);
		}
		final double[] totals = rows.get(ringSize).data;

		for (int k = 0; k < kernelWidth; k++) {
			copyRow(input, n, first + k, rows.get(k).data);
		}
		System.arraycopy(rows.get(0).data, 0, totals, 0, n);
		for (int k = 1; k < kernelWidth; k++) {
			final double[] row = rows.get(k).data;
			for (int i = 0; i < n; i++) {
				totals[i] = (totals[i] + row[i]);
			}
		}
		System.arraycopy(totals, 0, output.data, output.startIndex + y0*output.stride, n);

		for (int y = y0 + 1; y < y1; y++) {
			final double[] removed = rows.get((y - radius - 1 - first)%ringSize).data;
			final double[] added = rows.get((y + radius - first)%ringSize).data;
			copyRow(input, n, y + radius, added);
			for (int i = 0; i < n; i++) {
				double total = totals[i] - removed[i];
				totals[i] = total + added[i];
			}
			System.arraycopy(totals, 0, output.data, output.startIndex + y*output.stride, n);
		}
	}

	/// Copies the first n pixels of row y into 'row'
	private static void copyRow( GrayF64 input, int n, int y, double[] row ) {
		final int indexIn = input.startIndex + y*input.stride;
		System.arraycopy(input.data, indexIn, row, 0, n);
	}

}
