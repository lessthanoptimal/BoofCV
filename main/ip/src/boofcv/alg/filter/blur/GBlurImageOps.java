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

package boofcv.alg.filter.blur;

import boofcv.struct.image.*;


/**
 * Generalized functions for applying different image blur operators.  Invokes functions
 * from {@link BlurImageOps}, which provides type specific functions.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
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
	T mean(T input, T output, int radius, ImageBase storage ) {
		if( input instanceof GrayU8) {
			return (T)BlurImageOps.mean((GrayU8)input,(GrayU8)output,radius,(GrayU8)storage);
		} else if( input instanceof GrayF32) {
			return (T)BlurImageOps.mean((GrayF32)input,(GrayF32)output,radius,(GrayF32)storage);
		} else if( input instanceof GrayF64) {
			return (T)BlurImageOps.mean((GrayF64)input,(GrayF64)output,radius,(GrayF64)storage);
		} else if( input instanceof Planar) {
			return (T)BlurImageOps.mean((Planar)input,(Planar)output,radius,(ImageGray)storage);
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
		if( input instanceof GrayU8) {
			return (T)BlurImageOps.median((GrayU8) input, (GrayU8) output, radius);
		} else if( input instanceof GrayF32) {
			return (T)BlurImageOps.median((GrayF32) input, (GrayF32) output, radius);
		} else if( input instanceof Planar) {
			return (T)BlurImageOps.median((Planar)input,(Planar)output,radius);
		} else  {
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	/**
	 * Applies Gaussian blur to a {@link ImageGray}
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param sigma Gaussian distribution's sigma.  If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageBase>
	T gaussian(T input, T output, double sigma , int radius, T storage ) {
		if( input instanceof GrayU8) {
			return (T)BlurImageOps.gaussian((GrayU8)input,(GrayU8)output,sigma,radius,(GrayU8)storage);
		} else if( input instanceof GrayF32) {
			return (T)BlurImageOps.gaussian((GrayF32)input,(GrayF32)output,sigma,radius,(GrayF32)storage);
		} else if( input instanceof GrayF64) {
			return (T)BlurImageOps.gaussian((GrayF64)input,(GrayF64)output,sigma,radius,(GrayF64)storage);
		} else if( input instanceof Planar) {
			return (T)BlurImageOps.gaussian((Planar)input,(Planar)output,sigma,radius,(ImageGray)storage);
		} else  {
			throw new IllegalArgumentException("Unsupported image type: "+input.getClass().getSimpleName());
		}
	}
}
