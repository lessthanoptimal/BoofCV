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
 * An image where the primitive type is an unsigned short.
 * </p>
 *
 * @author Peter Abeles
 */
public class InterleavedS16 extends InterleavedI16<InterleavedS16> {

	/**
	 * Creates a new image with an arbitrary number of bands/colors.
	 *
	 * @param width	number of columns in the image.
	 * @param height   number of rows in the image.
	 * @param numBands number of bands/colors in the image.
	 */
	public InterleavedS16(int width, int height, int numBands) {
		super(width, height, numBands);
	}

	public InterleavedS16() {
	}

	@Override
	public void unsafe_get(int x, int y, int[] storage) {
		int index = getIndex(x, y, 0);
		for (int i = 0; i < numBands; i++, index++) {
			storage[i] = data[index];
		}
	}

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.S16;
	}

	/**
	 * Returns the value of the specified band in the specified pixel.
	 *
	 * @param x	pixel coordinate.
	 * @param y	pixel coordinate.
	 * @param band which color band in the pixel
	 * @return an intensity value.
	 */
	@Override
	public int getBand(int x, int y, int band) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds.");
		if (band < 0 || band >= numBands)
			throw new ImageAccessException("Invalid band requested.");

		return data[getIndex(x, y, band)];
	}

	@Override
	public InterleavedS16 createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new InterleavedS16();
		return new InterleavedS16(imgWidth, imgHeight, numBands);
	}
}
