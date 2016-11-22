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

package boofcv.struct.image;

/**
 * Base class for integer interleaved images.
 *
 * @author Peter Abeles
 */
public abstract class InterleavedInteger<T extends InterleavedInteger> extends ImageInterleaved<T> {

	/**
	 * Creates a new image with an arbitrary number of bands/colors.
	 *
	 * @param width number of columns in the image.
	 * @param height number of rows in the image.
	 * @param numBands number of bands/colors in the image.
	 */
	public InterleavedInteger(int width, int height, int numBands) {
		super(width, height, numBands);
	}

	public InterleavedInteger() {
	}

	/**
	 * Returns the pixel's value for all the bands as an array.
	 *
	 * @param x	   pixel coordinate.
	 * @param y	   pixel coordinate.
	 * @param storage If not null then the pixel's value is written here.  If null a new array is created.
	 * @return The pixel's value.
	 */
	public int[] get(int x, int y, int[] storage) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		if (storage == null) {
			storage = new int[numBands];
		}

		unsafe_get(x,y,storage);

		return storage;
	}

	public abstract void unsafe_get(int x, int y, int[] storage);

	/**
	 * Sets the pixel's value for all the bands using an array.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value for each band.
	 */
	public void set(int x, int y, int... value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		unsafe_set(x,y,value);
	}

	public abstract void unsafe_set(int x, int y, int... value);

	/**
	 * Returns the value of the specified band in the specified pixel.
	 *
	 * @param x	pixel coordinate.
	 * @param y	pixel coordinate.
	 * @param band which color band in the pixel
	 * @return an intensity value.
	 */
	public abstract int getBand(int x, int y, int band);

	/**
	 * Returns the value of the specified band in the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param band  which color band in the pixel
	 * @param value The new value of the element.
	 */
	public abstract void setBand(int x, int y, int band, int value);
}
