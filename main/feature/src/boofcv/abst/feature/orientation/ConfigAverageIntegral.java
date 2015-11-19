/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.orientation;

import boofcv.struct.BoofDefaults;

/**
 * Configuration for {@link boofcv.alg.feature.orientation.impl.ImplOrientationAverageGradientIntegral}.
 *
 * @author Peter Abeles
 */
public class ConfigAverageIntegral implements ConfigOrientation {

	/**
	 * How to convert the radius to the internal canonical scale.  Can be used to adjust how
	 * big or small the region is.
	 */
	public double objectRadiusToScale = 1.0/BoofDefaults.SURF_SCALE_TO_RADIUS;

	/**
	 * Radius of the region being considered in terms of samples. Typically 6.
	 */
	public int radius = 6;
	/**
	 * How often the image is sampled.  This number is scaled.  Typically 1.
	 */
	public double samplePeriod = 1;
	/**
	 * How wide of a kernel should be used to sample. Try 6
	 */
	public int sampleWidth = 6;
	/**
	 * Sigma for weighting.  zero for unweighted. less than zero for automatic. Try -1.
	 */
	public double weightSigma = -1;

	public ConfigAverageIntegral(int radius, double samplePeriod, int sampleWidth, double weightSigma) {
		this.radius = radius;
		this.samplePeriod = samplePeriod;
		this.sampleWidth = sampleWidth;
		this.weightSigma = weightSigma;
	}

	public ConfigAverageIntegral() {
	}

	@Override
	public void checkValidity() {
	}
}
