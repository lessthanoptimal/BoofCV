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

package boofcv.factory.shape;

import boofcv.abst.shapes.polyline.ConfigPolyline;
import boofcv.alg.shapes.polyline.MinimizeEnergyPrune;
import boofcv.alg.shapes.polyline.RefinePolyLineCorner;
import boofcv.alg.shapes.polyline.splitmerge.SplitMergeLineFitLoop;
import boofcv.struct.ConfigLength;

/**
 * Configuration for {@link SplitMergeLineFitLoop}
 *
 * @author Peter Abeles
 */
@Deprecated
public class ConfigSplitMergeLineFit extends ConfigPolyline {
	/**
	 * A line is split if a point along the contour between the two end points has a distance from the line
	 * which is greater than this fraction of the line's length
	 */
	public double splitFraction = 0.05;

	/**
	 * Number of split and merge iterations when converting contour into polygon
	 */
	public int iterations = 10;

	/**
	 * Number of refine iterations. Set to 0 to disable.
	 *
	 * @see RefinePolyLineCorner
	 */
	public int refine = 20;

	/**
	 * If a split adds too much energy to the contour it will be pruned. Disable by setting to a value &le; 0
	 *
	 * @see MinimizeEnergyPrune
	 */
	public double pruneSplitPenalty = 2.0;

	/**
	 * The minimum allowed length of a side as a fraction of the total contour length
	 */
	public ConfigLength minimumSide = ConfigLength.relative(0.025,10);

	/**
	 * Does the contour loop?
	 */
	public boolean loop = true;

	@Override
	public void checkValidity() {
		minimumSide.checkValidity();
	}

	@Override
	public String toString() {
		return "ConfigSplitMergeLineFit{" +
				"splitFraction=" + splitFraction +
				", iterations=" + iterations +
				", minimumSide=" + minimumSide +
				", loop=" + loop +
				'}';
	}
}
