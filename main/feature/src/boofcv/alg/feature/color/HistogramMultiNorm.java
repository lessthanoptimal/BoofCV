/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.color;

/**
 * Color histogram of an image which is designed to be used for feature detection.  The histogram is N-dimensional
 * where N is the number of color bands.  The sum of of the histogram is normalized to one.
 *
 * @author Peter Abeles
 */
public class HistogramMultiNorm {

	/**
	 * Number of color bands in the image
	 */
	public int numBands;
	/**
	 * Number of discrete bins each band is broken down into
	 */
	public int stride[];
	/**
	 * Value of each histogram.  For a 3 band image each index = index0 + index1*numBins + index2*numBins^2
	 */
	public float hist[];

	public HistogramMultiNorm(int... stride) {

		this.numBands = stride.length;
		this.stride = stride.clone();

		int N = 1;
		for( int i = 0; i < numBands; i++ )
			N *= stride[i];

		hist = new float[ N ];
	}

	/**
	 * Normalizes the histogram such that it's sum is equal to one.
	 */
	public void normalize() {
		float total = 0;
		for( int i = 0; i < hist.length; i++ ) {
			total += hist[i];
		}

		if( total == 0 )
			return;

		for( int i = 0; i < hist.length; i++ ) {
			hist[i] /= total;
		}
	}

	public float get( int ...coordinate ) {
		int index = 0;
		for (int i = 0; i < numBands; i++) {
			index = index * stride[i] + coordinate[i];
		}
		return hist[index];
	}

	/**
	 * Specialized get for 1-D histogram
	 * @param x index
	 * @return histogram value at index
	 */
	public float get( int x ) {
		return hist[x];
	}

	/**
	 * Specialized get for 2-D histogram
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return histogram value at coordinate
	 */
	public float get( int x , int y ) {
		return hist[x* stride[0] + y];
	}

	/**
	 * Specialized get for 3-D histogram
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @param z z-coordinate
	 * @return histogram value at coordinate
	 */
	public float get( int x , int y , int z ) {
		return hist[x*stride[0]*stride[1] + y*stride[1] + z];
	}

	public int getNumBands() {
		return numBands;
	}
}
