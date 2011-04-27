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

import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageFloat32;

/**
 * Performs a convolution around a single pixel only.  Borders are checked and if on an edge the kernel is truncated
 * and renormalized.
 *
 * @author Peter Abeles
 */
public class ConvolveNormalizedSparse {
	public static float horizontal(Kernel1D_F32 kernel, ImageFloat32 input, int x , int y ) {
		int radius = kernel.getRadius();

		int x0 = x - radius;
		int x1 = x + radius+1;

		if( x0 < 0 ) x0 = 0;
		if( x1 > input.width ) x1 = input.width;

		float total = 0;
		float div = 0;
		int indexSrc = input.startIndex + input.stride*y + x0;
		int indexEnd = indexSrc + (x1-x0);
		int indexKer = x0 - (x-radius);
		for( ; indexSrc < indexEnd; indexSrc++ ) {
			float v = kernel.data[indexKer++];
			total += input.data[indexSrc]*v;
			div += v;
		}
		return total/div;
	}

	public static float vertical(Kernel1D_F32 kernel, ImageFloat32 input, int x , int y ) {
		int radius = kernel.getRadius();

		int y0 = y - radius;
		int y1 = y + radius+1;

		if( y0 < 0 ) y0 = 0;
		if( y1 > input.height ) y1 = input.height;

		float total = 0;
		float div = 0;
		int indexSrc = input.startIndex + input.stride*y0 + x;
		int indexEnd = indexSrc + input.stride*(y1-y0);
		int indexKer = y0 - (y-radius);
		for( ; indexSrc < indexEnd; indexSrc += input.stride ) {
			float v = kernel.data[indexKer++];
			total += input.data[indexSrc]*v;
			div += v;
		}
		return total/div;
	}
}
