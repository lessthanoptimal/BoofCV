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

package boofcv.alg.filter.convolve;

import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.*;

/**
 * @author Peter Abeles
 */
public class ConvolutionTestHelper {
	/**
	 * Find the type of kernel based on the input image type.
	 */
	public static Class<?> kernelTypeByInputType( Class<?> imageType ) {
		if( imageType == ImageFloat32.class ) {
			return Kernel1D_F32.class;
		} else {
			return Kernel1D_I32.class;
		}
	}

	/**
	 * Creates an image of the specified type
	 */
	public static ImageSingleBand createImage(Class<?> imageType, int width, int height) {

		// swap generic types with an arbitrary specific one
		if( imageType == ImageInt8.class )
			imageType = ImageUInt8.class;
		else if( imageType == ImageInt16.class )
			imageType = ImageSInt16.class;

		try {
			ImageSingleBand img = (ImageSingleBand) imageType.newInstance();
			return (ImageSingleBand)img._createNew(width, height);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Searches for images and creates copies.  The same instance of all other variables is returned
	 */
	public static Object[] copyImgs(Object... input ) {
		Object[] output = new Object[input.length];
		for (int i = 0; i < input.length; i++) {
			Object o = input[i];
			if (o instanceof ImageSingleBand) {
				ImageSingleBand b = (ImageSingleBand)o;
				ImageSingleBand img = (ImageSingleBand)b._createNew(b.width, b.height);
				img.setTo((ImageSingleBand) o);
				output[i] = img;
			} else {
				output[i] = o;
			}
		}

		return output;
	}
}
