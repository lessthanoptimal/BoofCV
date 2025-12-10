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

			for (int i = 0; i < offset; i++) {
				int jEnd = j + i + length - offset;
				int total = 0;
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFF;
				}
				int count = jEnd - j;
				dataDst[indexDest++] = (byte)((total + count/2)/count);
			}

			int jEnd = j + width;
			j += width - (offset + offsetR);
			indexDest += width - (offset + offsetR);
			for (int i = 0; i < offsetR; i++) {
				int total = 0;
				for (int indexSrc = j + i; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFF;
				}
				int count = jEnd - j - i;
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
			output.data[indexOut++] = (byte)((total+halfDivisor)/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] & 0xFF;
				total += input.data[indexIn] & 0xFF;

				output.data[indexOut++] = (byte)((total+halfDivisor)/divisor);
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
			{
				int indexIn = input.startIndex + x0;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFF;
				}
			}
			for (int y = 1; y < count; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++]& 0xFF;
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (byte)((totals[x - x0] + count/2)/count);
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			{
				int indexIn = input.startIndex + x0 + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFF;
				}
			}

			for (int y = yStart + 1; y < height; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++]& 0xFF;
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
		final int backStep = length*input.stride;
		final int offsetEnd = length - offset - 1;

		final int divisor = length;
		final int halfDivisor = divisor/2;

		// To reduce cache misses it is processed along rows instead of going down columns, which is
		// more natural for a vertical convolution. For parallel processes this requires building
		// a book keeping array for each thread.

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = output.height - offsetEnd;
		int[] totals = BoofMiscOps.checkDeclare(work, input.width, false);
		for (int x = 0; x < input.width; x++) {
			int indexIn = input.startIndex + (y0 - offset)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*length;
			for (; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] & 0xFF;
			}
			totals[x] = total;
			output.data[indexOut] = (byte)((total + halfDivisor)/divisor);
		}

		// change the order it is processed in to reduce cache misses
		for (int y = y0 + 1; y < y1; y++) {
			int indexIn = input.startIndex + (y + offsetEnd)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++, indexIn++, indexOut++) {
				int total = totals[x] - (input.data[indexIn - backStep]& 0xFF);
				totals[x] = total += input.data[indexIn]& 0xFF;

				output.data[indexOut] = (byte)((total + halfDivisor)/divisor);
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

			for (int i = 0; i < offset; i++) {
				int jEnd = j + i + length - offset;
				int total = 0;
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				int count = jEnd - j;
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}

			int jEnd = j + width;
			j += width - (offset + offsetR);
			indexDest += width - (offset + offsetR);
			for (int i = 0; i < offsetR; i++) {
				int total = 0;
				for (int indexSrc = j + i; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				int count = jEnd - j - i;
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
			output.data[indexOut++] = (short)((total+halfDivisor)/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] ;
				total += input.data[indexIn] ;

				output.data[indexOut++] = (short)((total+halfDivisor)/divisor);
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
			{
				int indexIn = input.startIndex + x0;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}
			}
			for (int y = 1; y < count; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++];
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (short)((totals[x - x0] + count/2)/count);
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			{
				int indexIn = input.startIndex + x0 + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}
			}

			for (int y = yStart + 1; y < height; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++];
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
		final int backStep = length*input.stride;
		final int offsetEnd = length - offset - 1;

		final int divisor = length;
		final int halfDivisor = divisor/2;

		// To reduce cache misses it is processed along rows instead of going down columns, which is
		// more natural for a vertical convolution. For parallel processes this requires building
		// a book keeping array for each thread.

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = output.height - offsetEnd;
		int[] totals = BoofMiscOps.checkDeclare(work, input.width, false);
		for (int x = 0; x < input.width; x++) {
			int indexIn = input.startIndex + (y0 - offset)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*length;
			for (; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] ;
			}
			totals[x] = total;
			output.data[indexOut] = (short)((total + halfDivisor)/divisor);
		}

		// change the order it is processed in to reduce cache misses
		for (int y = y0 + 1; y < y1; y++) {
			int indexIn = input.startIndex + (y + offsetEnd)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++, indexIn++, indexOut++) {
				int total = totals[x] - (input.data[indexIn - backStep]);
				totals[x] = total += input.data[indexIn];

				output.data[indexOut] = (short)((total + halfDivisor)/divisor);
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

			for (int i = 0; i < offset; i++) {
				int jEnd = j + i + length - offset;
				int total = 0;
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFFFF;
				}
				int count = jEnd - j;
				dataDst[indexDest++] = (short)((total + count/2)/count);
			}

			int jEnd = j + width;
			j += width - (offset + offsetR);
			indexDest += width - (offset + offsetR);
			for (int i = 0; i < offsetR; i++) {
				int total = 0;
				for (int indexSrc = j + i; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc]& 0xFFFF;
				}
				int count = jEnd - j - i;
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
			output.data[indexOut++] = (short)((total+halfDivisor)/divisor);

			indexEnd = indexIn + input.width - length;
			for (; indexIn < indexEnd; indexIn++) {
				total -= input.data[indexIn - length] & 0xFFFF;
				total += input.data[indexIn] & 0xFFFF;

				output.data[indexOut++] = (short)((total+halfDivisor)/divisor);
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
			{
				int indexIn = input.startIndex + x0;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFFFF;
				}
			}
			for (int y = 1; y < count; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++]& 0xFFFF;
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = (short)((totals[x - x0] + count/2)/count);
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			{
				int indexIn = input.startIndex + x0 + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++]& 0xFFFF;
				}
			}

			for (int y = yStart + 1; y < height; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++]& 0xFFFF;
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
		final int backStep = length*input.stride;
		final int offsetEnd = length - offset - 1;

		final int divisor = length;
		final int halfDivisor = divisor/2;

		// To reduce cache misses it is processed along rows instead of going down columns, which is
		// more natural for a vertical convolution. For parallel processes this requires building
		// a book keeping array for each thread.

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = output.height - offsetEnd;
		int[] totals = BoofMiscOps.checkDeclare(work, input.width, false);
		for (int x = 0; x < input.width; x++) {
			int indexIn = input.startIndex + (y0 - offset)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*length;
			for (; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] & 0xFFFF;
			}
			totals[x] = total;
			output.data[indexOut] = (short)((total + halfDivisor)/divisor);
		}

		// change the order it is processed in to reduce cache misses
		for (int y = y0 + 1; y < y1; y++) {
			int indexIn = input.startIndex + (y + offsetEnd)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++, indexIn++, indexOut++) {
				int total = totals[x] - (input.data[indexIn - backStep]& 0xFFFF);
				totals[x] = total += input.data[indexIn]& 0xFFFF;

				output.data[indexOut] = (short)((total + halfDivisor)/divisor);
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

			for (int i = 0; i < offset; i++) {
				int jEnd = j + i + length - offset;
				float total = 0;
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				int count = jEnd - j;
				dataDst[indexDest++] = total/count;
			}

			int jEnd = j + width;
			j += width - (offset + offsetR);
			indexDest += width - (offset + offsetR);
			for (int i = 0; i < offsetR; i++) {
				float total = 0;
				for (int indexSrc = j + i; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				int count = jEnd - j - i;
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
			{
				int indexIn = input.startIndex + x0;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}
			}
			for (int y = 1; y < count; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++];
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = totals[x - x0]/count;
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			{
				int indexIn = input.startIndex + x0 + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}
			}

			for (int y = yStart + 1; y < height; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++];
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
		final int backStep = length*input.stride;
		final int offsetEnd = length - offset - 1;

		final float divisor = length;

		// To reduce cache misses it is processed along rows instead of going down columns, which is
		// more natural for a vertical convolution. For parallel processes this requires building
		// a book keeping array for each thread.

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = output.height - offsetEnd;
		float[] totals = BoofMiscOps.checkDeclare(work, input.width, false);
		for (int x = 0; x < input.width; x++) {
			int indexIn = input.startIndex + (y0 - offset)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			float total = 0;
			int indexEnd = indexIn + input.stride*length;
			for (; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] ;
			}
			totals[x] = total;
			output.data[indexOut] = (total/divisor);
		}

		// change the order it is processed in to reduce cache misses
		for (int y = y0 + 1; y < y1; y++) {
			int indexIn = input.startIndex + (y + offsetEnd)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++, indexIn++, indexOut++) {
				float total = totals[x] - (input.data[indexIn - backStep]);
				totals[x] = total += input.data[indexIn];

				output.data[indexOut] = (total/divisor);
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

			for (int i = 0; i < offset; i++) {
				int jEnd = j + i + length - offset;
				double total = 0;
				for (int indexSrc = j; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				int count = jEnd - j;
				dataDst[indexDest++] = total/count;
			}

			int jEnd = j + width;
			j += width - (offset + offsetR);
			indexDest += width - (offset + offsetR);
			for (int i = 0; i < offsetR; i++) {
				double total = 0;
				for (int indexSrc = j + i; indexSrc < jEnd; indexSrc++) {
					total += dataSrc[indexSrc];
				}
				int count = jEnd - j - i;
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
			{
				int indexIn = input.startIndex + x0;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}
			}
			for (int y = 1; y < count; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++];
				}
			}
			int indexOut = output.startIndex + (count - (length - offset))*output.stride;
			for (int x = x0; x < x1; x++) {
				dataDst[indexOut + x] = totals[x - x0]/count;
			}
		}
		// Image Bottom
		for (int yStart = height - length + 1; yStart < height - offset; yStart++) {
			{
				int indexIn = input.startIndex + x0 + yStart*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] = dataSrc[indexIn++];
				}
			}

			for (int y = yStart + 1; y < height; y++) {
				int indexIn = input.startIndex + x0 + y*input.stride;

				for (int x = x0; x < x1; x++) {
					totals[x - x0] += dataSrc[indexIn++];
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
		final int backStep = length*input.stride;
		final int offsetEnd = length - offset - 1;

		final double divisor = length;

		// To reduce cache misses it is processed along rows instead of going down columns, which is
		// more natural for a vertical convolution. For parallel processes this requires building
		// a book keeping array for each thread.

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = output.height - offsetEnd;
		double[] totals = BoofMiscOps.checkDeclare(work, input.width, false);
		for (int x = 0; x < input.width; x++) {
			int indexIn = input.startIndex + (y0 - offset)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			double total = 0;
			int indexEnd = indexIn + input.stride*length;
			for (; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] ;
			}
			totals[x] = total;
			output.data[indexOut] = (total/divisor);
		}

		// change the order it is processed in to reduce cache misses
		for (int y = y0 + 1; y < y1; y++) {
			int indexIn = input.startIndex + (y + offsetEnd)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < input.width; x++, indexIn++, indexOut++) {
				double total = totals[x] - (input.data[indexIn - backStep]);
				totals[x] = total += input.data[indexIn];

				output.data[indexOut] = (total/divisor);
			}
		}
		//CONCURRENT_INLINE });
	}

}
