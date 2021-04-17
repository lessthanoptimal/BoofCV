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

package boofcv.alg.filter.derivative.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>Generalized code for family of Gradient operators that have the kernels [-1 0 1]**[a b a]</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class GradientFamilyAB {
	/**
	 * Computes derivative of GrayU8. Inputs can be sub-images.
	 */
	public static void process( GrayU8 src,
								final int kerA, final int kerB,
								GrayS16 derivX, GrayS16 derivY ) {
		final byte[] data = src.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = src.getWidth();
		final int height = src.getHeight() - 1;
		final int strideSrc = src.getStride();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{
		for (int y = 1; y < height; y++) {
			int indexSrc = src.startIndex + src.stride*y + 1;
			final int endX = indexSrc + width - 2;

			int indexX = derivX.startIndex + derivX.stride*y + 1;
			int indexY = derivY.startIndex + derivY.stride*y + 1;

			int a11, a12, a21, a22, a31, a32;

			a11 = data[indexSrc - strideSrc - 1] & 0xFF;
			a12 = data[indexSrc - strideSrc] & 0xFF;
			a21 = data[indexSrc - 1] & 0xFF;
			a22 = data[indexSrc] & 0xFF;
			a31 = data[indexSrc + strideSrc - 1] & 0xFF;
			a32 = data[indexSrc + strideSrc] & 0xFF;

			for (; indexSrc < endX; indexSrc++) {
				int a13 = data[indexSrc - strideSrc + 1] & 0xFF;
				int a23 = data[indexSrc + 1] & 0xFF;
				int a33 = data[indexSrc + strideSrc + 1] & 0xFF;

				int v = (a33 - a11)*kerA;
				int w = (a31 - a13)*kerA;

				imgY[indexY++] = (short)((a32 - a12)*kerB + v + w);
				imgX[indexX++] = (short)((a23 - a21)*kerB + v - w);

				a11 = a12;
				a12 = a13;
				a21 = a22;
				a22 = a23;
				a31 = a32;
				a32 = a33;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void process( GrayS16 src,
								final int kerA, final int kerB,
								GrayS16 derivX,
								GrayS16 derivY ) {
		final short[] data = src.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = src.getWidth();
		final int height = src.getHeight() - 1;
		final int strideSrc = src.getStride();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{
		for (int y = 1; y < height; y++) {
			int indexSrc = src.startIndex + src.stride*y + 1;
			final int endX = indexSrc + width - 2;

			int indexX = derivX.startIndex + derivX.stride*y + 1;
			int indexY = derivY.startIndex + derivY.stride*y + 1;

			int a11, a12, a21, a22, a31, a32;

			a11 = data[indexSrc - strideSrc - 1];
			a12 = data[indexSrc - strideSrc];
			a21 = data[indexSrc - 1];
			a22 = data[indexSrc];
			a31 = data[indexSrc + strideSrc - 1];
			a32 = data[indexSrc + strideSrc];

			for (; indexSrc < endX; indexSrc++) {
				int a13 = data[indexSrc - strideSrc + 1];
				int a23 = data[indexSrc + 1];
				int a33 = data[indexSrc + strideSrc + 1];

				int v = (a33 - a11)*kerA;
				int w = (a31 - a13)*kerA;

				imgY[indexY++] = (short)((a32 - a12)*kerB + v + w);
				imgX[indexX++] = (short)((a23 - a21)*kerB + v - w);

				a11 = a12;
				a12 = a13;
				a21 = a22;
				a22 = a23;
				a31 = a32;
				a32 = a33;
			}
		}
		//CONCURRENT_ABOVE });
	}

	/**
	 * Computes derivative of GrayF32. None of the images can be sub-images.
	 */
	public static void process( GrayF32 src,
								final float kerA, final float kerB,
								GrayF32 derivX,
								GrayF32 derivY ) {
		final float[] data = src.data;
		final float[] imgX = derivX.data;
		final float[] imgY = derivY.data;

		final int width = src.getWidth();
		final int height = src.getHeight() - 1;
		final int strideSrc = src.getStride();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{
		for (int y = 1; y < height; y++) {
			int indexSrc = src.startIndex + src.stride*y + 1;
			final int endX = indexSrc + width - 2;

			int indexX = derivX.startIndex + derivX.stride*y + 1;
			int indexY = derivY.startIndex + derivY.stride*y + 1;

			float a11, a12, a21, a22, a31, a32;

			a11 = data[indexSrc - strideSrc - 1];
			a12 = data[indexSrc - strideSrc];
			a21 = data[indexSrc - 1];
			a22 = data[indexSrc];
			a31 = data[indexSrc + strideSrc - 1];
			a32 = data[indexSrc + strideSrc];

			for (; indexSrc < endX; indexSrc++) {
				float a13 = data[indexSrc - strideSrc + 1];
				float a23 = data[indexSrc + 1];
				float a33 = data[indexSrc + strideSrc + 1];

				float v = (a33 - a11)*kerA;
				float w = (a31 - a13)*kerA;

				imgY[indexY++] = (a32 - a12)*kerB + v + w;
				imgX[indexX++] = (a23 - a21)*kerB + v - w;

				a11 = a12;
				a12 = a13;
				a21 = a22;
				a22 = a23;
				a31 = a32;
				a32 = a33;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
