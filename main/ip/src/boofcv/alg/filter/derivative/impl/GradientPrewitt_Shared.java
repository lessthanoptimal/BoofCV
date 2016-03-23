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
 * Prewitt implementation that shares values for horizontal and vertical gradients
 *
 * @author Peter Abeles
 */
public class GradientPrewitt_Shared {
	public static void process(GrayU8 orig,
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
				// a33 - a11
				int w = (data[indexSrc + strideSrc + 1] & 0xFF) -(data[indexSrc - strideSrc - 1] & 0xFF);
				// a31 - a13
				int v = (data[indexSrc + strideSrc - 1] & 0xFF) -(data[indexSrc - strideSrc + 1] & 0xFF);

				//a32 + w + v - a12
				imgY[indexY++] = (short) ((data[indexSrc + strideSrc ] & 0xFF)+w+v-(data[indexSrc - strideSrc ] & 0xFF));

				//a23 + w - v - a21
				imgX[indexX++] = (short) ((data[indexSrc + 1] & 0xFF)+w-v-(data[indexSrc - 1] & 0xFF));
			}
		}
	}

	public static void process(GrayS16 orig,
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
				// a33 - a11
				int w = (data[indexSrc + strideSrc + 1] ) -(data[indexSrc - strideSrc - 1] );
				// a31 - a13
				int v = (data[indexSrc + strideSrc - 1] ) -(data[indexSrc - strideSrc + 1] );

				//a32 + w + v - a12
				imgY[indexY++] = (short) ((data[indexSrc + strideSrc ] )+w+v-(data[indexSrc - strideSrc ] ));

				//a23 + w - v - a21
				imgX[indexX++] = (short) ((data[indexSrc + 1] )+w-v-(data[indexSrc - 1] ));
			}
		}
	}

	public static void process(GrayF32 orig,
							   GrayF32 derivX,
							   GrayF32 derivY) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;
		final float[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int strideSrc = orig.getStride();

		for (int y = 1; y < height; y++) {
			int indexSrc = orig.startIndex + orig.stride * y + 1;
			final int endX = indexSrc + width - 2;

			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;

			for (; indexSrc < endX; indexSrc++) {
				// a33 - a11
				float w = data[indexSrc + strideSrc + 1] - data[indexSrc - strideSrc - 1];
				// a31 - a13
				float v = data[indexSrc + strideSrc - 1] - data[indexSrc - strideSrc + 1];

				//a32 + w + v - a12
				imgY[indexY++] = data[indexSrc + strideSrc ]+w+v-data[indexSrc - strideSrc ];

				//a23 + w - v - a21
				imgX[indexX++] = data[indexSrc + 1]+w-v-data[indexSrc - 1];
			}
		}
	}
}
