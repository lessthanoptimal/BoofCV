/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo;

import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.GeoModelEstimatorN;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper that allows {@link GeoModelEstimatorN} to be used as a {@link GeoModelEstimator1}.  If more than one
 * solution is found the ambiguity is resolved by computing the distance each hypothesis is away from a set of points
 * not used to compute the model.
 *
 * @author Peter Abeles
 */
public abstract class GeoModelEstimatorNto1<Model,Point> implements GeoModelEstimator1<Model,Point> {

	// Algorithm which generates multiple hypotheses
	private GeoModelEstimatorN<Model,Point> alg;

	// measures how close of a fit the observation is to the model
	private DistanceFromModel<Model,Point> distance;

	// number of sample points used to evaluate hypotheses
	private int numTest;

	// list of points passed to the algorithm
	private List<Point> list = new ArrayList<>();

	// storage for initial set of solutions
	private FastQueue<Model> solutions;

	public GeoModelEstimatorNto1(GeoModelEstimatorN<Model, Point> alg,
								 DistanceFromModel<Model,Point> distance ,
								 FastQueue<Model> solutions ,
								 int numTest) {
		this.alg = alg;
		this.numTest = numTest;
		this.distance = distance;
		this.solutions = solutions;
	}

	@Override
	public boolean process(List<Point> points , Model estimatedModel ) {

		// only pass in the required number of points
		list.clear();
		for (int i = 0; i < points.size() - numTest; i++) {
			list.add(points.get(i));
		}

		// compute the hypotheses
		if (!alg.process(list, solutions))
			return false;

		Model best = null;
		int N = solutions.size();
		if (N == 1) {
			best = solutions.get(0);
		} else if( N > 1 ) {
			double bestScore = Double.MAX_VALUE;
			for (int i = 0; i < N; i++) {
				Model m = solutions.get(i);

				distance.setModel(m);

				// select the best solution
				double score = 0;
				for (int j = list.size(); j < points.size(); j++) {
					score += distance.computeDistance(points.get(j));
				}

				if (score < bestScore) {
					bestScore = score;
					best = m;
				}
			}
		}

		if( best != null ) {
			copy(best, estimatedModel);
			return true;
		}

		return false;
	}

	/**
	 * Copies src into dst
	 */
	protected abstract void copy( Model src, Model dst );

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints() + numTest;
	}
}
