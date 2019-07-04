/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.feature.detect.line.HoughTransformBinary}
 *
 * @author Peter Abeles
 */
public class ConfigHoughBinary implements Configuration {

	/**
	 * Radius for local maximum suppression.  Try 2.
	 */
	public int localMaxRadius = 1;
	/**
	 * Minimum number of counts for detected line. This value is critical to speed.
	 * If absolute it will be the number of counts in a cell. If relative
	 * it will be relative to the total area of the transform image.
	 */
	public ConfigLength minCounts = ConfigLength.relative(0.001,1);

	/**
	 * Maximum number of lines to return. If &le; 0 it will return all
	 */
	public int maxLines = 10;

	/**
	 * If two lines have a slope within this tolerance (radians) then they can be merged.
	 */
	public double mergeAngle = Math.PI*0.05;

	/**
	 * If two lines are within this distance of each other then they can be merged. units = pixels.
	 */
	public double mergeDistance = 10;

	public ConfigHoughBinary() {
	}

	public ConfigHoughBinary(int maxLines) {
		this.maxLines = maxLines;
	}

	@Override
	public void checkValidity() {

	}
}
