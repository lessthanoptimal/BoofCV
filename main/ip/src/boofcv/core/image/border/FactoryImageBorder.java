/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.core.image.border;

import boofcv.struct.image.*;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImageBorder {

//	public static <T extends ImageBase> ImageBorder<T> extend( T image ) {
//		ImageBorder<T> ret = general(image.getClass(),BorderIndex1D_Extend.class);
//		ret.setImage(image);
//		return ret;
//	}
//
//	public static <T extends ImageBase> ImageBorder<T> reflect( T image ) {
//		ImageBorder<T> ret = general(image.getClass(),BorderIndex1D_Reflect.class);
//		ret.setImage(image);
//		return ret;
//	}
//
//	public static <T extends ImageBase> ImageBorder<T> wrap( T image ) {
//		ImageBorder<T> ret = general(image.getClass(),BorderIndex1D_Wrap.class);
//		ret.setImage(image);
//		return ret;
//	}
//
//	public static ImageBorder extend( Class<?> imageType ) {
//		if( imageType == ImageFloat32.class )
//			return new ImageBorder1D_F32(BorderIndex1D_Extend.class);
//		if( imageType == ImageFloat64.class )
//			return new ImageBorder1D_F64(BorderIndex1D_Extend.class);
//		else if( ImageInteger.class.isAssignableFrom(imageType) )
//			return new ImageBorder1D_I32(BorderIndex1D_Extend.class);
//		else if( imageType == ImageSInt64.class )
//			return new ImageBorder1D_I64(BorderIndex1D_Extend.class);
//		else
//			throw new IllegalArgumentException("Unknown image type");
//	}

	public static <T extends ImageBase> ImageBorder<T> general( T image , BorderType borderType ) {
		ImageBorder<T> ret = general(image.getClass(),borderType);
		ret.setImage(image);
		return ret;
	}

	public static Class<?> lookupBorderClassType( Class<?> imageType ) {
		if( imageType == ImageFloat32.class )
			return ImageBorder1D_F32.class;
		if( imageType == ImageFloat64.class )
			return ImageBorder1D_F64.class;
		else if( ImageInteger.class.isAssignableFrom(imageType) )
			return ImageBorder1D_I32.class;
		else if( imageType == ImageSInt64.class )
			return ImageBorder1D_I64.class;
		else
			throw new IllegalArgumentException("Unknown image type");
	}

	public static <T extends ImageBase> ImageBorder<T> value( Class<?> imageType , double value ) {
		if( imageType == ImageFloat32.class ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_F32((float)value);
		} else if( imageType == ImageFloat64.class ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_F64(value);
		} else if( ImageInteger.class.isAssignableFrom(imageType) ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_I((int)value);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}
	public static <T extends ImageBase> ImageBorder<T> general( Class<?> imageType , BorderType borderType ) {

		Class<?> borderClass;
		switch(borderType) {
			case SKIP:
				borderClass = BorderIndex1D_Exception.class;
				break;

			case NORMALIZED:
				throw new IllegalArgumentException("Normalized can't be supported by this border interface");
			
			case REFLECT:
				borderClass = BorderIndex1D_Reflect.class;
				break;

			case EXTENDED:
				borderClass = BorderIndex1D_Extend.class;
				break;

			case WRAP:
				borderClass = BorderIndex1D_Wrap.class;
				break;

			default:
				throw new IllegalArgumentException("Border type not supported: "+borderType);
		}

		if( imageType == ImageFloat32.class )
			return (ImageBorder<T>)new ImageBorder1D_F32(borderClass);
		if( imageType == ImageFloat64.class )
			return (ImageBorder<T>)new ImageBorder1D_F64(borderClass);
		else if( ImageInteger.class.isAssignableFrom(imageType) )
			return (ImageBorder<T>)new ImageBorder1D_I32(borderClass);
		else if( imageType == ImageSInt64.class )
			return (ImageBorder<T>)new ImageBorder1D_I64(borderClass);
		else
			throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
	}

	public static ImageBorder1D_F64 general( ImageFloat64 image , BorderIndex1D type) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(type,type);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F64 extend( ImageFloat64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F64 reflect( ImageFloat64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F64 wrap( ImageFloat64 image ) {
		ImageBorder1D_F64 ret = new ImageBorder1D_F64(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_F64 value( ImageFloat64 image , double value ) {
		return ImageBorderValue.wrap(image,value);
	}

	public static ImageBorder1D_F32 general( ImageFloat32 image , BorderIndex1D type) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(type,type);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F32 extend( ImageFloat32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F32 reflect( ImageFloat32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_F32 wrap( ImageFloat32 image ) {
		ImageBorder1D_F32 ret = new ImageBorder1D_F32(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_F32 value( ImageFloat32 image , float value ) {
		return ImageBorderValue.wrap(image,value);
	}

	public static ImageBorder1D_I32 general( ImageInteger image , BorderIndex1D type ) {
		ImageBorder1D_I32 ret = new ImageBorder1D_I32(type,type);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_I32 extend( ImageInteger image ) {
		ImageBorder1D_I32 ret = new ImageBorder1D_I32(BorderIndex1D_Extend.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_I32 reflect( ImageInteger image ) {
		ImageBorder1D_I32 ret = new ImageBorder1D_I32(BorderIndex1D_Reflect.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder1D_I32 wrap( ImageInteger image ) {
		ImageBorder1D_I32 ret = new ImageBorder1D_I32(BorderIndex1D_Wrap.class);
		ret.setImage(image);
		return ret;
	}

	public static ImageBorder_I32 value( ImageInteger image , int value ) {
		return ImageBorderValue.wrap(image,value);
	}
}
