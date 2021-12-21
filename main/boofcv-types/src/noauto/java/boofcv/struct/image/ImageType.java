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

import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * Specifies the type of image data structure.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"UnnecessaryParentheses"})
public class ImageType<T extends ImageBase> implements Serializable {
	// Short hand predefined image types
	public static final ImageType<GrayU8> SB_U8 = ImageType.single(GrayU8.class);
	public static final ImageType<GrayS8> SB_S8 = ImageType.single(GrayS8.class);
	public static final ImageType<GrayU16> SB_U16 = ImageType.single(GrayU16.class);
	public static final ImageType<GrayS16> SB_S16 = ImageType.single(GrayS16.class);
	public static final ImageType<GrayS32> SB_S32 = ImageType.single(GrayS32.class);
	public static final ImageType<GrayS64> SB_S64 = ImageType.single(GrayS64.class);
	public static final ImageType<GrayF32> SB_F32 = ImageType.single(GrayF32.class);
	public static final ImageType<GrayF64> SB_F64 = ImageType.single(GrayF64.class);
	public static final ImageType<InterleavedU8> IL_U8 = ImageType.il(0, InterleavedU8.class);
	public static final ImageType<InterleavedS8> IL_S8 = ImageType.il(0, InterleavedS8.class);
	public static final ImageType<InterleavedU16> IL_U16 = ImageType.il(0, InterleavedU16.class);
	public static final ImageType<InterleavedS16> IL_S16 = ImageType.il(0, InterleavedS16.class);
	public static final ImageType<InterleavedS32> IL_S32 = ImageType.il(0, InterleavedS32.class);
	public static final ImageType<InterleavedS64> IL_S64 = ImageType.il(0, InterleavedS64.class);
	public static final ImageType<InterleavedF32> IL_F32 = ImageType.il(0, InterleavedF32.class);
	public static final ImageType<InterleavedF64> IL_F64 = ImageType.il(0, InterleavedF64.class);
	public static final ImageType<Planar<GrayU8>> PL_U8 = ImageType.pl(0, GrayU8.class);
	public static final ImageType<Planar<GrayS8>> PL_S8 = ImageType.pl(0, GrayS8.class);
	public static final ImageType<Planar<GrayU16>> PL_U16 = ImageType.pl(0, GrayU16.class);
	public static final ImageType<Planar<GrayS16>> PL_S16 = ImageType.pl(0, GrayS16.class);
	public static final ImageType<Planar<GrayS32>> PL_S32 = ImageType.pl(0, GrayS32.class);
	public static final ImageType<Planar<GrayS64>> PL_S64 = ImageType.pl(0, GrayS64.class);
	public static final ImageType<Planar<GrayF32>> PL_F32 = ImageType.pl(0, GrayF32.class);
	public static final ImageType<Planar<GrayF64>> PL_F64 = ImageType.pl(0, GrayF64.class);

	/**
	 * Specifies the image data structure
	 */
	Family family;
	/**
	 * Specifies the type of data used to store pixel information
	 */
	ImageDataType dataType;
	/**
	 * Number of bands in the image. Single band images ignore this field.
	 */
	public int numBands;

	public ImageType( Family family, ImageDataType dataType, int numBands ) {
		this.family = family;
		this.dataType = dataType;
		this.numBands = numBands;
	}

	/**
	 * Create an image type with default values
	 */
	protected ImageType() {
		family = Family.GRAY;
		dataType = ImageDataType.U8;
		numBands = 1;
	}

	public static <I extends ImageGray<I>> ImageType<I> single( Class<I> imageType ) {
		return new ImageType<>(Family.GRAY, ImageDataType.classToType(imageType), 1);
	}

	public static <I extends ImageGray<I>> ImageType<I> single( ImageDataType type ) {
		return new ImageType<>(Family.GRAY, type, 1);
	}

	public static <I extends ImageGray<I>> ImageType<Planar<I>> pl( int numBands, Class<I> imageType ) {
		return new ImageType<>(Family.PLANAR, ImageDataType.classToType(imageType), numBands);
	}

	public static <I extends ImageGray<I>> ImageType<Planar<I>> pl( int numBands, ImageDataType type ) {
		return new ImageType<>(Family.PLANAR, type, numBands);
	}

	public static <I extends ImageInterleaved<I>> ImageType<I> il( int numBands, Class<I> imageType ) {
		return new ImageType<>(Family.INTERLEAVED, ImageDataType.classToType(imageType), numBands);
	}

	public static <I extends ImageInterleaved<I>> ImageType<I> il( int numBands, ImageDataType type ) {
		return new ImageType<>(Family.INTERLEAVED, type, numBands);
	}

	/**
	 * Converts the short hand string into an image type. A new instance is returned.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ImageBase> ImageType<T> stringToType( String name, int numBands ) {
		ImageType type = new ImageType();
		type.setTo(switch (name) {
			case "SB_U8" -> SB_U8;
			case "SB_S8" -> SB_S8;
			case "SB_S16" -> SB_S16;
			case "SB_U16" -> SB_U16;
			case "SB_S32" -> SB_S32;
			case "SB_S64" -> SB_S64;
			case "SB_F32" -> SB_F32;
			case "SB_F64" -> SB_F64;
			case "IL_U8" -> IL_U8;
			case "IL_S8" -> IL_S8;
			case "IL_S16" -> IL_S16;
			case "IL_U16" -> IL_U16;
			case "IL_S32" -> IL_S32;
			case "IL_S64" -> IL_S64;
			case "IL_F32" -> IL_F32;
			case "IL_F64" -> IL_F64;
			case "PL_U8" -> PL_U8;
			case "PL_S8" -> PL_S8;
			case "PL_S16" -> PL_S16;
			case "PL_U16" -> PL_U16;
			case "PL_S32" -> PL_S32;
			case "PL_S64" -> PL_S64;
			case "PL_F32" -> PL_F32;
			case "PL_F64" -> PL_F64;
			default -> throw new RuntimeException("Unknown " + name);
		});
		type.numBands = numBands;
		return type;
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
	public T createImage( int width, int height ) {
		return switch (family) {
			case GRAY -> (T)ImageGray.create(getImageClass(), width, height);
			case INTERLEAVED -> (T)ImageInterleaved.create(getImageClass(), width, height, numBands);
			case PLANAR -> (T)new Planar(getImageClass(), width, height, numBands);
			default -> throw new IllegalArgumentException("Type not yet supported");
		};
	}

	/**
	 * Creates an array of the specified iamge type
	 *
	 * @param length Number of elements in the array
	 * @return array of image type
	 */
	public T[] createArray( int length ) {
		return switch (family) {
			case GRAY, INTERLEAVED -> (T[])Array.newInstance(getImageClass(), length);
			case PLANAR -> (T[])new Planar[length];
			default -> throw new IllegalArgumentException("Type not yet supported");
		};
	}

	public int getNumBands() {
		return numBands;
	}

	public Family getFamily() {
		return family;
	}

	public Class getImageClass() {
		return getImageClass(family, dataType);
	}

	public static Class getImageClass( Family family, ImageDataType dataType ) {
		return switch (family) {
			case GRAY, PLANAR -> switch (dataType) {
				case F32 -> GrayF32.class;
				case F64 -> GrayF64.class;
				case U8 -> GrayU8.class;
				case S8 -> GrayS8.class;
				case U16 -> GrayU16.class;
				case S16 -> GrayS16.class;
				case S32 -> GrayS32.class;
				case S64 -> GrayS64.class;
				case I8 -> GrayI8.class;
				case I16 -> GrayI16.class;
				default -> throw new RuntimeException("Support this image type thing");
			};
			case INTERLEAVED -> switch (dataType) {
				case F32 -> InterleavedF32.class;
				case F64 -> InterleavedF64.class;
				case U8 -> InterleavedU8.class;
				case S8 -> InterleavedS8.class;
				case U16 -> InterleavedU16.class;
				case S16 -> InterleavedS16.class;
				case S32 -> InterleavedS32.class;
				case S64 -> InterleavedS64.class;
				case I8 -> InterleavedI8.class;
				case I16 -> InterleavedI16.class;
				default -> throw new RuntimeException("Support this image type thing");
			};
		};
	}

	@Override
	public String toString() {
		return "ImageType( " + family + " " + dataType + " " + numBands + " )";
	}

	/**
	 * Returns true if the passed in ImageType is the same as this image type
	 */
	public boolean isSameType( ImageType o ) {
		if (family != o.family)
			return false;
		if (dataType != o.dataType)
			return false;
		if (numBands != o.numBands)
			return false;
		return true;
	}

	/**
	 * Sets 'this' to be identical to 'o'
	 *
	 * @param o What is to be copied.
	 */
	public void setTo( ImageType o ) {
		this.family = o.family;
		this.dataType = o.dataType;
		this.numBands = o.numBands;
	}

	public enum Family {
		GRAY,
		PLANAR,
		INTERLEAVED
	}
}
