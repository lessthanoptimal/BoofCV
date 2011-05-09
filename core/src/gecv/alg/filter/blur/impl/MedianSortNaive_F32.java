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
import gecv.struct.image.ImageFloat32;
import pja.sorting.QuickSelectF;

/**
 * Median filter which uses Arrays.sort() to sort its inputs.  It is naive because the sort operation is started
 * from scratch for each pixel, discarding any information learned previously.
 *
 * @author Peter Abeles
 */
public class MedianSortNaive_F32 implements MedianImageFilter<ImageFloat32> {

	// size of the filter box
	private int radius;
	// temporary storage used when sorting
	private float[] values;

	public MedianSortNaive_F32(int radius) {
		this.radius = radius;
		int w = 2*radius+1;
		values = new float[ w*w ];
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public void process(ImageFloat32 input, ImageFloat32 output) {
		for( int y = radius; y < input.height-radius; y++ ) {
			for( int x = radius; x < input.width-radius; x++ ) {
				int index = 0;
				for( int i = -radius; i <= radius; i++ ) {
					for( int j = -radius; j <= radius; j++ ) {
						values[index++] = input.get(x+j,y+i);
					}
				}

				// use quick select to avoid sorting the whole list
				float median = QuickSelectF.select(values,values.length/2,values.length);
				output.set(x,y, median);
			}
		}
	}
}
