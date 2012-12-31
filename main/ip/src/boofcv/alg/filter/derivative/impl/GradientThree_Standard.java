/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;


/**
 * <p>
 * Basic implementation of {@link boofcv.alg.filter.derivative.GradientThree} with nothing fancy is done to improve its performance.
 * </p>
 *
 * @author Peter Abeles
 */
public class GradientThree_Standard {

	/**
	 * Computes the derivative along the x and y axes
	 */
	public static void process(ImageFloat32 orig, ImageFloat32 derivX, ImageFloat32 derivY) {
		final float[] data = orig.data;
		final float[] imgX = derivX.data;
		final float[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgX[indexX++] = (data[indexSrc + 1] - data[indexSrc - 1]) * 0.5f;
				imgY[indexY++] = (data[indexSrc + stride] - data[indexSrc - stride]) * 0.5f;
			}
		}
	}

	/**
	 * Computes the derivative along the x and y axes
	 */
	public static void process(ImageUInt8 orig, ImageSInt16 derivX, ImageSInt16 derivY) {
		final byte[] data = orig.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgX[indexX++] = (short) ((data[indexSrc + 1] & 0xFF) - (data[indexSrc - 1] & 0xFF));
				imgY[indexY++] = (short) ((data[indexSrc + stride] & 0xFF) - (data[indexSrc - stride] & 0xFF));
			}
		}
	}

	public static void process(ImageSInt16 orig, ImageSInt16 derivX, ImageSInt16 derivY) {
		final short[] data = orig.data;
		final short[] imgX = derivX.data;
		final short[] imgY = derivY.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int indexX = derivX.startIndex + derivX.stride * y + 1;
			int indexY = derivY.startIndex + derivY.stride * y + 1;
			int indexSrc = orig.startIndex + stride * y + 1;
			final int endX = indexSrc + width - 2;

			for (; indexSrc < endX; indexSrc++) {
				imgX[indexX++] = (short) (data[indexSrc + 1] - data[indexSrc - 1]);
				imgY[indexY++] = (short) (data[indexSrc + stride] - data[indexSrc - stride]);
			}
		}
	}

}