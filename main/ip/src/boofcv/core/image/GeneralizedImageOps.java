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

package boofcv.core.image;

import boofcv.alg.InputSanityCheck;
import boofcv.core.image.impl.ImplConvertImage;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * <p>
 * Operations that return information about the specific image.  Useful when writing highly abstracted code
 * which is independent of the input image.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO rename to GImageOps ?
public class GeneralizedImageOps {

	/**
	 * Converts an image from one type to another type.  Creates a new image instance if
	 * an output is not provided.
	 *
	 * @param src Input image. Not modified.
	 * @param dst Converted output image. If null a new one will be declared. Modified.
	 * @param typeDst The type of output image.
	 * @return Converted image.
	 */
	public static <T extends ImageSingleBand> T convert( ImageSingleBand<?> src , T dst , Class<T> typeDst  )
	{
		if (dst == null) {
			dst =(T) createSingleBand(typeDst, src.width, src.height);
		} else {
			InputSanityCheck.checkSameShape(src, dst);
		}
		convert(src,dst);

		return dst;
	}

	/**
	 * Converts an image from one type to another type.
	 *
	 * @param src Input image. Not modified.
	 * @param dst Converted output image. Modified.
	 * @return Converted image.
	 */
	@SuppressWarnings({"unchecked"})
	public static void convert( ImageSingleBand<?> src , ImageSingleBand<?> dst )
	{
		if( src.getClass() == dst.getClass()) {
			((ImageSingleBand)dst).setTo(src);
			return;
		}

		Method m = BoofTesting.findMethod(ImplConvertImage.class,"convert",src.getClass(),dst.getClass());
		try {
			m.invoke(null,src,dst);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isFloatingPoint(Class<?> imgType) {
		if( ImageFloat.class.isAssignableFrom(imgType) ) {
			return true;
		} else {
			return false;
		}
	}

	public static double get(ImageSingleBand img, int x, int y) {
		if (img instanceof ImageInt8) {
			return ((ImageInt8) img).get(x, y);
		} else if (img instanceof ImageInt16) {
			return ((ImageInt16) img).get(x, y);
		} else if (img instanceof ImageSInt32) {
			return ((ImageSInt32) img).get(x, y);
		} else if (img instanceof ImageFloat32) {
			return ((ImageFloat32) img).get(x, y);
		} else if (img instanceof ImageFloat64) {
			return ((ImageFloat64) img).get(x, y);
		} else if (img instanceof ImageSInt64) {
			return ((ImageSInt64) img).get(x, y);
		} else {
			throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
		}
	}

	public static <T extends ImageSingleBand> T createSingleBand(Class<T> type, int width, int height) {
		type = BoofTesting.convertGenericToSpecificType(type);

		if (type == ImageUInt8.class) {
			return (T)new ImageUInt8(width, height);
		} else if (type == ImageSInt8.class) {
			return (T)new ImageSInt8(width, height);
		} else if (type == ImageSInt16.class) {
			return (T)new ImageSInt16(width, height);
		} else if (type == ImageUInt16.class) {
			return (T)new ImageUInt16(width, height);
		} else if (type == ImageSInt32.class) {
			return (T)new ImageSInt32(width, height);
		} else if (type == ImageSInt64.class) {
			return (T)new ImageSInt64(width, height);
		} else if (type == ImageFloat32.class) {
			return (T)new ImageFloat32(width, height);
		} else if (type == ImageFloat64.class) {
			return (T)new ImageFloat64(width, height);
		} else if( type == ImageInteger.class ) {
			// ImageInteger is a generic type, so just create something
			return (T)new ImageSInt32(width,height);
		}
		throw new RuntimeException("Unknown type: "+type.getSimpleName());
	}

	public static void set(ImageSingleBand img, int x, int y, double value) {
		if (ImageInteger.class.isAssignableFrom(img.getClass())) {
			((ImageInteger)img).set(x,y,(int)value);
		} else if (img instanceof ImageFloat32) {
			((ImageFloat32) img).set(x, y,(float)value);
		} else if (img instanceof ImageFloat64) {
			((ImageFloat64) img).set(x, y, value);
		} else {
			throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
		}
	}
}
