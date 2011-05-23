/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.testing;

import gecv.struct.image.*;

import java.util.List;


/**
 * Image information for auto generated code
 *
 * @author Peter Abeles
 */
public enum TypeImage {
	I8("ImageInt8","byte",true),
	U8(ImageUInt8.class),
	S8(ImageSInt8.class),
	I16("ImageInt16","short",true),
	U16(ImageUInt16.class),
	S16(ImageSInt16.class),
	S32(ImageSInt32.class),
	F32(ImageFloat32.class);

	private String imageName;
	private String dataType;
	private String bitWise;
	private String sumType;
	private boolean isInteger;
	private boolean isSigned;

	private Class<?> primativeType;

	TypeImage( Class<?> imageType ) {

		imageName = imageType.getSimpleName();
		bitWise = "";
		try {
			ImageBase img = (ImageBase)imageType.newInstance();
			primativeType = img._getPrimitiveType();
			dataType = primativeType.getSimpleName();
			if( ImageInteger.class.isAssignableFrom(imageType)) {
				isInteger = true;
				sumType = "int";
				if( !((ImageInteger)img).isSigned() ) {
					isSigned = false;
					if( byte.class == primativeType ) {
						bitWise = "& 0xFF";
					} else if( short.class == primativeType ) {
						bitWise = "& 0xFFFF";
					}
				} else {
					isSigned = true;
				}
			} else {
				isSigned = true;
				isInteger = false;
				sumType = "float";
			}

		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	TypeImage(String imageName, String dataType, boolean isInteger ) {
		this.imageName = imageName;
		this.dataType = dataType;
		this.isInteger = isInteger;
		if( isInteger )
			this.sumType = "int";
		else
			this.sumType = "float";

	}

	public static TypeImage[] getIntegerTypes() {
		return new TypeImage[]{U8,S8,U16,S16,S32};
	}

	public static TypeImage[] getGenericTypes() {
		return new TypeImage[]{I8,I16,S32,F32};
	}

	public static TypeImage[] getSpecificTypes() {
		return new TypeImage[]{U8,S8,U16,S16,S32,F32};
	}

	public static TypeImage[] getSigned() {
		return new TypeImage[]{S8,S16,S32,F32};
	}

	public static TypeImage[] getUnsigned() {
		return new TypeImage[]{U8,U16};
	}

	public String getImageName() {
		return imageName;
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

	public boolean isInteger() {
		return isInteger;
	}

	public boolean isSigned() {
		return isSigned;
	}

	public Class<?> getPrimativeType() {
		return primativeType;
	}

	public String getTypeCastFromSum() {
		if( sumType.compareTo(dataType) != 0 )
			return "("+dataType+")";
		else
			return "";
	}

	public Number getMax() {
		if( isInteger ) {
			if( byte.class == primativeType ) {
				if( isSigned ) {
					return Byte.MAX_VALUE;
				} else {
					return 0xFF;
				}
			} else if( short.class == primativeType ) {
				if( isSigned ) {
					return Short.MAX_VALUE;
				} else {
					return 0xFFFF;
				}
			} else {
				return Integer.MAX_VALUE;
			}
		} else {
			return Float.MAX_VALUE;
		}
	}

	public Number getMin() {
		if( isInteger ) {
			if( byte.class == primativeType ) {
				if( isSigned ) {
					return Byte.MIN_VALUE;
				} else {
					return 0;
				}
			} else if( short.class == primativeType ) {
				if( isSigned ) {
					return Short.MIN_VALUE;
				} else {
					return 0;
				}
			} else {
				return Integer.MIN_VALUE;
			}
		} else {
			return Float.MIN_VALUE;
		}
	}
}
