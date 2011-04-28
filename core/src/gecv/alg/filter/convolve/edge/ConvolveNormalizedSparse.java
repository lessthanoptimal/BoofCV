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

package gecv.alg.filter.convolve.edge;

import gecv.alg.filter.convolve.edge.impl.ConvolveNormalizedStandardSparse;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

/**
 * Performs a convolution around a single pixel only using two 1D kernels in the horizontal and vertical direction.
 * Borders are checked and if on an edge the kernel is truncated and re-normalized.  Only to be used with kernels
 * that can be re-normalized
 *
 * @author Peter Abeles
 */
public class ConvolveNormalizedSparse {

	/**
	 * Convolves around the specified point in the horizontal and vertical direction.  When at the border of
	 * the image the kernel is re-normalized.
	 *
	 * @param horizontal Kernel convolved across the point in the horizontal direction.
	 * @param vertical Kernel convolved across the point in the vertical direction.
	 * @param input Image being convolved.
	 * @param c_x The x-coordinate of the point being convolved.
	 * @param c_y The y-coordinate of the point being convolved.
	 * @param storage Temporary storage.  Needs to be the same size as the kernels being convolved.
	 * @return Result of the convolution.
	 */
	public static float convolve( Kernel1D_F32 horizontal, Kernel1D_F32 vertical,
								  ImageFloat32 input, int c_x , int c_y, float storage[] )
	{
		if( horizontal.width != vertical.width )
			throw new IllegalArgumentException("Both kernels need to be the same width.  Change code if this is a problem.");

		return ConvolveNormalizedStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage);
	}

	/**
	 * Convolves around the specified point in the horizontal and vertical direction.  When at the border of
	 * the image the kernel is re-normalized.
	 *
	 * @param horizontal Kernel convolved across the point in the horizontal direction.
	 * @param vertical Kernel convolved across the point in the vertical direction.
	 * @param input Image being convolved.
	 * @param c_x The x-coordinate of the point being convolved.
	 * @param c_y The y-coordinate of the point being convolved.
	 * @param storage Temporary storage.  Needs to be the same size as the kernels being convolved.
	 * @return Result of the convolution.
	 */
	public static int convolve( Kernel1D_I32 horizontal, Kernel1D_I32 vertical,
								ImageInt8 input, int c_x , int c_y, int storage[] )
	{
		if( horizontal.width != vertical.width )
			throw new IllegalArgumentException("Both kernels need to be the same width.  Change code if this is a problem.");

		return ConvolveNormalizedStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage);
	}

	/**
	 * Convolves around the specified point in the horizontal and vertical direction.  When at the border of
	 * the image the kernel is re-normalized.
	 *
	 * @param horizontal Kernel convolved across the point in the horizontal direction.
	 * @param vertical Kernel convolved across the point in the vertical direction.
	 * @param input Image being convolved.
	 * @param c_x The x-coordinate of the point being convolved.
	 * @param c_y The y-coordinate of the point being convolved.
	 * @param storage Temporary storage.  Needs to be the same size as the kernels being convolved.
	 * @return Result of the convolution.
	 */
	public static int convolve( Kernel1D_I32 horizontal, Kernel1D_I32 vertical,
								ImageInt16 input, int c_x , int c_y, int storage[] )
	{
		if( horizontal.width != vertical.width )
			throw new IllegalArgumentException("Both kernels need to be the same width.  Change code if this is a problem.");

		return ConvolveNormalizedStandardSparse.convolve(horizontal,vertical,input,c_x,c_y,storage);
	}
}
