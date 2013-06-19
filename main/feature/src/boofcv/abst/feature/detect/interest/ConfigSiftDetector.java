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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.Configuration;

/**
 * Configures the SIFT feature detector.
 *
 * @see boofcv.alg.feature.detect.interest.SiftDetector
 *
 * @author Peter Abeles
 */
public class ConfigSiftDetector implements Configuration {

	/**
	 * Size of the feature used to detect the corners. Try 2
	 */
	public int extractRadius = 2;
	/**
	 * Minimum corner intensity required.  Set to -Float.MAX_VALUE to detect every possible feature.  Try 1.
	 *
	 * Feature intensity is computed using a Difference-of-Gaussian operation, which can have negative values.
	 */
	public float detectThreshold = 1;
	/**
	 * Max detected features per scale.  Disable with < 0. Tune, image dependent.
	 */
	public int maxFeaturesPerScale = -1;
	/**
	 * Threshold for edge filtering.  Disable with a value <= 0.  Try 5
	 */
	public double edgeThreshold = 5;

	public ConfigSiftDetector(int extractRadius, float detectThreshold,
							  int maxFeaturesPerScale, double edgeThreshold) {
		this.extractRadius = extractRadius;
		this.detectThreshold = detectThreshold;
		this.maxFeaturesPerScale = maxFeaturesPerScale;
		this.edgeThreshold = edgeThreshold;
	}

	public ConfigSiftDetector() {
	}

	@Override
	public void checkValidity() {
	}
}
