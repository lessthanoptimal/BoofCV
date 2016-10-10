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

package boofcv.core.image.border;

import boofcv.struct.image.*;


/**
 * Contains functions that create classes which handle pixels outside the image border differently.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryImageBorder {

	public static <T extends ImageGray> ImageBorder<T> single(T image, BorderType borderType) {
		ImageBorder<T> ret = single((Class) image.getClass(), borderType);
		ret.setImage(image);
		return ret;
	}

	public static <T extends ImageInterleaved> ImageBorder<T> interleaved(T image, BorderType borderType) {
		ImageBorder<T> ret = interleaved((Class) image.getClass(), borderType);
		ret.setImage(image);
		return ret;
	}

	/**
	 * Given an image type return the appropriate {@link ImageBorder} class type.
	 *
	 * @param imageType Type of image which is being processed.
	 * @return The ImageBorder for processing the image type.
	 */
	public static Class<ImageBorder> lookupBorderClassType( Class<ImageGray> imageType ) {
		if( (Class)imageType == GrayF32.class )
			return (Class)ImageBorder1D_F32.class;
		if( (Class)imageType == GrayF64.class )
			return (Class)ImageBorder1D_F64.class;
		else if( GrayI.class.isAssignableFrom(imageType) )
			return (Class)ImageBorder1D_S32.class;
		else if( (Class)imageType == GrayS64.class )
			return (Class)ImageBorder1D_S64.class;
		else
			throw new IllegalArgumentException("Unknown image type");
	}

	public static <T extends ImageBase> ImageBorder<T>
	generic( BorderType borderType, ImageType<T> imageType ) {
		switch( imageType.getFamily() ) {
			case GRAY:
				return single(imageType.getImageClass(),borderType);

			case PLANAR:
				return single(imageType.getImageClass(),borderType);

			case INTERLEAVED:
				return interleaved(imageType.getImageClass(),borderType);

			default:
				throw new IllegalArgumentException("Unknown family");
		}
	}

	public static <T extends ImageBase> ImageBorder<T>
	genericValue( double value, ImageType<T> imageType ) {
		switch( imageType.getFamily() ) {
			case GRAY:
				return singleValue(imageType.getImageClass(), value);

			case PLANAR:
				return singleValue(imageType.getImageClass(),value);

			case INTERLEAVED:
				return interleavedValue(imageType.getImageClass(),value);

			default:
				throw new IllegalArgumentException("Unknown family");
		}
	}

	/**
	 * Creates an instance of the requested algorithms for handling borders pixels on {@link ImageGray}.  If type
	 * {@link BorderType#ZERO} is passed in then the value will be set to 0.  Alternatively you could
	 * use {@link #singleValue(Class, double)} instead.
	 *
	 * @param imageType Type of image being processed.
	 * @param borderType Which border algorithm should it use.
	 * @return The requested {@link ImageBorder}.
	 */
	public static <T extends ImageGray> ImageBorder<T>
	single(Class<T> imageType, BorderType borderType)
	{
		Class<?> borderClass;
		switch(borderType) {
			case SKIP:
				throw new IllegalArgumentException("Skip border can't be implemented here and has to be done " +
						"externally.  Call this might be a bug. Instead pass in EXTENDED and manually skip over the " +
						"pixel in a loop some place.");
//				borderClass = BorderIndex1D_Exception.class;
//				break;

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

			case ZERO:
				return FactoryImageBorder.singleValue(imageType, 0);

			default:
				throw new IllegalArgumentException("Border type not supported: "+borderType);
		}

		if( imageType == GrayF32.class )
			return (ImageBorder<T>)new ImageBorder1D_F32(borderClass);
		if( imageType == GrayF64.class )
			return (ImageBorder<T>)new ImageBorder1D_F64(borderClass);
		else if( GrayI.class.isAssignableFrom(imageType) )
			return (ImageBorder<T>)new ImageBorder1D_S32((Class)borderClass);
		else if( imageType == GrayS64.class )
			return (ImageBorder<T>)new ImageBorder1D_S64(borderClass);
		else
			throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
	}

	/**
	 * Creates an instance of the requested algorithms for handling borders pixels on {@link ImageInterleaved}.  If type
	 * {@link BorderType#ZERO} is passed in then the value will be set to 0.  Alternatively you could
	 * use {@link #singleValue(Class, double)} instead.
	 *
	 * @param imageType Type of image being processed.
	 * @param borderType Which border algorithm should it use.
	 * @return The requested {@link ImageBorder}.
	 */
	public static <T extends ImageInterleaved> ImageBorder<T>
	interleaved(Class<T> imageType, BorderType borderType)
	{
		Class<?> borderClass;
		switch(borderType) {
			case SKIP:
				throw new IllegalArgumentException("Skip border can't be implemented here and has to be done " +
						"externally.  Call this might be a bug. Instead pass in EXTENDED and manually skip over the " +
						"pixel in a loop some place.");
//				borderClass = BorderIndex1D_Exception.class;
//				break;

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

			case ZERO:
				return FactoryImageBorder.interleavedValue(imageType, 0);

			default:
				throw new IllegalArgumentException("Border type not supported: "+borderType);
		}

		if( imageType == InterleavedF32.class )
			return (ImageBorder<T>)new ImageBorder1D_IL_F32(borderClass);
		else if( imageType == InterleavedF64.class )
			return (ImageBorder<T>)new ImageBorder1D_IL_F64(borderClass);
		else if( InterleavedInteger.class.isAssignableFrom(imageType) )
			return (ImageBorder<T>)new ImageBorder1D_IL_S32(borderClass);
		else if( imageType == InterleavedS64.class )
			return (ImageBorder<T>)new ImageBorder1D_IL_S64(borderClass);
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
	public static <T extends ImageGray> ImageBorder<T> singleValue(T image, double value) {
		ImageBorder border = singleValue(image.getClass(), value);
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
	public static <T extends ImageGray> ImageBorder<T> singleValue(Class<T> imageType, double value) {
		if( imageType == GrayF32.class ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_F32((float)value);
		} else if( imageType == GrayF64.class ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_F64(value);
		} else if( GrayI.class.isAssignableFrom(imageType) ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_I((int)value);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
		}
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
	public static <T extends ImageInterleaved> ImageBorder<T> interleavedValue(T image, double value) {
		ImageBorder border = interleavedValue(image.getClass(), value);
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
	public static <T extends ImageInterleaved> ImageBorder<T> interleavedValue(Class<T> imageType, double value) {
		if( imageType == InterleavedF32.class ) {
			return (ImageBorder<T>) new ImageBorderValue.Value_IL_F32((float) value);
		} else if( imageType == InterleavedF64.class ) {
				return (ImageBorder<T>)new ImageBorderValue.Value_IL_F64(value);
		} else if( InterleavedInteger.class.isAssignableFrom(imageType) ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_IL_S32((int)value);
		} else if( imageType == InterleavedS64.class ) {
			return (ImageBorder<T>)new ImageBorderValue.Value_IL_S64((long)value);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+imageType.getSimpleName());
		}
	}
}
