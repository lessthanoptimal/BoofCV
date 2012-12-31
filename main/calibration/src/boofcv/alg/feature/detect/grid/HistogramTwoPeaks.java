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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Searches for two distinct peaks inside the histogram.  The histogram is assumed to have two and only
 * two peaks.  The peaks are found by searching for local histogram maximums.  The the peak is simply
 * the one with the largest set of counts. The second is the next largest which is more than minSeparation
 * away from the first.
 *
 * @author Peter Abeles
 */
public class HistogramTwoPeaks {

	// values of low and high peak
	public double peakLow;
	public double peakHigh;

	// how far the second peak neesd to be away from the first
	public int minSeparation;

	/**
	 *
	 * @param minSeparation Minimum distance of second peak from first
	 */
	public HistogramTwoPeaks( int minSeparation ) {

		this.minSeparation = minSeparation;
	}


	/**
	 * Finds two peaks inside of the histogram
	 *
	 * @param h Histogram being processed
	 */
	public void computePeaks( IntensityHistogram h ) {

		List<Data> peaks = new ArrayList<Data>();

		int N = h.histogram.length;

		if( h.histogram[0] > h.histogram[1] )
			peaks.add( new Data(0,h.histogram[0]));
		if( h.histogram[N-1] > h.histogram[N-2])
			peaks.add( new Data(N-1,h.histogram[N-1]));
		
		// check middle
		for( int i = 1; i < N-1; i++ ) {
			int response = 2*h.histogram[i] - h.histogram[i-1] - h.histogram[i+1];
			if( response > 0 ) {
				peaks.add(new Data(i,h.histogram[i]));
			}
		}
		
		// handle situations where the "peak" has same value in a row
		for( int i = 1; i < N; i++ ) {
			if( h.histogram[i] == h.histogram[i-1] ) {
				boolean allGood = true;
				if( i > 1 ) {
					allGood = h.histogram[i] >= h.histogram[i-2];
				}
				
				if( allGood ) {
					for( int j = i+1; j < N; j++ ) {
						if( h.histogram[i] < h.histogram[j] ) {
							allGood = false;
							break;
						} else if( h.histogram[i] > h.histogram[j] ) {
							break;
						}
					}
				}
				if( allGood ) {
					peaks.add(new Data(i,h.histogram[i]));
				}
			}
		}
		
		// find the best
		Collections.sort(peaks);
		
		Data a,b=null;

		// find the next largest peak which is the minimum separation from the first
		a = peaks.get(0);
		for( int i = 1; i < peaks.size(); i++ ) {
			b = peaks.get(i);
			if( Math.abs(a.index-b.index) >= minSeparation )
				break;
		}

		if( a.index < b.index ) {
			peakLow = a.index*h.divisor+h.divisor/2;
			peakHigh = b.index*h.divisor+h.divisor/2;
		} else {
			peakLow = b.index*h.divisor+h.divisor/2;
			peakHigh = a.index*h.divisor+h.divisor/2;
		}
	}
	
	private static class Data implements Comparable<Data>
	{
		int index;
		int count;

		private Data(int index, int count) {
			this.index = index;
			this.count = count;
		}

		@Override
		public int compareTo(Data o) {
			if( o.count > count )
				return 1;
			else if( o.count < count )
				return -1;
			return 0;
		}
	}
	
}
