/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.struct.image;

/**
 * <p>
 * An image where the primitive type is a signed 64-bit int.  By default all operations treat elements
 * in this image as an unsigned bytes.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageSInt64 extends ImageBase<ImageSInt64> {

	public long data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public ImageSInt64(int width, int height) {
		super(width, height);
	}

	public ImageSInt64() {
	}

	public long get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");
		
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

		data[getIndex(x, y)] = (short) value;
	}

	@Override
	public ImageTypeInfo<ImageSInt64> getTypeInfo() {
		return ImageTypeInfo.S64;
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
	public ImageSInt64 _createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1)
			return new ImageSInt64();
		return new ImageSInt64(imgWidth, imgHeight);
	}
}
