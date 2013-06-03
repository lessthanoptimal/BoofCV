/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.GeneralizedImageOps;

import java.lang.reflect.Array;

/**
 * Specifies the type of image data structure.
 *
 * @author Peter Abeles
 */
public class ImageDataType<T extends ImageBase> {

	/**
	 * Specifies the image data structure
	 */
	Family family;
	/**
	 * Specifies the type of data used to store pixel information
	 */
	ImageTypeInfo dataType;
	/**
	 * Number of bands in the image.  Single band images ignore this field.
	 */
	int numBands;

	public ImageDataType(Family family, ImageTypeInfo dataType , int numBands ) {
		this.family = family;
		this.dataType = dataType;
		this.numBands = numBands;
	}

	public static <I extends ImageSingleBand> ImageDataType<I> single( Class<I> imageType ) {
		return new ImageDataType<I>(Family.SINGLE_BAND,ImageTypeInfo.classToType(imageType),1);
	}

	public static <I extends ImageSingleBand> ImageDataType<MultiSpectral<I>> ms( int numBands , Class<I> imageType ) {
		return new ImageDataType<MultiSpectral<I>>(Family.MULTI_SPECTRAL,ImageTypeInfo.classToType(imageType),numBands);
	}

	public ImageTypeInfo getDataType() {
		return dataType;
	}

	/**
	 * Creates a new image.
	 *
	 * @param width Number of columns in the image.
	 * @param height Number of rows in the image.
	 * @return New instance of the image.
	 */
	public T createImage( int width , int height ) {
		switch( family ) {
			case SINGLE_BAND:
				return (T)GeneralizedImageOps.createSingleBand(dataType.getImageClass(),width,height);

			case MULTI_SPECTRAL:
				return (T)new MultiSpectral(dataType.getImageClass(),width,height,numBands);

			default:
				throw new IllegalArgumentException("Type not yet supported");
		}
	}

	/**
	 * Creates an array of the specified iamge type
	 * @param length Number of elements in the array
	 * @return array of image type
	 */
	public T[] createArray( int length ) {
		switch( family ) {
			case SINGLE_BAND:
				return (T[])Array.newInstance(dataType.getImageClass(),length);

			case MULTI_SPECTRAL:
				return (T[])new MultiSpectral[ length ];

			default:
				throw new IllegalArgumentException("Type not yet supported");
		}
	}

	public int getNumBands() {
		return numBands;
	}

	public Family getFamily() {
		return family;
	}

	public static enum Family
	{
		SINGLE_BAND,
		MULTI_SPECTRAL,
		INTERLEAVED
	}
}
