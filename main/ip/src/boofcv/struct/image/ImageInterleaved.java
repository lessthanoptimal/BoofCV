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

import java.lang.reflect.Array;

/**
 * <p>
 * Base class for images that contain multiple interleaved bands. Typically each band represents
 * a different color or frequency of light detected in the imaging sensor.
 * </p>
 * <p>
 * Each pixel is composed of N bands.  In an RGB image each pixel would be composed of 3 elements, [red][green][blue].
 * The index of the green band at pixel (3,10) would be:<br>
 * index of green at (3,10) = startIndex + 10*stride + 3*numBands + 1
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class ImageInterleaved<T extends ImageInterleaved> extends ImageMultiBand<T> {
	/**
	 * How many color bands are contained in each pixel
	 */
	public int numBands;

	{
		this.imageType = (ImageType<T>)ImageType.il(0, getClass());
	}

	/**
	 * Creates a new image with all of its parameters initialized, including the
	 * data array.
	 *
	 * @param width	Image's width.
	 * @param height   Image's height.
	 * @param numBands Number of bands/colors.
	 */
	protected ImageInterleaved(int width, int height, int numBands) {
		_setData(Array.newInstance(getPrimitiveDataType(), width * height * numBands));
		this.startIndex = 0;
		this.stride = width * numBands;
		this.numBands = numBands;
		this.width = width;
		this.height = height;
		this.imageType.numBands = numBands;
	}

	protected ImageInterleaved() {
	}

	/**
	 * Creates a sub-image from 'this' image.  The subimage will share the same internal array
	 * that stores each pixel's value, but will only pertain to an axis-aligned rectangular segment
	 * of the original.
	 *
	 *
	 * @param x0 x-coordinate of top-left corner of the sub-image.
	 * @param y0 y-coordinate of top-left corner of the sub-image.
	 * @param x1 x-coordinate of bottom-right corner of the sub-image.
	 * @param y1 y-coordinate of bottom-right corner of the sub-image.
	 * @param subimage
	 * @return A sub-image of this image.
	 */
	@Override
	public T subimage(int x0, int y0, int x1, int y1, T subimage) {
		T ret = createNew(-1, -1);
		ret._setData(_getData());
		ret.stride = Math.max(width * numBands, stride); // ok why is this done?!?!  Shouldn't it always be stride?
		ret.width = x1 - x0;
		ret.height = y1 - y0;
		ret.numBands = numBands;
		ret.startIndex = startIndex + y0 * stride + x0 * numBands;
		ret.subImage = true;
		ret.imageType = imageType;

		return ret;
	}

	@Override
	public void reshape(int width, int height) {
		if( isSubimage() )
			throw new IllegalArgumentException("Can't reshape sub-images");

		Object data = _getData();

		if( Array.getLength(data) < width*height*numBands ) {
			ImageInterleaved<?> a = createNew(width,height);
			_setData(a._getData());
		}

		this.width = width;
		this.height = height;
		this.stride = width*numBands;
	}

	@Override
	public int getIndex(int x, int y) {
		return startIndex + y * stride + x * numBands;
	}

	public int getIndex(int x, int y, int band) {
		return startIndex + y * stride + x * numBands + band;
	}

	/**
	 * Sets this image equal to the specified image. Automatically resized to match the input image.
	 *
	 * @param orig The original image whose value is to be copied into this one
	 */
	@SuppressWarnings({"SuspiciousSystemArraycopy"})
	@Override
	public void setTo(T orig) {
		if (orig.width != width || orig.height != height)
			reshape(orig.width,orig.height);
		if (orig.numBands != numBands)
			throw new IllegalArgumentException("The two images have different number of bands");

		if (!orig.isSubimage() && !isSubimage()) {
			System.arraycopy(orig._getData(), orig.startIndex, _getData(), startIndex, stride * height);
		} else {
			int indexSrc = orig.startIndex;
			int indexDst = startIndex;
			for (int y = 0; y < height; y++) {
				System.arraycopy(orig._getData(), indexSrc, _getData(), indexDst, width * numBands);
				indexSrc += orig.stride;
				indexDst += stride;
			}
		}
	}

	@Override
	public int getNumBands() {
		return imageType.getNumBands();
	}

	public final void setNumBands(int numBands) {
		this.imageType.numBands = numBands;
		this.numBands = numBands;
	}

	/**
	 * Returns image type information
	 *
	 * @return The type of image.
	 */
	public abstract ImageDataType getDataType();

	/**
	 * Returns the data array the image is stored in.
	 *
	 * @return data array;
	 */
	protected abstract Object _getData();

	protected abstract Class getPrimitiveDataType();

	/**
	 * Sets the image's internal data array.
	 *
	 * @param data data array
	 */
	protected abstract void _setData(Object data);
}
