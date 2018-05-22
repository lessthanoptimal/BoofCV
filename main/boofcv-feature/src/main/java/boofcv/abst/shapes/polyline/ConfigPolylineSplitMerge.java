/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
	 * The minimum length of a side
	 */
	public int minimumSideLength = 2;

	/**
	 * How many corners past the max it will fit a polygon to. This enables it to recover from mistakes.
	 * Relative: Maximum number of sides.
 	 */
	public ConfigLength extraConsider = ConfigLength.relative(1.0,0);

	/**
	 * Used to adjust the penalty for adding a new corner. Larger numbers will bias it towards shapes with fewer
	 * sides.  For simple convex shapes 0.2 is a reasonable value. For complex concave shapes 0.025 seems to do better.
	 */
	public double cornerScorePenalty = 0.025;

	/**
	 *If the error forside is less than this it is considered a perfect fit and the side won't be split. Adjust
	 * this value to improve the speed. Try setting to zero if corners are precise enough.
	 */
	public double thresholdSideSplitScore = 0.2;

	/**
	 * maximum number of points along a side it will sample when computing a score
	 * used to limit computational cost of large contours
	 */
	public int maxNumberOfSideSamples = 50;

	/**
	 * If the contour between two corners is longer than this multiple of the distance
	 * between the two corners then it will be rejected as not convex. larger values
	 * make the tolerance weaker and smaller values make it more strict. Setting it too small can make it
	 * reject convex shapes.
	 */
	public double convexTest = 2.5;

	/**
	 * Maximum allowed error along a single side in Eclidean distance in pixels.
	 * Relative to number of pixels in contour.
	 */
	public ConfigLength maxSideError = ConfigLength.relative(0.05,3);

	/**
	 * Extra refinement that it does after the initial polyline has been found. Set to a value above zero to use
	 * this feature.
	 */
	public int refineIterations = 10;

	@Override
	public void checkValidity() {
		extraConsider.checkValidity();
	}

	@Override
	public String toString() {
		return "ConfigPolylineSplitMerge{" +
				"minimumSideLength=" + minimumSideLength +
				", extraConsider=" + extraConsider +
				", cornerScorePenalty=" + cornerScorePenalty +
				", thresholdSideSplitScore=" + thresholdSideSplitScore +
				", maxNumberOfSideSamples=" + maxNumberOfSideSamples +
				", convexTest=" + convexTest +
				", maxSideError=" + maxSideError +
				", refineIterations=" + refineIterations +
				", loops=" + loops +
				", minimumSides=" + minimumSides +
				", maximumSides=" + maximumSides +
				", convex=" + convex +
				'}';
	}
}
