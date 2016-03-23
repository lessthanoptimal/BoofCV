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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.noborder.ConvolveImageStandardSparse;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

/**
 * Performs a convolution around a single pixel only.  The whole kernel must be contained inside the image, border
 * cases are not handled.  Sparse convolutions are useful when sub-sampling an image.
 *
 * @author Peter Abeles
 */
public class ConvolveImageNoBorderSparse {

	/**
	 * Convolves a 1D kernels around the specified pixel in the horizontal and vertical direction.
	 * 
	 * @param horizontal Horizontal convolution kernel. Not modified.
	 * @param vertical Vertical convolution kernel. Not modified.
	 * @param input Image that is being convolved. Not modified.
	 * @param c_x Pixel the convolution is centered around. x-coordinate.
	 * @param c_y Pixel the convolution is centered around. y-coordinate.
	 * @param storage Must be as long as the kernel's width.
	 * @return The pixel's value after the convolution
	 */
	public static float convolve(Kernel1D_F32 horizontal, Kernel1D_F32 vertical,
								 GrayF32 input, int c_x , int c_y, float storage[] )
	{
		return ConvolveImageStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage);
	}

	/**
	 * Convolves a 1D kernels around the specified pixel in the horizontal and vertical direction.
	 *
	 * @param horizontal Horizontal convolution kernel. Not modified.
	 * @param vertical Vertical convolution kernel. Not modified.
	 * @param input Image that is being convolved. Not modified.
	 * @param c_x Pixel the convolution is centered around. x-coordinate.
	 * @param c_y Pixel the convolution is centered around. y-coordinate.
	 * @param storage Must be as long as the kernel's width.
	 * @return The pixel's value after the convolution
	 */
	public static float convolve(Kernel1D_I32 horizontal, Kernel1D_I32 vertical,
								 GrayU8 input, int c_x , int c_y, int storage[] )
	{
		return ConvolveImageStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage);
	}

	/**
	 * Convolves a 1D kernels around the specified pixel in the horizontal and vertical direction.
	 *
	 * @param horizontal Horizontal convolution kernel. Not modified.
	 * @param vertical Vertical convolution kernel. Not modified.
	 * @param input Image that is being convolved. Not modified.
	 * @param c_x Pixel the convolution is centered around. x-coordinate.
	 * @param c_y Pixel the convolution is centered around. y-coordinate.
	 * @param storage Must be as long as the kernel's width.
	 * @return The pixel's value after the convolution
	 */
	public static float convolve(Kernel1D_I32 horizontal, Kernel1D_I32 vertical,
								 GrayS16 input, int c_x , int c_y, int storage[] )
	{
		return ConvolveImageStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage);
	}

	/**
	 * Convolves a 1D kernels around the specified pixel in the horizontal and vertical direction.
	 * The convolution sum is divided by the specified divisors in the horizontal and vertical direction.
	 *
	 * @param horizontal Horizontal convolution kernel. Not modified.
	 * @param vertical Vertical convolution kernel. Not modified.
	 * @param input Image that is being convolved. Not modified.
	 * @param c_x Pixel the convolution is centered around. x-coordinate.
	 * @param c_y Pixel the convolution is centered around. y-coordinate.
	 * @param storage Must be as long as the kernel's width.
	 * @param divisorHorizontal Divisor for horizontal convolution.
	 * @param divisorVertical Divisor for vertical convolution.
	 * @return The pixel's value after the convolution
	 */
	public static float convolve(Kernel1D_I32 horizontal, Kernel1D_I32 vertical,
								 GrayU8 input, int c_x , int c_y, int storage[] ,
								 int divisorHorizontal, int divisorVertical)
	{
		return ConvolveImageStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage,divisorHorizontal,divisorVertical);
	}

	/**
	 * Convolves a 1D kernels around the specified pixel in the horizontal and vertical direction.
	 * The convolution sum is divided by the specified divisors in the horizontal and vertical direction.
	 *
	 * @param horizontal Horizontal convolution kernel. Not modified.
	 * @param vertical Vertical convolution kernel. Not modified.
	 * @param input Image that is being convolved. Not modified.
	 * @param c_x Pixel the convolution is centered around. x-coordinate.
	 * @param c_y Pixel the convolution is centered around. y-coordinate.
	 * @param storage Must be as long as the kernel's width.
	 * @param divisorHorizontal Divisor for horizontal convolution.
	 * @param divisorVertical Divisor for vertical convolution.
	 * @return The pixel's value after the convolution
	 */
	public static float convolve(Kernel1D_I32 horizontal, Kernel1D_I32 vertical,
								 GrayS16 input, int c_x , int c_y, int storage[] ,
								 int divisorHorizontal, int divisorVertical)
	{
		return ConvolveImageStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage,divisorHorizontal,divisorVertical);
	}

}
