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

import boofcv.struct.Configuration;

/**
 * @author Peter Abeles
 */
public class ConfigPolylineSplitMerge implements Configuration{
	// Can it assume the shape is convex? If so it can reject shapes earlier
	public boolean convex = true;

	// maximum number of sides it will consider
	public int maxSides = 20;

	// maximum number of sides it will consider
	public int minSides = 3;

	// The minimum length of a side
	public int minimumSideLength = 5;

	// When selecting the best model how much is a split penalized
	public double cornerScorePenalty = 0.1;

	// If the score of a side is less than this it is considered a perfect fit and won't be split any more
	public double thresholdSideSplitScore = 1;

	// maximum number of points along a side it will sample when computing a score
	// used to limit computational cost of large contours
	public int maxNumberOfSideSamples = 50;

	@Override
	public void checkValidity() {
		if( minSides < 3 )
			throw new RuntimeException("Minimum sides must be >= 3");
	}
}
