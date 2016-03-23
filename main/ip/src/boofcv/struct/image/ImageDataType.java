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


/**
 * Describes the physical characteristics of the internal primitive data types inside the image
 *
 * @author Peter Abeles
 */
public enum ImageDataType {
	/** Unsigned 8-bit image */
	U8(false,byte.class),
	/** Signed 8-bit image */
	S8(true,byte.class),
	/** Unsigned 16-bit image */
	U16(false,short.class),
	/** Signed 16-bit integer image */
	S16(true,short.class),
	/** Signed 32-bit integer image */
	S32(true,int.class),
	/** Signed 64-bit integer image */
	S64(true,long.class),
	/** 32-bit floating point image */
	F32(true,float.class),
	/** 64-bit floating point image */
	F64(true,double.class),
	/** 8-bit integer image */
	I8(byte.class),
	/** 16-bit integer image */
	I16(short.class),
	/** Integer image */
	I(true),
	/** floating point image */
	F(true);

	private int numBits;
	private boolean isAbstract;
	private boolean isSigned;
	private boolean isInteger;
	private double maxValue;
	private double minValue;
	private Class dataType;
	private Class sumType;

	public static ImageDataType classToType( Class imageClass ) {
		if( imageClass == GrayU8.class )
			return U8;
		else if( imageClass == GrayS8.class )
			return S8;
		else if( imageClass == GrayU16.class )
			return U16;
		else if( imageClass == GrayS16.class )
			return S16;
		else if( imageClass == GrayS32.class )
			return S32;
		else if( imageClass == GrayS64.class )
			return S64;
		else if( imageClass == GrayF32.class )
			return F32;
		else if( imageClass == GrayF64.class )
			return F64;
		else if( imageClass == GrayI8.class )
			return I8;
		else if( imageClass == GrayI16.class )
			return I16;
		else if( imageClass == GrayI.class )
			return I;
		else if( imageClass == GrayF.class )
			return F;
		else if( imageClass == InterleavedU8.class )
			return U8;
		else if( imageClass == InterleavedS8.class )
			return S8;
		else if( imageClass == InterleavedU16.class )
			return U16;
		else if( imageClass == InterleavedS16.class )
			return S16;
		else if( imageClass == InterleavedS32.class )
			return S32;
		else if( imageClass == InterleavedS64.class )
			return S64;
		else if( imageClass == InterleavedF32.class )
			return F32;
		else if( imageClass == InterleavedF64.class )
			return F64;
		else if( imageClass == InterleavedI8.class )
			return I8;
		else if( imageClass == InterleavedI16.class )
			return I16;
		else
			return null;
	}

	public static Class typeToSingleClass(ImageDataType type) {
		switch (type) {
			case U8:
				return GrayU8.class;
			case S8:
				return GrayS8.class;
			case U16:
				return GrayU16.class;
			case S16:
				return GrayS16.class;
			case S32:
				return GrayS32.class;
			case S64:
				return GrayS64.class;
			case F32:
				return GrayF32.class;
			case F64:
				return GrayF64.class;
			case I8:
				return GrayI8.class;
			case I16:
				return GrayI16.class;
			case I:
				return GrayI.class;
			case F:
				return GrayF.class;
		}

		throw new RuntimeException("Add");
	}

	public static Class typeToInterleavedClass(ImageDataType type) {
		switch (type) {
			case U8:
				return InterleavedU8.class;
			case S8:
				return InterleavedS8.class;
			case U16:
				return InterleavedU16.class;
			case S16:
				return InterleavedS16.class;
			case S32:
				return InterleavedS32.class;
			case S64:
				return InterleavedS64.class;
			case F32:
				return InterleavedF32.class;
			case F64:
				return InterleavedF64.class;
			case I8:
				return InterleavedI8.class;
			case I16:
				return InterleavedI16.class;
		}

		throw new RuntimeException("Add");
	}

	ImageDataType(boolean isInteger) {
		this.isAbstract = true;
		this.isInteger = isInteger;
	}

	ImageDataType(Class dataType) {
		this.isAbstract = true;
		this.dataType = dataType;
		configureByDataType(dataType);
	}

	ImageDataType(boolean isSigned, Class<?> dataType) {
		this.isAbstract = false;
		this.isSigned = isSigned;
		this.dataType = dataType;

		configureByDataType(dataType);
	}

	private void configureByDataType(Class<?> dataType ) {
		if( dataType == float.class || dataType == double.class ) {
			sumType = dataType;
			isInteger = false;
			if( dataType == float.class )
				numBits = 32;
			else
				numBits = 64;
		} else {
			isInteger = true;
			if( dataType == byte.class )
				numBits = 8;
			else if( dataType == short.class )
				numBits = 16;
			else if( dataType == int.class )
				numBits = 32;
			else if( dataType == long.class )
				numBits = 64;

			if( numBits <= 32 )
				sumType = int.class;
			else
				sumType = long.class;
		}

		configureMinMaxValues();
	}

	private void configureMinMaxValues() {
		if( isInteger ) {
			switch( numBits ) {
				case 8:
					minValue = Byte.MIN_VALUE;
					maxValue = Byte.MAX_VALUE;
					break;

				case 16:
					minValue = Short.MIN_VALUE;
					maxValue = Short.MAX_VALUE;
					break;

				case 32:
					minValue = Integer.MIN_VALUE;
					maxValue = Integer.MAX_VALUE;
					break;

				case 64:
					minValue = Long.MIN_VALUE;
					maxValue = Long.MAX_VALUE;
					break;
			}
		} else {
			switch( numBits ) {
				case 32:
					minValue = Float.MIN_VALUE;
					maxValue = Float.MAX_VALUE;
					break;

				case 64:
					minValue = Double.MIN_VALUE;
					maxValue = Double.MAX_VALUE;
					break;
			}
		}

		if( !isSigned ) {
			maxValue += -minValue;
			minValue = 0;
		}
	}

	/**
	 * Number of bits per pixel in the image.
	 */
	public int getNumBits() {
		return numBits;
	}

	/**
	 * If the image is an abstract data type or not.
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * If the image has a signed or unsigned data type.
	 */
	public boolean isSigned() {
		return isSigned;
	}

	/**
	 * If each pixel is an integer or not.
	 */
	public boolean isInteger() {
		return isInteger;
	}

	/**
	 * The primitive data type used by each pixel.
	 */
	public Class getDataType() {
		return dataType;
	}

	/**
	 * Type of data used when summing elements in the image.
	 */
	public Class getSumType() {
		return sumType;
	}

	/**
	 * Returns the maximum allowed value for data elements in this data type
	 */
	public double getMaxValue() {
		return maxValue;
	}

	/**
	 * Returns the minimum allowed value for data elements in this data type
	 */
	public double getMinValue() {
		return maxValue;
	}
}
