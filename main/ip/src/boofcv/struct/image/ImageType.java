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

import boofcv.core.image.GeneralizedImageOps;

import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * Specifies the type of image data structure.
 *
 * @author Peter Abeles
 */
public class ImageType<T extends ImageBase> implements Serializable {

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

	public static <I extends ImageGray> ImageType<I> single(Class<I> imageType ) {
		return new ImageType<>(Family.GRAY, ImageDataType.classToType(imageType), 1);
	}

	public static <I extends ImageGray> ImageType<I> single(ImageDataType type ) {
		return new ImageType<>(Family.GRAY, type, 1);
	}

	public static <I extends ImageGray> ImageType<Planar<I>> pl(int numBands , Class<I> imageType ) {
		return new ImageType<>(Family.PLANAR, ImageDataType.classToType(imageType), numBands);
	}

	public static <I extends ImageGray> ImageType<Planar<I>> pl(int numBands , ImageDataType type ) {
		return new ImageType<>(Family.PLANAR, type, numBands);
	}

	public static <I extends ImageInterleaved> ImageType<I> il(int numBands, Class<I> imageType) {
		return new ImageType<>(Family.INTERLEAVED, ImageDataType.classToType(imageType), numBands);
	}

	public static <I extends ImageInterleaved> ImageType<I> il(int numBands, ImageDataType type) {
		return new ImageType<>(Family.INTERLEAVED, type, numBands);
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
			case GRAY:
				return (T)GeneralizedImageOps.createSingleBand(getImageClass(),width,height);

			case INTERLEAVED:
				return (T)GeneralizedImageOps.createInterleaved(getImageClass(), width, height, numBands);

			case PLANAR:
				return (T)new Planar(getImageClass(),width,height,numBands);

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
			case GRAY:
			case INTERLEAVED:
				return (T[])Array.newInstance(getImageClass(),length);

			case PLANAR:
				return (T[])new Planar[ length ];

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
			case GRAY:
			case PLANAR:
				switch( dataType ) {
					case F32: return GrayF32.class;
					case F64: return GrayF64.class;
					case U8: return GrayU8.class;
					case S8: return GrayS8.class;
					case U16: return GrayU16.class;
					case S16: return GrayS16.class;
					case S32: return GrayS32.class;
					case S64: return GrayS64.class;
					case I8: return GrayI8.class;
					case I16: return GrayI16.class;
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
		GRAY,
		PLANAR,
		INTERLEAVED
	}
}
