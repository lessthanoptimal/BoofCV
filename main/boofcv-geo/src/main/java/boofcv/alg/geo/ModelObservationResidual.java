/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
 * Residual function for epipolar matrices with a single output for a single input.
 * </p>
 *
 * <p>
 * Note that "residual = predicted - observed". Typically the "error = residual^T*residual".
 * </p>
 *
 * @author Peter Abeles
 */
public interface ModelObservationResidual<Model, Observation> {

	/**
	 * Specify the epipolar matrix being evaluated
	 *
	 * @param model The model being optimized
	 */
	public void setModel( Model model );

	/**
	 * Compute the error for the observation
	 *
	 * @param observation Observation of point feature in two views
	 * @return residual Error magnitude
	 */
	public double computeResidual( Observation observation );
}
