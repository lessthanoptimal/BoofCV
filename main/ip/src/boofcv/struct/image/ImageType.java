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

import boofcv.core.image.GeneralizedImageOps;

import java.lang.reflect.Array;

/**
 * Specifies the type of image data structure.
 *
 * @author Peter Abeles
 */
public class ImageType<T extends ImageBase> {

	/**
	 * Specifies the image data structure
	 */
	Family family;
	/**
	 * Specifies the type of data used to store pixel information
	 */
	ImageDataType dataType;
	/**
	 * Number of bands in the image.  Single band images ignore this field.
	 */
	int numBands;

	public ImageType(Family family, ImageDataType dataType, int numBands) {
		this.family = family;
		this.dataType = dataType;
		this.numBands = numBands;
	}

	public static <I extends ImageSingleBand> ImageType<I> single( Class<I> imageType ) {
		return new ImageType<I>(Family.SINGLE_BAND, ImageDataType.classToType(imageType),1);
	}

	public static <I extends ImageSingleBand> ImageType<MultiSpectral<I>> ms( int numBands , Class<I> imageType ) {
		return new ImageType<MultiSpectral<I>>(Family.MULTI_SPECTRAL, ImageDataType.classToType(imageType),numBands);
	}

	public static <I extends ImageInterleaved> ImageType<I> interleaved( int numBands , Class<I> imageType ) {
		return new ImageType<I>(Family.INTERLEAVED, ImageDataType.classToType(imageType),numBands);
	}

	public ImageDataType getDataType() {
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
				return (T)GeneralizedImageOps.createSingleBand(getImageClass(),width,height);

			case INTERLEAVED:
				return (T)GeneralizedImageOps.createInterleaved(getImageClass(), width, height, numBands);

			case MULTI_SPECTRAL:
				return (T)new MultiSpectral(getImageClass(),width,height,numBands);

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
			case INTERLEAVED:
				return (T[])Array.newInstance(getImageClass(),length);

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

	public Class getImageClass() {
		return getImageClass(family,dataType);
	}

	public static Class getImageClass( Family family , ImageDataType dataType ) {
		switch( family ) {
			case SINGLE_BAND:
			case MULTI_SPECTRAL:
				switch( dataType ) {
					case F32: return ImageFloat32.class;
					case F64: return ImageFloat64.class;
					case U8: return ImageUInt8.class;
					case S8: return ImageSInt8.class;
					case U16: return ImageUInt16.class;
					case S16: return ImageSInt16.class;
					case S32: return ImageSInt32.class;
					case S64: return ImageSInt64.class;
					case I8: return ImageInt8.class;
					case I16: return ImageInt16.class;
				}
				break;

			case INTERLEAVED:
				switch( dataType ) {
					case F32: return InterleavedF32.class;
					case F64: return InterleavedF64.class;
					case U8: return InterleavedU8.class;
					case S8: return InterleavedS8.class;
					case U16: return InterleavedU16.class;
					case S16: return InterleavedS16.class;
					case S32: return InterleavedS32.class;
					case S64: return InterleavedS64.class;
					case I8: return InterleavedI8.class;
					case I16: return InterleavedI16.class;
				}
				break;
		}
		throw new RuntimeException("Support this image type thing");
	}

	@Override
	public String toString() {
		return "ImageType( "+family+" "+dataType+" "+numBands+" )";
	}

	public static enum Family
	{
		SINGLE_BAND,
		MULTI_SPECTRAL,
		INTERLEAVED
	}
}
