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

import pja.geometry.struct.point.Point2D_I32;

import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * <p>
 * A base class for a single band intensity image.  The image is an rectangular array where each pixel represents
 * an intensity measurement from an imaging sensor.  Internally the pixels are stored a 1D array in a row-major format.
 * Different primitive types (e.g. byte, short, float, double) are implemented by children of this class.
 * This image format is designed to allow quick and easy read/write access to each pixel and automatically supports sub-images.
 * </p>
 * <p/>
 * <p>
 * Most image operations work off of direct children of this class.  For operations which support images with
 * multiple bands or colors (e.g. RGB or multi-spectral cameras) there is the {@link ImageInterleaved} class and others.
 * </p>
 * <p/>
 * <p>
 * The image is defined by the following parameters:<br>
 * <dl>
 * <dt> width
 * <dd> number of columns in the image.
 * <dt> height
 * <dd> number of rows in the image.
 * <dt> startIndex
 * <dd> The index of the first pixel in the data array.
 * <dt> stride
 * <dd> Number of elements which need to be skipped over in the data array between rows.
 * </dl>
 * </p>
 * <p/>
 * <p>
 * Sub-images are images that are a rectangular image inside of a larger image.  The original image and the sub-image
 * share the same data array, so an operation in one will affect the other.   They are useful when only part of
 * the image needs to be processed.  All image processing operations support sub-images.
 * </p>
 * <p/>
 * <p>
 * Pixels can be directly accessed by elements. For example, to access the second band in an image with three color
 * bands at pixel (3,10) one would do:<br>
 * secondBand = img.data[ startIndex + 10*stride + 3 ]
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class ImageBase<T extends ImageBase> implements Serializable, Cloneable {

	/**
	 * Index of the first pixel in the data array
	 */
	public int startIndex;
	/**
	 * How many elements need to be skipped over to go one row down.
	 */
	public int stride;

	/**
	 * Number of columns in the image.
	 */
	public int width;
	/**
	 * Number of rows in the image.
	 */
	public int height;

	/**
	 * Creates a new image with all of its parameters initialized, including the
	 * data array.
	 *
	 * @param width  Image's width.
	 * @param height Image's height.
	 */
	protected ImageBase(int width, int height) {
		_setData(Array.newInstance(_getPrimitiveType(), width * height));
		this.startIndex = 0;
		this.stride = width;
		this.width = width;
		this.height = height;
	}

	protected ImageBase() {
	}

	/**
	 * Creates a sub-image from 'this' image.  The subimage will share the same internal array
	 * that stores each pixel's value, but will only pertain to an axis-aligned rectangular segment
	 * of the original.
	 *
	 * @param x0 x-coordinate of top-left corner of the sub-image.
	 * @param y0 y-coordinate of top-left corner of the sub-image.
	 * @param x1 x-coordinate of bottom-right corner of the sub-image.
	 * @param y1 y-coordinate of bottom-right corner of the sub-image.
	 * @return A sub-image of this image.
	 */
	public T subimage(int x0, int y0, int x1, int y1) {
		if (x0 < 0 || y0 < 0)
			throw new IllegalArgumentException("x0 or y0 is less than zero");
		if (x1 < x0 || y1 < y0)
			throw new IllegalArgumentException("x1 or y1 is less than x0 or y0 respectively");
		if (x1 > width || y1 > height)
			throw new IllegalArgumentException("x1 or y1 is more than the width or height respectively");

		T ret = _createNew(-1, -1);
		ret._setData(_getData());
		ret.stride = Math.max(width, stride);
		ret.width = x1 - x0;
		ret.height = y1 - y0;
		ret.startIndex = startIndex + y0 * stride + x0;

		return ret;
	}

	/**
	 * Changes the image's width and height without declaring new memory.  If the internal array
	 * is not large enough to store the new image an IllegalArgumentException is thrown.
	 *
	 * @param width The new width.
	 * @param height The new height.
	 */
	public void reshape(int width, int height) {
		if( isSubimage() )
			throw new IllegalArgumentException("Can't reshape sub-images");
		
		Object data = _getData();

		if( Array.getLength(data) < width*height ) {
			// declare a new larger image to store the data
			ImageBase a = _createNew(width,height);
			_setData(a._getData());
		}

		this.stride = width;
		this.width = width;
		this.height = height;
	}

	/**
	 * If this matrix is a sub-image or not.
	 *
	 * @return true if it is a subimage, otherwise false.
	 */
	public boolean isSubimage() {
		return startIndex != 0 || width != stride;
	}

	public final boolean isInBounds(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	public int getIndex(int x, int y) {
		return startIndex + y * stride + x;
	}

	public final void setDimension(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public final int getWidth() {
		return width;
	}

	public final void setWidth(int width) {
		this.width = width;
	}

	public final int getHeight() {
		return height;
	}

	public final void setHeight(int height) {
		this.height = height;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getStride() {
		return stride;
	}

	public void setStride(int stride) {
		this.stride = stride;
	}

	/**
	 * Sets the values of each pixel equal to the pixels in the specified matrix.  Both image's shape
	 * must be the same.
	 *
	 * @param orig The original image whose value is to be copied into this one
	 */
	@SuppressWarnings({"SuspiciousSystemArraycopy"})
	public void setTo(T orig) {
		if (orig.width != width || orig.height != height)
			throw new IllegalArgumentException("The width and/or height of 'orig' is not the same as this class");

		if (!orig.isSubimage() && !isSubimage()) {
			System.arraycopy(orig._getData(), orig.startIndex, _getData(), startIndex, stride * height);
		} else {
			int indexSrc = orig.startIndex;
			int indexDst = startIndex;
			for (int y = 0; y < height; y++) {
				System.arraycopy(orig._getData(), indexSrc, _getData(), indexDst, width);
				indexSrc += orig.stride;
				indexDst += stride;
			}
		}
	}

	/**
	 * Creates an identical image.  Note that if this image is a sub-image portions of hte image which are not part
	 * of the sub-image are not copied.
	 *
	 * @return Clone of this image.
	 */
	@SuppressWarnings({"unchecked", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
	@Override
	public T clone() {
		T ret = _createNew(width,height);

		ret.setTo(this);

		return ret;
	}

	public Point2D_I32 indexToPixel( int index )
	{
		index -= startIndex;
		return new Point2D_I32( index % stride , index / stride );
	}

	/**
	 * Returns true if the image stores pixel values as integers.
	 *
	 * @return True if it is an integer image.
	 */
	public abstract boolean isInteger();

	/**
	 * If the data is assumed to be signed or unsigned.
	 *
	 * @return true for signed and false for unsigned.
	 */
	public abstract boolean isSigned();

	/**
	 * Returns the data array the image is stored in.
	 *
	 * @return data array;
	 */
	protected abstract Object _getData();

	/**
	 * Returns the image's primitive data type
	 *
	 * @return primitive data type
	 */
	public abstract Class<?> _getPrimitiveType();

	/**
	 * Sets the image's internal data array.
	 *
	 * @param data data array
	 */
	protected abstract void _setData(Object data);

	/**
	 * Returns a new image.  If either width or height are
	 * set to -1 then none of the class parameters set. Otherwise
	 * a new image is created with the specified dimensions which has all
	 * other parameters the same as the original matrix.
	 *
	 * @param imgWidth
	 * @param imgHeight
	 * @return new image
	 */
	public abstract T _createNew(int imgWidth, int imgHeight);
}
