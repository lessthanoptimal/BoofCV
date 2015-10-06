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

package boofcv.factory.filter.binary;

import boofcv.struct.Configuration;

/**
 * Configuration for all threshold types.
 *
 * @author Peter Abeles
 */
public class ConfigThreshold implements Configuration {

	/**
	 * Which algorithm to use
	 */
	public ThresholdType type;

	/**
	 * The threshold to apply to the image.  Only valid for fixed threshold.
	 */
	public double fixedThreshold;

	/**
	 * Bias added the pixel value.  Only used with adaptive.
	 */
	public double bias = 0;

	/**
	 * If true then it thresholds down
	 */
	public boolean down = true;

	/**
	 * Radius of adaptive threshold.

	 */
	public int radius;

	/**
	 * Positive parameter used to tune threshold in Savola.  Try 0.3
	 *
	 * @see ThresholdType#LOCAL_SAVOLA
	 */
	public float savolaK = 0.3f;

	/**
	 * Minimum pixel value.  Only used for some algorithms.
	 */
	public int minPixelValue = 0;
	/**
	 * Maximum pixel value.  Only used for some algorithms.
	 */
	public int maxPixelValue = 255;

	public static ConfigThreshold fixed( double value ) {
		ConfigThreshold config = new ConfigThreshold();
		config.type = ThresholdType.FIXED;
		config.fixedThreshold = value;
		return config;
	}

	public static ConfigThreshold global( ThresholdType type ) {
		if( !type.isAdaptive() )
			throw new IllegalArgumentException("Type must be adaptive");

		if( !type.isGlobal() )
			throw new IllegalArgumentException("Type must be global");

		ConfigThreshold config = new ConfigThreshold();
		config.type = type;
		return config;
	}

	public static ConfigThreshold local( ThresholdType type , int radius ) {
		if( !type.isAdaptive() )
			throw new IllegalArgumentException("Type must be adaptive");

		if( type.isGlobal() )
			throw new IllegalArgumentException("Type must be local");

		ConfigThreshold config = new ConfigThreshold();
		config.type = type;
		config.radius = radius;
		return config;
	}

	@Override
	public void checkValidity() {

	}
}
