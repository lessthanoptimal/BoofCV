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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.feature.detect.intensity.HarrisCornerIntensity Harris} corner.
 *
 * @author Peter Abeles
 */
public class ConfigHarrisCorner implements Configuration {

	/**
	 * If true a Gaussian kernel will be used. False runs much faster but selects a different location. Application
	 * specific which one is preferred. False is more commonly used.
	 */
	public boolean weighted = false;

	/**
	 * Radius of the kernel.
	 */
	public int radius = 2;

	/**
	 * Tuning parameter, typically a small number around 0.04
	 */
	public double kappa = 0.04;

	public ConfigHarrisCorner() {}

	public ConfigHarrisCorner( boolean weighted, int radius, double kappa ) {
		this.weighted = weighted;
		this.radius = radius;
		this.kappa = kappa;
	}

	public ConfigHarrisCorner( boolean weighted, int radius ) {
		this.weighted = weighted;
		this.radius = radius;
	}

	@Override
	public void checkValidity() {
		if (radius <= 0)
			throw new IllegalArgumentException("Radius must be greater than zero");
	}

	public ConfigHarrisCorner setTo( ConfigHarrisCorner src ) {
		this.weighted = src.weighted;
		this.radius = src.radius;
		this.kappa = src.kappa;
		return this;
	}

	public ConfigHarrisCorner copy() {
		return new ConfigHarrisCorner().setTo(this);
	}
}
