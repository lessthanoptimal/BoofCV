/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Base class for all image types.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class ImageBase<T extends ImageBase> implements Serializable, Cloneable {
	/** Index of the first pixel in the data array */
	public @Getter @Setter int startIndex;
	/** How many elements need to be skipped over to go one row down. */
	public @Getter @Setter int stride;

	/** Number of columns in the image. */
	public @Getter @Setter int width;
	/** Number of rows in the image. */
	public @Getter @Setter int height;

	/** Indicates if it is a sub-image or not */
	public boolean subImage = false;

	/** Description of the image data structure */
	public ImageType imageType;

	/**
	 * <p>
	 * Creates a rectangular sub-image from 'this' image. The subimage will share the same internal array
	 * that stores each pixel's value. Any changes to pixel values in the original image or the sub-image will
	 * affect the other. A sub-image must be a sub-set of the original image and cannot specify a bounds larger
	 * than the original.
	 *</p>
	 *
	 * <p>
	 * When specifying the sub-image, the top-left corner is inclusive and the bottom right corner exclusive. Thus,
	 * a sub-image will contain all the original pixels if the following is used: subimage(0,0,width,height,null).
	 * </p>
	 *
	 * @param x0 x-coordinate of top-left corner of the sub-image, inclusive.
	 * @param y0 y-coordinate of top-left corner of the sub-image, inclusive.
	 * @param x1 x-coordinate of bottom-right corner of the sub-image, exclusive.
	 * @param y1 y-coordinate of bottom-right corner of the sub-image, exclusive.
	 * @param subimage Optional output for sub-image. If not null the subimage will be written into this image.
	 * @return A sub-image of 'this' image.
	 */
	public abstract T subimage(int x0, int y0, int x1, int y1, @Nullable T subimage);

	/**
	 * Same as {@link #subimage(int, int, int, int, ImageBase)}, but sets the storage for the input subimage
	 * to null.
	 *
	 * @param x0 x-coordinate of top-left corner of the sub-image, inclusive.
	 * @param y0 y-coordinate of top-left corner of the sub-image, inclusive.
	 * @param x1 x-coordinate of bottom-right corner of the sub-image, exclusive.
	 * @param y1 y-coordinate of bottom-right corner of the sub-image, exclusive.
	 * @return A sub-image of 'this' image.
	 */
	public T subimage(int x0, int y0, int x1, int y1 ) {
		return subimage(x0,y0,x1,y1,null);
	}

	/**
	 * Changes the width and height of the image. If the image data array isn't large enough to hold an
	 * image of this size then a new array is declared. Otherwise the image data array is left unchanged
	 * and only the width and height variables are modified.
	 *
	 * @param width Desired image width
	 * @param height Desired image height
	 */
	public abstract void reshape(int width, int height);

	/**
	 * Reshapes this image so that it's shape matches the input image
	 * @param image Image whose shape will be matched
	 */
	public void matchShape( ImageBase image ) {
		reshape(image.width,image.height);
	}

	/**
	 * Sets the value of 'this' image to be identical to the passed in image. All structural
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

	/**
	 * Returns true if the pixel coordinate is inside the image or false if not.
	 *
	 * @param x pixel location x-axis
	 * @param y pixel location y-axis
	 * @return true if inside and false if outside
	 */
	public final boolean isInBounds(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	public int getIndex(int x, int y) {
		return startIndex + y * stride + x;
	}

	public int indexToPixelX( int index ) {
		return (index-startIndex)%stride;
	}
	public int indexToPixelY( int index ) {
		return (index-startIndex)/stride;
	}

	/**
	 * Returns a new image. If either width or height are
	 * set to -1 then none of the class parameters set. Otherwise
	 * a new image is created with the specified dimensions which has all
	 * other parameters the same as the original matrix.
	 *
	 * @param imgWidth Width of the new image
	 * @param imgHeight height of the new image
	 * @return new image
	 */
	public abstract T createNew(int imgWidth, int imgHeight);

	/**
	 * Description of the image data structure
	 *
	 * @return Description of the image data structure
	 */
	public ImageType<T> getImageType() {
		return imageType;
	}

	/**
	 * Creates a new image of the same type which also has the same shape. This is the same as calling
	 * {@link #createNew(int, int)} with this image's width and height.
	 *
	 * @return new image with the same shape as this.
	 */
	public T createSameShape() {
		return createNew(width,height);
	}

	/**
	 * Returns the number of pixels in the image. width*height
	 */
	public int totalPixels() {
		return width*height;
	}

	/**
	 * For each for each (x,y) coordinate inside the image.
	 */
	public void forEachXY( PixelXY function ) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				function.process(x,y);
			}
		}
	}

	/**
	 * Copies the row into the array.
	 * @param row Which row to copy.
	 * @param col0 First column. Inclusive.
	 * @param col1 Last column. Exclusive.
	 * @param offset First index in output array
	 * @param array Output array
	 */
	public abstract void copyRow( int row, int col0 , int col1 , int offset , Object array );

	/**
	 * Copies the column into the array.
	 * @param col Which column to copy.
	 * @param row0 First row. Inclusive.
	 * @param row1 Last row. Exclusive.
	 * @param offset First index in output array
	 * @param array Output array
	 */
	public abstract void copyCol(int col , int row0 , int row1 , int offset , Object array );

	/**
	 * Creates an identical image. Note that if this image is a sub-image portions of hte image which are not part
	 * of the sub-image are not copied.
	 *
	 * @return Clone of this image.
	 */
	@SuppressWarnings({"unchecked", "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"})
	@Override
	public T clone() {
		T ret = createSameShape();

		ret.setTo((T)this);

		return ret;
	}

	/** Lambda for each (x,y) coordinate in the image */
	public @FunctionalInterface interface PixelXY { void process( int x, int y); }
}
