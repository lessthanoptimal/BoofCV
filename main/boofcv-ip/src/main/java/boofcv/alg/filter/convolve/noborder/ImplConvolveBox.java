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
 * Convolves a box filter across an image. A box filter is equivalent to convolving a kernel with all 1's.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateImplConvolveBox</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.noborder.GenerateImplConvolveBox")
@SuppressWarnings({"ForLoopReplaceableByForEach","Duplicates"})
public class ImplConvolveBox {

	public static void horizontal( GrayU8 input , GrayI16 output , int radius ) {
		final int kernelWidth = radius*2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + radius;

			int total = 0;

			int indexEnd = indexIn + kernelWidth;
			
			for( ; indexIn < indexEnd; indexIn++ ) {
				total += input.data[indexIn] & 0xFF;
			}
			output.data[indexOut++] = (short)total;

			indexEnd = indexIn + input.width - kernelWidth;
			for( ; indexIn < indexEnd; indexIn++ ) {
				total -= input.data[ indexIn - kernelWidth ] & 0xFF;
				total += input.data[ indexIn ] & 0xFF;

				output.data[indexOut++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(GrayU8 input, GrayI16 output, int radius, @Nullable GrowArray<DogArray_I32> workspaces) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int kernelWidth = radius*2 + 1;

		final int backStep = kernelWidth*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius, output.height-radius, kernelWidth, workspaces, (work, y0,y1)->{
		final int y0 = radius, y1 = output.height-radius;
		int totals[] = BoofMiscOps.checkDeclare(work,input.width,false);
		for( int x = 0; x < input.width; x++ ) {
			int indexIn = input.startIndex + (y0-radius)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*kernelWidth;
			for( ; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] & 0xFF;
			}
			totals[x] = total;
			output.data[indexOut] = (short)total;
		}

		// change the order it is processed in to reduce cache misses
		for( int y = y0+1; y < y1; y++ ) {
			int indexIn = input.startIndex + (y+radius)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {
				int total = totals[ x ]  - (input.data[ indexIn - backStep ]& 0xFF);
				totals[ x ] = total += input.data[ indexIn ]& 0xFF;

				output.data[indexOut] = (short)total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( GrayU8 input , GrayS32 output , int radius ) {
		final int kernelWidth = radius*2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + radius;

			int total = 0;

			int indexEnd = indexIn + kernelWidth;
			
			for( ; indexIn < indexEnd; indexIn++ ) {
				total += input.data[indexIn] & 0xFF;
			}
			output.data[indexOut++] = total;

			indexEnd = indexIn + input.width - kernelWidth;
			for( ; indexIn < indexEnd; indexIn++ ) {
				total -= input.data[ indexIn - kernelWidth ] & 0xFF;
				total += input.data[ indexIn ] & 0xFF;

				output.data[indexOut++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(GrayU8 input, GrayS32 output, int radius, @Nullable GrowArray<DogArray_I32> workspaces) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int kernelWidth = radius*2 + 1;

		final int backStep = kernelWidth*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius, output.height-radius, kernelWidth, workspaces, (work, y0,y1)->{
		final int y0 = radius, y1 = output.height-radius;
		int totals[] = BoofMiscOps.checkDeclare(work,input.width,false);
		for( int x = 0; x < input.width; x++ ) {
			int indexIn = input.startIndex + (y0-radius)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*kernelWidth;
			for( ; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] & 0xFF;
			}
			totals[x] = total;
			output.data[indexOut] = total;
		}

		// change the order it is processed in to reduce cache misses
		for( int y = y0+1; y < y1; y++ ) {
			int indexIn = input.startIndex + (y+radius)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {
				int total = totals[ x ]  - (input.data[ indexIn - backStep ]& 0xFF);
				totals[ x ] = total += input.data[ indexIn ]& 0xFF;

				output.data[indexOut] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( GrayS16 input , GrayI16 output , int radius ) {
		final int kernelWidth = radius*2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + radius;

			int total = 0;

			int indexEnd = indexIn + kernelWidth;
			
			for( ; indexIn < indexEnd; indexIn++ ) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut++] = (short)total;

			indexEnd = indexIn + input.width - kernelWidth;
			for( ; indexIn < indexEnd; indexIn++ ) {
				total -= input.data[ indexIn - kernelWidth ] ;
				total += input.data[ indexIn ] ;

				output.data[indexOut++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(GrayS16 input, GrayI16 output, int radius, @Nullable GrowArray<DogArray_I32> workspaces) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int kernelWidth = radius*2 + 1;

		final int backStep = kernelWidth*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius, output.height-radius, kernelWidth, workspaces, (work, y0,y1)->{
		final int y0 = radius, y1 = output.height-radius;
		int totals[] = BoofMiscOps.checkDeclare(work,input.width,false);
		for( int x = 0; x < input.width; x++ ) {
			int indexIn = input.startIndex + (y0-radius)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*kernelWidth;
			for( ; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] ;
			}
			totals[x] = total;
			output.data[indexOut] = (short)total;
		}

		// change the order it is processed in to reduce cache misses
		for( int y = y0+1; y < y1; y++ ) {
			int indexIn = input.startIndex + (y+radius)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {
				int total = totals[ x ]  - (input.data[ indexIn - backStep ]);
				totals[ x ] = total += input.data[ indexIn ];

				output.data[indexOut] = (short)total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( GrayU16 input , GrayI16 output , int radius ) {
		final int kernelWidth = radius*2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + radius;

			int total = 0;

			int indexEnd = indexIn + kernelWidth;
			
			for( ; indexIn < indexEnd; indexIn++ ) {
				total += input.data[indexIn] & 0xFFFF;
			}
			output.data[indexOut++] = (short)total;

			indexEnd = indexIn + input.width - kernelWidth;
			for( ; indexIn < indexEnd; indexIn++ ) {
				total -= input.data[ indexIn - kernelWidth ] & 0xFFFF;
				total += input.data[ indexIn ] & 0xFFFF;

				output.data[indexOut++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(GrayU16 input, GrayI16 output, int radius, @Nullable GrowArray<DogArray_I32> workspaces) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int kernelWidth = radius*2 + 1;

		final int backStep = kernelWidth*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius, output.height-radius, kernelWidth, workspaces, (work, y0,y1)->{
		final int y0 = radius, y1 = output.height-radius;
		int totals[] = BoofMiscOps.checkDeclare(work,input.width,false);
		for( int x = 0; x < input.width; x++ ) {
			int indexIn = input.startIndex + (y0-radius)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*kernelWidth;
			for( ; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] & 0xFFFF;
			}
			totals[x] = total;
			output.data[indexOut] = (short)total;
		}

		// change the order it is processed in to reduce cache misses
		for( int y = y0+1; y < y1; y++ ) {
			int indexIn = input.startIndex + (y+radius)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {
				int total = totals[ x ]  - (input.data[ indexIn - backStep ]& 0xFFFF);
				totals[ x ] = total += input.data[ indexIn ]& 0xFFFF;

				output.data[indexOut] = (short)total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( GrayS32 input , GrayS32 output , int radius ) {
		final int kernelWidth = radius*2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + radius;

			int total = 0;

			int indexEnd = indexIn + kernelWidth;
			
			for( ; indexIn < indexEnd; indexIn++ ) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut++] = total;

			indexEnd = indexIn + input.width - kernelWidth;
			for( ; indexIn < indexEnd; indexIn++ ) {
				total -= input.data[ indexIn - kernelWidth ] ;
				total += input.data[ indexIn ] ;

				output.data[indexOut++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(GrayS32 input, GrayS32 output, int radius, @Nullable GrowArray<DogArray_I32> workspaces) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int kernelWidth = radius*2 + 1;

		final int backStep = kernelWidth*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius, output.height-radius, kernelWidth, workspaces, (work, y0,y1)->{
		final int y0 = radius, y1 = output.height-radius;
		int totals[] = BoofMiscOps.checkDeclare(work,input.width,false);
		for( int x = 0; x < input.width; x++ ) {
			int indexIn = input.startIndex + (y0-radius)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			int total = 0;
			int indexEnd = indexIn + input.stride*kernelWidth;
			for( ; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] ;
			}
			totals[x] = total;
			output.data[indexOut] = total;
		}

		// change the order it is processed in to reduce cache misses
		for( int y = y0+1; y < y1; y++ ) {
			int indexIn = input.startIndex + (y+radius)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {
				int total = totals[ x ]  - (input.data[ indexIn - backStep ]);
				totals[ x ] = total += input.data[ indexIn ];

				output.data[indexOut] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( GrayF32 input , GrayF32 output , int radius ) {
		final int kernelWidth = radius*2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + radius;

			float total = 0;

			int indexEnd = indexIn + kernelWidth;
			
			for( ; indexIn < indexEnd; indexIn++ ) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut++] = total;

			indexEnd = indexIn + input.width - kernelWidth;
			for( ; indexIn < indexEnd; indexIn++ ) {
				total -= input.data[ indexIn - kernelWidth ] ;
				total += input.data[ indexIn ] ;

				output.data[indexOut++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(GrayF32 input, GrayF32 output, int radius, @Nullable GrowArray<DogArray_F32> workspaces) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F32::new);
		final DogArray_F32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int kernelWidth = radius*2 + 1;

		final int backStep = kernelWidth*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius, output.height-radius, kernelWidth, workspaces, (work, y0,y1)->{
		final int y0 = radius, y1 = output.height-radius;
		float totals[] = BoofMiscOps.checkDeclare(work,input.width,false);
		for( int x = 0; x < input.width; x++ ) {
			int indexIn = input.startIndex + (y0-radius)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			float total = 0;
			int indexEnd = indexIn + input.stride*kernelWidth;
			for( ; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] ;
			}
			totals[x] = total;
			output.data[indexOut] = total;
		}

		// change the order it is processed in to reduce cache misses
		for( int y = y0+1; y < y1; y++ ) {
			int indexIn = input.startIndex + (y+radius)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {
				float total = totals[ x ]  - (input.data[ indexIn - backStep ]);
				totals[ x ] = total += input.data[ indexIn ];

				output.data[indexOut] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( GrayF64 input , GrayF64 output , int radius ) {
		final int kernelWidth = radius*2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for( int y = 0; y < input.height; y++ ) {
			int indexIn = input.startIndex + input.stride*y;
			int indexOut = output.startIndex + output.stride*y + radius;

			double total = 0;

			int indexEnd = indexIn + kernelWidth;
			
			for( ; indexIn < indexEnd; indexIn++ ) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut++] = total;

			indexEnd = indexIn + input.width - kernelWidth;
			for( ; indexIn < indexEnd; indexIn++ ) {
				total -= input.data[ indexIn - kernelWidth ] ;
				total += input.data[ indexIn ] ;

				output.data[indexOut++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(GrayF64 input, GrayF64 output, int radius, @Nullable GrowArray<DogArray_F64> workspaces) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F64::new);
		final DogArray_F64 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int kernelWidth = radius*2 + 1;

		final int backStep = kernelWidth*input.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radius, output.height-radius, kernelWidth, workspaces, (work, y0,y1)->{
		final int y0 = radius, y1 = output.height-radius;
		double totals[] = BoofMiscOps.checkDeclare(work,input.width,false);
		for( int x = 0; x < input.width; x++ ) {
			int indexIn = input.startIndex + (y0-radius)*input.stride + x;
			int indexOut = output.startIndex + output.stride*y0 + x;

			double total = 0;
			int indexEnd = indexIn + input.stride*kernelWidth;
			for( ; indexIn < indexEnd; indexIn += input.stride) {
				total += input.data[indexIn] ;
			}
			totals[x] = total;
			output.data[indexOut] = total;
		}

		// change the order it is processed in to reduce cache misses
		for( int y = y0+1; y < y1; y++ ) {
			int indexIn = input.startIndex + (y+radius)*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < input.width; x++ ,indexIn++,indexOut++) {
				double total = totals[ x ]  - (input.data[ indexIn - backStep ]);
				totals[ x ] = total += input.data[ indexIn ];

				output.data[indexOut] = total;
			}
		}
		//CONCURRENT_INLINE });
	}

}
