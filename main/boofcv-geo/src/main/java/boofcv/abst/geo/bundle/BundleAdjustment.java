/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

/**
 * Interface for bundle adjustment.
 *
 * @author Peter Abeles
 */
public interface BundleAdjustment extends Stoppable {

	/**
	 * Configures optimization parameters. meaning of all of these parameters is implementation dependent. They
	 * might even be ignored.
	 *
	 * @param ftol Relative threshold for change in function value between iterations. 0 &le; ftol &le; 1.  Try 1e-12
	 * @param gtol Absolute threshold for convergence based on the gradient's norm. 0 disables test.  0 &le; gtol.
	 *             Try 1e-12
	 * @param maxIterations Maximum number of iterations.
	 */
	void configure( double ftol , double gtol , int maxIterations );

	/**
	 * Optimises the parameters contained in 'structure' to minimize the error in the 'observations'. This function
	 * call will block until complete. Output is written back into 'structure'
	 *
	 * @param structure Input: Initial parameters. Output: Optimized parameters
	 * @param observations Observation of features in each image.
	 * @return true If the cost function has been improved. If the data is perfect to start with this
	 * will return false since it has not improved
	 */
	boolean optimize( BundleAdjustmentSceneStructure structure , BundleAdjustmentObservations observations );
}
