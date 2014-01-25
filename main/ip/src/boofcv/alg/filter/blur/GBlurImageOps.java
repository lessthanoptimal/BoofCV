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

package boofcv.alg.filter.blur;

import boofcv.struct.image.*;


/**
 * Generalized functions for applying different image blur operators.  Invokes functions
 * from {@link BlurImageOps}, which provides type specific functions.
 *
 * @author Peter Abeles
 */
public class GBlurImageOps {

	/**
	 * Applies a mean box filter.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageBase>
	T mean(T input, T output, int radius, T storage ) {
		if( input instanceof ImageUInt8 ) {
			return (T)BlurImageOps.mean((ImageUInt8)input,(ImageUInt8)output,radius,(ImageUInt8)storage);
		} else if( input instanceof ImageFloat32) {
			return (T)BlurImageOps.mean((ImageFloat32)input,(ImageFloat32)output,radius,(ImageFloat32)storage);
		} else if( input instanceof MultiSpectral ) {
			return (T)BlurImageOps.mean((MultiSpectral)input,(MultiSpectral)output,radius,(ImageSingleBand)storage);
		} else  {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	/**
	 * Applies a median filter.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the median blur function.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageBase>
	T median(T input, T output, int radius ) {
		if( input instanceof ImageUInt8 ) {
			return (T)BlurImageOps.median((ImageUInt8) input, (ImageUInt8) output, radius);
		} else if( input instanceof ImageFloat32) {
			return (T)BlurImageOps.median((ImageFloat32) input, (ImageFloat32) output, radius);
		} else if( input instanceof MultiSpectral ) {
			return (T)BlurImageOps.median((MultiSpectral)input,(MultiSpectral)output,radius);
		} else  {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	/**
	 * Applies Gaussian blur to a {@link ImageSingleBand}
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param sigma Gaussian distribution's sigma.  If <= 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If <= 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageBase>
	T gaussian(T input, T output, double sigma , int radius, T storage ) {
		if( input instanceof ImageUInt8 ) {
			return (T)BlurImageOps.gaussian((ImageUInt8)input,(ImageUInt8)output,sigma,radius,(ImageUInt8)storage);
		} else if( input instanceof ImageFloat32) {
			return (T)BlurImageOps.gaussian((ImageFloat32)input,(ImageFloat32)output,sigma,radius,(ImageFloat32)storage);
		} else if( input instanceof MultiSpectral ) {
			return (T)BlurImageOps.gaussian((MultiSpectral)input,(MultiSpectral)output,sigma,radius,(ImageSingleBand)storage);
		} else  {
			throw new IllegalArgumentException("Unsupported image type: "+input.getClass().getSimpleName());
		}
	}
}
