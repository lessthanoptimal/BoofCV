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

package boofcv.struct.pyramid;

import boofcv.struct.Configuration;

/**
 * Specifies number of layers in the pyramid. This can be done explicitly or based on the minimum allowed width
 * and height.
 *
 * @author Peter Abeles
 */
public class ConfigDiscreteLevels implements Configuration {
	/** if not -1 then it specifies the number of levels in the pyramid */
	public int numLevelsRequested = -1;
	/** if not -1 then it specifies the minimum width/height of the highest level in the pyramid */
	public int minWidth = -1, minHeight = -1;

	/**
	 * Creates a configuration where the number of levels in the pyramid is specified
	 */
	public static ConfigDiscreteLevels levels( int numLevels ) {
		return new ConfigDiscreteLevels(numLevels, -1, -1);
	}

	/**
	 * Creates a configuration where the minimum image size is specified
	 */
	public static ConfigDiscreteLevels minSize( int minWidth, int minHeight ) {
		return new ConfigDiscreteLevels(-1, minWidth, minHeight);
	}

	/**
	 * Specifies that a pyramid should be created until the smallest size of width or height is the specified value
	 */
	public static ConfigDiscreteLevels minSize( int sideLength ) {
		return new ConfigDiscreteLevels(-1, sideLength, sideLength);
	}

	public ConfigDiscreteLevels() {}

	public ConfigDiscreteLevels( int numLevelsRequested, int minWidth, int minHeight ) {
		this.numLevelsRequested = numLevelsRequested;
		this.minWidth = minWidth;
		this.minHeight = minHeight;
	}

	/**
	 * Compute the number of layers in a pyramid given the input image's shape.
	 *
	 * @param width Input image's width
	 * @param height Input image's height
	 * @return Number of layers
	 */
	public int computeLayers( int width, int height ) {
		if (isFixedLevels()) {
			return numLevelsRequested;
		} else if (minWidth > 0) {
			return computeNumLevels(width, minWidth);
		} else if (minHeight > 0) {
			return computeNumLevels(height, minHeight);
		} else {
			throw new IllegalArgumentException("Need to specify numLevels or minWidth or minHeight");
		}
	}

	int computeNumLevels( int length, int minLength ) {
		if (length <= minLength)
			return 1;
		double scale = length/(double)minLength;
		double levels = Math.log(scale)/Math.log(2);
		return (int)Math.floor(levels) + 1;
	}

	/**
	 * Returns true if configured to have a fixed number of levels in the pyramid
	 */
	public boolean isFixedLevels() {
		return numLevelsRequested > 0;
	}

	public ConfigDiscreteLevels setTo( ConfigDiscreteLevels config ) {
		this.numLevelsRequested = config.numLevelsRequested;
		this.minWidth = config.minWidth;
		this.minHeight = config.minHeight;
		return this;
	}

	@Override
	public void checkValidity() {
		if (numLevelsRequested <= 0 && minWidth <= 0 && minHeight <= 0)
			throw new IllegalArgumentException("Must specify a valid pyramid");
	}

	@Override
	public String toString() {
		if (isFixedLevels()) {
			return "DiscreteLevels{ levels=" + numLevelsRequested + " }";
		} else {
			return "DiscreteLevels{ minWidth=" + minWidth + " minHeight=" + minHeight + " }";
		}
	}
}
