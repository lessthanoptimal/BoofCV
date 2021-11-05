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

package boofcv.factory.geo;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Standard configuration for {@link org.ddogleg.fitting.modelset.ransac.Ransac RANSAC}.
 *
 * @author Peter Abeles
 */
public class ConfigRansac implements Configuration {
	/**
	 * Random seed that's used internally
	 */
	public long randSeed = 0xDEADBEEF;
	/**
	 * Maximum number of iterations RANSAC will perform
	 */
	public int iterations;
	/**
	 * Inlier threshold.
	 */
	public double inlierThreshold;

	public ConfigRansac( int iterations, double inlierThreshold ) {
		this.iterations = iterations;
		this.inlierThreshold = inlierThreshold;
	}

	public ConfigRansac() {
	}

	@Override
	public void checkValidity() {
		BoofMiscOps.checkTrue(iterations >= 0, "Must specify a non-negative number for number of iterations");
	}

	public ConfigRansac setTo( ConfigRansac src ) {
		this.randSeed = src.randSeed;
		this.iterations = src.iterations;
		this.inlierThreshold = src.inlierThreshold;
		return this;
	}
}
