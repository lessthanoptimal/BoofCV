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
 * List of different algorithms for estimating Fundamental or Essential matrices
 *
 * @author Peter Abeles
 */
public enum EnumEpipolar {

	/**
	 * <ul>
	 *     <li> 8 or more points
	 *     <li> Single solution
	 *     <li> Uncalibrated camera
	 * </ul>
	 *
	 * @see boofcv.alg.geo.f.FundamentalLinear8
	 */
	FUNDAMENTAL_8_LINEAR,
	/**
	 * <ul>
	 *     <li> Exactly 7 points
	 *     <li> Multiple solutions
	 *     <li> Uncalibrated camera
	 * </ul>
	 *
	 * @see boofcv.alg.geo.f.FundamentalLinear7
	 */
	FUNDAMENTAL_7_LINEAR,
	/**
	 * <ul>
	 *     <li> 8 or more points
	 *     <li> Single solution
	 *     <li> Calibrated camera
	 * </ul>
	 *
	 * @see boofcv.alg.geo.f.FundamentalLinear8
	 */
	ESSENTIAL_8_LINEAR,
	/**
	 * <ul>
	 *     <li> Exactly 7 points
	 *     <li> Multiple solutions
	 *     <li> Calibrated camera
	 * </ul>
	 *
	 * @see boofcv.alg.geo.f.FundamentalLinear7
	 */
	ESSENTIAL_7_LINEAR,
	/**
	 * <ul>
	 *     <li> Exactly 5 points (minimal solution)
	 *     <li> Multiple solutions
	 *     <li> Calibrated camera
	 * </ul>
	 *
	 * @see boofcv.alg.geo.f.EssentialNister5
	 */
	ESSENTIAL_5_NISTER

}
