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
 * Given a set of intensity values, segment the points into two sets by fitting a Gaussian
 * model to the points.
 *
 * @author Peter Abeles
 */
public class SegmentGaussianEM {
	// number of data elements
	int N;
	// storage for data elements
	double data[];

	// convergence tolerance
	double tolerance;
	// maximum number of iterations
	int maxIterations;
	
	// model probability
	double prob0[];
	double prob1[];

	// description of Gaussian models
	double mean0;
	double variance0;

	double mean1;
	double variance1;
	
	
	public SegmentGaussianEM(int maxElements , int maxIterations , double tolerance ) {
		this.maxIterations = maxIterations;
		this.tolerance = tolerance;

		data = new double[maxElements];
		prob0 = new double[maxElements];
		prob1 = new double[maxElements];
	}

	public void setMaxElements( int maxElements ) {
		if( data.length < maxElements ) {
			data = new double[maxElements];
			prob0 = new double[maxElements];
			prob1 = new double[maxElements];
		}
	}

	/**
	 * Discards all the data
	 */
	public void reset() {
		N = 0;
	}

	/**
	 * Adds a new sample point
	 *
	 * @param value
	 */
	public void addValue( double value ) {
		data[N++] = value;
	}

	/**
	 * Iterates until it converges or the maximum number of iterations has been exceeded
	 */
	public void process() {
		computeInitialWeights();
		
		double prevMean0 = mean0;
		
		for( int i = 0; i < maxIterations; i++ ) {
			System.out.println("--------------------");
			System.out.println("mean0 = "+mean0+" variance0 "+variance0);
			System.out.println("mean1 = "+mean1+" variance1 "+variance1);

			expectation();
			maximization();
			
			if( Math.abs(prevMean0-mean0) < tolerance )
				break;
			prevMean0 = mean0;
		}
	}
	
	private void computeInitialWeights() {
		double mean = 0;
		for( int i = 0; i < N; i++ ) {
			mean += data[i];
		}
		mean /= N;

		for( int i = 0; i < N; i++ ) {
			if( data[i] < mean ) {
				prob0[i] = 1;
				prob1[i] = 0;
			} else {
				prob0[i] = 0;
				prob1[i] = 1;
			}
		}
		maximization();
	}
	
	private void maximization() {
		mean0 = computeModelMean(prob0);
		variance0 = computeModelVariance(prob0, mean0);

		mean1 = computeModelMean(prob1);
		variance1 = computeModelVariance(prob1, mean1);
	}
	
	private void expectation() {
		for( int i = 0; i < N; i++ ) {
			double value = data[i];
			double l0 = normal(mean0,variance0,value);
			double l1 = normal(mean1,variance1,value);
			prob0[i]=l0/(l0+l1);
			prob1[i]=l1/(l0+l1);
		}
	}
	
	private double normal( double mean , double variance , double sample )
	{
		double error = sample-mean;
		double a = error*error/(2*variance);
		
		return Math.exp(-a);
	}
	
	private double computeModelMean( double prob[] ) {
		double mean = 0;
		double total = 0;
		
		for( int i = 0; i < N; i++ ) {
			double w = prob[i];
			total += w;
			mean += w*data[i];
		}
		
		return mean / total;
	}

	private double computeModelVariance(double prob[], double mean) {
		double total = 0;
		double variance = 0;

		for( int i = 0; i < N; i++ ) {
			double error = data[i]-mean;
			double w = prob[i];
			total += w;
			variance += w*error*error;
		}

		return variance / total;
	}

	public double[] getProb0() {
		return prob0;
	}

	public double[] getProb1() {
		return prob1;
	}

	public double getMean0() {
		return mean0;
	}

	public double getVariance0() {
		return variance0;
	}

	public double getMean1() {
		return mean1;
	}

	public double getVariance1() {
		return variance1;
	}
}
