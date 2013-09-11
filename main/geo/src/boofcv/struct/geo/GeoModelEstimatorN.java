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

import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * <p>
 * Creates multiple hypotheses for the parameters in a model a set of sample points/observations.
 * </p>
 *
 * @author Peter Abeles
 */
public interface GeoModelEstimatorN<Model,Sample> {
	/**
	 * Estimates a set of models which fit the given a set of observations.  A FastQueue is used to store
	 * the found models.  Each time this function is invoked 'estimatedModels' is reset and new models are
	 * requested using the FastQueue.pop() function.
	 *
	 * @param points Input: Set of observations. Not modified.
	 * @param estimatedModels Output: Storage for the set of estimated models.  Modified.
	 * @return true if successful
	 */
	public boolean process(List<Sample> points , FastQueue<Model> estimatedModels );

	/**
	 * Minimum number of points required to estimate the model.
	 *
	 * @return Minimum number of points.
	 */
	public int getMinimumPoints();
}
