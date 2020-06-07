/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for FAST feature detector.
 *
 * @see FastCornerDetector
 *
 * @author Peter Abeles
 */
public class ConfigFastCorner implements Configuration {

	/**
	 * How different pixels need to be to be considered part of a corner. Image dependent.  Try 20 to start.
	 */
	public int pixelTol = 20;

	/**
	 * Minimum number of pixels around the circle that are required to be a corner.  Can be 9 to 12
	 */
	public int minContinuous=9;

	/**
	 * Maximum number of pixels it will mark as corners. Tuning FAST is difficult and this is to prevent it from using
	 * an extremely large amount of resources unexpectedly. If a fractional value is specified then that's relative
	 * to the total number of pixels in the image.
	 */
	public ConfigLength maxFeatures = ConfigLength.fixed(2000);

	/**
	 * If true then a non-maximum suppression will be used with corner intensity based on the heuristic (see code).
	 * Otherwise it will just return the raw detections. Faster with out non-max but a lot more false positives.
	 */
	public boolean nonMax = true;

	public ConfigFastCorner(int pixelTol, int minContinuous) {
		this.pixelTol = pixelTol;
		this.minContinuous = minContinuous;
	}

	public ConfigFastCorner() {
	}

	@Override
	public void checkValidity() {
		maxFeatures.checkValidity();

		if( minContinuous < 9 || minContinuous > 12 )
			throw new IllegalArgumentException("minContinuous must be from 9 to 12, inclusive");
	}

	public void setTo(ConfigFastCorner src ) {
		this.pixelTol = src.pixelTol;
		this.minContinuous = src.minContinuous;
		this.maxFeatures = src.maxFeatures;
		this.nonMax = src.nonMax;
	}

	public ConfigFastCorner copy() {
		var dst = new ConfigFastCorner();
		dst.setTo(this);
		return dst;
	}
}
