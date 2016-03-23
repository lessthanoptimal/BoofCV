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
 * DESIGN NOTE: Java does not support unsigned data.  If an image is unsigned this is only directly enforced by the get()
 * function.  When directly accessing the data array the data's unsigned nature must be enforced manually using the
 * bitwise and operator.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class GrayI<T extends GrayI> extends ImageGray<T> {

	protected GrayI(int width, int height ) {
		super(width, height);
	}

	protected GrayI() {
	}

	/**
	 * Returns the value of the specified pixel.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	public int get(int x, int y) {
		if (!isInBounds(x, y))
			throw new ImageAccessException("Requested pixel is out of bounds: "+x+" "+y);

		return unsafe_get(x,y);
	}

	/**
	 * Sets the value of the specified pixel.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public abstract void set(int x, int y, int value );

	/**
	 * Set function which does not perform bounds checking.
	 *
	 * @param x	 pixel coordinate.
	 * @param y	 pixel coordinate.
	 * @param value The pixel's new value.
	 */
	public abstract void unsafe_set( int x , int y , int value );

	/**
	 * Get function which does not perform bounds checking.
	 *
	 * @param x pixel coordinate.
	 * @param y pixel coordinate.
	 * @return an intensity value.
	 */
	public abstract int unsafe_get( int x , int y );

	@Override
	public ImageDataType getDataType() {
		return ImageDataType.I;
	}

	public void print() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				System.out.printf("%3d ",get(x,y));
			}
			System.out.println();
		}
	}

	public void printBinary() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				System.out.printf("%1d",get(x,y));
			}
			System.out.println();
		}
	}
	public void printNotZero() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if( unsafe_get(x, y) == 0 )
					System.out.print("0");
				else
					System.out.print("1");
			}
			System.out.println();
		}
	}
}
