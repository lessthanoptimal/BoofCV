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

package boofcv.misc;

import boofcv.struct.Configuration;

/**
 * Generic configuration for optimization routines
 *
 * @author Peter Abeles
 */
public class ConfigConverge implements Configuration {

	public double ftol;
	public double gtol;
	public int maxIterations;

	public ConfigConverge(double ftol, double gtol, int maxIterations) {
		this.ftol = ftol;
		this.gtol = gtol;
		this.maxIterations = maxIterations;
	}

	public ConfigConverge() {
	}

	public void set( ConfigConverge src ) {
		this.ftol = src.ftol;
		this.gtol = src.gtol;
		this.maxIterations = src.maxIterations;
	}

	@Override
	public void checkValidity() {
		if( maxIterations <= 0)
			throw new IllegalArgumentException("Max iterations has to be set to a value more than zero");
	}
}
