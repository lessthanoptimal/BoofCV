/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * <p>
 * A related problem to the full P3P problem is to estimate the distance between the camera center and each of the 3
 * points being viewed.  Once those distances are known the full 3D rigid body transform can be computed.
 * </p>
 *
 * <p>
 * More formally states the problem is: Three points (P1,P2,P3) in 3D space are observed in the image plane in
 * normalized image coordinates (obs1,obs2,obs3).  The distance in 3D space between pairs of points (P1,P3), (P1,P2),
 * and (P2,P3) is known.  Solve for the distance between the camera's origin and each of the three points.
 * </p>
 *
 * @author Peter Abeles
 */
public interface P3PLineDistance {

	/**
	 * Solve for the distance between the camera's origin and each of the 3 points in 3D space.
	 *
	 * @param obs1 Observation of P1 in normalized image coordinates
	 * @param obs2 Observation of P2 in normalized image coordinates
	 * @param obs3 Observation of P3 in normalized image coordinates
	 * @param length23 Distance between points P2 and P3
	 * @param length13 Distance between points P1 and P3
	 * @param length12 Distance between points P1 and P2
	 * @return true if successful or false if it failed to generate any solutions
	 */
	public boolean process( Point2D_F64 obs1 , Point2D_F64 obs2, Point2D_F64 obs3,
							double length23 , double length13 , double length12 );

	/**
	 * Returns a set of solutions.  Each solution contains the distance to the respective point.
	 *
	 * @return List of solutions.
	 */
	public FastQueue<PointDistance3> getSolutions();
}
