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

import boofcv.struct.Configuration;

/**
 * Common class for all polyline algorithms. All variables here can also be set using the
 * {@link PointsToPolyline} interface.
 */
public abstract class ConfigPolyline implements Configuration {
	/**
	 * If true then the polyline forms a loops. Otherwise the end points are disconnected from each other.
	 */
	public boolean loops = true;

	/**
	 * Minimum number of sides. Inclusive
	 */
	public int minimumSides = 3;

	/**
	 * Maximum number of sides. Inclusive
	 */
	public int maximumSides = Integer.MAX_VALUE;

	/**
	 * Does it require that the found polygons be convex?
	 */
	public boolean convex = true;
}
