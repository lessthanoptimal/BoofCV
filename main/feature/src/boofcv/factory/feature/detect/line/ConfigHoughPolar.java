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

package boofcv.factory.feature.detect.line;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.abst.feature.detect.line.DetectLineHoughPolar}
 *
 * @author Peter Abeles
 */
public class ConfigHoughPolar implements Configuration {

	/**
	 * Radius for local maximum suppression.  Try 2.
	 */
	public int localMaxRadius = 2;
	/**
	 * Minimum number of counts for detected line.  Critical tuning parameter and image dependent.
	 */
	public int minCounts;
	/**
	 * Resolution of line range in pixels.  Try 2
	 */
	public double resolutionRange = 2;
	/**
	 * Resolution of line angle in radius.  Try PI/180
	 */
	public double resolutionAngle = Math.PI/180.0;
	/**
	 * Edge detection threshold. Try 50.
	 */
	public float thresholdEdge = 50;
	/**
	 * Maximum number of lines to return. If &le; 0 it will return all
	 */
	public int maxLines = 0;

	public ConfigHoughPolar(int minCounts) {
		this.minCounts = minCounts;
	}

	public ConfigHoughPolar(int minCounts, int maxLines) {
		this.minCounts = minCounts;
		this.maxLines = maxLines;
	}

	public ConfigHoughPolar(int localMaxRadius, int minCounts, double resolutionRange,
							double resolutionAngle, float thresholdEdge, int maxLines) {
		this.localMaxRadius = localMaxRadius;
		this.minCounts = minCounts;
		this.resolutionRange = resolutionRange;
		this.resolutionAngle = resolutionAngle;
		this.thresholdEdge = thresholdEdge;
		this.maxLines = maxLines;
	}

	@Override
	public void checkValidity() {

	}
}
