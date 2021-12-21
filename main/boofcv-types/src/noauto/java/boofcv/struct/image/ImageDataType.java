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

/**
 * Describes the physical characteristics of the internal primitive data types inside the image
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ImmutableEnumChecker", "UnnecessaryParentheses", "NullAway.Init", "rawtypes"})
// UnnecessaryParentheses is probably a bug
public enum ImageDataType {
	/** Unsigned 8-bit image */
	U8(false, byte.class, byte[]::new),
	/** Signed 8-bit image */
	S8(true, byte.class, byte[]::new),
	/** Unsigned 16-bit image */
	U16(false, short.class, short[]::new),
	/** Signed 16-bit integer image */
	S16(true, short.class, short[]::new),
	/** Signed 32-bit integer image */
	S32(true, int.class, int[]::new),
	/** Signed 64-bit integer image */
	S64(true, long.class, long[]::new),
	/** 32-bit floating point image */
	F32(true, float.class, float[]::new),
	/** 64-bit floating point image */
	F64(true, double.class, double[]::new),
	/** 8-bit integer image */
	I8(byte.class),
	/** 16-bit integer image */
	I16(short.class),
	/** Integer image */
	I(true),
	/** floating point image */
	F(true);

	/** Number of bits per pixel in the image. */
	private @Getter final int numBits;
	/** If the image is an abstract data type or not. */
	private @Getter final boolean isAbstract;
	/** If the image has a signed or unsigned data type. */
	private @Getter final boolean isSigned;
	/** If each pixel is an integer or not. */
	private @Getter final boolean isInteger;
	/** maximum allowed value for data elements in this data type */
	private @Getter final double maxValue;
	/** Returns the minimum allowed value for data elements in this data type */
	private @Getter final double minValue;
	/** The primitive data type used by each pixel. */
	private @Getter final Class dataType;
	/** Type of data used when summing elements in the image. */
	private @Getter final Class sumType;
	/** Creates an array of type 'dataType' */
	private @Getter final CreateArray createArray;

	// WARNING: DO NOT ADD CHANGE Class -> Class<?> this will cause headaches in situations where generics are used

	ImageDataType( boolean isInteger ) {
		this.isAbstract = true;
		this.isInteger = isInteger;
		this.isSigned = false; // Arbitrary value. Not applicable
		this.numBits = -1;
		this.dataType = Object.class;
		this.sumType = Object.class;
		this.maxValue = this.minValue = Double.NaN;
		this.createArray = ( i ) -> {throw new RuntimeException("abstract type, no array");};
	}

	ImageDataType( Class dataType ) {
		this.isAbstract = true;
		this.dataType = dataType;
		this.isSigned = false; // Arbitrary value. Not applicable
		this.numBits = selectNumBits(dataType);
		this.isInteger = selectInteger(dataType);
		this.sumType = selectSumType(dataType);
		this.createArray = ( i ) -> {throw new RuntimeException("abstract type, no array");};
		this.maxValue = this.minValue = Double.NaN;
	}

	ImageDataType( boolean isSigned, Class dataType, CreateArray createArray ) {
		this.isAbstract = false;
		this.isSigned = isSigned;
		this.dataType = dataType;
		this.createArray = createArray;
		this.numBits = selectNumBits(dataType);
		this.isInteger = selectInteger(dataType);
		this.sumType = selectSumType(dataType);
		this.minValue = selectMinValue();
		this.maxValue = selectMaxValue();
	}

	@SuppressWarnings("unchecked")
	public static ImageDataType classToType( Class imageClass ) {
		// @formatter:off
		if      (GrayU8.class.isAssignableFrom(imageClass))         return U8;
		else if (GrayS8.class.isAssignableFrom(imageClass))         return S8;
		else if (GrayU16.class.isAssignableFrom(imageClass))        return U16;
		else if (GrayS16.class.isAssignableFrom(imageClass))        return S16;
		else if (GrayS32.class.isAssignableFrom(imageClass))        return S32;
		else if (GrayS64.class.isAssignableFrom(imageClass))        return S64;
		else if (GrayF32.class.isAssignableFrom(imageClass))        return F32;
		else if (GrayF64.class.isAssignableFrom(imageClass))        return F64;
		else if (GrayI8.class.isAssignableFrom(imageClass))         return I8;
		else if (GrayI16.class.isAssignableFrom(imageClass))        return I16;
		else if (GrayI.class.isAssignableFrom(imageClass))          return I;
		else if (GrayF.class.isAssignableFrom(imageClass))          return F;
		else if (InterleavedU8.class.isAssignableFrom(imageClass))  return U8;
		else if (InterleavedS8.class.isAssignableFrom(imageClass))  return S8;
		else if (InterleavedU16.class.isAssignableFrom(imageClass)) return U16;
		else if (InterleavedS16.class.isAssignableFrom(imageClass)) return S16;
		else if (InterleavedS32.class.isAssignableFrom(imageClass)) return S32;
		else if (InterleavedS64.class.isAssignableFrom(imageClass)) return S64;
		else if (InterleavedF32.class.isAssignableFrom(imageClass)) return F32;
		else if (InterleavedF64.class.isAssignableFrom(imageClass)) return F64;
		else if (InterleavedI8.class.isAssignableFrom(imageClass))  return I8;
		else if (InterleavedI16.class.isAssignableFrom(imageClass)) return I16;
		else
			throw new IllegalArgumentException("Unknown image type. class="+imageClass.getCanonicalName());
		// @formatter:on
	}

	public static Class typeToSingleClass( ImageDataType type ) {
		return switch (type) {
			case U8 -> GrayU8.class;
			case S8 -> GrayS8.class;
			case U16 -> GrayU16.class;
			case S16 -> GrayS16.class;
			case S32 -> GrayS32.class;
			case S64 -> GrayS64.class;
			case F32 -> GrayF32.class;
			case F64 -> GrayF64.class;
			case I8 -> GrayI8.class;
			case I16 -> GrayI16.class;
			case I -> GrayI.class;
			case F -> GrayF.class;
		};
	}

	public static Class typeToInterleavedClass( ImageDataType type ) {
		return switch (type) {
			case U8 -> InterleavedU8.class;
			case S8 -> InterleavedS8.class;
			case U16 -> InterleavedU16.class;
			case S16 -> InterleavedS16.class;
			case S32 -> InterleavedS32.class;
			case S64 -> InterleavedS64.class;
			case F32 -> InterleavedF32.class;
			case F64 -> InterleavedF64.class;
			case I8 -> InterleavedI8.class;
			case I16 -> InterleavedI16.class;
			default -> throw new RuntimeException("Add");
		};
	}

	private int selectNumBits( Class dataType ) {
		if (dataType == float.class || dataType == double.class) {
			if (dataType == float.class)
				return 32;
			else
				return 64;
		} else {
			if (dataType == byte.class)
				return 8;
			else if (dataType == short.class)
				return 16;
			else if (dataType == int.class)
				return 32;
			else if (dataType == long.class)
				return 64;
		}
		return -1;
	}

	private boolean selectInteger( Class dataType ) {
		return !(dataType == float.class || dataType == double.class);
	}

	private Class selectSumType( Class dataType ) {
		if (dataType == float.class || dataType == double.class) {
			return dataType;
		} else {
			if (numBits <= 32)
				return int.class;
			else
				return long.class;
		}
	}

	private double selectMinValue() {
		if (isInteger && !isSigned)
			return 0;
		if (isInteger) {
			return switch (numBits) {
				case 8 -> Byte.MIN_VALUE;
				case 16 -> Short.MIN_VALUE;
				case 32 -> Integer.MIN_VALUE;
				case 64 -> Long.MIN_VALUE;
				default -> throw new RuntimeException("Unexpected number of bits for integer type: " + numBits);
			};
		} else {
			return switch (numBits) {
				case 32 -> -Float.MAX_VALUE;
				case 64 -> -Double.MAX_VALUE;
				default -> throw new RuntimeException("Unexpected number of bits for float type: " + numBits);
			};
		}
	}

	private double selectMaxValue() {
		if (isInteger) {
			return switch (numBits) {
				case 8 -> Byte.MAX_VALUE - (double)(isSigned ? 0 : Byte.MIN_VALUE);
				case 16 -> Short.MAX_VALUE - (double)(isSigned ? 0 : Short.MIN_VALUE);
				case 32 -> Integer.MAX_VALUE - (double)(isSigned ? 0 : Integer.MIN_VALUE);
				case 64 -> Long.MAX_VALUE - (double)(isSigned ? 0 : Long.MIN_VALUE);
				default -> throw new RuntimeException("Unexpected number of bits for integer type: " + numBits);
			};
		} else {
			return switch (numBits) {
				case 32 -> Float.MAX_VALUE;
				case 64 -> Double.MAX_VALUE;
				default -> throw new RuntimeException("Unexpected number of bits for float type: " + numBits);
			};
		}
	}

	public <T> T newArray( int length ) {
		return (T)createArray.create(length);
	}

	/** Creates an array of this primitive type */
	private interface CreateArray {
		Object create( int length );
	}
}
