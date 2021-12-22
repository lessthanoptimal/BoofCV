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

package boofcv.struct.image;

/**
 * <p>
 * Base class for images with 8-bit pixels.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class GrayI8<T extends GrayI8<T>> extends GrayI<T> {

	public byte[] data;

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width number of columns in the image.
	 * @param height number of rows in the image.
	 */
	protected GrayI8( int width, int height ) {
		super(width, height);
	}

	protected GrayI8() {data = new byte[0];}

	/**
	 * Create a copy from the two array. input[y][x]
	 */
	protected GrayI8( byte[][] input ) {
		this.height = input.length;
		if (height == 0) {
			width = 0;
		} else {
			width = input[0].length;
		}

		initialize(width, height);

		for (int y = 0; y < height; y++) {
			if (input[y].length != width)
				throw new IllegalArgumentException("rows must have constant length");
			System.arraycopy(input[y], 0, data, y*width, width);
		}
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @param value The pixel's new value.
	 */
	@Override
	public void set( int x, int y, int value ) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds: " + x + " " + y);

		data[getIndex(x, y)] = (byte)value;
	}

	@Override
	public void copyCol( int col, int row0, int row1, int offset, Object array ) {
		byte[] dst = (byte[])array;
		int idxSrc = startIndex + stride*row0 + col;
		int idxDst = offset;
		int end = idxSrc + (row1 - row0)*stride;
		while (idxSrc < end) {
			dst[idxDst++] = data[idxSrc];
			idxSrc += stride;
		}
	}

	@Override
	public void unsafe_set( int x, int y, int value ) {
		data[getIndex(x, y)] = (byte)value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override public void _setData( Object data ) {
		this.data = (byte[])data;
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.I8;
	}

	public byte[] getData() {
		return data;
	}

	public void setData( byte[] data ) {
		this.data = data;
	}
}
