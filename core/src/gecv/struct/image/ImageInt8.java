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
 * An image where the primitive type is a byte.  By default all operations treat elements
 * in this image as an unsigned bytes.
 * </p>
 * <p/>
 * <p>
 * NOTE: An integer is returned and not a byte since Java will convert all bytes into integers internally.  No
 * performance boost by using a byte and its more of a hassle.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImageInt8 extends ImageInteger<ImageInt8> {

	public byte data[];

	/**
	 * Creates a new gray scale (single band/color) image.
	 *
	 * @param width  number of columns in the image.
	 * @param height number of rows in the image.
	 */
	public ImageInt8(int width, int height) {
		super(width, height,false);
	}

	public ImageInt8(int width, int height , boolean signed) {
		super(width, height,signed);
	}

	public ImageInt8() {
	}

	/**
	 * <p>
	 * Returns the value of the specified pixel.
	 * </p>
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	@Override
	public int get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		if( signed )
			return data[getIndex(x, y)];
		else
			return data[getIndex(x, y)] & 0xFF;
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	@Override
	public void set(int x, int y, int value) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds");

		data[getIndex(x, y)] = (byte) value;
	}

	@Override
	protected Object _getData() {
		return data;
	}

	@Override
	protected void _setData(Object data) {
		this.data = (byte[]) data;
	}

	@Override
	public ImageInt8 _createNew(int imgWidth, int imgHeight) {
		if (imgWidth == -1 || imgHeight == -1) {
			ImageInt8 ret = new ImageInt8();
			ret.signed = signed;
			return ret;
		}
		return new ImageInt8(imgWidth, imgHeight, signed);
	}

	@Override
	protected Class<?> _getPrimitiveType() {
		return byte.class;
	}

	public void printBinary() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (get(x, y) == 0)
					System.out.print("0");
				else
					System.out.print("1");
			}
			System.out.println();
		}
	}
}
