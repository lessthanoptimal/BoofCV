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

package gecv.alg.drawing.impl;

import gecv.struct.image.ImageFloat32;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class ImageInitialization_F32 {

	/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param img   An image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill(ImageFloat32 img, float value) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		float[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = value;
			}
		}
	}

	/**
	 * Fills the whole image with random values
	 *
	 * @param img  An image.
	 * @param rand The value that the image is being filled with.
	 */
	public static void randomize(ImageFloat32 img, Random rand,
								 float min, float max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		float[] data = img.data;
		float range = max - min;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = rand.nextFloat() * range + min;
			}
		}
	}

	/**
	 * Sets a rectangle inside the image with the specified value.
	 */
	public static void fillRectangle(ImageFloat32 img, float value, int x0, int y0, int width, int height) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if( img.isInBounds(x,y ))
					img.set(x, y, value);
			}
		}
	}

	public static void print(ImageFloat32 a) {
		for (int y = 0; y < a.height; y++) {
			for (int x = 0; x < a.width; x++) {
				System.out.printf("%5.2f ", a.get(x, y));
			}
			System.out.println();
		}
	}
}
