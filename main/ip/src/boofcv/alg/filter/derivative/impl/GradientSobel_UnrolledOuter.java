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
 * This is a further improvement on {@link GradientSobel_Outer} where it reduces the number of times the array needs to be
 * read from by saving past reads in a local variable.  This required the loops to be partially unwound.  In
 * tests it runs about 25% faster than {@link GradientSobel_Outer}.
 * </p>
 *
 * @author Peter Abeles
 * @see boofcv.alg.filter.derivative.GradientSobel
 */
public class GradientSobel_UnrolledOuter {

	/**
	 * Can only process images which are NOT sub-images.
	 */
	public static void process_I8(GrayU8 orig,
								  GrayS16 derivX,
								  GrayS16 derivY) {
		final byte[] data = orig.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;

		final int adjWidth = width - 2;

		for (int y = 1; y < height; y++) {
			int endX_alt = width * y + (width - adjWidth % 3) - 1;
			int endX = endX_alt + adjWidth % 3;

			int a11, a12, a13;
			int a21, a22, a23;
			int a31, a32, a33;

			int index = width * y + 1;

			a11 = data[index - width - 1] & 0xFF;
			a12 = data[index - width] & 0xFF;
			a21 = data[index - 1] & 0xFF;
			a22 = data[index] & 0xFF;
			a31 = data[index + width - 1] & 0xFF;
			a32 = data[index + width] & 0xFF;

			for (; index < endX_alt;) {
				a13 = data[index - width + 1] & 0xFF;
				a23 = data[index + 1] & 0xFF;
				a33 = data[index + width + 1] & 0xFF;

				int v = (a33 - a11);
				int w = (a31 - a13);

				imgY[index] = (short) ((a32 - a12) * 2 + v + w);
				imgX[index] = (short) ((a23 - a21) * 2 + v - w);

				index++;

				a11 = data[index - width + 1] & 0xFF;
				a21 = data[index + 1] & 0xFF;
				a31 = data[index + width + 1] & 0xFF;

				v = (a31 - a12);
				w = (a32 - a11);

				imgY[index] = (short) ((a33 - a13) * 2 + v + w);
				imgX[index] = (short) ((a21 - a22) * 2 + v - w);

				index++;

				a12 = data[index - width + 1] & 0xFF;
				a22 = data[index + 1] & 0xFF;
				a32 = data[index + width + 1] & 0xFF;

				v = (a32 - a13);
				w = (a33 - a12);

				imgY[index] = (short) ((a31 - a11) * 2 + v + w);
				imgX[index] = (short) ((a22 - a23) * 2 + v - w);

				index++;
			}

			for (; index < endX; index++) {
				int v = (data[index + width + 1] & 0xFF) - (data[index - width - 1] & 0xFF);
				int w = (data[index + width - 1] & 0xFF) - (data[index - width + 1] & 0xFF);

				imgY[index] = (short) (((data[index + width] & 0xFF) - (data[index - width] & 0xFF)) * 2 + v + w);

				imgX[index] = (short) (((data[index + 1] & 0xFF) - (data[index - 1] & 0xFF)) * 2 + v - w);
			}
		}
	}

	/**
	 * Can only process images which are NOT sub-images.
	 */
	public static void process_F32(GrayF32 orig,
								   GrayF32 derivX,
								   GrayF32 derivY) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;
		final float[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;

		final int adjWidth = width - 2;
		for (int y = 1; y < height; y++) {
			int endX_alt = width * y + (width - adjWidth % 3) - 1;
			int endX = endX_alt + adjWidth % 3;

			float a11, a12, a13;
			float a21, a22, a23;
			float a31, a32, a33;

			int index = width * y + 1;

			a11 = data[index - width - 1];
			a12 = data[index - width];
			a21 = data[index - 1];
			a22 = data[index];
			a31 = data[index + width - 1];
			a32 = data[index + width];


			for (; index < endX_alt;) {

				a13 = data[index - width + 1];
				a23 = data[index + 1];
				a33 = data[index + width + 1];

				float v = (a33 - a11) * 0.25F;
				float w = (a31 - a13) * 0.25F;

				imgY[index] = (a32 - a12) * 0.5F + v + w;
				imgX[index] = (a23 - a21) * 0.5F + v - w;

				index++;

				a11 = data[index - width + 1];
				a21 = data[index + 1];
				a31 = data[index + width + 1];

				v = (a31 - a12) * 0.25F;
				w = (a32 - a11) * 0.25F;

				imgY[index] = (a33 - a13) * 0.5F + v + w;
				imgX[index] = (a21 - a22) * 0.5F + v - w;

				index++;

				a12 = data[index - width + 1];
				a22 = data[index + 1];
				a32 = data[index + width + 1];

				v = (a32 - a13) * 0.25F;
				w = (a33 - a12) * 0.25F;

				imgY[index] = (a31 - a11) * 0.5F + v + w;
				imgX[index] = (a22 - a23) * 0.5F + v - w;

				index++;
			}

			// handle the remaining
			for (; index < endX; index++) {
				float v = (data[index + width + 1] - data[index - width - 1]) * 0.25F;
				float w = (data[index + width - 1] - data[index - width + 1]) * 0.25F;

				imgY[index] = (data[index + width] - data[index - width]) * 0.5F + v + w;
				imgX[index] = (data[index + 1] - data[index - 1]) * 0.5F + v - w;
			}
		}
	}

	/**
	 * Can process any but regular and sub-images.
	 */
	public static void process_F32_sub(GrayF32 orig,
									   GrayF32 derivX,
									   GrayF32 derivY) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;
		final float[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int strideSrc = orig.stride;

		final int adjWidth = width - 2;
		for (int y = 1; y < height; y++) {
			int index = orig.startIndex + strideSrc * y + 1;
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;

			int endX_alt = index + (width - adjWidth % 3) - 2;
			int endX = endX_alt + adjWidth % 3;

			float a11, a12, a13;
			float a21, a22, a23;
			float a31, a32, a33;


			a11 = data[index - strideSrc - 1];
			a12 = data[index - strideSrc];
			a21 = data[index - 1];
			a22 = data[index];
			a31 = data[index + strideSrc - 1];
			a32 = data[index + strideSrc];


			for (; index < endX_alt;) {

				a13 = data[index - strideSrc + 1];
				a23 = data[index + 1];
				a33 = data[index + strideSrc + 1];

				float v = (a33 - a11) * 0.25F;
				float w = (a31 - a13) * 0.25F;

				imgY[indexY++] = (a32 - a12) * 0.5F + v + w;
				imgX[indexX++] = (a23 - a21) * 0.5F + v - w;

				index++;

				a11 = data[index - strideSrc + 1];
				a21 = data[index + 1];
				a31 = data[index + strideSrc + 1];

				v = (a31 - a12) * 0.25F;
				w = (a32 - a11) * 0.25F;

				imgY[indexY++] = (a33 - a13) * 0.5F + v + w;
				imgX[indexX++] = (a21 - a22) * 0.5F + v - w;

				index++;

				a12 = data[index - strideSrc + 1];
				a22 = data[index + 1];
				a32 = data[index + strideSrc + 1];

				v = (a32 - a13) * 0.25F;
				w = (a33 - a12) * 0.25F;

				imgY[indexY++] = (a31 - a11) * 0.5F + v + w;
				imgX[indexX++] = (a22 - a23) * 0.5F + v - w;

				index++;


			}

			// handle the remaining
			for (; index < endX; index++) {
				float v = (data[index + strideSrc + 1] - data[index - strideSrc - 1]) * 0.25F;
				float w = (data[index + strideSrc - 1] - data[index - strideSrc + 1]) * 0.25F;

				imgY[indexY++] = (data[index + strideSrc] - data[index - strideSrc]) * 0.5F + v + w;
				imgX[indexX++] = (data[index + 1] - data[index - 1]) * 0.5F + v - w;
			}
		}
	}
}
