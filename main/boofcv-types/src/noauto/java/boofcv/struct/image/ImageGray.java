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

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

/**
 * <p>
 * A base class for a single band intensity image. The image is an rectangular array where each pixel represents
 * an intensity measurement from an imaging sensor. Internally the pixels are stored a 1D array in a row-major format.
 * Different primitive types (e.g. byte, short, float, double) are implemented by children of this class.
 * This image format is designed to allow quick and easy read/write access to each pixel and automatically supports sub-images.
 * </p>
 * <p>
 * Most image operations work off of direct children of this class. For operations which support images with
 * multiple bands or colors (e.g. RGB or planar cameras) there is the {@link ImageInterleaved} class and others.
 * </p>
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
 * <p>
 * Sub-images are images that are a rectangular image inside of a larger image. The original image and the sub-image
 * share the same data array, so an operation in one will affect the other. They are useful when only part of
 * the image needs to be processed. All image processing operations support sub-images.
 * </p>
 * <p>
 * Pixels can be directly accessed by elements. For example, to access the pixel at (x = 3, y = 10) one would do:<br>
 * pixelValue = img.data[ startIndex + 10*stride ]
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public abstract class ImageGray<T extends ImageGray<T>> extends ImageBase<T> {

	{
		this.imageType = (ImageType)ImageType.single(getClass());
	}

	/**
	 * Creates a new image with all of its parameters initialized, including the
	 * data array.
	 *
	 * @param width Image's width.
	 * @param height Image's height.
	 */
	protected ImageGray( int width, int height ) {
		initialize(width, height);
	}

	protected ImageGray() {}

	protected void initialize( int width, int height ) {
		_setData(Array.newInstance(getDataType().getDataType(), width*height));
		this.startIndex = 0;
		this.stride = width;
		this.width = width;
		this.height = height;
	}

	/**
	 * <p>
	 * Creates a rectangular sub-image from 'this' image. The subimage will share the same internal array
	 * that stores each pixel's value. Any changes to pixel values in the original image or the sub-image will
	 * affect the other. A sub-image must be a sub-set of the original image and cannot specify a bounds larger
	 * than the original.
	 * </p>
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
	@Override
	public T subimage( int x0, int y0, int x1, int y1, @Nullable T subimage ) {
		if (x0 < 0 || y0 < 0)
			throw new IllegalArgumentException("x0 or y0 is less than zero");
		if (x1 < x0 || y1 < y0)
			throw new IllegalArgumentException("x1 or y1 is less than x0 or y0 respectively");
		if (x1 > width || y1 > height)
			throw new IllegalArgumentException("x1 or y1 is more than the width or height respectively");

		if (subimage == null) {
			subimage = createNew(-1, -1);
		}

		subimage._setData(_getData());
		subimage.stride = Math.max(width, stride);// ok why is this done?!?!  Shouldn't it always be stride?
		subimage.width = x1 - x0;
		subimage.height = y1 - y0;
		subimage.startIndex = startIndex + y0*stride + x0;
		subimage.subImage = true;
		subimage.imageType = imageType;

		return subimage;
	}

	/**
	 * Changes the image's width and height without declaring new memory. If the internal array
	 * is not large enough to store the new image an IllegalArgumentException is thrown.
	 *
	 * @param width The new width.
	 * @param height The new height.
	 */
	@Override
	public void reshape( int width, int height ) {
		if (this.width == width && this.height == height)
			return;
		if (isSubimage())
			throw new IllegalArgumentException("Can't reshape sub-images");

		Object data = _getData();

		if (Array.getLength(data) < width*height) {
			// declare a new larger image to store the data
			ImageGray a = createNew(width, height);
			_setData(a._getData());
		}

		this.stride = width;
		this.width = width;
		this.height = height;
	}

	/**
	 * Reshape to match the input image's width and height
	 */
	public void reshape( ImageBase img ) {
		reshape(img.width, img.height);
	}

	/**
	 * Sets the values of each pixel equal to the pixels in the specified matrix. If the images are not
	 * the same shape this will be resized.
	 *
	 * @param orig The original image whose value is to be copied into this one
	 */
	@SuppressWarnings({"SuspiciousSystemArraycopy"})
	@Override
	public void setTo( T orig ) {
		if (width != orig.width || height != orig.height)
			reshape(orig.width, orig.height);

		if (!orig.isSubimage() && !isSubimage()) {
			System.arraycopy(orig._getData(), orig.startIndex, _getData(), startIndex, stride*height);
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
	 * Creates a single band image of the specified type that will have the same
	 * shape as this image
	 */
	public <B extends ImageGray<B>> B createSameShape( Class<B> type ) {
		return create(type, width, height);
	}

	public static <B extends ImageGray<B>> B create( Class<B> type, int width, int height ) {
		if (type == GrayU8.class) {
			return (B)new GrayU8(width, height);
		} else if (type == GrayS8.class) {
			return (B)new GrayS8(width, height);
		} else if (type == GrayS16.class) {
			return (B)new GrayS16(width, height);
		} else if (type == GrayU16.class) {
			return (B)new GrayU16(width, height);
		} else if (type == GrayS32.class) {
			return (B)new GrayS32(width, height);
		} else if (type == GrayS64.class) {
			return (B)new GrayS64(width, height);
		} else if (type == GrayF32.class) {
			return (B)new GrayF32(width, height);
		} else if (type == GrayF64.class) {
			return (B)new GrayF64(width, height);
		} else if ((Class)type == GrayI.class) {
			// ImageInteger is a generic type, so just create something
			return (B)new GrayS32(width, height);
		}
		throw new IllegalArgumentException("Unknown type " + type);
	}

	@Override
	public void copyRow( int row, int col0, int col1, int offset, Object array ) {
		int idxSrc = startIndex + stride*row + col0;
		System.arraycopy(_getData(), idxSrc, array, offset, col1 - col0);
	}

	/**
	 * Prints the image to standard out
	 */
	public abstract void print();

	/**
	 * Returns the data array the image is stored in.
	 *
	 * @return data array;
	 */
	protected abstract Object _getData();

	/**
	 * Returns image type information
	 *
	 * @return The type of image.
	 */
	public abstract ImageDataType getDataType();

	/**
	 * Sets the image's internal data array.
	 *
	 * @param data data array
	 */
	public abstract void _setData( Object data );
}
