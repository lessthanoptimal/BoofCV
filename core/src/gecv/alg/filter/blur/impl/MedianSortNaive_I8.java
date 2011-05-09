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
import gecv.struct.image.ImageInt8;
import pja.sorting.QuickSelectI;

/**
 * Median filter which uses Arrays.sort() to sort its inputs.  It is naive because the sort operation is started
 * from scratch for each pixel, discarding any information learned previously.
 *
 * @author Peter Abeles
 */
public class MedianSortNaive_I8 implements MedianImageFilter<ImageInt8> {

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
	public void process(ImageInt8 input, ImageInt8 output) {
		for( int y = radius; y < input.height-radius; y++ ) {
			for( int x = radius; x < input.width-radius; x++ ) {
				int index = 0;
				for( int i = -radius; i <= radius; i++ ) {
					for( int j = -radius; j <= radius; j++ ) {
						values[index++] = input.get(x+j,y+i);
					}
				}
				
				// use quick select to avoid sorting the whole list
				int median = QuickSelectI.select(values,values.length/2,values.length);
				output.set(x,y, median );
			}
		}
	}
}
