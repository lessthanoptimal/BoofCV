/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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


/**
 * <p>
 * While not as fast as {@link boofcv.alg.filter.derivative.GradientSobel} it a big improvement over {@link GradientSobel_Naive} and much
 * more readable.  The general idea is that the outer diagonal elements are both read by the horizontal
 * and vertical operations.  This can be taken advantage of with some simple arithmetic to reduce the number
 * of floating point operations and matrix reads and writes.
 * </p>
 * <p>
 * This code is being saved to avoid repeating past work and make it easier to understand other implementations.
 * </p>
 *
 * @author Peter Abeles
 * @see boofcv.alg.filter.derivative.GradientSobel
 */
public class GradientSobel_Outer {

	/**
	 * Computes derivative of GrayU8.  None of the images can be sub-images.
	 */
	public static void process_I8(GrayU8 orig,
								  GrayS16 derivX,
								  GrayS16 derivY) {
		final byte[] data = orig.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;

		for (int y = 1; y < height; y++) {
			int endX = width * y + width - 1;

			for (int index = width * y + 1; index < endX; index++) {
				int v = (data[index + width + 1] & 0xFF) - (data[index - width - 1] & 0xFF);
				int w = (data[index + width - 1] & 0xFF) - (data[index - width + 1] & 0xFF);

				imgY[index] = (short) (((data[index + width] & 0xFF) - (data[index - width] & 0xFF)) * 2 + v + w);

				imgX[index] = (short) (((data[index + 1] & 0xFF) - (data[index - 1] & 0xFF)) * 2 + v - w);
			}
		}
	}

	/**
	 * Computes derivative of GrayU8.  Inputs can be sub-images.
	 */
	public static void process_I8_sub(GrayU8 orig,
									  GrayS16 derivX,
									  GrayS16 derivY) {
		final byte[] data = orig.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int strideSrc = orig.getStride();

		for (int y = 1; y < height; y++) {
			int indexSrc = orig.startIndex + orig.stride * y + 1;
			final int endX = indexSrc + width - 2;

			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;

			for (; indexSrc < endX; indexSrc++) {
				int v = (data[indexSrc + strideSrc + 1] & 0xFF) - (data[indexSrc - strideSrc - 1] & 0xFF);
				int w = (data[indexSrc + strideSrc - 1] & 0xFF) - (data[indexSrc - strideSrc + 1] & 0xFF);

				imgY[indexY++] = (short) (((data[indexSrc + strideSrc] & 0xFF) - (data[indexSrc - strideSrc] & 0xFF)) * 2 + v + w);

				imgX[indexX++] = (short) (((data[indexSrc + 1] & 0xFF) - (data[indexSrc - 1] & 0xFF)) * 2 + v - w);
			}
		}
	}

	public static void process_I8_sub(GrayS16 orig,
									  GrayS16 derivX,
									  GrayS16 derivY) {
		final short[] data = orig.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int strideSrc = orig.getStride();

		for (int y = 1; y < height; y++) {
			int indexSrc = orig.startIndex + orig.stride * y + 1;
			final int endX = indexSrc + width - 2;

			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;

			for (; indexSrc < endX; indexSrc++) {
				int v = (data[indexSrc + strideSrc + 1] ) - (data[indexSrc - strideSrc - 1] );
				int w = (data[indexSrc + strideSrc - 1] ) - (data[indexSrc - strideSrc + 1] );

				imgY[indexY++] = (short) (((data[indexSrc + strideSrc] ) - (data[indexSrc - strideSrc] )) * 2 + v + w);

				imgX[indexX++] = (short) (((data[indexSrc + 1] ) - (data[indexSrc - 1] )) * 2 + v - w);
			}
		}
	}

	/**
	 * Computes derivative of GrayF32.  None of the images can be sub-images.
	 */
	public static void process_F32(GrayF32 orig,
								   GrayF32 derivX,
								   GrayF32 derivY) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;
		final float[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;

		for (int y = 1; y < height; y++) {
			int endX = width * y + width - 1;

			for (int index = width * y + 1; index < endX; index++) {
				float v = (data[index + width + 1] - data[index - width - 1]) * 0.25F;
				float w = (data[index + width - 1] - data[index - width + 1]) * 0.25F;

				imgY[index] = (data[index + width] - data[index - width]) * 0.5F + v + w;
				imgX[index] = (data[index + 1] - data[index - 1]) * 0.5F + v - w;
			}
		}
	}
}