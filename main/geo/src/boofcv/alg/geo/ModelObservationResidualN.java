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

package boofcv.alg.geo;

/**
 * <p>
 * Residual function for epipolar matrices where there are multiple outputs for a single input.
 * </p>
 *
 * <p>
 * Note that "residual = predicted - observed".  Typically the "error = residual^T*residual".
 * </p>
 * @author Peter Abeles
 */
public interface ModelObservationResidualN<Model,Observation> {

	/**
	 * Specify the model being evaluated
	 *
	 * @param model The model.
	 */
	public void setModel(Model model);

	/**
	 * Compute the residual errors for the observation
	 *
	 * @param observation Observation of point feature in two views
	 * @return The new index.  index + getN()
	 */
	public int computeResiduals(Observation observation , double residuals[] , int index );

	/**
	 * The number of outputs
	 *
	 * @return number of outputs
	 */
	public int getN();
}
