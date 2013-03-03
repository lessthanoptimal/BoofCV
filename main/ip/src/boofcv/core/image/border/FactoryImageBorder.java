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

package boofcv.core.image.border;

import boofcv.struct.image.*;


/**
 * Contains functions that create classes which handle pixels outside the image border differently.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImageBorder {

	public static <T extends ImageSingleBand> ImageBorder<T> general( T image , BorderType borderType ) {
		ImageBorder<T> ret = general((Class)image.getClass(),borderType);
		ret.setImage(image);
		return ret;
	}

	/**
	 * Given an image type return the appropriate {@link ImageBorder} class type.
	 *
	 * @param imageType Type of image which is being processed.
	 * @return The ImageBorder for processing the image type.
	 */
	public static Class<ImageBorder> lookupBorderClassType( Class<ImageSingleBand> imageType ) {
		if( (Class)imageType == ImageFloat32.class )
			return (Class)ImageBorder1D_F32.class;
		if( (Class)imageType == ImageFloat64.class )
			return (Class)ImageBorder1D_F64.class;
		else if( ImageInteger.class.isAssignableFrom(imageType) )
			return (Class)ImageBorder1D_I32.class;
		else if( (Class)imageType == ImageSInt64.class )
			return (Class)ImageBorder1D_I64.class;
		else
			throw new IllegalArgumentException("Unknown image type");
	}

	/**
	 * Creates an instance of the requested algorithms for handling borders pixels.  For
	 * borders that return the same pixel value always use {@link #value(Class, double)} instead.
	 *
	 * @param imageType Type of image being processed.
	 * @param borderType Which border algorithm should it use.
	 * @return The requested {@link ImageBorder).
	 */
	public static <T extends ImageSingleBand> ImageBorder<T>
	general( Class<T> imageType , BorderType borderType )
	{

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
			return (ImageBorder<T>)new ImageBorder1D_I32((Class)borderClass);
		else if( imageType == ImageSInt64.class )
			return (ImageBorder<T>)new ImageBorder1D_I64(borderClass);
		else
			throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
	}

	/**
	 * Creates an {@link ImageBorder} that returns the specified value always.
	 *
	 * @see ImageBorderValue
	 *
	 * @param image The image the border is being created for.
	 * @param value The value which will be returned.
	 * @return An {@link ImageBorder}
	 */
	public static <T extends ImageSingleBand> ImageBorder<T> value( T image , double value ) {
		ImageBorder border = value(image.getClass(),value);
		border.setImage(image);
		return border;
	}

	/**
	 * Creates an {@link ImageBorder} that returns the specified value always.
	 *
	 * @see ImageBorderValue
	 *
	 * @param imageType The image type the border is being created for.
	 * @param value The value which will be returned.
	 * @return An {@link ImageBorder}
	 */
	public static <T extends ImageSingleBand> ImageBorder<T> value( Class<T> imageType , double value ) {
		if( imageType == ImageFloat32.class ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_F32((float)value);
		} else if( imageType == ImageFloat64.class ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_F64(value);
		} else if( ImageInteger.class.isAssignableFrom(imageType) ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_I((int)value);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
		}
	}
}
