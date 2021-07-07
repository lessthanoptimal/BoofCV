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

package boofcv.abst.geo.bundle;

import org.ddogleg.struct.Stoppable;
import org.ddogleg.struct.VerbosePrint;

/**
 * High level interface for bundle adjustment. Bundle adjustment is the process of optimizing in batch parameters
 * for the scene's structure, camera pose, and intrinsic camera parameters.
 *
 * @author Peter Abeles
 */
public interface BundleAdjustment<Structure extends SceneStructure> extends Stoppable, VerbosePrint {

	/**
	 * Configures optimization parameters. meaning of all of these parameters is implementation dependent. They
	 * might even be ignored.
	 *
	 * @param ftol Relative threshold for change in function value between iterations. 0 &le; ftol &le; 1. Try 1e-12
	 * @param gtol Absolute threshold for convergence based on the gradient's norm. 0 disables test. 0 &le; gtol.
	 * Try 1e-12
	 * @param maxIterations Maximum number of iterations.
	 */
	void configure( double ftol, double gtol, int maxIterations );

	/**
	 * Specifies the optimization parameters. After this the initial fit score will return
	 * a valid value.
	 *
	 * @param structure Input: Initial parameters. Output: Optimized parameters
	 * @param observations Observation of features in each image.
	 */
	void setParameters( Structure structure, SceneObservations observations );

	/**
	 * Optimises the parameters contained in 'structure' to minimize the error in the 'observations'. This function
	 * call will block until complete. Output is written back into 'structure'
	 *
	 * @param output Storage for optimized parameters. Can be the same structure passed into {@link #setParameters}.
	 * @return true If the cost function has been improved. If the data is perfect to start with this
	 * will return false since it has not improved
	 */
	boolean optimize( Structure output );

	/**
	 * Returns the fit score. Meaning is implementation specific.
	 *
	 * @return score
	 */
	double getFitScore();
}
