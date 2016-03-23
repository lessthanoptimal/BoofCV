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

package boofcv.alg.filter.blur.impl;

import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Simple implementation of a histogram based median filter.  Only processes the inner portion of the image.
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplMedianHistogramInnerNaive {

	/**
	 * Applies a median image filter.
	 *
	 * @param input Input image. Not modified.
	 * @param output Filtered output image. Modified.
	 * @param radius Size of the filter region.
	 * @param offset Array used to store relative pixel offsets.
	 * @param histogram Saves the image histogram.  Must be at least 256 elements.
	 */
	public static void process(GrayU8 input, GrayU8 output, int radius, int offset[], int histogram[] ) {
		if( histogram == null )
			histogram = new int[ 256 ];
		else if( histogram.length < 256 )
			throw new IllegalArgumentException("'histogram' must have at least 256 elements.");

		int w = 2*radius+1;
		if( offset == null ) {
			offset = new int[ w*w ];
		} else if( offset.length < w*w ) {
			throw new IllegalArgumentException("'offset' must be at least of length "+(w*w));
		}
		int threshold = (w*w)/2+1;

		int index = 0;
		for( int i = -radius; i <= radius; i++ ) {
			for( int j = -radius; j <= radius; j++ ) {
				offset[index++] = i*input.stride + j;
			}
		}

		for( int y = radius; y < input.height-radius; y++ ) {
			for( int x = radius; x < input.width-radius; x++ ) {
				int seed = input.startIndex + y*input.stride+x;

				for( int i = 0; i < 256; i++ ) {
					histogram[i] = 0;
				}

				for( int i = 0; i < offset.length; i++ ) {
					int val = input.data[seed+offset[i]] & 0xFF;
					histogram[val]++;
				}

				int count = 0;
				int median;
				for( median = 0; median < 256; median++ ) {
					count += histogram[median];
					if( count >= threshold )
						break;
				}
				output.data[ output.startIndex+y*output.stride+x] = (byte)median;
			}
		}
	}
}
