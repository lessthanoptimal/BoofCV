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
 * Image with a pixel type of signed 64-bit integer
 * </p>
 *
 * @author Peter Abeles
 */
public class GrayS64 extends ImageGray<GrayS64> {

	public long data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public GrayS64(int width, int height) {
		super(width, height);
	}

	/**
	 * Creates an image with no data declared and the width/height set to zero.
	 */
	public GrayS64() {
	}

	/**
	 * Returns the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return Pixel intensity value.
	 */
	public long get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		return unsafe_get(x,y);
	}

	public long unsafe_get(int x, int y) {
		return data[getIndex(x, y)];
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public void set(int x, int y, long value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		unsafe_set(x, y, value);
	}

	public void unsafe_set(int x, int y, long value) {
		data[getIndex(x, y)] = value;
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.S64;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (long[]) data;
	}

	@Override
	public GrayS64 createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new GrayS64();
		return new GrayS64(imgWidth, imgHeight);
	}

	public long[] getData() {
		return data;
	}

	public void setData(long[] data) {
		this.data = data;
	}
}
