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
								final int a, final int b,
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

			for (; indexSrc < endX; indexSrc++) {
				int v = ((data[indexSrc + strideSrc + 1] & 0xFF) - (data[indexSrc - strideSrc - 1] & 0xFF))*a;
				int w = ((data[indexSrc + strideSrc - 1] & 0xFF) - (data[indexSrc - strideSrc + 1] & 0xFF))*a;

				imgY[indexY++] = (short)(((data[indexSrc + strideSrc] & 0xFF) - (data[indexSrc - strideSrc] & 0xFF))*b + v + w);

				imgX[indexX++] = (short)(((data[indexSrc + 1] & 0xFF) - (data[indexSrc - 1] & 0xFF))*b + v - w);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void process( GrayS16 src,
								final int a, final int b,
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

			for (; indexSrc < endX; indexSrc++) {
				int v = (data[indexSrc + strideSrc + 1] - data[indexSrc - strideSrc - 1])*a;
				int w = (data[indexSrc + strideSrc - 1] - data[indexSrc - strideSrc + 1])*a;

				imgY[indexY++] = (short)((data[indexSrc + strideSrc] - data[indexSrc - strideSrc])*b + v + w);

				imgX[indexX++] = (short)((data[indexSrc + 1] - data[indexSrc - 1])*b + v - w);
			}
		}
		//CONCURRENT_ABOVE });
	}

	/**
	 * Computes derivative of GrayF32. None of the images can be sub-images.
	 */
	public static void process( GrayF32 src,
								final float a, final float b,
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

			for (; indexSrc < endX; indexSrc++) {
				float v = (data[indexSrc + strideSrc + 1] - data[indexSrc - strideSrc - 1])*a;
				float w = (data[indexSrc + strideSrc - 1] - data[indexSrc - strideSrc + 1])*a;

				imgY[indexY++] = (data[indexSrc + strideSrc] - data[indexSrc - strideSrc])*b + v + w;

				imgX[indexX++] = (data[indexSrc + 1] - data[indexSrc - 1])*b + v - w;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
