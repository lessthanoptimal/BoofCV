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

package boofcv.factory.feature.detect.line;

import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.struct.Configuration;

/**
 * Configuration for computing a binary image from a thresholded gradient.
 *
 * @author Peter Abeles
 */
public class ConfigEdgeThreshold implements Configuration {

	/**
	 * Which method to use to compute the gradient
	 */
	public DerivativeType gradient = DerivativeType.PREWITT;

	/**
	 * Threshold for classifying pixels as edge or not. Try 30.
	 */
	public float threshold = 30;

	/**
	 * Should it apply edge based non-maximum suppression
	 */
	public boolean nonMax = true;

	public ConfigEdgeThreshold setTo( ConfigEdgeThreshold src ) {
		this.gradient = src.gradient;
		this.threshold = src.threshold;
		this.nonMax = src.nonMax;
		return this;
	}

	@Override
	public void checkValidity() {
		assert (threshold >= 0.0f);
	}
}
