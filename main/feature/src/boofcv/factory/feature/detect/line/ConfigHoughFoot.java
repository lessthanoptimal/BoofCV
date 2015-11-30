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
 * Configuration for {@link boofcv.abst.feature.detect.line.DetectLineHoughFoot}
 *
 * @author Peter Abeles
 */
public class ConfigHoughFoot implements Configuration {
	/**
	 * Lines in transform space must be a local max in a region with this radius. Try 5;
	 */
	int localMaxRadius = 5;
	/**
	 * Minimum number of counts/votes inside the transformed image. Try 5.
	 */
	int minCounts = 5;
	/**
	 * Lines which are this close to the origin of the transformed image are ignored.  Try 5.
	 */
	int minDistanceFromOrigin = 5;
	/**
	 * Threshold for classifying pixels as edge or not.  Try 30.
	 */
	float thresholdEdge = 30;
	/**
	 * Maximum number of lines to return. If &le; 0 it will return them all.
	 */
	int maxLines = 0;

	public ConfigHoughFoot() {
	}

	public ConfigHoughFoot(int maxLines) {
		this.maxLines = maxLines;
	}

	public ConfigHoughFoot(int localMaxRadius, int minCounts, int minDistanceFromOrigin,
						   float thresholdEdge, int maxLines) {
		this.localMaxRadius = localMaxRadius;
		this.minCounts = minCounts;
		this.minDistanceFromOrigin = minDistanceFromOrigin;
		this.thresholdEdge = thresholdEdge;
		this.maxLines = maxLines;
	}

	@Override
	public void checkValidity() {

	}
}
