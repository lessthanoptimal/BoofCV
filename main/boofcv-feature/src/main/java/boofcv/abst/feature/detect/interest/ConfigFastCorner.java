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

import boofcv.alg.feature.detect.intensity.FastCornerDetector;
import boofcv.struct.Configuration;

/**
 * Configuration for FAST feature detector.
 *
 * @author Peter Abeles
 * @see FastCornerDetector
 */
public class ConfigFastCorner implements Configuration {

	/**
	 * How different pixels need to be considered part of a corner. Image dependent. Try 20 to start.
	 */
	public int pixelTol = 20;

	/**
	 * Minimum number of pixels around the circle that are required to be a corner. Can be 9 to 12
	 */
	public int minContinuous = 9;

	public ConfigFastCorner( int pixelTol, int minContinuous ) {
		this.pixelTol = pixelTol;
		this.minContinuous = minContinuous;
	}

	public ConfigFastCorner() {
	}

	@Override
	public void checkValidity() {
		if (minContinuous < 9 || minContinuous > 12)
			throw new IllegalArgumentException("minContinuous must be from 9 to 12, inclusive");
	}

	public ConfigFastCorner setTo( ConfigFastCorner src ) {
		this.pixelTol = src.pixelTol;
		this.minContinuous = src.minContinuous;
		return this;
	}

	public ConfigFastCorner copy() {
		return new ConfigFastCorner().setTo(this);
	}
}
