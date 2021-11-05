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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.feature.detect.interest.FastHessianFeatureDetector} plus feature extractor.
 *
 * @author Peter Abeles
 */
public class ConfigFastHessian implements Configuration {

	/**
	 * Configuration for non-maximum thresholding
	 */
	public ConfigExtract extract = new ConfigExtract(2, 1, 0, true);

	/**
	 * The maximum number of features it can detect in a single scale. Useful if you want to prevent high frequency
	 * features from dominating. If &le; 0 then it will have no limit.
	 */
	public int maxFeaturesPerScale = -1;

	/**
	 * Maximum number of features it will return in total. If &le; 0 then it will have no limit.
	 */
	public int maxFeaturesAll = -1;

	/**
	 * Approach used to select features when more than the maximum have been detected
	 */
	public ConfigSelectLimit selector = ConfigSelectLimit.selectBestN();

	/**
	 * How often pixels are sampled in the first octave. Typically 1 or 2.
	 */
	public int initialSampleStep = 1;

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
	 * Increment between kernel sizes as it goes up in scale. In some data sets, increasing this value beyound
	 * the default value results in an improvement in stability. Default 6
	 */
	public int scaleStepSize = 6;

	public ConfigFastHessian( float detectThreshold,
							  int extractRadius,
							  int maxFeaturesPerScale,
							  int initialSampleStep,
							  int initialSize,
							  int numberScalesPerOctave,
							  int numberOfOctaves ) {
		this.extract.threshold = detectThreshold;
		this.extract.radius = extractRadius;
		this.maxFeaturesPerScale = maxFeaturesPerScale;
		this.initialSampleStep = initialSampleStep;
		this.initialSize = initialSize;
		this.numberScalesPerOctave = numberScalesPerOctave;
		this.numberOfOctaves = numberOfOctaves;
	}

	public ConfigFastHessian() {}

	@Override public void checkValidity() {}

	public ConfigFastHessian setTo( ConfigFastHessian src ) {
		this.extract.setTo(src.extract);
		this.maxFeaturesPerScale = src.maxFeaturesPerScale;
		this.initialSampleStep = src.initialSampleStep;
		this.initialSize = src.initialSize;
		this.numberScalesPerOctave = src.numberScalesPerOctave;
		this.numberOfOctaves = src.numberOfOctaves;
		this.scaleStepSize = src.scaleStepSize;
		this.maxFeaturesAll = src.maxFeaturesAll;
		this.selector.setTo(src.selector);
		return this;
	}

	public ConfigFastHessian copy() {
		return new ConfigFastHessian().setTo(this);
	}
}
