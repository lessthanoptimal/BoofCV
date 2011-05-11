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
import pja.sorting.QuickSelectI;

/**
 * Median filter which uses Arrays.sort() to sort its inputs.  It is naive because the sort operation is started
 * from scratch for each pixel, discarding any information learned previously.
 *
 * @author Peter Abeles
 */
public class MedianSortNaive_I8 implements MedianImageFilter<ImageUInt8> {

	// size of the filter box
	private int radius;
	// temporary storage used when sorting
	private int[] values;

	public MedianSortNaive_I8(int radius) {
		this.radius = radius;
		int w = 2*radius+1;
		values = new int[ w*w ];
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public void process(ImageUInt8 input, ImageUInt8 output) {
		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x < input.width; x++ ) {
				int minI = y - radius;
				int maxI = y + radius+1;
				int minJ = x - radius;
				int maxJ = x + radius+1;

				// bound it ot be inside the image
				if( minI < 0 ) minI = 0;
				if( minJ < 0 ) minJ = 0;
				if( maxI > input.height ) maxI = input.height;
				if( maxJ > input.width ) maxJ = input.width;

				int index = 0;

				for( int i = minI; i < maxI; i++ ) {
					for( int j = minJ; j < maxJ; j++ ) {
						values[index++] = input.get(j,i);
					}
				}
				
				// use quick select to avoid sorting the whole list
				int median = QuickSelectI.select(values,index/2,index);
				output.set(x,y, median );
			}
		}
	}
}
