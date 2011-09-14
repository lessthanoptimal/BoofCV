/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
 * An image where the primitive type is a 64-bit floating point number.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageFloat64 extends ImageFloat<ImageFloat64> {

	public double data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public ImageFloat64(int width, int height) {
		super(width, height);
	}

	public ImageFloat64() {
	}

	/**
	 * Returns the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	public double get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds: ( " + x + " , " + y + " )");

		return unsafe_get(x,y);
	}

	protected double unsafe_get(int x, int y) {
		return data[getIndex(x, y)];
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public void set(int x, int y, double value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds: "+x+" "+y);

		data[getIndex(x, y)] = value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (double[]) data;
	}

	@Override
	public ImageFloat64 _createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new ImageFloat64();
		return new ImageFloat64(imgWidth, imgHeight);
	}

	@Override
	public ImageTypeInfo<ImageFloat64> getTypeInfo() {
		return ImageTypeInfo.F64;
	}
}
