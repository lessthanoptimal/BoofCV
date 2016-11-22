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
 * <p>
 * {@link ImageInterleaved} for data of type byte.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class InterleavedI8<T extends InterleavedI8> extends InterleavedInteger<T> {

	public byte data[];

	/**
	 * Creates a new image with an arbitrary number of bands/colors.
	 *
	 * @param width number of columns in the image.
	 * @param height number of rows in the image.
	 * @param numBands number of bands/colors in the image.
	 */
	public InterleavedI8(int width, int height, int numBands) {
		super(width, height, numBands);
	}

	public InterleavedI8() {
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.I8;
	}

	/**
	 * Returns the pixel's value for all the bands as an array.
	 *
	 * @param x	   pixel coordinate.
	 * @param y	   pixel coordinate.
	 * @param storage If not null then the pixel's value is written here.  If null a new array is created.
	 * @return The pixel's value.
	 */
	public byte[] get(int x, int y, byte[] storage) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		if (storage == null) {
			storage = new byte[numBands];
		}

		int index = getIndex(x, y, 0);
		for (int i = 0; i < numBands; i++, index++) {
			storage[i] = data[index];
		}

		return storage;
	}

	/**
	 * Sets the pixel's value for all the bands using an array.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value for each band.
	 */
	public void set(int x, int y, byte... value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		int index = getIndex(x, y, 0);
		for (int i = 0; i < numBands; i++, index++) {
			data[index] = value[i];
		}
	}

	/**
	 * Returns the value of the specified band in the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param band  which color band in the pixel
	 * @param value The new value of the element.
	 */
	public void setBand(int x, int y, int band, byte value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds.");
		if (band < 0 || band >= numBands)
			throw new ImageAccessException("Invalid band requested.");

		data[getIndex(x, y, band)] = value;
	}

	@Override
	public void unsafe_set(int x, int y, int... value) {
		int index = getIndex(x, y, 0);
		for (int i = 0; i < numBands; i++, index++) {
			data[index] = (byte)value[i];
		}
	}

	@Override
	public void setBand(int x, int y, int band, int value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds.");
		if (band < 0 || band >= numBands)
			throw new ImageAccessException("Invalid band requested.");

		data[getIndex(x, y, band)] = (byte)value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected Class getPrimitiveDataType() {
		return byte.class;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (byte[]) data;
	}

}
