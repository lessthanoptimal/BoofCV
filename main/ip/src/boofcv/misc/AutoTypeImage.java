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

package boofcv.misc;

import boofcv.struct.image.*;


/**
 * Image information for auto generated code
 *
 * @author Peter Abeles
 */
public enum AutoTypeImage {
	I("ImageInteger","int",true,0),
	I8("ImageInt8","byte",true,8),
	U8(ImageUInt8.class),
	S8(ImageSInt8.class),
	I16("ImageInt16","short",true,16),
	U16(ImageUInt16.class),
	S16(ImageSInt16.class),
	S32(ImageSInt32.class),
	S64(ImageSInt64.class),
	F32(ImageFloat32.class),
	F64(ImageFloat64.class);

	private String imageSingleName;
	private String dataType;
	private String bitWise;
	private String sumType;
	private String largeSumType;
	private boolean isInteger;
	private boolean isSigned;
	private int numBits;
	private String abbreviatedType;

	private Class<?> primitiveType;

	AutoTypeImage( Class<?> imageType ) {

		imageSingleName = imageType.getSimpleName();
		bitWise = "";
		try {
			ImageSingleBand img = (ImageSingleBand)imageType.newInstance();
			primitiveType = img.getDataType().getDataType();
			dataType = primitiveType.getSimpleName();
			numBits = img.getDataType().getNumBits();

			if( img.getDataType().isInteger() ) {
				isInteger = true;
				if( numBits <= 32 )
					sumType = "int";
				else
					sumType = "long";
				if( numBits <= 16 )
					largeSumType = "int";
				else
					largeSumType = "long";

				if( !img.getDataType().isSigned() ) {
					abbreviatedType = "U";
					isSigned = false;
					if( byte.class == primitiveType) {
						bitWise = "& 0xFF";
					} else if( short.class == primitiveType) {
						bitWise = "& 0xFFFF";
					}
				} else {
					abbreviatedType = "S";
					isSigned = true;
				}

			} else {
				abbreviatedType = "F";
				isSigned = true;
				isInteger = false;
				if( imageType == ImageFloat32.class ) {
					sumType = "float";
				} else {
					sumType = "double";
				}
				largeSumType = "double";
			}

			abbreviatedType += numBits;

		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	AutoTypeImage(String imageSingleName, String dataType, boolean isInteger , int numBits ) {
		this.imageSingleName = imageSingleName;
		this.dataType = dataType;
		this.isInteger = isInteger;
		this.numBits = numBits;

		if( isInteger ) {
			this.abbreviatedType = "I";
			this.sumType = "int";
		} else {
			this.sumType = "double";
		}
		abbreviatedType += numBits;

	}

	public static AutoTypeImage[] getIntegerTypes() {
		return new AutoTypeImage[]{U8,S8,U16,S16,S32,S64};
	}

	public static AutoTypeImage[] getFloatingTypes() {
		return new AutoTypeImage[]{F32,F64};
	}

	public static AutoTypeImage[] getGenericTypes() {
		return new AutoTypeImage[]{I8,I16,S32,S64,F32,F64};
	}

	public static AutoTypeImage[] getReallyGenericTypes() {
		return new AutoTypeImage[]{I,S64,F32,F64};
	}

	public static AutoTypeImage[] getSpecificTypes() {
		return new AutoTypeImage[]{U8,S8,U16,S16,S32,S64,F32,F64};
	}

	public static AutoTypeImage[] getSigned() {
		return new AutoTypeImage[]{S8,S16,S32,S64,F32,F64};
	}

	public static AutoTypeImage[] getUnsigned() {
		return new AutoTypeImage[]{U8,U16};
	}

	public String getInterleavedName() {
		return "Interleaved"+toString();
	}

	public String getSingleBandName() {
		return imageSingleName;
	}

	public String getDataType() {
		return dataType;
	}

	public String getBitWise() {
		return bitWise;
	}

	public String getSumType() {
		return sumType;
	}

	public String getLargeSumType() {
		return largeSumType;
	}

	public boolean isInteger() {
		return isInteger;
	}

	public boolean isSigned() {
		return isSigned;
	}

	public int getNumBits() {
		return numBits;
	}

	public Class<?> getPrimitiveType() {
		return primitiveType;
	}

	public String getTypeCastFromSum() {
		if( sumType.compareTo(dataType) != 0 )
			return "("+dataType+")";
		else
			return "";
	}

	public String getAbbreviatedType() {
		return abbreviatedType;
	}

	public String getRandType() {
		return primitiveType == float.class ? "Float" : "Double";
	}

	public Number getMax() {
		if( isInteger ) {
			if( byte.class == primitiveType) {
				if( isSigned ) {
					return Byte.MAX_VALUE;
				} else {
					return 0xFF;
				}
			} else if( short.class == primitiveType) {
				if( isSigned ) {
					return Short.MAX_VALUE;
				} else {
					return 0xFFFF;
				}
			} else {
				return Integer.MAX_VALUE;
			}
		} else if( float.class == primitiveType) {
			return Float.MAX_VALUE;
		} else {
			return Double.MAX_VALUE;
		}
	}

	public Number getMin() {
		if( isInteger ) {
			if( byte.class == primitiveType) {
				if( isSigned ) {
					return Byte.MIN_VALUE;
				} else {
					return 0;
				}
			} else if( short.class == primitiveType) {
				if( isSigned ) {
					return Short.MIN_VALUE;
				} else {
					return 0;
				}
			} else {
				return Integer.MIN_VALUE;
			}
		} else if( float.class == primitiveType) {
			return Float.MIN_VALUE;
		} else {
			return Double.MIN_VALUE;
		}
	}
}
