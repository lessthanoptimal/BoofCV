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
 * {@link boofcv.struct.image.ImageInterleaved} for data of type double.
 * </p>
 *
 * @author Peter Abeles
 */
public class InterleavedF64 extends ImageInterleaved<InterleavedF64> {

	public double data[];

	/**
	 * Creates a new image with an arbitrary number of bands/colors.
	 *
	 * @param width	number of columns in the image.
	 * @param height   number of rows in the image.
	 * @param numBands number of bands/colors in the image.
	 */
	public InterleavedF64(int width, int height, int numBands) {
		super(width, height, numBands);
	}

	public InterleavedF64() {
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.F64;
	}

	/**
	 * Returns the pixel's value for all the bands as an array.
	 *
	 * @param x	   pixel coordinate.
	 * @param y	   pixel coordinate.
	 * @param storage If not null then the pixel's value is written here.  If null a new array is created.
	 * @return The pixel's value.
	 */
	public double[] get(int x, int y, double[] storage) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		if (storage == null) {
			storage = new double[numBands];
		}

		unsafe_get(x,y,storage);

		return storage;
	}

	public void unsafe_get(int x, int y, double[] storage) {
		int index = getIndex(x, y, 0);
		for (int i = 0; i < numBands; i++, index++) {
			storage[i] = data[index];
		}
	}

	/**
	 * Sets the pixel's value for all the bands using an array.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value for each band.
	 */
	public void set(int x, int y, double... value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		unsafe_set(x,y,value);
	}

	public void unsafe_set(int x, int y, double... value) {
		int index = getIndex(x, y, 0);
		for (int i = 0; i < numBands; i++, index++) {
			data[index] = value[i];
		}
	}

	/**
	 * Returns the value of the specified band in the specified pixel.
	 *
	 * @param x	pixel coordinate.
	 * @param y	pixel coordinate.
	 * @param band which color band in the pixel
	 * @return an intensity value.
	 */
	public double getBand(int x, int y, int band) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds.");
		if (band < 0 || band >= numBands)
			throw new ImageAccessException("Invalid band requested.");

		return data[getIndex(x, y, band)];
	}

	/**
	 * Returns the value of the specified band in the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param band  which color band in the pixel
	 * @param value The new value of the element.
	 */
	public void setBand(int x, int y, int band, double value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds.");
		if (band < 0 || band >= numBands)
			throw new ImageAccessException("Invalid band requested.");

		data[getIndex(x, y, band)] = value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected Class getPrimitiveDataType() {
		return double.class;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (double[]) data;
	}

	@Override
	public InterleavedF64 createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new InterleavedF64();
		return new InterleavedF64(imgWidth, imgHeight, numBands);
	}
}
