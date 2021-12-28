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

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.feature.detect.line.HoughTransformGradient}
 *
 * @author Peter Abeles
 */
public class ConfigHoughGradient implements Configuration {
	/**
	 * Lines in transform space must be a local max in a region with this radius. Try 5;
	 */
	public int localMaxRadius = 2;
	/**
	 * Minimum number of counts/votes inside the transformed image. Try 5.
	 */
	public int minCounts = 5;
	/**
	 * Lines which are this close to the origin of the transformed image are ignored. Try 5.
	 */
	public int minDistanceFromOrigin = 5;

	/**
	 * Maximum number of lines to return. If &le; 0 it will return them all.
	 */
	public int maxLines = 0;

	/**
	 * If two lines have a slope within this tolerance (radians) then they can be merged.
	 */
	public double mergeAngle = Math.PI*0.05;

	/**
	 * If two lines are within this distance of each other then they can be merged. units = pixels.
	 */
	public double mergeDistance = 10;

	/**
	 * Radius of mean-shift refinement. Set to zero to turn off.
	 */
	public int refineRadius = 3;

	/**
	 * How the gradient is thresholded
	 */
	public ConfigEdgeThreshold edgeThreshold = new ConfigEdgeThreshold();

	public ConfigHoughGradient() {
	}

	public ConfigHoughGradient( int maxLines ) {
		this.maxLines = maxLines;
	}

	public ConfigHoughGradient( int localMaxRadius, int minCounts, int minDistanceFromOrigin,
								float thresholdEdge, int maxLines ) {
		this.localMaxRadius = localMaxRadius;
		this.minCounts = minCounts;
		this.minDistanceFromOrigin = minDistanceFromOrigin;
		this.edgeThreshold.threshold = thresholdEdge;
		this.maxLines = maxLines;
	}

	public ConfigHoughGradient setTo( ConfigHoughGradient src ) {
		this.localMaxRadius = src.localMaxRadius;
		this.minCounts = src.minCounts;
		this.minDistanceFromOrigin = src.minDistanceFromOrigin;
		this.edgeThreshold.setTo(src.edgeThreshold);
		this.maxLines = src.maxLines;
		this.mergeAngle = src.mergeAngle;
		this.mergeDistance = src.mergeDistance;
		this.refineRadius = src.refineRadius;
		return this;
	}

	@Override public void checkValidity() {}
}
