/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

/**
 * @author Peter Abeles
 */
public class FitGaussianPrune {
	int maxIterations;
	int N;
	double data[] = new double[1];
	double inliers[] = new double[1];

	double thresholdSigma;

	double mean;
	double sigma;

	/**
	 *
	 * @param maxIterations Maximum number of iterations.  Try 20
	 * @param thresholdSigma Number of standard deviations a point is away for it to be pruned.  Try 4
	 */
	public FitGaussianPrune(int maxIterations, double thresholdSigma) {
		this.maxIterations = maxIterations;
		this.thresholdSigma = thresholdSigma;
	}

	public void reset( int max ) {
		if( data.length < max ) {
			data = new double[max];
			inliers = new double[max];
		}
		N = 0;
	}
	
	public void add( double d ) {
		data[N++] = d;
	}

	public void process() {
		for( int i = 0; i < maxIterations; i++ ) {
			updateStatistics();
			if( !prune() )
				break;
		}
	}
	
	private void updateStatistics() {
		mean = 0;
		for( int i = 0; i < N; i++ ) {
			mean += data[i];
		}
		mean /= N;
		sigma = 0;
		for( int i = 0; i < N; i++ ) {
			double dx = data[i]-mean;
			sigma += dx*dx;
		}
		sigma = Math.sqrt(sigma/N);
	}
	
	private boolean prune() {
		int inlierN = 0;
		boolean change = false;
		for( int i = 0; i < N; i++ ) {
			double d = data[i];
			if( Math.abs(d-mean)/sigma < thresholdSigma ) {
				inliers[inlierN++] = d;
			} else {
				change = true;
			}
		}
		double[] temp = inliers;
		inliers = data;
		data = temp;
		N = inlierN;

		return change;
	}

	public double getMean() {
		return mean;
	}

	public double getSigma() {
		return sigma;
	}
}
