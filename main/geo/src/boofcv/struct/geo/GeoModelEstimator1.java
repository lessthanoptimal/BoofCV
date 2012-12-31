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

package boofcv.struct.geo;

import java.util.List;

/**
 * <p>
 * Creates a single hypothesis for the parameters in a model a set of sample points/observations.
 * </p>
 *
 * @author Peter Abeles
 */
public interface GeoModelEstimator1<Model,Sample> {
	/**
	 * Estimates the model given a set of observations.
	 *
	 * @param points Input: Set of observations. Not modified.
	 * @param estimatedModel Output: Storage for the estimated model.  Modified.
	 * @return true if successful
	 */
	public boolean process( List<Sample> points , Model estimatedModel );

	/**
	 * Minimum number of points required to estimate the model.
	 *
	 * @return Minimum number of points.
	 */
	public int getMinimumPoints();
}
