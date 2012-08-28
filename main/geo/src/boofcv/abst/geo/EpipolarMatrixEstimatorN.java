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

package boofcv.abst.geo;

import boofcv.alg.geo.AssociatedPair;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Interface for computing fundamental, essential, or homography matrix given a set of associated pairs. Returns
 * a list of solutions.
 *
 * @author Peter Abeles
 */
public interface EpipolarMatrixEstimatorN {

	/**
	 * Estimates the epipolar matrix given a set of observations.
	 *
	 * @param points Observations. Pixel if fundamental and normalized if essential.
	 * @return true if successful
	 */
	public boolean process(List<AssociatedPair> points);

	/**
	 * Set of estimated 3x3 epipolar matrices.
	 *
	 * @return Estimated matrices.
	 */
	public List<DenseMatrix64F> getSolutions();

	/**
	 * Minimum number of points required to estimate the fundamental matrix.
	 *
	 * @return number of points.
	 */
	public int getMinimumPoints();
}
