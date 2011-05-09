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
 * Base class for all integer images.  An integer image can either be signed or unsigned.  Since all integers in
 * Java are signed (which is really f*ing annoying) a boolean variable is used to differentiate these image types.
 * </p>
 *
 * <p>
 * DESIGN NOTE: No performance or generality is lost by using the 'int' type for setters and getters.  At a low level
 * the JavaVM converts all integer types into an int when not in an array.
 * </p>
 *
 * <p>
 * DESIGN NOTE: Java does not support unsigned data.  IF an image is unsigned this is only directly enforced by the get()
 * function.  When directly accessing the data array the data's unsigned nature must be enforced manually using the
 * bitwise and operator.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class ImageInteger<T extends ImageInteger> extends ImageBase<T>{

	protected ImageInteger(int width, int height ) {
		super(width, height);
	}

	protected ImageInteger() {
	}

	/**
	 * If the data is assumed to be signed or unsigned.
	 *
	 * @return true for signed and false for unsigned.
	 */
	public abstract boolean isSigned();

	/**
	 * Returns the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	public abstract int get(int x, int y);

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public abstract void set(int x, int y, int value );
}
