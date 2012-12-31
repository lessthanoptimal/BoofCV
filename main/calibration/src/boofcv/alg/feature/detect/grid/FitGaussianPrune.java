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

/**
 * Given a histogram of pixel intensity values and a valid range of indexes in the histogram, compute the mean
 * and standard deviation.  Prune bins which go outside the preset threshold.
 *
 * @author Peter Abeles
 */
public class FitGaussianPrune {
	// maximum number of allowed iterations
	private int maxIterations;

	// number of standard deviations away that points are pruned
	private double thresholdSigma;

	// data's statistics
	private double mean;
	private double sigma;


	// histogram of pixel intensity values
	private IntensityHistogram h;
	// valid range of bins in histogram.  indexLow <= i < indexHigh
	protected int indexLow;
	protected int indexHigh;

	// the closest that a point will be pruned. prevents some pathological situations from over fitting
	int minSeparation = 1;

	/**
	 *
	 * @param maxIterations Maximum number of iterations.  Try 20
	 * @param thresholdSigma Number of standard deviations a point is away for it to be pruned.  Try 4
	 * @param minSeparation A bin will not be pruned if it is this close to the mean.  1 or 3 typically
	 */
	public FitGaussianPrune(int maxIterations, double thresholdSigma , int minSeparation ) {
		this.maxIterations = maxIterations;
		this.thresholdSigma = thresholdSigma;
		this.minSeparation = minSeparation;
	}

	/**
	 * Computes statistics of pixel intensity values inside histogram.
	 * Valid range is indexLow <= i < indexHigh
	 *
	 * @param h Histogram being processed
	 * @param low Index of the lower bound being analyzed
	 * @param high Index of the upper bound being analyzed
	 */
	public void process( IntensityHistogram h , int low , int high ) {
		this.h = h;
		this.indexLow = low;
		this.indexHigh = high;
		
		for( int i = 0; i < maxIterations; i++ ) {
			updateStatistics();
			if( sigma == 0 || !prune() )
				break;
		}

		// translate from being in units of indexes into the original units
		// set the mean to the middle of the divider
		mean = mean*h.divisor + h.divisor/2.0;
		sigma = sigma*h.divisor;
	}

	/**
	 * Compute the mean and standard deviation in terms of indexes
	 */
	private void updateStatistics() {

//		int max = 0;
//		int total = 0;
//		for( int i = indexLow; i < indexHigh; i++ ) {
//			total += h.histogram[i];
//			if( h.histogram[i] > max ) {
//				max = h.histogram[i];
//				mean = i;
//			}
//		}

		int total = 0;
		mean = 0;
		for( int i = indexLow; i < indexHigh; i++ ) {
			total += h.histogram[i];
			mean += i*h.histogram[i];
		}
		mean /= total;

		sigma = 0;
		for( int i = indexLow; i < indexHigh; i++ ) {
			double dx = i-mean;
			sigma += h.histogram[i]*dx*dx;
		}
		sigma = Math.sqrt(sigma/total);
	}

	/**
	 * Reduce the distance of end points until they are within tolerance
	 */
	private boolean prune() {
		boolean change = false;
		
		// see if the extreme ends are outside the prune threshold
		while( Math.abs(mean-indexLow)/sigma > thresholdSigma ) {
			double diff = Math.abs(mean-indexLow);

			if( diff > sigma*thresholdSigma && diff >= minSeparation ) {
				change = true;
				indexLow++;
			} else {
				break;
			}
		}

		while( true ) {
			double diff = Math.abs(mean-indexHigh);

			if( diff > sigma*thresholdSigma && diff >= minSeparation ) {
				change = true;
				indexHigh--;
			} else {
				break;
			}
		}
		
		return change;
	}

	public double getMean() {
		return mean;
	}

	public double getSigma() {
		return sigma;
	}
}
