/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

/**
 * Operations related to computing statistics from histograms.
 *
 * @author Peter Abeles
 * @author Marius Orfgen
 */
public class HistogramStatistics {
	/**
	 * Computes the variance of pixel intensity values for a GrayU8 image represented by the given histogram.
	 *
	 * @param histogram Histogram with N bins
	 * @param mean Mean of the image.
	 * @param N number of bins in the histogram.
	 * @return variance
	 */
	public static double variance(int[] histogram, double mean , int N ) {
		return variance(histogram, mean, count(histogram,N), N);
	}

	/**
	 * Computes the variance of elements in the histogram
	 *
	 * @param histogram Histogram with N bins
	 * @param mean Histogram's mean.
	 * @param counts Sum of all bins in the histogram.
	 * @param N number of bins in the histogram.
	 * @return variance of values inside the histogram
	 */
	public static double variance(int[] histogram, double mean, int counts , int N) {
		double sum = 0.0;
		for(int i=0;i<N;i++) {
			double d = i - mean;
			sum += (d*d) * histogram[i];
		}

		return sum / counts;
	}

	/**
	 * Counts sum of all the bins inside the histogram
	 *
	 * @param histogram Histogram with N bins
	 * @param N number of bins in the histogram.
	 * @return Sum of all values in the histogram array.
	 */
	public static int count(int[] histogram, int N) {
		int counts = 0;
		for(int i=0;i<N;i++) {
			counts += histogram[i];
		}
		return counts;
	}

	/**
	 * Returns the mean value of the histogram
	 *
	 * @param histogram Histogram with N bins
	 * @param N number of bins in the histogram.
	 * @return Mean pixel intensity value
	 */
	public static double mean(int[] histogram, int N ) {
		return mean(histogram, count(histogram,N),N);
	}

	public static double mean(int[] histogram, int counts, int N) {
		double sum = 0.0;
		for(int i=0;i<N;i++) {
			sum += (histogram[i]*i);
		}

		return sum/counts;
	}

	public static int percentile(int[] histogram, double fraction , int N) {
		return percentile(histogram, count(histogram,N),fraction,N);
	}
	public static int percentile(int[] histogram, int counts, double fraction , int N) {
		int target = (int)(counts*fraction+0.5);
		int count = 0;
		int i;
		if( count >= target )
			return 0;
		for(i=0;i<N;i++) {
			count += histogram[i];
			if( count >= target )
				break;
		}

		return i;
	}
}
