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

import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link OrientationHistogramSift}
 *
 * @author Peter Abeles
 */
public class ConfigSiftOrientation implements Configuration {

	/**
	 * Number of elements in the histogram.  Standard is 36
	 */
	public int histogramSize = 36;
	/**
	 * How much the scale is enlarged by.  Standard is 2.0
	 */
	public double sigmaEnlarge = 2.0;

	/**
	 * Creates a configuration similar to how it was originally described in the paper
	 */
	public static ConfigSiftOrientation createPaper() {
		ConfigSiftOrientation config = new ConfigSiftOrientation();
		config.histogramSize = 36;
		config.sigmaEnlarge = 1.5;

		return config;
	}

	@Override
	public void checkValidity() {

	}
}
