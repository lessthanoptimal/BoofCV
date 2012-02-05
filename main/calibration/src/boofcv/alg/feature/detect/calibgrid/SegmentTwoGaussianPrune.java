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

import pja.stats.UtilGaussian;

/**
 * @author Peter Abeles
 */
public class SegmentTwoGaussianPrune {
	FitGaussianPrune low = new FitGaussianPrune(20,4);
	FitGaussianPrune high = new FitGaussianPrune(20,4);

	int N;
	double data[] = new double[1];
	
	double probLow[] = new double[1];
	double probHigh[] = new double[1];

	/**
	 * Discards all the data
	 */
	public void reset( int maxElements ) {
		N = 0;
		if( data.length < maxElements ) {
			data = new double[maxElements];
			probLow = new double[maxElements];
			probHigh = new double[maxElements];
		}
		low.reset(maxElements);
		high.reset(maxElements);
	}

	/**
	 * Adds a new sample point
	 *
	 * @param value
	 */
	public void addValue( double value ) {
		data[N++] = value;
	}

	public void process() {
		double mean = 0;
		for( int i = 0; i < N; i++ ) {
			mean += data[i];
		}
		mean /= N;

		for( int i = 0; i < N; i++ ) {
			if( data[i] < mean ) {
				low.add(data[i]);
			} else {
				high.add(data[i]);
			}
		}
		
		low.process();
		high.process();
		
		for( int i = 0; i < N; i++ ) {
			double x = data[i];
			double l = UtilGaussian.computePDF(low.mean,low.sigma,x);
			double u = UtilGaussian.computePDF(high.mean,high.sigma,x);
			
			double divisor = l+u;
			if( Math.abs(divisor) <= 1e-10 ) {
				probLow[i]=0;
				probHigh[i]=0;
			} else {
				probLow[i] = l/divisor;
				probHigh[i] = u/divisor;
			}
		}
	}

	public double[] getProbLow() {
		return probLow;
	}

	public double[] getProbHigh() {
		return probHigh;
	}

	public double getMeanLow() {
		return low.getMean();
	}

	public double getMeanHigh() {
		return high.getMean();
	}
}
