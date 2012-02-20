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

package boofcv.alg.geo.d3.epipolar;

/**
 * <p>
 * Implementation of the EPnP algorithm from [1] for solving the PnP problem when N >= 4.  Given a calibrated
 * camera, n pairs of 2D point observations and the known 3D world coordinates, it solves the for camera's pose..
 * This solution is non-iterative and claims to be much faster and more accurate than the alternatives.  Works
 * for both planar and non-planar configurations.
 * </p>
 *
 * <p>
 * Expresses the n 3D point as a weighted sum of four virtual control points.  Problem then becomes to estimate
 * the coordinates of the control points in the camera referential, which can be done in O(n) time.
 * </p>
 *
 * <p>
 * [1]  Vincent Lepetit, Francesc Moreno-Noguer, and Pascal Fua, "EPnP: An Accurate O(n) Solution to the PnP Problem"
 * Int. J. Comput. Visionm, vol 81, issue 2, 2009
 * </p>
 *
 * @author Peter Abeles
 */
public class PnPLepetitEPnP {
}
