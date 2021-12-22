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
 * Base class for images with 16-bit pixels.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class GrayI16<T extends GrayI16<T>> extends GrayI<T> {

	public short[] data;

	protected GrayI16( int width, int height ) {
		super(width, height);
	}

	protected GrayI16() {data = new short[0];}

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

		data[getIndex(x, y)] = (short)value;
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @param value The pixel's new value.
	 */
	@Override
	public void unsafe_set( int x, int y, int value ) {
		data[getIndex(x, y)] = (short)value;
	}

	@Override
	public void copyCol( int col, int row0, int row1, int offset, Object array ) {
		short[] dst = (short[])array;
		int idxSrc = startIndex + stride*row0 + col;
		int idxDst = offset;
		int end = idxSrc + (row1 - row0)*stride;
		while (idxSrc < end) {
			dst[idxDst++] = data[idxSrc];
			idxSrc += stride;
		}
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override public void _setData( Object data ) {
		this.data = (short[])data;
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.I16;
	}

	public short[] getData() {
		return data;
	}

	public void setData( short[] data ) {
		this.data = data;
	}
}
