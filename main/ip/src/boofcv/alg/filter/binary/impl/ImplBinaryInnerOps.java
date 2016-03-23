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

package boofcv.alg.filter.binary.impl;

import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Optimized binary operations for the interior of an image only.  Allows less bounds checking
 * </p>
 * <p>
 * DESIGN NOTE: Minimizing logical operations seems to boost performance significantly.  Even if this means increasing
 * the number of array accesses.  So always summing the neighbors is faster than checking one and then summing
 * </p>
 *
 * @author Peter Abeles
 * @see boofcv.alg.filter.binary.BinaryImageOps
 */
public class ImplBinaryInnerOps {

	public static void erode4(GrayU8 input, GrayU8 output) {

		final int h = input.height - 1;
		final int w = input.width - 2;

		for (int y = 1; y < h; y++) {
			int indexIn = input.startIndex + y * input.stride + 1;
			int indexOut = output.startIndex + y * output.stride + 1;

			final int end = indexIn + w;

			for (; indexIn < end; indexIn++, indexOut++) {
				if ((input.data[indexIn] + input.data[indexIn - 1] + input.data[indexIn + 1] +
						input.data[indexIn - input.stride] + input.data[indexIn + input.stride]) == 5)
					output.data[indexOut] = 1;
				else
					output.data[indexOut] = 0;
			}
		}
	}

	public static void dilate4(GrayU8 input, GrayU8 output) {

		final int h = input.height - 1;
		final int w = input.width - 2;

		for (int y = 1; y < h; y++) {
			int indexIn = input.startIndex + y * input.stride + 1;
			int indexOut = output.startIndex + y * output.stride + 1;

			final int end = indexIn + w;

			for (; indexIn < end; indexIn++, indexOut++) {
				if ((input.data[indexIn] +
						input.data[indexIn - 1] + input.data[indexIn + 1] +
						input.data[indexIn - input.stride] + input.data[indexIn + input.stride]) > 0)
					output.data[indexOut] = 1;
				else
					output.data[indexOut] = 0;
			}
		}
	}

	public static void edge4(GrayU8 input, GrayU8 output) {

		final int h = input.height - 1;
		final int w = input.width - 2;

		for (int y = 1; y < h; y++) {
			int indexIn = input.startIndex + y * input.stride + 1;
			int indexOut = output.startIndex + y * output.stride + 1;

			final int end = indexIn + w;

			for (; indexIn < end; indexIn++, indexOut++) {
				if ((input.data[indexIn - 1] + input.data[indexIn + 1] +
						input.data[indexIn - input.stride] + input.data[indexIn + input.stride]) == 4)
					output.data[indexOut] = 0;
				else
					output.data[indexOut] = input.data[indexIn];
			}
		}
	}

	public static void erode8(GrayU8 input, GrayU8 output) {

		final int h = input.height - 1;
		final int w = input.width - 2;

		for (int y = 1; y < h; y++) {
			int indexIn = input.startIndex + y * input.stride + 1;
			int indexOut = output.startIndex + y * output.stride + 1;

			final int end = indexIn + w;

			for (; indexIn < end; indexIn++, indexOut++) {
				if ((input.data[indexIn] + input.data[indexIn - 1] + input.data[indexIn + 1] +
						input.data[indexIn + input.stride - 1] + input.data[indexIn + input.stride] + input.data[indexIn + input.stride + 1] +
						input.data[indexIn - input.stride - 1] + input.data[indexIn - input.stride] + input.data[indexIn - input.stride + 1]) == 9)
					output.data[indexOut] = 1;
				else
					output.data[indexOut] = 0;
			}
		}
	}

	public static void dilate8(GrayU8 input, GrayU8 output) {

		final int h = input.height - 1;
		final int w = input.width - 2;

		for (int y = 1; y < h; y++) {
			int indexIn = input.startIndex + y * input.stride + 1;
			int indexOut = output.startIndex + y * output.stride + 1;

			final int end = indexIn + w;

			for (; indexIn < end; indexIn++, indexOut++) {
				if ((input.data[indexIn] + input.data[indexIn - 1] + input.data[indexIn + 1] +
						input.data[indexIn + input.stride - 1] + input.data[indexIn + input.stride] + input.data[indexIn + input.stride + 1] +
						input.data[indexIn - input.stride - 1] + input.data[indexIn - input.stride] + input.data[indexIn - input.stride + 1]) > 0)
					output.data[indexOut] = 1;
				else
					output.data[indexOut] = 0;
			}
		}
	}

	public static void edge8(GrayU8 input, GrayU8 output) {

		final int h = input.height - 1;
		final int w = input.width - 2;

		for (int y = 1; y < h; y++) {
			int indexIn = input.startIndex + y * input.stride + 1;
			int indexOut = output.startIndex + y * output.stride + 1;

			final int end = indexIn + w;

			for (; indexIn < end; indexIn++, indexOut++) {
				if ((input.data[indexIn - 1] + input.data[indexIn + 1] +
						input.data[indexIn + input.stride - 1] + input.data[indexIn + input.stride] + input.data[indexIn + input.stride + 1] +
						input.data[indexIn - input.stride - 1] + input.data[indexIn - input.stride] + input.data[indexIn - input.stride + 1]) == 8)
					output.data[indexOut] = 0;
				else
					output.data[indexOut] = input.data[indexIn];
			}
		}
	}

	public static void removePointNoise(GrayU8 input, GrayU8 output) {

		final int h = input.height - 1;
		final int w = input.width - 2;

		for (int y = 1; y < h; y++) {
			int indexIn = input.startIndex + y * input.stride + 1;
			int indexOut = output.startIndex + y * output.stride + 1;

			final int end = indexIn + w;

			for (; indexIn < end; indexIn++, indexOut++) {
				int total = input.data[indexIn - 1] + input.data[indexIn + 1] +
						input.data[indexIn + input.stride - 1] + input.data[indexIn + input.stride] + input.data[indexIn + input.stride + 1] +
						input.data[indexIn - input.stride - 1] + input.data[indexIn - input.stride] + input.data[indexIn - input.stride + 1];
				if (total < 2)
					output.data[indexOut] = 0;
				else if (total > 6)
					output.data[indexOut] = 1;
				else
					output.data[indexOut] = input.data[indexIn];
			}
		}
	}
}
