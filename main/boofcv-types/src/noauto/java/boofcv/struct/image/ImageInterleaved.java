/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
 * Base class for images that contain multiple interleaved bands. Typically each band represents
 * a different color or frequency of light detected in the imaging sensor.
 * </p>
 * <p>
 * Each pixel is composed of N bands. In an RGB image each pixel would be composed of 3 elements, [red][green][blue].
 * The index of the green band at pixel (3,10) would be:<br>
 * index of green at (3,10) = startIndex + 10*stride + 3*numBands + 1
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class ImageInterleaved<T extends ImageInterleaved<T>> extends ImageMultiBand<T> {
	/**
	 * How many color bands are contained in each pixel
	 */
	public int numBands;

	{
		this.imageType = ImageType.il(0, getClass());
	}

	/**
	 * Creates a new image with all of its parameters initialized, including the
	 * data array.
	 *
	 * @param width Image's width.
	 * @param height Image's height.
	 * @param numBands Number of bands/colors.
	 */
	protected ImageInterleaved( int width, int height, int numBands ) {
		_setData(Array.newInstance(getPrimitiveDataType(), width*height*numBands));
		this.startIndex = 0;
		this.stride = width*numBands;
		this.numBands = numBands;
		this.width = width;
		this.height = height;
		this.imageType.numBands = numBands;
	}

	protected ImageInterleaved() {}

	/**
	 * Creates a sub-image from 'this' image. The subimage will share the same internal array
	 * that stores each pixel's value, but will only pertain to an axis-aligned rectangular segment
	 * of the original.
	 *
	 * @param x0 x-coordinate of top-left corner of the sub-image.
	 * @param y0 y-coordinate of top-left corner of the sub-image.
	 * @param x1 x-coordinate of bottom-right corner of the sub-image.
	 * @param y1 y-coordinate of bottom-right corner of the sub-image.
	 * @param subimage Optional storage for the subimage. Nullable.
	 * @return A sub-image of this image.
	 */
	@Override
	public T subimage( int x0, int y0, int x1, int y1, @Nullable T subimage ) {
		T ret = createNew(-1, -1);
		ret._setData(_getData());
		ret.stride = Math.max(width*numBands, stride); // ok why is this done?!?!  Shouldn't it always be stride?
		ret.width = x1 - x0;
		ret.height = y1 - y0;
		ret.numBands = numBands;
		ret.startIndex = startIndex + y0*stride + x0*numBands;
		ret.subImage = true;
		ret.imageType = imageType;

		return ret;
	}

	@Override
	public void reshape( int width, int height ) {
		if (this.width == width && this.height == height)
			return;

		if (isSubimage())
			throw new IllegalArgumentException("Can't reshape sub-images");

		Object data = _getData();

		if (Array.getLength(data) < width*height*numBands) {
			ImageInterleaved<?> a = createNew(width, height);
			_setData(a._getData());
		}

		this.width = width;
		this.height = height;
		this.stride = width*numBands;
	}

	@Override
	public void reshape( int width, int height, int numberOfBands ) {
		if (this.numBands != numberOfBands) {
			if (isSubimage())
				throw new IllegalArgumentException("Can't reshape sub-images");
			this.numBands = -1; // force it to redeclare memory
			this.width = width;
			this.height = height;
			setNumberOfBands(numberOfBands);
		} else {
			reshape(width, height);
		}
	}

	@Override
	public int getIndex( int x, int y ) {
		return startIndex + y*stride + x*numBands;
	}

	public int getIndex( int x, int y, int band ) {
		return startIndex + y*stride + x*numBands + band;
	}

	/**
	 * Sets this image equal to the specified image. Automatically resized to match the input image.
	 *
	 * @param orig The original image whose value is to be copied into this one
	 */
	@SuppressWarnings({"SuspiciousSystemArraycopy"})
	@Override
	public T setTo( T orig ) {
		if (orig.width != width || orig.height != height || orig.numBands != numBands)
			reshape(orig.width, orig.height, orig.numBands);

		if (!orig.isSubimage() && !isSubimage()) {
			System.arraycopy(orig._getData(), orig.startIndex, _getData(), startIndex, stride*height);
		} else {
			int indexSrc = orig.startIndex;
			int indexDst = startIndex;
			for (int y = 0; y < height; y++) {
				System.arraycopy(orig._getData(), indexSrc, _getData(), indexDst, width*numBands);
				indexSrc += orig.stride;
				indexDst += stride;
			}
		}
		return (T)this;
	}

	@Override
	public int getNumBands() {
		return imageType.getNumBands();
	}

	@Override
	public final void setNumberOfBands( int numBands ) {
		if (this.numBands == numBands)
			return;

		if (isSubimage())
			throw new IllegalArgumentException("Can't reshape sub-images");

		this.imageType.numBands = numBands;
		this.numBands = numBands;
		this.stride = width*numBands;

		Object data = _getData();

		if (data == null || Array.getLength(data) < width*height*numBands) {
			ImageInterleaved<?> a = createNew(width, height);
			_setData(a._getData());
		}
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
	protected abstract void _setData( Object data );

	/**
	 * Creates an interleaved image of the specified type that will have the same shape
	 */
	public <B extends ImageInterleaved<B>> B createSameShape( Class<B> type ) {
		return create(type, width, height, numBands);
	}

	public static <B extends ImageInterleaved<B>> B create( Class<B> type, int width, int height, int numBands ) {
		if (type == InterleavedU8.class) {
			return (B)new InterleavedU8(width, height, numBands);
		} else if (type == InterleavedS8.class) {
			return (B)new InterleavedS8(width, height, numBands);
		} else if (type == InterleavedU16.class) {
			return (B)new InterleavedU16(width, height, numBands);
		} else if (type == InterleavedS16.class) {
			return (B)new InterleavedS16(width, height, numBands);
		} else if (type == InterleavedS32.class) {
			return (B)new InterleavedS32(width, height, numBands);
		} else if (type == InterleavedS64.class) {
			return (B)new InterleavedS64(width, height, numBands);
		} else if (type == InterleavedF32.class) {
			return (B)new InterleavedF32(width, height, numBands);
		} else if (type == InterleavedF64.class) {
			return (B)new InterleavedF64(width, height, numBands);
		}
		throw new IllegalArgumentException("Unknown type " + type);
	}

	@Override
	public void copyRow( int row, int col0, int col1, int offset, Object array ) {
		int idxSrc = startIndex + stride*row + col0*numBands;
		System.arraycopy(_getData(), idxSrc, array, offset, (col1 - col0)*numBands);
	}

	@Override
	public String toString() {
		String out = getClass().getSimpleName() + " : w=" + width + ", h=" + height + ", c=" + numBands + "\n";
		for (int y = 0; y < height; y++) {
			int index = startIndex + y*stride;
			for (int x = 0; x < width; x++) {
				for (int band = 0; band < numBands; band++) {
					out += toString_element(index++) + " ";
				}
				if (x < width - 1)
					out += ", ";
			}
			out += "\n";
		}
		return out;
	}

	/**
	 * Convert an individual data element into a string
	 */
	public abstract String toString_element( int index );
}
