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

import gecv.struct.image.ImageInt8;
import gecv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * Basic drawing operations for {@link gecv.struct.image.ImageUInt8}.
 *
 * @see gecv.alg.drawing.BasicDrawing
 *
 * @author Peter Abeles
 */
public class BasicDrawing_I8 {

	public static void addNoise(ImageUInt8 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = 1+max-min;

		byte[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				int value = (data[index] & 0xFF) + rand.nextInt(range)+min;
				if( value < 0 ) value = 0;
				if( value > 255 ) value = 255;

				data[index++] = (byte) value;
			}
		}
	}

	public static void fill(ImageUInt8 img, int value) {
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

	public static void rectangle( ImageUInt8 img , int value , int x0 , int y0 , int x1 , int y1 ) {
		for( int y = y0; y < y1; y++ ) {
			for( int x = x0; x < x1; x++ ) {
				img.set(x,y,value);
			}
		}
	}

	/**
	 * Fills the whole image with random values
	 *
	 * @param img  An image.
	 * @param rand The value that the image is being filled with.
	 */
	public static void randomize(ImageUInt8 img, Random rand) {
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
