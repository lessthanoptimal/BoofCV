/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_I32;

import java.io.Serializable;

/**
 * Base class for all image types.
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
	 * Indicates if it is a sub-image or not
	 */
	public boolean subImage = false;

	/**
	 * Description of the image data structure
	 */
	public ImageType<T> imageType;

	public abstract T subimage(int x0, int y0, int x1, int y1, T subimage);

	public abstract void reshape(int width, int height);

	/**
	 * Sets the value of 'this' image to be identical to the passed in image.  All structural
	 * attributes of the images must be the same.
	 *
	 * @param orig Image for which 'this' is to be a copy of.
	 */
	public abstract void setTo( T orig );

	/**
	 * If this matrix is a sub-image or not.
	 *
	 * @return true if it is a subimage, otherwise false.
	 */
	public boolean isSubimage() {
		return subImage;
	}

	public final boolean isInBounds(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	public int getIndex(int x, int y) {
		return startIndex + y * stride + x;
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

	public Point2D_I32 indexToPixel( int index )
	{
		index -= startIndex;
		return new Point2D_I32( index % stride , index / stride );
	}

	/**
	 * Returns a new image.  If either width or height are
	 * set to -1 then none of the class parameters set. Otherwise
	 * a new image is created with the specified dimensions which has all
	 * other parameters the same as the original matrix.
	 *
	 * @param imgWidth Width of the new image
	 * @param imgHeight height of the new image
	 * @return new image
	 */
	public abstract T _createNew(int imgWidth, int imgHeight);

	/**
	 * Description of the image data structure
	 *
	 * @return Description of the image data structure
	 */
	public ImageType<T> getImageType() {
		return imageType;
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
}
