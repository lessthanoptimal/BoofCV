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
 * Configuration for {@link boofcv.alg.feature.orientation.impl.ImplOrientationSlidingWindowIntegral}.
 *
 * @author Peter Abeles
 */
public class ConfigSlidingIntegral implements ConfigOrientation.Integral {

	/**
	 * How to convert the radius to the internal canonical scale.  Can be used to adjust how
	 * big or small the region is.
	 */
	public double objectRadiusToScale = 1.0/BoofDefaults.SURF_SCALE_TO_RADIUS;
	/**
	 * How often the image is sampled.  This number is scaled.  Typically 0.65.
	 */
	public double samplePeriod = 0.65;
	/**
	 * Angular window that is slide across.  Try PI/3
	 */
	public double windowSize = Math.PI / 3.0;
	/**
	 * Radius of the region being considered in terms of samples. Typically 8.
	 */
	public int radius = 8;
	/**
	 * Sigma for weighting distribution.  0 for unweighted. less than zero for automatic. Try -1
	 */
	public double weightSigma = -1;
	/**
	 * Size of kernel doing the sampling.  Typically 6.
	 */
	public int sampleWidth = 6;

	public ConfigSlidingIntegral(double samplePeriod, double windowSize, int radius,
								 double weightSigma, int sampleWidth) {
		this.samplePeriod = samplePeriod;
		this.windowSize = windowSize;
		this.radius = radius;
		this.weightSigma = weightSigma;
		this.sampleWidth = sampleWidth;
	}

	public ConfigSlidingIntegral() {
	}

	@Override
	public void checkValidity() {
	}
}
