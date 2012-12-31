/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.Configuration;

/**
 * Configures the SIFT feature orientation estimator.
 *
 * @see boofcv.alg.feature.orientation.OrientationHistogramSift
 *
 * @author Peter Abeles
 */
public class ConfigSiftOrientation implements Configuration {
	/**
	 * Number of elements in the histogram.  Standard is 36
	 */
	public int histogramSize = 36;
	/**
	 * Convert a sigma to region radius.  Try 2.5
	 */
	public double sigmaToRadius = 2.5;
	/**
	 * How much the scale is enlarged by.  Standard is 1.5
	 */
	public double sigmaEnlarge = 1.5;

	public ConfigSiftOrientation(int histogramSize, double sigmaToRadius, double sigmaEnlarge) {
		this.histogramSize = histogramSize;
		this.sigmaToRadius = sigmaToRadius;
		this.sigmaEnlarge = sigmaEnlarge;
	}

	public ConfigSiftOrientation() {
	}

	@Override
	public void checkValidity() {
	}
}
