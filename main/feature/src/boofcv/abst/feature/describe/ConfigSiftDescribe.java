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

package boofcv.abst.feature.describe;

import boofcv.struct.Configuration;

/**
 * Configures the SIFT feature descriptor.
 *
 * @see boofcv.alg.feature.describe.DescribePointSift
 *
 * @author Peter Abeles
 */
public class ConfigSiftDescribe implements Configuration {

	/**
	 * Number of grid elements along a side.  Typically 4
	 */
	public int gridWidth = 4;
	/**
	 * Number of samples along a grid. Typically 8
	 */
	public int numSamples = 8;
	/**
	 * Number of bins in the orientation histogram.  Typically 8
	 */
	public int numHistBins = 8;
	/**
	 * Adjusts descriptor element's weighting from center.  Typically 0.5
	 */
	public double weightSigma = 0.5;
	/**
	 * Conversation from scale space to pixels.  Typically 3
	 */
	public double sigmaToRadius = 3;

	public ConfigSiftDescribe(int gridWidth, int numSamples,
							  int numHistBins, double weightSigma, double sigmaToRadius) {
		this.gridWidth = gridWidth;
		this.numSamples = numSamples;
		this.numHistBins = numHistBins;
		this.weightSigma = weightSigma;
		this.sigmaToRadius = sigmaToRadius;
	}

	public ConfigSiftDescribe() {
	}

	@Override
	public void checkValidity() {
	}
}
