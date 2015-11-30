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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.feature.detect.interest.FastHessianFeatureDetector} plus feature extractor.
 *
 * @author Peter Abeles
 */
public class ConfigFastHessian implements Configuration {

	/**
	 * Minimum feature intensity. Image dependent.  Start tuning at 1.
	 */
	public float detectThreshold = 1;
	/**
	 * Radius used for non-max-suppression.  Typically 1 or 2.
	 */
	public int extractRadius = 2;
	/**
	 * Number of features it will find or if &le; 0 it will return all features it finds.
	 */
	public int maxFeaturesPerScale = -1;
	/**
	 * How often pixels are sampled in the first octave.  Typically 1 or 2.
	 */
	public int initialSampleSize = 1;
	/**
	 * Typically 9.
	 */
	public int initialSize = 9;
	/**
	 * Typically 4.
	 */
	public int numberScalesPerOctave = 4;
	/**
	 * Typically 4.
	 */
	public int numberOfOctaves = 4;

	/**
	 * Increment between kernel sizes as it goes up in scale.  In some data sets, increasing this value beyound
	 * the default value results in an improvement in stability.  Default 6
	 */
	public int scaleStepSize = 6;

	public ConfigFastHessian(float detectThreshold,
							 int extractRadius,
							 int maxFeaturesPerScale,
							 int initialSampleSize,
							 int initialSize,
							 int numberScalesPerOctave,
							 int numberOfOctaves) {
		this.detectThreshold = detectThreshold;
		this.extractRadius = extractRadius;
		this.maxFeaturesPerScale = maxFeaturesPerScale;
		this.initialSampleSize = initialSampleSize;
		this.initialSize = initialSize;
		this.numberScalesPerOctave = numberScalesPerOctave;
		this.numberOfOctaves = numberOfOctaves;
	}

	public ConfigFastHessian() {
	}

	@Override
	public void checkValidity() {
	}
}
