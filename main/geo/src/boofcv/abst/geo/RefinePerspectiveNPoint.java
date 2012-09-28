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
 * Refines a position estimate given a set of observed point feature locations and 3D location.
 *
 * @author Peter Abeles
 */
public interface RefinePerspectiveNPoint {

	/**
	 * Processes and refines the position estimate.
	 *
	 * @param pose Initial position estimate
	 * @param obs List of observations.  Normalized coordinates.
	 * @return true if it was successful.
	 */
	public boolean process( Se3_F64 pose , List<PointPositionPair> obs );

	/**
	 * The refined pose
	 *
	 * @return Found solution.
	 */
	public Se3_F64 getRefinement();
}
