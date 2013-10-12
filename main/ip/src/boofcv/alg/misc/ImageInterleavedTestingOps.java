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

package boofcv.alg.misc;

import boofcv.struct.image.InterleavedI8;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class ImageInterleavedTestingOps {
	/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param img   An image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill(InterleavedI8 img, byte... value) {
		if (value.length != img.numBands)
			throw new IllegalArgumentException("Unexpected number of bands");

		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				for (int k = 0; k < img.numBands; k++)
					data[index++] = value[k];
			}
		}
	}

	/**
	 * Fills the whole image with random values
	 *
	 * @param img  An image.
	 * @param rand The value that the image is being filled with.
	 */
	public static void randomize(InterleavedI8 img, Random rand) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;
		int range = Byte.MAX_VALUE - Byte.MIN_VALUE;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				for (int k = 0; k < img.numBands; k++)
					data[index++] = (byte) (rand.nextInt(range) + Byte.MIN_VALUE);
			}
		}
	}
}
