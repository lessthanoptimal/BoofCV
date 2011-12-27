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

package boofcv.numerics.fitting.modelset.distance;

import boofcv.numerics.fitting.modelset.DistanceFromModel;
import pja.sorting.QuickSort_F64;

import java.util.List;


/**
 * Computes the median error and prunes points if they have more than the specified percentile
 * error.
 *
 * @author Peter Abeles
 */
public class FitByMedianStatistics<Model, Point> implements StatisticalFit<Model, Point> {

	private DistanceFromModel<Model, Point> modelError;
	// set of points which contains all the inliers
	private List<Point> inliers;

	// The fraction of samples that are not pruned
	private double pruneThreshold;

	// the found median error of the points
	private double medianError;
	// points which have this error or more are pruned
	private double pruneVal;
	// initial array containing all the errors
	// sorting is faster with raw arrays
	double[] errors = new double[100];
	double[] origErrors = new double[100];

	QuickSort_F64 sorter = new QuickSort_F64();

	/**
	 * Creates a new FitByMedianStatistics.
	 *
	 * @param pruneThreshold Fraction of samples that are not pruned.
	 */
	public FitByMedianStatistics(double pruneThreshold) {
		if (pruneThreshold < 0 || pruneThreshold > 1.0)
			throw new IllegalArgumentException("The threshold must be between 0 and 1");

		this.pruneThreshold = pruneThreshold;
	}

	@Override
	public void init(DistanceFromModel<Model, Point> modelError, List<Point> inliers) {
		this.modelError = modelError;
		this.inliers = inliers;
	}

	@Override
	public void computeStatistics() {
		int size = inliers.size();

		if (errors.length < size) {
			errors = new double[size * 3 / 2];
			origErrors = new double[errors.length];
		}

		modelError.computeDistance(inliers, errors);
		System.arraycopy(errors, 0, origErrors, 0, size);

		int where = (int) (size * pruneThreshold);

		sorter.sort(errors, size);
		medianError = errors[size / 2];
		pruneVal = errors[where];
	}

	/**
	 * Removes all samples which have an error larger than the specified percentile error.
	 */
	@Override
	public void prune() {

		for (int j = inliers.size() - 1; j >= 0; j--) {

			if (origErrors[j] >= pruneVal) {
				inliers.remove(j);
			}
		}
	}

	@Override
	public double getErrorMetric() {
		return medianError;
	}
}