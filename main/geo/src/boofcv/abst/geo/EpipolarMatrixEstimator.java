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

/**
 * <p>
 * Marker interface for computing the fundamental, essential, or homography matrix given a set of associated pairs.
 * Each of these matrices describes the relationship between two views.
 * </p>
 *
 * <p>
 * For Fundamental and Essential matrices the following constraint is always true:
 * x2<sup>T</sup>*F*x1 = 0, where F is the 3x3 epipolar matrix, x1 = keyLoc, and x2 = currLoc.
 * </p>
 *
 * <p>
 * Image coordinates: For fundamental matrix the input should be in pixels, for Essential it should be in normalized
 * image coordinates, and for homography it can be either.
 * </p>
 *
 * @author Peter Abeles
 */
public interface EpipolarMatrixEstimator {

}
