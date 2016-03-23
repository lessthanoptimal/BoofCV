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

import boofcv.struct.image.GrayF32;

/**
 * Alternative ordering for box vertical convolve
 * 
 * @author Peter Abeles
 */
public class ConvolveBoxAlt {

	public static void vertical(GrayF32 input , GrayF32 output , int radius , boolean includeBorder ) {
		final int kernelWidth = radius*2 + 1;

		final int startX = includeBorder ? 0 : radius;
		final int endX = includeBorder ? input.width : input.width - radius;

		for( int x = startX; x < endX; x++ ) {
			int indexIn = input.startIndex + x;
			int indexOut = output.startIndex + x + radius*output.stride;

			float total = 0;

			int indexEnd = indexIn + input.stride*kernelWidth;

			for( ; indexIn < indexEnd; indexIn += input.stride ) {
				total += input.data[indexIn] ;
			}
			output.data[indexOut] = total;
			indexOut += output.stride;

			indexEnd = indexIn + (input.height - kernelWidth)*input.stride;
			for( ; indexIn < indexEnd; indexIn += input.stride , indexOut += output.stride ) {
				total -= input.data[ indexIn - kernelWidth ] ;
				total += input.data[ indexIn ] ;

				output.data[indexOut] = total;
			}
		}
	}
}
