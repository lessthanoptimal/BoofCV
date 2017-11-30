/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.shapes.polyline;

import boofcv.struct.ConfigLength;

/**
 * Configuration for {@link boofcv.alg.shapes.polyline.splitmerge.PolylineSplitMerge}
 *
 * @author Peter Abeles
 */
public class ConfigPolylineSplitMerge extends ConfigPolyline {
	/**
	 * Can it assume the shape is convex? If so it can reject shapes earlier
	 */
	public boolean convex = true;

	/**
	 * maximum number of sides it will consider
	 */
	public int maxSides = Integer.MAX_VALUE;

	/**
	 * maximum number of sides it will consider
	 */
	public int minSides = 3;

	/**
	 * The minimum length of a side
	 */
	public int minimumSideLength = 5;

	/**
	 * How many corners past the max it will fit a polygon to. This enables it to recover from mistakes.
	 * Relative: Maximum number of sides.
 	 */
	public ConfigLength extraConsider = ConfigLength.relative(1.0,0);

	/**
	 * When selecting the best model how much is a split penalized
	 */
	public double cornerScorePenalty = 0.1;

	/**
	 *If the score of a side is less than this it is considered a perfect fit and won't be split any more
	 */
	public double thresholdSideSplitScore = 0.2;

	/**
	 * maximum number of points along a side it will sample when computing a score
	 * used to limit computational cost of large contours
	 */
	public int maxNumberOfSideSamples = 50;

	/**
	 * If the contour between two corners is longer than this multiple of the distance
	 * between the two corners then it will be rejected as not convex
	 */
	public double convexTest = 2.5;

	/**
	 * Maximum allowed error along a single side in Eclidean distance in pixels.
	 * Relative to number of pixels in contour.
	 */
	public ConfigLength maxSideError = ConfigLength.relative(0.05,3);

	@Override
	public void checkValidity() {
		if( minSides < 3 )
			throw new RuntimeException("Minimum sides must be >= 3");
	}

	@Override
	public String toString() {
		return "ConfigPolylineSplitMerge{" +
				"convex=" + convex +
				", maxSides=" + maxSides +
				", minSides=" + minSides +
				", minimumSideLength=" + minimumSideLength +
				", extraConsider=" + extraConsider +
				", cornerScorePenalty=" + cornerScorePenalty +
				", thresholdSideSplitScore=" + thresholdSideSplitScore +
				", maxNumberOfSideSamples=" + maxNumberOfSideSamples +
				", convexTest=" + convexTest +
				", maxSideError=" + maxSideError +
				'}';
	}
}
