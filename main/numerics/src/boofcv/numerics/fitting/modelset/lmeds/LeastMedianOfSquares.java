/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.fitting.modelset.lmeds;


import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.numerics.fitting.modelset.ransac.Ransac;
import pja.sorting.QuickSelectArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * <p>
 * Another technique similar to RANSAC known as Least Median of Squares (LMedS).  For each iteration a small number N points are selected. A model
 * is fit to these points and then the error is computed for the whole set.  The model which minimizes the
 * median is selected as the final model.  No pruning or formal selection of inlier set is done.
 * </p>
 * @author Peter Abeles
 */
// TODO Better algorithm for selecting the inlier set.
// Maybe revert this back to the way it was before and just have it be a separate alg entirely.
public class LeastMedianOfSquares<Model, Point> implements ModelMatcher<Model, Point> {
	// random number generator for selecting points
	private Random rand;

	// number of times it performs its fit cycle
	private int totalCycles;
	// how many points it samples to generate a model from
	private int sampleSize;
	// if the best model has more than this error then it is considered a bad match
	private double maxMedianError;
	// fits a model to the provided data
	private ModelGenerator<Model,Point> generator;
	// computes the error for a point to the model
	private DistanceFromModel<Model,Point> errorMetric;

	// where the initial small set of points is stored
	private List<Point> smallSet = new ArrayList<Point>();

	// parameter being considered
	private Model candidate;
	// the parameter with the best error
	private Model bestParam;
	private double bestMedian;

	// stores all the errors for quicker sorting
	private double []errors = new double[1];

	// list of indexes converting it from match set to input list
	private int []matchToInput = new int[1];

	private List<Point> inlierSet;
	private double inlierFrac;

	/**
	 *
	 * @param randSeed Random seed used internally.
	 * @param sampleSize Number of points it samples to compute a model from.  Typically this is the minimum number of points needed.
	 * @param totalCycles Number of random draws it will make when estimating model parameters.
	 * @param maxMedianError If the best median error is larger than this it is considered a failure.
	 * @param inlierFraction Data which is this fraction or lower is considered an inlier and used to recompute model parameters at the end.  Set to 0 to turn off. Domain: 0 to 1.
	 * @param generator Creates a list of model hypotheses from a small set of points.
	 * @param errorMetric
	 */
	public LeastMedianOfSquares( long randSeed ,
								 int sampleSize,
								 int totalCycles ,
								 double maxMedianError ,
								 double inlierFraction ,
								 ModelGenerator<Model,Point> generator,
								 DistanceFromModel<Model,Point> errorMetric )
	{
		this.rand = new Random(randSeed);
		this.sampleSize = sampleSize;
		this.totalCycles = totalCycles;
		this.maxMedianError = maxMedianError;
		this.inlierFrac = inlierFraction;
		this.generator = generator;
		this.errorMetric = errorMetric;

		bestParam = generator.createModelInstance();
		candidate = generator.createModelInstance();

		if( inlierFrac > 0.0 ) {
			inlierSet = new ArrayList<Point>();
		} else if( inlierFrac > 1.0 ) {
			throw new IllegalArgumentException("Inlier fraction must be <= 1");
		}
	}

	@Override
	public boolean process(List<Point> dataSet) {
		if( dataSet.size() < sampleSize )
			return false;
        
		int N = dataSet.size();

		// make sure the array is large enough.  If not declare a new one that is
		if( errors.length < N ) {
			errors = new double[ N ];
			matchToInput = new int[N];
		}

		bestMedian = Double.MAX_VALUE;

		for( int i = 0; i < totalCycles; i++ ) {
			Ransac.randomDraw(dataSet, sampleSize, smallSet, rand);

			if( generator.generate(smallSet, candidate) ) {
				errorMetric.setModel(candidate);
				errorMetric.computeDistance(dataSet,errors);

				double median = QuickSelectArray.select(errors,N/2,N);

				if( median < bestMedian ) {
					bestMedian = median;
					Model t = bestParam;
					bestParam = candidate;
					candidate = t;
				}
			}
		}

		// if configured to do so compute the inlier set
		computeInlierSet(dataSet, N);

		return bestMedian <= maxMedianError;
	}

	private void computeInlierSet(List<Point> dataSet, int n) {
		int numPts = (int)(n *inlierFrac);

		if( inlierFrac > 0 && numPts > sampleSize ) {
			inlierSet.clear();
			errorMetric.setModel(bestParam);
			errorMetric.computeDistance(dataSet,errors);

			int []indexes = new int[n];
			QuickSelectArray.selectIndex(errors,numPts, n,indexes);
			for( int i = 0; i < numPts; i++ ) {
				int origIndex = indexes[i];
				inlierSet.add( dataSet.get(origIndex) );
				matchToInput[i] = origIndex;
			}
		} else {
			inlierSet = dataSet;
		}
	}

	@Override
	public Model getModel() {
		return bestParam;
	}

	/**
	 * If configured to computer the inlier set it returns the computed inliers.  Otherwise
	 * it returns the data set orginally passed in.
	 *
	 * @return Set of points that are inliers to the returned model parameters..
	 */
	@Override
	public List<Point> getMatchSet() {
		return inlierSet;
	}

	@Override
	public int getInputIndex(int matchIndex) {
		return matchToInput[matchIndex];
	}

	/**
	 * Value of the best median error.
	 * @return
	 */
	@Override
	public double getError() {
		return bestMedian;
	}

	@Override
	public int getMinimumSize() {
		return sampleSize;
	}
}
