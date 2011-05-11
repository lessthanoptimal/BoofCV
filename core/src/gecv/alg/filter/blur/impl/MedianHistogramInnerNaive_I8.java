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

package gecv.alg.filter.blur.impl;

import gecv.alg.filter.blur.MedianImageFilter;
import gecv.struct.image.ImageUInt8;

/**
 * Simple implementation of a histogram based median filter.
 *
 * @author Peter Abeles
 */
public class MedianHistogramInnerNaive_I8 implements MedianImageFilter<ImageUInt8> {

	int radius;
	int histogram[] = new int[ 256 ];
	int offset[];
	int threshold;

	public MedianHistogramInnerNaive_I8(int radius) {
		this.radius = radius;

		int w = radius*2+1;
		offset = new int[w*w];
		threshold = offset.length/2+1;
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public void process(ImageUInt8 input, ImageUInt8 output) {
		initialize(input);

		for( int y = radius; y < input.height-radius; y++ ) {
			for( int x = radius; x < input.width-radius; x++ ) {
				int seed = input.startIndex + y*input.stride+x;

				zeroHistogram();
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

	private void initialize(ImageUInt8 input) {
		int index = 0;
		for( int i = -radius; i <= radius; i++ ) {
			for( int j = -radius; j <= radius; j++ ) {
				offset[index++] = i*input.stride + j;
			}
		}
	}

	private void zeroHistogram() {
		for( int i =0; i < histogram.length; i++ ) {
			histogram[i] = 0;
		}
	}

	private void printHistogram() {
		for( int i = 0; i < histogram.length; i++ ) {
			if( histogram[i] != 0 ) {
				System.out.printf("[%d] = %d\n",i,histogram[i]);
			}
		}
	}
}
