/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.*;

/**
 * @author Peter Abeles
 */
public class ConvolutionTestHelper {
	/**
	 * Find the type of kernel based on the input image type.
	 */
	public static Class<?> kernelTypeByInputType( Class<?> imageType ) {
		if( imageType == GrayF32.class ) {
			return Kernel1D_F32.class;
		} else {
			return Kernel1D_S32.class;
		}
	}

	/**
	 * Creates an image of the specified type
	 */
	public static ImageBase createImage(Class<?> imageType, int width, int height) {

		if( ImageGray.class.isAssignableFrom(imageType)) {
			if( imageType == GrayI8.class )
				imageType = GrayU8.class;
			else if( imageType == GrayI16.class )
				imageType = GrayS16.class;

			return GeneralizedImageOps.createSingleBand((Class)imageType, width, height);
		} else if( ImageInterleaved.class.isAssignableFrom(imageType)) {
			if( imageType == InterleavedI8.class )
				imageType = InterleavedU8.class;
			else if( imageType == InterleavedI16.class )
				imageType = InterleavedS16.class;

			return GeneralizedImageOps.createInterleaved((Class)imageType, width, height, 2);
		}  else {
			throw new RuntimeException("Unknown image class");
		}
	}

	/**
	 * Searches for images and creates copies.  The same instance of all other variables is returned
	 */
	public static Object[] copyImgs(Object... input ) {
		Object[] output = new Object[input.length];
		for (int i = 0; i < input.length; i++) {
			Object o = input[i];
			if (o instanceof ImageGray) {
				ImageGray b = (ImageGray)o;
				ImageGray img = (ImageGray)b.createNew(b.width, b.height);
				img.setTo((ImageGray) o);
				output[i] = img;
			} else {
				output[i] = o;
			}
		}

		return output;
	}
}
