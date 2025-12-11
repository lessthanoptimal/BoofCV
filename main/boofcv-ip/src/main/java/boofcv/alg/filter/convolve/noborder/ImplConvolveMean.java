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

package boofcv.alg.filter.convolve.noborder;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>
 * Convolves a mean filter across the image. The mean value of all the pixels are computed inside the kernel.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplConvolveMean</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.noborder.GenerateImplConvolveMean")
@SuppressWarnings({"ForLoopReplaceableByForEach","Duplicates"})
public class ImplConvolveMean {

	public static void horizontalBorder( GrayU8 input, GrayI8 output, int offset, int length ) {
		final byte[] dataSrc = input.data;
		final byte[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;

			int total = 0;
			int count = length - offset;
			int jEnd = j + count;
			if (offset > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFF;
				}
				dataDst[indexDest++] = (byte)((total + count/2)/count);
			}

			while (++count < length) {
				total += dataSrc[jEnd++]& 0xFF;
				dataDst[indexDest++] = (byte)((total + count/2)/count);
			}

			jEnd = j + width;
			count = offset + offsetR;
			j += width - count;
			indexDest += width - count;
			total = 0;
			if (offsetR > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFF;
				}
				dataDst[indexDest++] = (byte)((total + count/2)/count);
			}
			while (--count > offset) {
				total -= dataSrc[j++]& 0xFF;
				dataDst[indexDest++] = (byte)((total + count/2)/count);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( GrayU8 input,GrayI8 output, int offset, int length ) {
		final int divisor = length;
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			int total = 0;

			int indexEnd = indexIn + length;
			
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn] & 0xFF;
			}
			output.data[indexOut++] = (byte)((total + halfDivisor)/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] & 0xFF;
				total += input.data[indexIn] & 0xFF;

				output.data[indexOut++] = (byte)((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void verticalBorder( GrayU8 input, GrayI8 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final byte[] dataSrc = input.data;
		final byte[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, length, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Image Top
		for (int count = length - offset; count < length; count++) {
			final int indexInRow = input.startIndex + x0;
			if (count == length - offset) {
				int indexIn = indexInRow;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFF;
				}

				for (int y = 1; y < count; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++]& 0xFF;
					}
				}
			} else {
				int indexIn0 = indexInRow + (count - 1)*input.stride;
				int end = indexIn0 + x1 - x0;
				for (int i = indexIn0; i < end; i++) {
					totals[i - indexIn0] += dataSrc[i]& 0xFF;
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (byte)((totals[x - x0] + count/2)/count);
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			final int indexInRow = input.startIndex + x0;
			if (yStart == height - length + 1) {
				int indexIn = indexInRow + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFF;
				}

				for (int y = yStart + 1; y < height; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++]& 0xFF;
					}
				}
			} else {
				int indexIn0 = indexInRow + (yStart - 1)*input.stride;
				int indexIn1 = indexIn0 + x1 - x0;

				for (int i = indexIn0; i < indexIn1; i++) {
					totals[i - indexIn0] -= dataSrc[i]& 0xFF;
				}
			}

			int count = height - yStart;
			int indexOut = output.startIndex + (yStart + offset)*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (byte)((totals[x - x0] + count/2)/count);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void vertical( GrayU8 input, GrayI8 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE

		final int divisor = length;
		final int halfDivisor = divisor/2;
		final int regionStepY = length*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Sum up along x-axis to avoid cache misses when reading from input image
		// Initialize recursion by summing up the first kernels along the x-axis
		{
			int indexIn = input.startIndex + x0;

			for (int x = x0; x < x1; x++) {
				totals[x - x0] = input.data[indexIn++]& 0xFF;
			}
		}
		for (int y = 1; y < length; y++) {
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++) {
				totals[i - indexIn] += input.data[i]& 0xFF;
			}
		}

		int indexOut = output.startIndex + output.stride*offset + x0;
		for (int x = x0; x < x1; x++, indexOut++) {
			final int total = totals[x - x0];
			output.data[indexOut] = (byte)((total + halfDivisor)/divisor);
		}

		// For the reminder we only need to add and remove the first and last elements to update the solution
		for (int y = 0; y < input.height - length; y++) {
			indexOut = output.startIndex + output.stride*(offset + y + 1) + x0;
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++, indexOut++) {
				int total = totals[i - indexIn] - (input.data[i]& 0xFF);
				total += input.data[i + regionStepY]& 0xFF;
				output.data[indexOut] = (byte)((total + halfDivisor)/divisor);
				totals[i - indexIn] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontalBorder( GrayS16 input, GrayI16 output, int offset, int length ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;

			int total = 0;
			int count = length - offset;
			int jEnd = j + count;
			if (offset > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}

			while (++count < length) {
				total += dataSrc[jEnd++];
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}

			jEnd = j + width;
			count = offset + offsetR;
			j += width - count;
			indexDest += width - count;
			total = 0;
			if (offsetR > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}
			while (--count > offset) {
				total -= dataSrc[j++];
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( GrayS16 input,GrayI16 output, int offset, int length ) {
		final int divisor = length;
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			int total = 0;

			int indexEnd = indexIn + length;
			
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut++] = (short)((total + halfDivisor)/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] ;
				total += input.data[indexIn] ;

				output.data[indexOut++] = (short)((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void verticalBorder( GrayS16 input, GrayI16 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, length, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Image Top
		for (int count = length - offset; count < length; count++) {
			final int indexInRow = input.startIndex + x0;
			if (count == length - offset) {
				int indexIn = indexInRow;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}

				for (int y = 1; y < count; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++];
					}
				}
			} else {
				int indexIn0 = indexInRow + (count - 1)*input.stride;
				int end = indexIn0 + x1 - x0;
				for (int i = indexIn0; i < end; i++) {
					totals[i - indexIn0] += dataSrc[i];
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (short)((totals[x - x0] + count/2)/count);
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			final int indexInRow = input.startIndex + x0;
			if (yStart == height - length + 1) {
				int indexIn = indexInRow + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}

				for (int y = yStart + 1; y < height; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++];
					}
				}
			} else {
				int indexIn0 = indexInRow + (yStart - 1)*input.stride;
				int indexIn1 = indexIn0 + x1 - x0;

				for (int i = indexIn0; i < indexIn1; i++) {
					totals[i - indexIn0] -= dataSrc[i];
				}
			}

			int count = height - yStart;
			int indexOut = output.startIndex + (yStart + offset)*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (short)((totals[x - x0] + count/2)/count);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void vertical( GrayS16 input, GrayI16 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE

		final int divisor = length;
		final int halfDivisor = divisor/2;
		final int regionStepY = length*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Sum up along x-axis to avoid cache misses when reading from input image
		// Initialize recursion by summing up the first kernels along the x-axis
		{
			int indexIn = input.startIndex + x0;

			for (int x = x0; x < x1; x++) {
				totals[x - x0] = input.data[indexIn++];
			}
		}
		for (int y = 1; y < length; y++) {
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++) {
				totals[i - indexIn] += input.data[i];
			}
		}

		int indexOut = output.startIndex + output.stride*offset + x0;
		for (int x = x0; x < x1; x++, indexOut++) {
			final int total = totals[x - x0];
			output.data[indexOut] = (short)((total + halfDivisor)/divisor);
		}

		// For the reminder we only need to add and remove the first and last elements to update the solution
		for (int y = 0; y < input.height - length; y++) {
			indexOut = output.startIndex + output.stride*(offset + y + 1) + x0;
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++, indexOut++) {
				int total = totals[i - indexIn] - (input.data[i]);
				total += input.data[i + regionStepY];
				output.data[indexOut] = (short)((total + halfDivisor)/divisor);
				totals[i - indexIn] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontalBorder( GrayU16 input, GrayI16 output, int offset, int length ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;

			int total = 0;
			int count = length - offset;
			int jEnd = j + count;
			if (offset > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFFFF;
				}
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}

			while (++count < length) {
				total += dataSrc[jEnd++]& 0xFFFF;
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}

			jEnd = j + width;
			count = offset + offsetR;
			j += width - count;
			indexDest += width - count;
			total = 0;
			if (offsetR > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFFFF;
				}
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}
			while (--count > offset) {
				total -= dataSrc[j++]& 0xFFFF;
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( GrayU16 input,GrayI16 output, int offset, int length ) {
		final int divisor = length;
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			int total = 0;

			int indexEnd = indexIn + length;
			
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn] & 0xFFFF;
			}
			output.data[indexOut++] = (short)((total + halfDivisor)/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] & 0xFFFF;
				total += input.data[indexIn] & 0xFFFF;

				output.data[indexOut++] = (short)((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void verticalBorder( GrayU16 input, GrayI16 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, length, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Image Top
		for (int count = length - offset; count < length; count++) {
			final int indexInRow = input.startIndex + x0;
			if (count == length - offset) {
				int indexIn = indexInRow;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFFFF;
				}

				for (int y = 1; y < count; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++]& 0xFFFF;
					}
				}
			} else {
				int indexIn0 = indexInRow + (count - 1)*input.stride;
				int end = indexIn0 + x1 - x0;
				for (int i = indexIn0; i < end; i++) {
					totals[i - indexIn0] += dataSrc[i]& 0xFFFF;
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (short)((totals[x - x0] + count/2)/count);
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			final int indexInRow = input.startIndex + x0;
			if (yStart == height - length + 1) {
				int indexIn = indexInRow + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFFFF;
				}

				for (int y = yStart + 1; y < height; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++]& 0xFFFF;
					}
				}
			} else {
				int indexIn0 = indexInRow + (yStart - 1)*input.stride;
				int indexIn1 = indexIn0 + x1 - x0;

				for (int i = indexIn0; i < indexIn1; i++) {
					totals[i - indexIn0] -= dataSrc[i]& 0xFFFF;
				}
			}

			int count = height - yStart;
			int indexOut = output.startIndex + (yStart + offset)*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (short)((totals[x - x0] + count/2)/count);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void vertical( GrayU16 input, GrayI16 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE

		final int divisor = length;
		final int halfDivisor = divisor/2;
		final int regionStepY = length*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Sum up along x-axis to avoid cache misses when reading from input image
		// Initialize recursion by summing up the first kernels along the x-axis
		{
			int indexIn = input.startIndex + x0;

			for (int x = x0; x < x1; x++) {
				totals[x - x0] = input.data[indexIn++]& 0xFFFF;
			}
		}
		for (int y = 1; y < length; y++) {
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++) {
				totals[i - indexIn] += input.data[i]& 0xFFFF;
			}
		}

		int indexOut = output.startIndex + output.stride*offset + x0;
		for (int x = x0; x < x1; x++, indexOut++) {
			final int total = totals[x - x0];
			output.data[indexOut] = (short)((total + halfDivisor)/divisor);
		}

		// For the reminder we only need to add and remove the first and last elements to update the solution
		for (int y = 0; y < input.height - length; y++) {
			indexOut = output.startIndex + output.stride*(offset + y + 1) + x0;
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++, indexOut++) {
				int total = totals[i - indexIn] - (input.data[i]& 0xFFFF);
				total += input.data[i + regionStepY]& 0xFFFF;
				output.data[indexOut] = (short)((total + halfDivisor)/divisor);
				totals[i - indexIn] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontalBorder( GrayF32 input, GrayF32 output, int offset, int length ) {
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;

			float total = 0;
			int count = length - offset;
			int jEnd = j + count;
			if (offset > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				dataDst[indexDest++] = total/count;
			}

			while (++count < length) {
				total += dataSrc[jEnd++];
				dataDst[indexDest++] = total/count;
			}

			jEnd = j + width;
			count = offset + offsetR;
			j += width - count;
			indexDest += width - count;
			total = 0;
			if (offsetR > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				dataDst[indexDest++] = total/count;
			}
			while (--count > offset) {
				total -= dataSrc[j++];
				dataDst[indexDest++] = total/count;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( GrayF32 input,GrayF32 output, int offset, int length ) {
		final float divisor = length;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			float total = 0;

			int indexEnd = indexIn + length;
			
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut++] = (total/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] ;
				total += input.data[indexIn] ;

				output.data[indexOut++] = (total/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void verticalBorder( GrayF32 input, GrayF32 output, int offset, int length, @Nullable GrowArray<DogArray_F32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F32::new);
		final DogArray_F32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, length, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		float[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Image Top
		for (int count = length - offset; count < length; count++) {
			final int indexInRow = input.startIndex + x0;
			if (count == length - offset) {
				int indexIn = indexInRow;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}

				for (int y = 1; y < count; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++];
					}
				}
			} else {
				int indexIn0 = indexInRow + (count - 1)*input.stride;
				int end = indexIn0 + x1 - x0;
				for (int i = indexIn0; i < end; i++) {
					totals[i - indexIn0] += dataSrc[i];
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = totals[x - x0]/count;
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			final int indexInRow = input.startIndex + x0;
			if (yStart == height - length + 1) {
				int indexIn = indexInRow + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}

				for (int y = yStart + 1; y < height; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++];
					}
				}
			} else {
				int indexIn0 = indexInRow + (yStart - 1)*input.stride;
				int indexIn1 = indexIn0 + x1 - x0;

				for (int i = indexIn0; i < indexIn1; i++) {
					totals[i - indexIn0] -= dataSrc[i];
				}
			}

			int count = height - yStart;
			int indexOut = output.startIndex + (yStart + offset)*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = totals[x - x0]/count;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void vertical( GrayF32 input, GrayF32 output, int offset, int length, @Nullable GrowArray<DogArray_F32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F32::new);
		final DogArray_F32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE

		final float divisor = length;
		final int regionStepY = length*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		float[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Sum up along x-axis to avoid cache misses when reading from input image
		// Initialize recursion by summing up the first kernels along the x-axis
		{
			int indexIn = input.startIndex + x0;

			for (int x = x0; x < x1; x++) {
				totals[x - x0] = input.data[indexIn++];
			}
		}
		for (int y = 1; y < length; y++) {
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++) {
				totals[i - indexIn] += input.data[i];
			}
		}

		int indexOut = output.startIndex + output.stride*offset + x0;
		for (int x = x0; x < x1; x++, indexOut++) {
			final float total = totals[x - x0];
			output.data[indexOut] = total/divisor;
		}

		// For the reminder we only need to add and remove the first and last elements to update the solution
		for (int y = 0; y < input.height - length; y++) {
			indexOut = output.startIndex + output.stride*(offset + y + 1) + x0;
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++, indexOut++) {
				float total = totals[i - indexIn] - (input.data[i]);
				total += input.data[i + regionStepY];
				output.data[indexOut] = total/divisor;
				totals[i - indexIn] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontalBorder( GrayF64 input, GrayF64 output, int offset, int length ) {
		final double[] dataSrc = input.data;
		final double[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;

			double total = 0;
			int count = length - offset;
			int jEnd = j + count;
			if (offset > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				dataDst[indexDest++] = total/count;
			}

			while (++count < length) {
				total += dataSrc[jEnd++];
				dataDst[indexDest++] = total/count;
			}

			jEnd = j + width;
			count = offset + offsetR;
			j += width - count;
			indexDest += width - count;
			total = 0;
			if (offsetR > 0) {
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				dataDst[indexDest++] = total/count;
			}
			while (--count > offset) {
				total -= dataSrc[j++];
				dataDst[indexDest++] = total/count;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( GrayF64 input,GrayF64 output, int offset, int length ) {
		final double divisor = length;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < input.height; y++) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + offset;

			double total = 0;

			int indexEnd = indexIn + length;
			
			for (; indexIn < indexEnd; indexIn++) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut++] = (total/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] ;
				total += input.data[indexIn] ;

				output.data[indexOut++] = (total/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void verticalBorder( GrayF64 input, GrayF64 output, int offset, int length, @Nullable GrowArray<DogArray_F64> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F64::new);
		final DogArray_F64 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final double[] dataSrc = input.data;
		final double[] dataDst = output.data;

		final int offsetR = length - offset - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, length, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		double[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Image Top
		for (int count = length - offset; count < length; count++) {
			final int indexInRow = input.startIndex + x0;
			if (count == length - offset) {
				int indexIn = indexInRow;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}

				for (int y = 1; y < count; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++];
					}
				}
			} else {
				int indexIn0 = indexInRow + (count - 1)*input.stride;
				int end = indexIn0 + x1 - x0;
				for (int i = indexIn0; i < end; i++) {
					totals[i - indexIn0] += dataSrc[i];
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = totals[x - x0]/count;
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			final int indexInRow = input.startIndex + x0;
			if (yStart == height - length + 1) {
				int indexIn = indexInRow + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}

				for (int y = yStart + 1; y < height; y++) {
					indexIn = indexInRow + y*input.stride;

					for (int x = x0; x < x1; x++) {
						totals[x - x0] += dataSrc[indexIn++];
					}
				}
			} else {
				int indexIn0 = indexInRow + (yStart - 1)*input.stride;
				int indexIn1 = indexIn0 + x1 - x0;

				for (int i = indexIn0; i < indexIn1; i++) {
					totals[i - indexIn0] -= dataSrc[i];
				}
			}

			int count = height - yStart;
			int indexOut = output.startIndex + (yStart + offset)*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = totals[x - x0]/count;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void vertical( GrayF64 input, GrayF64 output, int offset, int length, @Nullable GrowArray<DogArray_F64> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F64::new);
		final DogArray_F64 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE

		final double divisor = length;
		final int regionStepY = length*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1)->{
		final int x0 = 0, x1 = input.width;
		double[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);

		// Sum up along x-axis to avoid cache misses when reading from input image
		// Initialize recursion by summing up the first kernels along the x-axis
		{
			int indexIn = input.startIndex + x0;

			for (int x = x0; x < x1; x++) {
				totals[x - x0] = input.data[indexIn++];
			}
		}
		for (int y = 1; y < length; y++) {
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++) {
				totals[i - indexIn] += input.data[i];
			}
		}

		int indexOut = output.startIndex + output.stride*offset + x0;
		for (int x = x0; x < x1; x++, indexOut++) {
			final double total = totals[x - x0];
			output.data[indexOut] = total/divisor;
		}

		// For the reminder we only need to add and remove the first and last elements to update the solution
		for (int y = 0; y < input.height - length; y++) {
			indexOut = output.startIndex + output.stride*(offset + y + 1) + x0;
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexInEnd = indexIn + x1 - x0;
			for (int i = indexIn; i < indexInEnd; i++, indexOut++) {
				double total = totals[i - indexIn] - (input.data[i]);
				total += input.data[i + regionStepY];
				output.data[indexOut] = total/divisor;
				totals[i - indexIn] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

}
