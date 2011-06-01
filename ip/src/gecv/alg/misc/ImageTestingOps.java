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

package gecv.alg.misc;

import gecv.struct.image.*;

import java.util.Random;


/**
 * Image operations which are primarily used for testing and evaluation.
 *
 * @author Peter Abeles
 */
public class ImageTestingOps {

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
				data[index++] = (byte)value;
			}
		}
	}

	/**
	 * Sets a rectangle inside the image with the specified value.
	 */
	public static void fillRectangle(ImageInt8 img, int value, int x0, int y0, int width, int height) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if( img.isInBounds(x,y ))
					img.set(x, y, value);
			}
		}
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void randomize(ImageInt8 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

		byte[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (byte)(rand.nextInt(range)+min);
			}
		}
	}

/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param img   An image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill(ImageInt16 img, int value) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		short[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (short)value;
			}
		}
	}

	/**
	 * Sets a rectangle inside the image with the specified value.
	 */
	public static void fillRectangle(ImageInt16 img, int value, int x0, int y0, int width, int height) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if( img.isInBounds(x,y ))
					img.set(x, y, value);
			}
		}
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void randomize(ImageInt16 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

		short[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (short)(rand.nextInt(range)+min);
			}
		}
	}

/**
	 * Fills the whole image with the specified pixel value
	 *
	 * @param img   An image.
	 * @param value The value that the image is being filled with.
	 */
	public static void fill(ImageSInt32 img, int value) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = value;
			}
		}
	}

	/**
	 * Sets a rectangle inside the image with the specified value.
	 */
	public static void fillRectangle(ImageSInt32 img, int value, int x0, int y0, int width, int height) {
		int x1 = x0 + width;
		int y1 = y0 + height;

		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				if( img.isInBounds(x,y ))
					img.set(x, y, value);
			}
		}
	}

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void randomize(ImageSInt32 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

		int[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = (rand.nextInt(range)+min);
			}
		}
	}

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

	/**
	 * Sets each value in the image to a value drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void randomize(ImageFloat32 img, Random rand , float min , float max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		float range = max-min;

		float[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] = rand.nextFloat()*range+min;
			}
		}
	}

	/**
	 * Adds noise to the image drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void addUniform(ImageUInt8 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

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

	/**
	 * Adds noise to the image drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void addUniform(ImageSInt8 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

		byte[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				int value = (data[index] ) + rand.nextInt(range)+min;
				if( value < -128 ) value = -128;
				if( value > 127 ) value = 127;

				data[index++] = (byte) value;
			}
		}
	}

	/**
	 * Adds noise to the image drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void addUniform(ImageUInt16 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

		short[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				int value = (data[index] & 0xFFFF) + rand.nextInt(range)+min;
				if( value < 0 ) value = 0;
				if( value > 65535 ) value = 65535;

				data[index++] = (short) value;
			}
		}
	}

	/**
	 * Adds noise to the image drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void addUniform(ImageSInt16 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

		short[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				int value = (data[index] ) + rand.nextInt(range)+min;
				if( value < -32768 ) value = -32768;
				if( value > 32767 ) value = 32767;

				data[index++] = (short) value;
			}
		}
	}

	/**
	 * Adds noise to the image drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void addUniform(ImageSInt32 img, Random rand , int min , int max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		int range = max-min;

		int[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				int value = (data[index] ) + rand.nextInt(range)+min;
				data[index++] =  value;
			}
		}
	}

	/**
	 * Adds noise to the image drawn from an uniform distribution that has a range of min <= X < max.
	 */
	public static void addUniform(ImageFloat32 img, Random rand , float min , float max) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		float range = max-min;

		float[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				float value = data[index] + rand.nextFloat()*range+min;
				data[index++] =  value;
			}
		}
	}

}
