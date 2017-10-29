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

package boofcv.factory.shape;

import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFitLoop;
import boofcv.struct.ConfigLength;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link SplitMergeLineFitLoop}
 *
 * @author Peter Abeles
 */
public class ConfigPointSplitMergePolyline implements Configuration {
	/**
	 * A line is split if a point along the contour between the two end points has a distance from the line
	 * which is greater than this fraction of the line's length
	 */
	public double splitFraction = 0.05;

	/**
	 * Minimum feature intensity allowed
	 */
	public double featureThreshold = 1;

	/**
	 * Number of split and merge iterations when converting contour into polygon
	 */
	public int iterations = 10;

	/**
	 * Distance between pixels on contour when computing feature intensity
	 */
	public ConfigLength period = ConfigLength.relative(0.1,5);

	/**
	 * True if the contour loops or false if it is a line segment
	 */
	public boolean looping = true;

	@Override
	public void checkValidity() {
		period.checkValidity();
	}

	@Override
	public String toString() {
		return "ConfigPointSplitMergePolyline{" +
				"splitFraction=" + splitFraction +
				", iterations=" + iterations +
				", period=" + period +
				", looping=" + looping +
				'}';
	}
}
