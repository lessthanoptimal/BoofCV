/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.core.image;

import gecv.struct.image.ImageInt8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class UtilImageInt8 {

	/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param img   An image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill(ImageInt8 img, int value) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (byte) value;
			}
		}
	}

	/**
	 * Fills the whole image with random values
	 *
	 * @param img  An image.
	 * @param rand The value that the image is being filled with.
	 */
	public static void randomize(ImageInt8 img, Random rand) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;
		int range = Byte.MAX_VALUE - Byte.MIN_VALUE;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (byte) (rand.nextInt(range) + Byte.MIN_VALUE);
			}
		}
	}

	public static void randomize(ImageInt8 img, Random rand, int min, int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		byte[] data = img.data;
		int range = 1 + max - min;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (byte) (rand.nextInt(range) + min);
			}
		}
	}
}
