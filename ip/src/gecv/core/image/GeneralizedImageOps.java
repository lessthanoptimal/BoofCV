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

package gecv.core.image;

import gecv.alg.InputSanityCheck;
import gecv.alg.misc.ImageTestingOps;
import gecv.core.image.impl.ImplConvertImage;
import gecv.struct.image.*;
import gecv.testing.GecvTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * <p>
 * Operations that return information about the specific image.  Useful when writing highly abstracted code
 * which is independent of the input image.
 * </p>
 *
 * @author Peter Abeles
 */
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
	public static <T extends ImageBase> T convert( ImageBase<?> src , T dst , Class<?> typeDst  )
	{
		if (dst == null) {
			dst =(T) GecvTesting.createImage(typeDst,src.width, src.height);
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
	public static void convert( ImageBase<?> src , ImageBase<?> dst )
	{
		if( src.getClass() == dst.getClass()) {
			((ImageBase)dst).setTo(src);
			return;
		}

		Method m = GecvTesting.findMethod(ImplConvertImage.class,"convert",src.getClass(),dst.getClass());
		try {
			m.invoke(null,src,dst);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public static double computeMeanSquaredError(ImageBase imgA, ImageBase imgB ) {
		if (imgA.getClass() == ImageUInt8.class ) {
			return ImageTestingOps.computeMeanSquaredError((ImageUInt8) imgA, (ImageUInt8) imgB);
		} else if (imgA.getClass() == ImageSInt8.class ) {
			return ImageTestingOps.computeMeanSquaredError((ImageSInt8) imgA, (ImageSInt8) imgB);
		} else if (imgA.getClass() == ImageUInt16.class ) {
			return ImageTestingOps.computeMeanSquaredError((ImageUInt16) imgA, (ImageUInt16) imgB );
		} else if (imgA.getClass() == ImageSInt16.class ) {
			return ImageTestingOps.computeMeanSquaredError((ImageSInt16) imgA, (ImageSInt16) imgB);
		} else if (imgA.getClass() == ImageSInt32.class ) {
			return ImageTestingOps.computeMeanSquaredError((ImageSInt32) imgA, (ImageSInt32) imgB );
		} else if (imgA.getClass() == ImageFloat32.class) {
			return ImageTestingOps.computeMeanSquaredError((ImageFloat32) imgA, (ImageFloat32) imgB );
		} else if (imgA.getClass() == ImageFloat64.class) {
			return ImageTestingOps.computeMeanSquaredError((ImageFloat64) imgA, (ImageFloat64) imgB );
		} else {
			throw new RuntimeException("Unknown type: "+imgA.getClass().getSimpleName());
		}
	}

	public static void fillRectangle(ImageBase img, double value , int x0 , int y0 , int x1 , int y1 ) {
		if (img.getClass() == ImageUInt8.class ) {
			ImageTestingOps.fillRectangle((ImageUInt8) img, (int)value , x0 , y0 , x1 , y1 );
		} else if (img.getClass() == ImageSInt8.class ) {
			ImageTestingOps.fillRectangle((ImageSInt8) img, (int)value , x0 , y0 , x1 , y1 );
		} else if (img.getClass() == ImageUInt16.class ) {
			ImageTestingOps.fillRectangle((ImageUInt16) img, (int)value , x0 , y0 , x1 , y1 );
		} else if (img.getClass() == ImageSInt16.class ) {
			ImageTestingOps.fillRectangle((ImageSInt16) img, (int)value , x0 , y0 , x1 , y1 );
		} else if (img.getClass() == ImageSInt32.class ) {
			ImageTestingOps.fillRectangle((ImageSInt32) img, (int)value , x0 , y0 , x1 , y1 );
		} else if (img.getClass() == ImageFloat32.class) {
			ImageTestingOps.fillRectangle((ImageFloat32) img, (float)value , x0 , y0 , x1 , y1 );
		} else if (img.getClass() == ImageFloat64.class) {
			ImageTestingOps.fillRectangle((ImageFloat64) img, value , x0 , y0 , x1 , y1 );
		} else {
			throw new RuntimeException("Unknown type: "+img.getClass().getSimpleName());
		}
	}

	public static void addGaussian(ImageBase img, Random rand, double sigma) {
		if (img.getClass() == ImageUInt8.class ) {
			ImageTestingOps.addGaussian((ImageUInt8) img, rand, sigma );
		} else if (img.getClass() == ImageSInt8.class ) {
			ImageTestingOps.addGaussian((ImageSInt8) img, rand, sigma );
		} else if (img.getClass() == ImageUInt16.class ) {
			ImageTestingOps.addGaussian((ImageUInt16) img, rand, sigma );
		} else if (img.getClass() == ImageSInt16.class ) {
			ImageTestingOps.addGaussian((ImageSInt16) img, rand, sigma );
		} else if (img.getClass() == ImageSInt32.class ) {
			ImageTestingOps.addGaussian((ImageSInt32) img, rand, sigma );
		} else if (img.getClass() == ImageFloat32.class) {
			ImageTestingOps.addGaussian((ImageFloat32) img, rand, sigma );
		} else if (img.getClass() == ImageFloat64.class) {
			ImageTestingOps.addGaussian((ImageFloat64) img, rand, sigma );
		} else {
			throw new RuntimeException("Unknown type: "+img.getClass().getSimpleName());
		}
	}

	public static void randomize(ImageBase img, Random rand, int min, int max) {
		if (img.getClass() == ImageUInt8.class || img.getClass() == ImageSInt8.class ) {
			ImageTestingOps.randomize((ImageInt8) img, rand, min, max);
		} else if (img.getClass() == ImageSInt16.class || img.getClass() == ImageUInt16.class) {
			ImageTestingOps.randomize((ImageInt16) img, rand, min, max);
		} else if (img.getClass() == ImageSInt32.class ) {
			ImageTestingOps.randomize((ImageSInt32) img, rand, min, max);
		} else if (img.getClass() == ImageSInt64.class ) {
			ImageTestingOps.randomize((ImageSInt64) img, rand, min, max);
		} else if (img.getClass() == ImageFloat32.class) {
			ImageTestingOps.randomize((ImageFloat32) img, rand, min, max);
		} else if (img.getClass() == ImageFloat64.class) {
			ImageTestingOps.randomize((ImageFloat64) img, rand, min, max);
		} else {
			throw new RuntimeException("Unknown type: "+img.getClass().getSimpleName());
		}
	}

	public static boolean isFloatingPoint(Class<?> imgType) {
		if( ImageFloat.class.isAssignableFrom(imgType) ) {
			return true;
		} else {
			return false;
		}
	}

	public static double get(ImageBase img, int x, int y) {
		if (img instanceof ImageUInt8) {
			return ((ImageUInt8) img).get(x, y);
		} else if (img instanceof ImageSInt16) {
			return ((ImageSInt16) img).get(x, y);
		} else if (img instanceof ImageSInt32) {
			return ((ImageSInt32) img).get(x, y);
		} else if (img instanceof ImageFloat32) {
			return ((ImageFloat32) img).get(x, y);
		} else if (img instanceof ImageFloat64) {
			return ((ImageFloat64) img).get(x, y);
		} else {
			throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
		}
	}

	public static void fill(ImageBase img, double value) {
		if( ImageInt8.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageInt8)img,(int)value);
		} else if( ImageInt16.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageInt16)img,(int)value);
		} else if ( ImageSInt32.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageSInt32)img,(int)value);
		} else if ( ImageSInt64.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageSInt64)img,(int)value);
		} else if (ImageFloat32.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageFloat32)img,(int)value);
		} else if (ImageFloat64.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageFloat64)img,(int)value);
		} else {
			throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
		}
	}
}
