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
 * Basic implementation of {@link boofcv.alg.filter.derivative.HessianThree} with nothing fancy is done to improve its performance.
 * </p>
 *
 * @author Peter Abeles
 */
public class HessianThree_Standard {

	/**
	 * Computes the derivative along the x and y axes
	 */
	public static void process(GrayF32 orig,
							   GrayF32 derivXX,
							   GrayF32 derivYY,
							   GrayF32 derivXY) {
		final float[] data = orig.data;
		final float[] imgX = derivXX.data;
		final float[] imgY = derivYY.data;
		final float[] imgXY = derivXY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 2;
		final int stride = orig.stride;

		for (int y = 2; y < height; y++) {
			int indexX = derivXX.startIndex + derivXX.stride * y + 2;
			int indexY = derivYY.startIndex + derivYY.stride * y + 2;
			int indexXY = derivXY.startIndex + derivXY.stride * y + 2;

			int indexSrc = orig.startIndex + stride * y + 2;
			
			final int endX = indexSrc + width - 4;

			for (; indexSrc < endX; indexSrc++) {
				float center = 2*data[indexSrc];
				imgX[indexX++] = (data[indexSrc - 2] - center + data[indexSrc + 2]) * 0.5f;
				imgY[indexY++] = (data[indexSrc - 2*stride] - center + data[indexSrc + 2*stride]) * 0.5f;
				imgXY[indexXY++] = (data[indexSrc - stride -1] - data[indexSrc - stride + 1]
						- data[indexSrc + stride - 1] + data[indexSrc + stride + 1]) * 0.5f;
			}
		}
	}

	/**
	 * Computes the derivative along the x and y axes
	 */
	public static void process(GrayU8 orig,
							   GrayS16 derivXX,
							   GrayS16 derivYY,
							   GrayS16 derivXY) {
		final byte[] data = orig.data;
		final short[] imgX = derivXX.data;
		final short[] imgY = derivYY.data;
		final short[] imgXY = derivXY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 2;
		final int stride = orig.stride;

		for (int y = 2; y < height; y++) {
			int indexX = derivXX.startIndex + derivXX.stride * y + 2;
			int indexY = derivYY.startIndex + derivYY.stride * y + 2;
			int indexXY = derivXY.startIndex + derivXY.stride * y + 2;

			int indexSrc = orig.startIndex + stride * y + 2;

			final int endX = indexSrc + width - 4;

			for (; indexSrc < endX; indexSrc++) {
				int center = 2*(data[indexSrc] & 0xFF);
				imgX[indexX++] = (short)((data[indexSrc - 2] & 0xFF) - center + (data[indexSrc + 2] & 0xFF));
				imgY[indexY++] = (short)((data[indexSrc - 2*stride] & 0xFF) - center + (data[indexSrc + 2*stride] & 0xFF));
				imgXY[indexXY++] = (short)((data[indexSrc - stride -1] & 0xFF) - (data[indexSrc - stride + 1] & 0xFF)
						- (data[indexSrc + stride - 1] & 0xFF) + (data[indexSrc + stride + 1] & 0xFF));
			}
		}
	}
}