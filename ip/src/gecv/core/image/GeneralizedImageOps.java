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

import gecv.alg.misc.ImageTestingOps;
import gecv.struct.image.*;

import java.util.Random;

/**
 * Operations that return information about the specific image.  Useful when writing highly abstracted code
 * which is independent of the input image.
 *
 * @author Peter Abeles
 */
public class GeneralizedImageOps {

	public static ImageBase<?> convert( ImageFloat32 input , Class<?> outputType )
	{
		if( ImageUInt8.class == outputType ) {
			ImageUInt8 ret = new ImageUInt8(input.width,input.height);
			ConvertImage.convert(input,ret);
			return ret;
		} else if( ImageSInt16.class == outputType ) {
			ImageSInt16 ret = new ImageSInt16(input.width,input.height);
			ConvertImage.convert(input,ret);
			return ret;
		} else if( ImageFloat32.class == outputType ) {
			ImageFloat32 ret = new ImageFloat32(input.width,input.height);
			ret.setTo(input);
			return ret;
		} else {
			throw new RuntimeException("Add output type: "+outputType);
		}
	}

	public static void randomize(ImageBase img, Random rand, int min, int max) {
		if (img.getClass() == ImageUInt8.class || img.getClass() == ImageSInt8.class ) {
			ImageTestingOps.randomize((ImageInt8) img, rand, min, max);
		} else if (img.getClass() == ImageSInt16.class || img.getClass() == ImageUInt16.class) {
			ImageTestingOps.randomize((ImageInt16) img, rand, min, max);
		} else if (img.getClass() == ImageSInt32.class ) {
			ImageTestingOps.randomize((ImageSInt32) img, rand, min, max);
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
		} else if (ImageFloat32.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageFloat32)img,(int)value);
		} else if (ImageFloat64.class.isAssignableFrom(img.getClass()) ) {
			ImageTestingOps.fill((ImageFloat64)img,(int)value);
		} else {
			throw new IllegalArgumentException("Unknown or incompatible image type: " + img.getClass().getSimpleName());
		}
	}
}
