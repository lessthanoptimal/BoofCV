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

package boofcv.numerics.fitting.modelset.ransac;

import boofcv.numerics.fitting.modelset.DistanceFromModel;
import boofcv.numerics.fitting.modelset.ModelGenerator;

import java.util.List;


/**
 * <p>
 * This implementation of RANSAC tries to maximize the size of the inlier set.
 * </p>
 *
 * @author Peter Abeles
 */
public class SimpleInlierRansac<Model, Point> extends SimpleRansacCommon<Model, Point> {
	// how many points are drawn to generate the initial model
	protected int numInitialSample;
	// how many points must fit a set of model parameters for it to be accepted
	protected int minFitPoints;
	// if this many points fit it immediately stops and exits
	protected int exitFitPoints;

	// how close a point needs to be considered part of the model
	protected double thresholdFit;

	protected int bestFitCount;

	/**
	 * Creates a new instance of the ransac algorithm.
	 *
	 * @param randSeed		 The random seed used by the random number generator.
	 * @param modelGenerator	Creates new model(s) given a small number of points.
	 * @param modelDistance	Computes the difference between a point an a model.
	 * @param maxIterations	The maximum number of iterations the RANSAC algorithm will perform.
	 * @param numInitialSample How many points will it initially draw to create a model.
	 * @param minFitPoints	 The minimum number of points that must match.
	 * @param exitFitPoints	If this many points fit it immediately exits.
	 * @param thresholdFit	 How close of a fit a points needs to be to the model to be considered a fit.  In pixels.
	 */
	public SimpleInlierRansac(long randSeed,
							  ModelGenerator<Model,Point> modelGenerator ,
							  DistanceFromModel<Model,Point> modelDistance,
							  int maxIterations,
							  int numInitialSample,
							  int minFitPoints,
							  int exitFitPoints,
							  double thresholdFit) {
		super(modelGenerator, modelDistance,randSeed, maxIterations);
		this.numInitialSample = numInitialSample;
		this.minFitPoints = minFitPoints;
		this.exitFitPoints = exitFitPoints;

		this.thresholdFit = thresholdFit;
	}

	/**
	 * Constructor primarily used for debugging
	 */
	protected SimpleInlierRansac() {
	}

	/**
	 * Returns the error for the best fit parameters.
	 */
	@Override
	public double getError() {
		return bestFitCount;
	}

	@Override
	public boolean process(List<Point> dataSet, Model paramInitial ) {

		bestFitCount = 0;
		bestFitPoints.clear();

		// see if it has the minimum number of points
		if (dataSet.size() < minFitPoints)
			return false;

		// reduce the exit condition to something more reasonable if possible
		int exitFitPoints = this.exitFitPoints < dataSet.size() - 2 ?
				this.exitFitPoints : dataSet.size() - 2;

		int i;
		for (i = 0; i < maxIterations; i++) {
			// sample the a small set of points
			randomDraw(dataSet, numInitialSample, initialSample, rand);
			
			// get the candidate(s) for this sample set
			candidates.reset();
			modelGenerator.generate(initialSample, candidates);

			for( int j = 0; j < candidates.size(); j++ ) {
				Model candidate = candidates.get(j);

				// determine how may points are needed to either produce a valid model set or beat the
				// current best model set
				int numFitPoints = bestFitCount < minFitPoints ? minFitPoints : bestFitCount + 1;

				// see if the minimum number of points fit this set
				if (!selectMatchSet(dataSet, thresholdFit, numFitPoints, candidate)) {
					continue;
				}

				// save this results
				if (bestFitCount < candidatePoints.size()) {
					bestFitCount = candidatePoints.size();
					bestFitPoints.clear();
					bestFitPoints.addAll(candidatePoints);

					// set the current model to be the best
					bestFitParam = candidates.swap(j,bestFitParam);

					// see if it has reached an exit condition
					if (bestFitCount >= exitFitPoints)
						break;
				}
			}
		}

		return bestFitCount > 0;
	}

	public int getMinFitPoints() {
		return minFitPoints;
	}

	public void setMinFitPoints(int minFitPoints) {
		this.minFitPoints = minFitPoints;
	}

	public int getExitFitPoints() {
		return exitFitPoints;
	}

	public void setExitFitPoints(int exitFitPoints) {
		this.exitFitPoints = exitFitPoints;
	}

	public int getNumInitialSample() {
		return numInitialSample;
	}

	public void setNumInitialSample(int numInitialSample) {
		this.numInitialSample = numInitialSample;
	}

	public double getThresholdFit() {
		return thresholdFit;
	}

	public void setThresholdFit(double thresholdFit) {
		this.thresholdFit = thresholdFit;
	}
}