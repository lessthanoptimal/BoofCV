/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.sfot;

import boofcv.alg.tracker.klt.KltConfig;

/**
 * fContains configuration parameters for {@link SparseFlowObjectTracker}.
 *
 * @author Peter Abeles
 */
public class SfotConfig {

	/**
	 * Random seed used by random number generator
	 */
	public long randSeed = 0xFEED;
	/**
	 * Number of iterative cycles used by LeastMedianOfSquares
	 */
	public int robustCycles = 50;
	/**
	 * Maximum allowed error in pixels when performing robust model fitting using LeastMedianOfSquares
	 */
	public double robustMaxError = 10;
	public int trackerFeatureRadius = 5;


	/**
	 * Number of points it samples along one side of the grid.
	 */
	public int numberOfSamples = 15;

	/**
	 * Maximum allowed forward-backwards error in pixels
	 */
	public double maximumErrorFB = 10;

	/**
	 * Basic parameters for tracker.  KltConfig.createDefault() with maxIterations = 50 is suggested.
	 */
	public KltConfig trackerConfig;

	public SfotConfig() {
		trackerConfig = new KltConfig();
		trackerConfig.maxIterations = 50;
	}
}
