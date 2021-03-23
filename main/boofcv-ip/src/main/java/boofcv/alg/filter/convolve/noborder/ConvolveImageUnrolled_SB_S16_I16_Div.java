/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayI16;
import boofcv.struct.image.GrayS16;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

//CONCURRENT_CLASS_NAME ConvolveImageUnrolled_SB_MT_S16_I16_Div
//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>
 * Unrolls the convolution kernel to reduce array accessing and save often used variables to the stack.
 * </p>
 *
 * <p>
 * Unrolling the image being convolved resulting in an additional 10% performance boost on a Core i7 processor,
 * see commented out code below. Due to the added complexity it was decided that this performance boost was
 * not worth it. By comparison, unrolling the kernel causes a performance boost between 2 and 3 times.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateConvolvedUnrolled_SB</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.noborder.GenerateConvolvedUnrolled_SB")
@SuppressWarnings({"ForLoopReplaceableByForEach","Duplicates"})
public class ConvolveImageUnrolled_SB_S16_I16_Div {
	public static boolean horizontal( Kernel1D_S32 kernel,
								   GrayS16 image, GrayI16 dest, int divisor ) {

		// Unrolled functions only exist for symmetric kernels with an odd width
		if( kernel.offset != kernel.width/2 || kernel.width%2 == 0 )
			return false;

		switch (kernel.width) {
			case 3: horizontal3(kernel, image, dest, divisor); break;
			case 5: horizontal5(kernel, image, dest, divisor); break;
			case 7: horizontal7(kernel, image, dest, divisor); break;
			case 9: horizontal9(kernel, image, dest, divisor); break;
			case 11: horizontal11(kernel, image, dest, divisor); break;
			default: return false;
		}
		return true;
	}

	public static boolean vertical( Kernel1D_S32 kernel,
								   GrayS16 image, GrayI16 dest, int divisor, GrowArray<DogArray_I32> work ) {

		// Unrolled functions only exist for symmetric kernels with an odd width
		if( kernel.offset != kernel.width/2 || kernel.width%2 == 0 )
			return false;

		switch (kernel.width) {
			case 3: vertical3(kernel, image, dest, divisor, work); break;
			case 5: vertical5(kernel, image, dest, divisor, work); break;
			case 7: vertical7(kernel, image, dest, divisor, work); break;
			case 9: vertical9(kernel, image, dest, divisor, work); break;
			case 11: vertical11(kernel, image, dest, divisor, work); break;
			default: return false;
		}
		return true;
	}

	public static boolean convolve( Kernel2D_S32 kernel,
								   GrayS16 image, GrayI16 dest, int divisor, GrowArray<DogArray_I32> work ) {

		// Unrolled functions only exist for symmetric kernels with an odd width
		if( kernel.offset != kernel.width/2 || kernel.width%2 == 0 )
			return false;

		switch (kernel.width) {
			case 3: convolve3(kernel, image, dest, divisor, work); break;
			case 5: convolve5(kernel, image, dest, divisor, work); break;
			case 7: convolve7(kernel, image, dest, divisor, work); break;
			case 9: convolve9(kernel, image, dest, divisor, work); break;
			case 11: convolve11(kernel, image, dest, divisor, work); break;
			default: return false;
		}
		return true;
	}

	public static void horizontal3( Kernel1D_S32 kernel , GrayS16 image, GrayI16 dest , int divisor )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];

		final int radius = kernel.getRadius();

		final int width = image.getWidth();
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
		for (int i = 0; i < image.height; i++) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int indexSrc = j;
				int total = (dataSrc[indexSrc++])*k1;
				total += (dataSrc[indexSrc++])*k2;
				total += (dataSrc[indexSrc])*k3;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal5( Kernel1D_S32 kernel , GrayS16 image, GrayI16 dest , int divisor )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];

		final int radius = kernel.getRadius();

		final int width = image.getWidth();
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
		for (int i = 0; i < image.height; i++) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int indexSrc = j;
				int total = (dataSrc[indexSrc++])*k1;
				total += (dataSrc[indexSrc++])*k2;
				total += (dataSrc[indexSrc++])*k3;
				total += (dataSrc[indexSrc++])*k4;
				total += (dataSrc[indexSrc])*k5;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal7( Kernel1D_S32 kernel , GrayS16 image, GrayI16 dest , int divisor )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];
		final int k6 = kernel.data[5];
		final int k7 = kernel.data[6];

		final int radius = kernel.getRadius();

		final int width = image.getWidth();
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
		for (int i = 0; i < image.height; i++) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int indexSrc = j;
				int total = (dataSrc[indexSrc++])*k1;
				total += (dataSrc[indexSrc++])*k2;
				total += (dataSrc[indexSrc++])*k3;
				total += (dataSrc[indexSrc++])*k4;
				total += (dataSrc[indexSrc++])*k5;
				total += (dataSrc[indexSrc++])*k6;
				total += (dataSrc[indexSrc])*k7;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal9( Kernel1D_S32 kernel , GrayS16 image, GrayI16 dest , int divisor )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];
		final int k6 = kernel.data[5];
		final int k7 = kernel.data[6];
		final int k8 = kernel.data[7];
		final int k9 = kernel.data[8];

		final int radius = kernel.getRadius();

		final int width = image.getWidth();
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
		for (int i = 0; i < image.height; i++) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int indexSrc = j;
				int total = (dataSrc[indexSrc++])*k1;
				total += (dataSrc[indexSrc++])*k2;
				total += (dataSrc[indexSrc++])*k3;
				total += (dataSrc[indexSrc++])*k4;
				total += (dataSrc[indexSrc++])*k5;
				total += (dataSrc[indexSrc++])*k6;
				total += (dataSrc[indexSrc++])*k7;
				total += (dataSrc[indexSrc++])*k8;
				total += (dataSrc[indexSrc])*k9;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal11( Kernel1D_S32 kernel , GrayS16 image, GrayI16 dest , int divisor )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];
		final int k6 = kernel.data[5];
		final int k7 = kernel.data[6];
		final int k8 = kernel.data[7];
		final int k9 = kernel.data[8];
		final int k10 = kernel.data[9];
		final int k11 = kernel.data[10];

		final int radius = kernel.getRadius();

		final int width = image.getWidth();
		final int halfDivisor = divisor/2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
		for (int i = 0; i < image.height; i++) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int indexSrc = j;
				int total = (dataSrc[indexSrc++])*k1;
				total += (dataSrc[indexSrc++])*k2;
				total += (dataSrc[indexSrc++])*k3;
				total += (dataSrc[indexSrc++])*k4;
				total += (dataSrc[indexSrc++])*k5;
				total += (dataSrc[indexSrc++])*k6;
				total += (dataSrc[indexSrc++])*k7;
				total += (dataSrc[indexSrc++])*k8;
				total += (dataSrc[indexSrc++])*k9;
				total += (dataSrc[indexSrc++])*k10;
				total += (dataSrc[indexSrc])*k11;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical3( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst , int divisor, @Nullable GrowArray<DogArray_I32> workspaces )
	{
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];

		final int radius = kernel.getRadius();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int halfDivisor = divisor/2;

		final int yEnd = imgHeight - radius;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(radius, yEnd, y -> {
		for (int y = radius; y < yEnd; y++) {
			int indexDst = dst.startIndex + y*dst.stride;
			int i = src.startIndex + (y - radius)*src.stride;
			final int iEnd = i + imgWidth;

			for (; i < iEnd; i++) {
				int indexSrc = i;

				int total = (dataSrc[indexSrc]) * k1;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k2;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k3;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical5( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst , int divisor, @Nullable GrowArray<DogArray_I32> workspaces )
	{
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];

		final int radius = kernel.getRadius();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int halfDivisor = divisor/2;

		final int yEnd = imgHeight - radius;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(radius, yEnd, y -> {
		for (int y = radius; y < yEnd; y++) {
			int indexDst = dst.startIndex + y*dst.stride;
			int i = src.startIndex + (y - radius)*src.stride;
			final int iEnd = i + imgWidth;

			for (; i < iEnd; i++) {
				int indexSrc = i;

				int total = (dataSrc[indexSrc]) * k1;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k2;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k3;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k4;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k5;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical7( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst , int divisor, @Nullable GrowArray<DogArray_I32> workspaces )
	{
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];
		final int k6 = kernel.data[5];
		final int k7 = kernel.data[6];

		final int radius = kernel.getRadius();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int halfDivisor = divisor/2;

		final int yEnd = imgHeight - radius;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(radius, yEnd, y -> {
		for (int y = radius; y < yEnd; y++) {
			int indexDst = dst.startIndex + y*dst.stride;
			int i = src.startIndex + (y - radius)*src.stride;
			final int iEnd = i + imgWidth;

			for (; i < iEnd; i++) {
				int indexSrc = i;

				int total = (dataSrc[indexSrc]) * k1;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k2;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k3;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k4;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k5;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k6;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k7;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical9( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst , int divisor, @Nullable GrowArray<DogArray_I32> workspaces )
	{
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];
		final int k6 = kernel.data[5];
		final int k7 = kernel.data[6];
		final int k8 = kernel.data[7];
		final int k9 = kernel.data[8];

		final int radius = kernel.getRadius();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int halfDivisor = divisor/2;

		final int yEnd = imgHeight - radius;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(radius, yEnd, y -> {
		for (int y = radius; y < yEnd; y++) {
			int indexDst = dst.startIndex + y*dst.stride;
			int i = src.startIndex + (y - radius)*src.stride;
			final int iEnd = i + imgWidth;

			for (; i < iEnd; i++) {
				int indexSrc = i;

				int total = (dataSrc[indexSrc]) * k1;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k2;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k3;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k4;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k5;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k6;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k7;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k8;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k9;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical11( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst , int divisor, @Nullable GrowArray<DogArray_I32> workspaces )
	{
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int k1 = kernel.data[0];
		final int k2 = kernel.data[1];
		final int k3 = kernel.data[2];
		final int k4 = kernel.data[3];
		final int k5 = kernel.data[4];
		final int k6 = kernel.data[5];
		final int k7 = kernel.data[6];
		final int k8 = kernel.data[7];
		final int k9 = kernel.data[8];
		final int k10 = kernel.data[9];
		final int k11 = kernel.data[10];

		final int radius = kernel.getRadius();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int halfDivisor = divisor/2;

		final int yEnd = imgHeight - radius;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(radius, yEnd, y -> {
		for (int y = radius; y < yEnd; y++) {
			int indexDst = dst.startIndex + y*dst.stride;
			int i = src.startIndex + (y - radius)*src.stride;
			final int iEnd = i + imgWidth;

			for (; i < iEnd; i++) {
				int indexSrc = i;

				int total = (dataSrc[indexSrc]) * k1;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k2;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k3;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k4;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k5;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k6;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k7;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k8;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k9;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k10;
				indexSrc += src.stride;
				total += (dataSrc[indexSrc])*k11;

				dataDst[indexDst++] = ( short )((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve3( Kernel2D_S32 kernel, GrayS16 src, GrayI16 dest , int divisor , @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		final int kernelRadius = kernel.getRadius();
		final int kernelWidth = 2*kernelRadius+1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(kernelRadius, height-kernelRadius, kernelWidth, workspaces, (work, y0, y1)->{
		final int y0 = kernelRadius, y1 = height-kernelRadius;
		int totalRow[] = BoofMiscOps.checkDeclare(work,src.width,false);
		for( int y = y0; y < y1; y++ ) {

			// first time through the value needs to be set
			int k1 = kernel.data[0];
			int k2 = kernel.data[1];
			int k3 = kernel.data[2];
			int indexSrcRow = src.startIndex+(y-kernelRadius)*src.stride-kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				total += (dataSrc[indexSrc++] )* k1;
				total += (dataSrc[indexSrc++] )* k2;
				total += (dataSrc[indexSrc] )* k3;

				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < 3; i++ ) {
				indexSrcRow = src.startIndex+(y+i-kernelRadius)*src.stride-kernelRadius;
				
				k1 = kernel.data[i*3 + 0];
				k2 = kernel.data[i*3 + 1];
				k3 = kernel.data[i*3 + 2];

				for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					total += (dataSrc[indexSrc++] )* k1;
					total += (dataSrc[indexSrc++] )* k2;
					total += (dataSrc[indexSrc] )* k3;

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				dataDst[indexDst++] = ( short )((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve5( Kernel2D_S32 kernel, GrayS16 src, GrayI16 dest , int divisor , @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		final int kernelRadius = kernel.getRadius();
		final int kernelWidth = 2*kernelRadius+1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(kernelRadius, height-kernelRadius, kernelWidth, workspaces, (work, y0, y1)->{
		final int y0 = kernelRadius, y1 = height-kernelRadius;
		int totalRow[] = BoofMiscOps.checkDeclare(work,src.width,false);
		for( int y = y0; y < y1; y++ ) {

			// first time through the value needs to be set
			int k1 = kernel.data[0];
			int k2 = kernel.data[1];
			int k3 = kernel.data[2];
			int k4 = kernel.data[3];
			int k5 = kernel.data[4];
			int indexSrcRow = src.startIndex+(y-kernelRadius)*src.stride-kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				total += (dataSrc[indexSrc++] )* k1;
				total += (dataSrc[indexSrc++] )* k2;
				total += (dataSrc[indexSrc++] )* k3;
				total += (dataSrc[indexSrc++] )* k4;
				total += (dataSrc[indexSrc] )* k5;

				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < 5; i++ ) {
				indexSrcRow = src.startIndex+(y+i-kernelRadius)*src.stride-kernelRadius;
				
				k1 = kernel.data[i*5 + 0];
				k2 = kernel.data[i*5 + 1];
				k3 = kernel.data[i*5 + 2];
				k4 = kernel.data[i*5 + 3];
				k5 = kernel.data[i*5 + 4];

				for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					total += (dataSrc[indexSrc++] )* k1;
					total += (dataSrc[indexSrc++] )* k2;
					total += (dataSrc[indexSrc++] )* k3;
					total += (dataSrc[indexSrc++] )* k4;
					total += (dataSrc[indexSrc] )* k5;

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				dataDst[indexDst++] = ( short )((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve7( Kernel2D_S32 kernel, GrayS16 src, GrayI16 dest , int divisor , @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		final int kernelRadius = kernel.getRadius();
		final int kernelWidth = 2*kernelRadius+1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(kernelRadius, height-kernelRadius, kernelWidth, workspaces, (work, y0, y1)->{
		final int y0 = kernelRadius, y1 = height-kernelRadius;
		int totalRow[] = BoofMiscOps.checkDeclare(work,src.width,false);
		for( int y = y0; y < y1; y++ ) {

			// first time through the value needs to be set
			int k1 = kernel.data[0];
			int k2 = kernel.data[1];
			int k3 = kernel.data[2];
			int k4 = kernel.data[3];
			int k5 = kernel.data[4];
			int k6 = kernel.data[5];
			int k7 = kernel.data[6];
			int indexSrcRow = src.startIndex+(y-kernelRadius)*src.stride-kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				total += (dataSrc[indexSrc++] )* k1;
				total += (dataSrc[indexSrc++] )* k2;
				total += (dataSrc[indexSrc++] )* k3;
				total += (dataSrc[indexSrc++] )* k4;
				total += (dataSrc[indexSrc++] )* k5;
				total += (dataSrc[indexSrc++] )* k6;
				total += (dataSrc[indexSrc] )* k7;

				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < 7; i++ ) {
				indexSrcRow = src.startIndex+(y+i-kernelRadius)*src.stride-kernelRadius;
				
				k1 = kernel.data[i*7 + 0];
				k2 = kernel.data[i*7 + 1];
				k3 = kernel.data[i*7 + 2];
				k4 = kernel.data[i*7 + 3];
				k5 = kernel.data[i*7 + 4];
				k6 = kernel.data[i*7 + 5];
				k7 = kernel.data[i*7 + 6];

				for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					total += (dataSrc[indexSrc++] )* k1;
					total += (dataSrc[indexSrc++] )* k2;
					total += (dataSrc[indexSrc++] )* k3;
					total += (dataSrc[indexSrc++] )* k4;
					total += (dataSrc[indexSrc++] )* k5;
					total += (dataSrc[indexSrc++] )* k6;
					total += (dataSrc[indexSrc] )* k7;

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				dataDst[indexDst++] = ( short )((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve9( Kernel2D_S32 kernel, GrayS16 src, GrayI16 dest , int divisor , @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		final int kernelRadius = kernel.getRadius();
		final int kernelWidth = 2*kernelRadius+1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(kernelRadius, height-kernelRadius, kernelWidth, workspaces, (work, y0, y1)->{
		final int y0 = kernelRadius, y1 = height-kernelRadius;
		int totalRow[] = BoofMiscOps.checkDeclare(work,src.width,false);
		for( int y = y0; y < y1; y++ ) {

			// first time through the value needs to be set
			int k1 = kernel.data[0];
			int k2 = kernel.data[1];
			int k3 = kernel.data[2];
			int k4 = kernel.data[3];
			int k5 = kernel.data[4];
			int k6 = kernel.data[5];
			int k7 = kernel.data[6];
			int k8 = kernel.data[7];
			int k9 = kernel.data[8];
			int indexSrcRow = src.startIndex+(y-kernelRadius)*src.stride-kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				total += (dataSrc[indexSrc++] )* k1;
				total += (dataSrc[indexSrc++] )* k2;
				total += (dataSrc[indexSrc++] )* k3;
				total += (dataSrc[indexSrc++] )* k4;
				total += (dataSrc[indexSrc++] )* k5;
				total += (dataSrc[indexSrc++] )* k6;
				total += (dataSrc[indexSrc++] )* k7;
				total += (dataSrc[indexSrc++] )* k8;
				total += (dataSrc[indexSrc] )* k9;

				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < 9; i++ ) {
				indexSrcRow = src.startIndex+(y+i-kernelRadius)*src.stride-kernelRadius;
				
				k1 = kernel.data[i*9 + 0];
				k2 = kernel.data[i*9 + 1];
				k3 = kernel.data[i*9 + 2];
				k4 = kernel.data[i*9 + 3];
				k5 = kernel.data[i*9 + 4];
				k6 = kernel.data[i*9 + 5];
				k7 = kernel.data[i*9 + 6];
				k8 = kernel.data[i*9 + 7];
				k9 = kernel.data[i*9 + 8];

				for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					total += (dataSrc[indexSrc++] )* k1;
					total += (dataSrc[indexSrc++] )* k2;
					total += (dataSrc[indexSrc++] )* k3;
					total += (dataSrc[indexSrc++] )* k4;
					total += (dataSrc[indexSrc++] )* k5;
					total += (dataSrc[indexSrc++] )* k6;
					total += (dataSrc[indexSrc++] )* k7;
					total += (dataSrc[indexSrc++] )* k8;
					total += (dataSrc[indexSrc] )* k9;

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				dataDst[indexDst++] = ( short )((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve11( Kernel2D_S32 kernel, GrayS16 src, GrayI16 dest , int divisor , @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		final int kernelRadius = kernel.getRadius();
		final int kernelWidth = 2*kernelRadius+1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(kernelRadius, height-kernelRadius, kernelWidth, workspaces, (work, y0, y1)->{
		final int y0 = kernelRadius, y1 = height-kernelRadius;
		int totalRow[] = BoofMiscOps.checkDeclare(work,src.width,false);
		for( int y = y0; y < y1; y++ ) {

			// first time through the value needs to be set
			int k1 = kernel.data[0];
			int k2 = kernel.data[1];
			int k3 = kernel.data[2];
			int k4 = kernel.data[3];
			int k5 = kernel.data[4];
			int k6 = kernel.data[5];
			int k7 = kernel.data[6];
			int k8 = kernel.data[7];
			int k9 = kernel.data[8];
			int k10 = kernel.data[9];
			int k11 = kernel.data[10];
			int indexSrcRow = src.startIndex+(y-kernelRadius)*src.stride-kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				total += (dataSrc[indexSrc++] )* k1;
				total += (dataSrc[indexSrc++] )* k2;
				total += (dataSrc[indexSrc++] )* k3;
				total += (dataSrc[indexSrc++] )* k4;
				total += (dataSrc[indexSrc++] )* k5;
				total += (dataSrc[indexSrc++] )* k6;
				total += (dataSrc[indexSrc++] )* k7;
				total += (dataSrc[indexSrc++] )* k8;
				total += (dataSrc[indexSrc++] )* k9;
				total += (dataSrc[indexSrc++] )* k10;
				total += (dataSrc[indexSrc] )* k11;

				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < 11; i++ ) {
				indexSrcRow = src.startIndex+(y+i-kernelRadius)*src.stride-kernelRadius;
				
				k1 = kernel.data[i*11 + 0];
				k2 = kernel.data[i*11 + 1];
				k3 = kernel.data[i*11 + 2];
				k4 = kernel.data[i*11 + 3];
				k5 = kernel.data[i*11 + 4];
				k6 = kernel.data[i*11 + 5];
				k7 = kernel.data[i*11 + 6];
				k8 = kernel.data[i*11 + 7];
				k9 = kernel.data[i*11 + 8];
				k10 = kernel.data[i*11 + 9];
				k11 = kernel.data[i*11 + 10];

				for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					total += (dataSrc[indexSrc++] )* k1;
					total += (dataSrc[indexSrc++] )* k2;
					total += (dataSrc[indexSrc++] )* k3;
					total += (dataSrc[indexSrc++] )* k4;
					total += (dataSrc[indexSrc++] )* k5;
					total += (dataSrc[indexSrc++] )* k6;
					total += (dataSrc[indexSrc++] )* k7;
					total += (dataSrc[indexSrc++] )* k8;
					total += (dataSrc[indexSrc++] )* k9;
					total += (dataSrc[indexSrc++] )* k10;
					total += (dataSrc[indexSrc] )* k11;

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				dataDst[indexDst++] = ( short )((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		//CONCURRENT_INLINE });
	}

}
