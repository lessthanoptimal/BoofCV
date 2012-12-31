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

package boofcv.factory.geo;

/**
 * List of algorithms for solving the Perspective n-Point (PnP) problem
 *
 * @author Peter Abeles
 */
public enum EnumPNP {
	/**
	 * <ul>
	 *     <li> Minimal 3-point solution
	 *     <li> At most 4 solutions.
	 *     <li> No iteration
	 * </ul>
	 *
	 * @see boofcv.alg.geo.pose.P3PGrunert
	 */
	P3P_GRUNERT,
	/**
	 * <ul>
	 *     <li> Minimal 3-point solution
	 *     <li> At most 4 solutions.
	 *     <li> No iteration
	 * </ul>
	 *
	 * @see boofcv.alg.geo.pose.P3PFinsterwalder
	 */
	P3P_FINSTERWALDER,
	/**
	 * <ul>
	 *     <li> Four or more points.
	 *     <li> Single solution
	 *     <li> Requires iteration.  See class JavaDoc for recommendations and peculiarities.
	 *     <li> Efficient algorithm for many sample points
	 * </ul>
	 *
	 * @see boofcv.alg.geo.pose.PnPLepetitEPnP
	 */
	EPNP
}
