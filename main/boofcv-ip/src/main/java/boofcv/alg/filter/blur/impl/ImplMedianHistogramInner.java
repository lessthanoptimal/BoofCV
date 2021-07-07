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

package boofcv.alg.filter.blur.impl;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.DogArray_I32;
import pabeles.concurrency.GrowArray;

import java.util.Arrays;

/**
 * <p>
 * A faster version of the histogram median filter that only processes the inner portion of the image. Instead of
 * rebuilding the histogram from scratch for each pixel the histogram is updated using results from the previous pixel.
 * When computing the histogram the previous median is used as a hint to the new median. The original implementation
 * is similar to the algorithm proposed in [1]. If you need to cite something cite this library and mention that paper.
 * </p>
 *
 * <p>
 * [1] Huang, T.S., Yang, G.J. and Tang, G.Y. (1979) A fast two-dimensional median filtering algorithm. IEEE Trans.
 * Acoust. Speech Signal Process. 27, 13-18
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ImplMedianHistogramInner {

	/**
	 * Applies a median image filter.
	 *
	 * @param input Input image. Not modified.
	 * @param output Filtered output image. Modified.
	 * @param radiusX Size of the filter region. x-axis
	 * @param radiusY Size of the filter region. Y-axis
	 * @param work Creates local work space arrays
	 */
	public static void process( GrayU8 input, GrayU8 output, int radiusX, int radiusY, GrowArray<DogArray_I32> work ) {
		int w = 2*radiusX + 1;
		int h = 2*radiusY + 1;

		//CONCURRENT_REMOVE_BELOW
		DogArray_I32 array = work.grow();

		// sanity check to make sure the image isn't too small to be processed by this algorithm
		if (input.width < w || input.height < h)
			return;

		// defines what the median is. technically this is an approximation because if even it's the ave
		// of the two elements in the middle. I'm not aware of libraries which actually do this.
		int threshold = (w*h)/2 + 1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(radiusY, output.height-radiusY, h, work, (array,y0,y1)->{
		final int y0 = radiusY, y1 = input.height - radiusY;
		int[] histogram = BoofMiscOps.checkDeclare(array, 256, false);

		for (int y = y0; y < y1; y++) {
			int seed = input.startIndex + (y - radiusY)*input.stride;
			Arrays.fill(histogram, 0);

			// compute the median value for the first x component and initialize the system
			for (int yi = 0; yi < h; yi++) {
				int idx = seed + yi*input.stride;
				int end = idx + w;
				while (idx < end) {
					histogram[(input.data[idx++] & 0xFF)]++;
				}
			}

			// Compute the median value
			int count = 0, median = 0;
			while (true) {
				count += histogram[median];
				if (count >= threshold)
					break;
				median++;
			}
			output.data[output.startIndex + y*output.stride + radiusX] = (byte)median;

			// remove the left most pixel from the histogram
			count += removeSide(input.data, input.stride, h, histogram, seed, median);

			for (int x = radiusX + 1; x < input.width - radiusX; x++) {
				seed = input.startIndex + (y - radiusY)*input.stride + (x - radiusX);

				// add the right most pixels to the histogram
				count += addSide(input.data, input.stride, h, histogram, seed + w - 1, median);

				// find the median, using the previous solution as a starting point
				if (count >= threshold) {
					while (count >= threshold) {
						count -= histogram[median--];
					}
					median += 1;
					count += histogram[median];
				} else {
					while (count < threshold) {
						median += 1;
						count += histogram[median];
					}
				}
				output.data[output.startIndex + y*output.stride + x] = (byte)median;

				// remove the left most pixels from the histogram
				count += removeSide(input.data, input.stride, h, histogram, seed, median);
			}
		}
		//CONCURRENT_ABOVE }});
	}

	private static int removeSide( final byte[] data, final int stride, final int height, int[] histogram,
								   int seedIdx, int oldMedian ) {
		int count = 0;
		for (int i = 0; i < height; i++, seedIdx += stride) {
			int value = data[seedIdx] & 0xFF;
			histogram[value]--;
			if (value <= oldMedian) {
				count--;
			}
		}
		return count;
	}

	private static int addSide( final byte[] data, final int stride, final int height, int[] histogram,
								int seedIdx, int oldValue ) {
		int count = 0;
		for (int i = 0; i < height; i++, seedIdx += stride) {
			int value = data[seedIdx] & 0xFF;
			histogram[value]++;
			if (value <= oldValue) {
				count++;
			}
		}
		return count;
	}
}
