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
 * Given an initial model refine its parameters to improve its fit score to the provided set of sample points.
 *
 * @author Peter Abeles
 */
public interface GeoModelRefine<Model,Sample> {

	/**
	 * Processes and refines the position estimate to reduce the error between the model and the observations.
	 *
	 * @param initialModel Input: The initial model which is to be refined. Not modified.
	 * @param observations Input: List of observations that the refined model is fit to.  Not modified.
	 * @param refinedModel Output: Storage for the refined model.  Modified.
	 * @return true if it was successful.
	 */
	public boolean process( Model initialModel, List<Sample> observations , Model refinedModel );
}
