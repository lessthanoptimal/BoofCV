/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
 * An image where the primitive type is an unsigned byte.
 * </p>
 *
 * @author Peter Abeles
 */
public class InterleavedU8 extends InterleavedI8<InterleavedU8> {

	/**
	 * Creates a new image with an arbitrary number of bands/colors.
	 *
	 * @param width number of columns in the image.
	 * @param height number of rows in the image.
	 * @param numBands number of bands/colors in the image.
	 */
	public InterleavedU8( int width, int height, int numBands ) {
		super(width, height, numBands);
	}

	public InterleavedU8() {}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.U8;
	}

	/**
	 * Returns an integer formed from 4 bands. a[i]<<24 | a[i+1] << 16 | a[i+2]<<8 | a[3]
	 *
	 * @param x column
	 * @param y row
	 * @return 32 bit integer
	 */
	public int get32( int x, int y ) {
		int i = startIndex + y*stride + x*4;
		return ((data[i] & 0xFF) << 24) | ((data[i + 1] & 0xFF) << 16) | ((data[i + 2] & 0xFF) << 8) | (data[i + 3] & 0xFF);
	}

	/**
	 * Returns an integer formed from 3 bands. a[i] << 16 | a[i+1]<<8 | a[i+2]
	 *
	 * @param x column
	 * @param y row
	 * @return 32 bit integer
	 */
	public int get24( int x, int y ) {
		int i = startIndex + y*stride + x*3;
		return ((data[i] & 0xFF) << 16) | ((data[i + 1] & 0xFF) << 8) | (data[i + 2] & 0xFF);
	}

	public void set24( int x, int y, int value ) {
		int i = startIndex + y*stride + x*3;
		data[i++] = (byte)(value >>> 16);
		data[i++] = (byte)(value >>> 8);
		data[i] = (byte)value;
	}

	public void set32( int x, int y, int value ) {
		int i = startIndex + y*stride + x*4;
		data[i++] = (byte)(value >>> 24);
		data[i++] = (byte)(value >>> 16);
		data[i++] = (byte)(value >>> 8);
		data[i] = (byte)value;
	}

	/**
	 * Returns the value of the specified band in the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @param band which color band in the pixel
	 * @return an intensity value.
	 */
	@Override
	public int getBand( int x, int y, int band ) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds.");
		if (band < 0 || band >= numBands)
			throw new ImageAccessException("Invalid band requested.");

		return data[getIndex(x, y, band)] & 0xFF;
	}

	@Override
	public void unsafe_get( int x, int y, int[] storage ) {
		int index = getIndex(x, y, 0);
		for (int i = 0; i < numBands; i++, index++) {
			storage[i] = data[index] & 0xFF;
		}
	}

	@Override
	public InterleavedU8 createNew( int imgWidth, int imgHeight ) {
		if (imgWidth == -1 || imgHeight == -1)
			return new InterleavedU8();
		return new InterleavedU8(imgWidth, imgHeight, numBands);
	}
}
