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

package boofcv.misc;

import boofcv.struct.Configuration;

/**
 * Generic configuration for optimization routines
 *
 * @author Peter Abeles
 */
public class ConfigConverge implements Configuration {
	/**
	 * Relative threshold for change in function value between iterations. 0 &le; ftol &le; 1.
	 */
	public double ftol;

	/**
	 * Absolute threshold for convergence based on the gradient's norm. 0 disables test. gtol &ge; 0.
	 */
	public double gtol;

	/**
	 * Maximum number of iterations. What is defined as an iteration is implementation specific.
	 */
	public int maxIterations;

	public ConfigConverge( double ftol, double gtol, int maxIterations ) {
		this.ftol = ftol;
		this.gtol = gtol;
		this.maxIterations = maxIterations;
	}

	public ConfigConverge( ConfigConverge src ) {
		setTo(src);
	}

	public ConfigConverge() {}

	public ConfigConverge setTo( double ftol, double gtol, int maxIterations ) {
		this.ftol = ftol;
		this.gtol = gtol;
		this.maxIterations = maxIterations;
		return this;
	}

	public ConfigConverge setTo( ConfigConverge src ) {
		this.ftol = src.ftol;
		this.gtol = src.gtol;
		this.maxIterations = src.maxIterations;
		return this;
	}

	@Override
	public void checkValidity() {
		BoofMiscOps.checkTrue(ftol >= 0.0 && ftol <= 1.0, "ftol is out of range");
		BoofMiscOps.checkTrue(gtol >= 0.0, "gtol is out of range");

		if (maxIterations < 0)
			throw new IllegalArgumentException("Max iterations has to be set to a value more than or equal to zero");
	}
}
