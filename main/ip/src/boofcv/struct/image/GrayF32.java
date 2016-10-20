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
 * Image with a pixel type of 32-bit float.
 * </p>
 *
 * @author Peter Abeles
 */
public class GrayF32 extends GrayF<GrayF32> {

	public float data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public GrayF32(int width, int height) {
		super(width, height);
	}

	/**
	 * Creates an image with no data declared and the width/height set to zero.
	 */
	public GrayF32() {
	}

	/**
	 * Returns the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return Pixel intensity value.
	 */
	public float get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds: ( " + x + " , " + y + " )");

		return unsafe_get(x,y);
	}

	public float unsafe_get(int x, int y) {
		return data[getIndex(x, y)];
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public void set(int x, int y, float value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds: "+x+" "+y);

		unsafe_set(x,y,value);
	}

	public void unsafe_set(int x, int y, float value) {
		data[getIndex(x, y)] = value;
	}

	public void print() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				System.out.printf("%4.1f ",unsafe_get(x, y));
			}
			System.out.println();
		}
	}

	public void print( String format ) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				System.out.printf(format+" ",unsafe_get(x, y));
			}
			System.out.println();
		}
	}

	public void printInt() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				System.out.printf("%3d ",(int)unsafe_get(x,y));
			}
			System.out.println();
		}
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (float[]) data;
	}

	@Override
	public GrayF32 createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new GrayF32();
		return new GrayF32(imgWidth, imgHeight);
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.F32;
	}

	public float[] getData() {
		return data;
	}

	public void setData(float[] data) {
		this.data = data;
	}
}
