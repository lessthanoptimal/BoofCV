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

import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Interface for algorithm which convert a continuous sequence of pixel coordinates into a polyline.
 * A polyline is a polygon which is specified using points along the contour. Polyline can be a loop or it
 * can be disconnected.
 *
 * @author Peter Abeles
 */
public interface PointsToPolyline {
	/**
	 * Computes a polyline from the set of image pixels.
	 *
	 * @param input (Input) List of points in order
	 * @param vertexes (Output) Indexes in the input list which are corners in the polyline
	 *
	 * @return true if successful or false if no fit could be found which matched the requirements
	 */
	boolean process(List<Point2D_I32> input , GrowQueue_I32 vertexes );

	/**
	 * Specifies the minimum number of sides in a polyline that's returned

	 * By default the minimum is 3.
	 *
	 * @param minimum The minimum number of allowed vertices
	 */
	void setMinimumSides(int minimum );

	int getMinimumSides();

	/**
	 * Specifies the maximum allowed sides. How this is interpreted is implementation specific.
	 * For example, two possible interpretations are that it could abort if the number of sides is more
	 * than this or to only considered up to this number of sides. The number of found sides
	 * will never exceed this number.

	 * By default the maximum is Integer.MAX_VALUE.
	 *
	 * @param maximum The maximum number of allowed vertices
	 */
	void setMaximumSides(int maximum );

	int getMaximumSides();

	/**
	 * Is it configured for loops or lines?
	 *
	 * @return true means it assumes the input points form a loop.
	 */
	boolean isLoop();

	/**
	 * Specifies if the found polygons will be convex or not. If the polyline doesn't sloop this should
	 * be set to false. Default is true.
	 */
	void setConvex( boolean convex );

	boolean isConvex();
}
