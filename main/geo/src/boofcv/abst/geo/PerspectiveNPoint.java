/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo;

import boofcv.struct.geo.PointPositionPair;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Solve the Perspective N-Point (PnP) problem.
 *
 * @author Peter Abeles
 */
public interface PerspectiveNPoint {

	/**
	 * Estimate camera location given the set of observations.
	 *
	 * @param inputs Observation and location pairs. Observations are in normalized coordinates.
	 */
	public void process( List<PointPositionPair> inputs );

	/**
	 * Estimated camera location.
	 *
	 * @return Camera position and orientation
	 */
	public Se3_F64 getPose();

	/**
	 * Minimum number of points required to estimate the fundamental matrix.
	 *
	 * @return number of points.
	 */
	public int getMinPoints();
}
