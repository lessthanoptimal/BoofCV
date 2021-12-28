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
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link SiftDetector}
 *
 * @author Peter Abeles
 */
public class ConfigSiftDetector implements Configuration {

	/**
	 * Configures non-maximum feature detector across the image
	 */
	public ConfigExtract extract = new ConfigExtract(2, 0, 1, true, true, true);

	/**
	 * The maximum number of features it can detect in a single scale. Useful if you want to prevent high frequency
	 * features from dominating. If &le; 0 then it will have no limit.
	 */
	public int maxFeaturesPerScale = 0;

	/**
	 * Maximum number of features it will return in total. If &le; 0 then it will have no limit.
	 */
	public int maxFeaturesAll = -1;

	/**
	 * Approach used to select features when more than the maximum have been detected
	 */
	public ConfigSelectLimit selector = ConfigSelectLimit.selectBestN();

	/**
	 * Threshold used to remove edge responses. Larger values means its less strict. Try 10
	 */
	public double edgeR = 10;

	{
		extract.ignoreBorder = 1;
	}

	/**
	 * Creates a configuration similar to how it was originally described in the paper
	 */
	public static ConfigSiftDetector createPaper() {
		ConfigSiftDetector config = new ConfigSiftDetector();
		config.extract = new ConfigExtract(1, 0, 1, true, true, true);
		config.extract.ignoreBorder = 1;
		config.maxFeaturesPerScale = 0;
		config.edgeR = 10;
		return config;
	}

	public ConfigSiftDetector() {
	}

	public ConfigSiftDetector( int maxFeaturesPerScale ) {
		this.maxFeaturesPerScale = maxFeaturesPerScale;
	}

	@Override public void checkValidity() {}

	public ConfigSiftDetector setTo( ConfigSiftDetector src ) {
		this.extract.setTo(src.extract);
		this.maxFeaturesPerScale = src.maxFeaturesPerScale;
		this.maxFeaturesAll = src.maxFeaturesAll;
		this.selector.setTo(src.selector);
		this.edgeR = src.edgeR;
		return this;
	}

	public ConfigSiftDetector copy() {
		return new ConfigSiftDetector().setTo(this);
	}
}
