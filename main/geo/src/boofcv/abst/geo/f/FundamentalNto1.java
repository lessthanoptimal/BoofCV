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

package boofcv.abst.geo.f;

import boofcv.abst.geo.EpipolarMatrixEstimator;
import boofcv.abst.geo.EpipolarMatrixEstimatorN;
import boofcv.alg.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>>
 * Given a set of solutions for the Fundamental/Essential matrix, use the epipolar constraint to
 * select the best solution.  This requires an extra point be passed in beyond the minimum number.
 * </p>
 * <p/>
 * <p>
 * Epipolar constraint: x'*F*x = 0
 * </p>
 *
 * @author Peter Abeles
 */
public class FundamentalNto1 implements EpipolarMatrixEstimator {

	// Algorithm which generates multiple hypotheses
	private EpipolarMatrixEstimatorN alg;

	// number of sample points used to evaluate hypotheses
	private int numTest;

	// list of points passed to the algorithm
	private List<AssociatedPair> list = new ArrayList<AssociatedPair>();
	// the best hypothesis
	private DenseMatrix64F best;

	public FundamentalNto1(EpipolarMatrixEstimatorN alg, int numTest) {
		this.alg = alg;
		this.numTest = numTest;
	}

	@Override
	public boolean process(List<AssociatedPair> points) {
		best = null;

		// only pass in the required number of points
		list.clear();
		for (int i = 0; i < points.size() - numTest; i++) {
			list.add(points.get(i));
		}

		// compute the hypotheses
		if (!alg.process(list))
			return false;

		// select best solution
		List<DenseMatrix64F> solutions = alg.getSolutions();

		int N = solutions.size();
		if (N == 0) {
			return false;
		} else if (N == 1) {
			best = solutions.get(0);
		} else {
			double bestScore = Double.MAX_VALUE;
			for (int i = 0; i < N; i++) {
				DenseMatrix64F F = solutions.get(i);

				// Make sure all the solutions have the same scale factor to avoid biasing the scores
				CommonOps.scale(1.0 / NormOps.fastNormF(F), F);

				// select the best solution
				double score = 0;
				for (int j = list.size(); j < points.size(); j++) {
					AssociatedPair p = points.get(j);

					score += Math.abs(GeometryMath_F64.innerProd(p.currLoc, F, p.keyLoc));
				}

				if (score < bestScore) {
					bestScore = score;
					best = F;
				}
			}
		}

		return true;
	}

	@Override
	public DenseMatrix64F getEpipolarMatrix() {
		return best;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints() + numTest;
	}
}
