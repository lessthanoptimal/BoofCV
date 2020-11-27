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

	public static void horizontal( GrayU8 input ,GrayI8 output, int offset, int length ) {
		final int divisor = length;
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
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

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0,y1)->{
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

	public static void horizontal( GrayS16 input ,GrayI16 output, int offset, int length ) {
		final int divisor = length;
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
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

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0,y1)->{
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

	public static void horizontal( GrayU16 input ,GrayI16 output, int offset, int length ) {
		final int divisor = length;
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
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

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0,y1)->{
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

	public static void horizontal( GrayF32 input ,GrayF32 output, int offset, int length ) {
		final float divisor = length;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
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

	public static void vertical( GrayF32 input, GrayF32 output, int offset, int length, @Nullable GrowArray<DogArray_F32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F32::new);
		final DogArray_F32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int backStep = length*input.stride;
		final int offsetEnd = length - offset - 1;

		final float divisor = length;

		// To reduce cache misses it is processed along rows instead of going down columns, which is
		// more natural for a vertical convolution. For parallel processes this requires building
		// a book keeping array for each thread.

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0,y1)->{
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

	public static void horizontal( GrayF64 input ,GrayF64 output, int offset, int length ) {
		final double divisor = length;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
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

	public static void vertical( GrayF64 input, GrayF64 output, int offset, int length, @Nullable GrowArray<DogArray_F64> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F64::new);
		final DogArray_F64 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int backStep = length*input.stride;
		final int offsetEnd = length - offset - 1;

		final double divisor = length;

		// To reduce cache misses it is processed along rows instead of going down columns, which is
		// more natural for a vertical convolution. For parallel processes this requires building
		// a book keeping array for each thread.

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, output.height - offsetEnd, length, workspaces, (work, y0,y1)->{
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
