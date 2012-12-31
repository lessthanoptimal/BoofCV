/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.grid;

import boofcv.struct.image.ImageFloat32;

/**
 * Histogram of pixel intensity values.
 *
 * @author Peter Abeles
 */
public class IntensityHistogram {

	// counts in each bin
	public int histogram[];
	// number of samples added
	public int total;
	// maximum allowed pixel value
	public double maxValue;
	// divisor = maxValue/total
	public double divisor;

	/**
	 *
	 * @param numBins Number of bins in histogram
	 * @param maxValue Maximum possible pixel value
	 */
	public IntensityHistogram(int numBins, double maxValue) {
		histogram = new int[numBins];
		this.maxValue = maxValue;
		divisor = maxValue/numBins;
	}

	/**
	 * Resets the histogram to the initial state
	 */
	public void reset() {
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] = 0;
		}
		total = 0;
	}

	/**
	 * Adds a new value to the histogram
	 */
	public void add( double value ) {
		int index = (int)(value/divisor);
		histogram[index]++;
		total++;
	}

	/**
	 * Adds all the values in the image to the histogram
	 */
	public void add( ImageFloat32 image ) {
		for( int y = 0; y < image.height; y++ ) {
			int index = image.startIndex + image.stride*y;
			int end = index + image.width;
			for( ; index < end; index++ ) {
				histogram[(int)(image.data[index]/divisor)]++;
			}
		}

		total += image.width*image.height;
	}

	/**
	 * Converts one histogram into another one which has a courser resolution
	 * @param h Histogram being down sampled
	 */
	public void downSample( IntensityHistogram h ) {
		for( int i = 0; i < h.histogram.length; i++) {
			int index = (int)((i*h.divisor+h.divisor/2.0)/divisor);
			histogram[index] += h.histogram[i];
		}
		total = h.total;
	}

	public int getTotal() {
		return total;
	}
}
