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

package boofcv.abst.geo.bundle;

import georegression.struct.point.Point2D_F64;

/**
 * Generalized camera model for bundle adjustment. By implementing this function you can swap in and out
 * arbitrary camera models.
 *
 * @author Peter Abeles
 */
public abstract class BundleAdjustmentCamera {

	/**
	 * Specifies the input parameters.
	 * @param parameters Array containing the parameters
	 * @param offset Location of first index in the array which the parameters are stored
	 */
	public abstract void setParameters( double parameters[] , int offset );

	/**
	 * Returns the current intrinsic camera parameters
	 */
	public abstract void getParameters( double parameters[] , int offset );

	/**
	 * Project the 3D point in the camera reference frame onto the camera's image plane.
	 * @param camX 3D point in camera reference frame
	 * @param camY 3D point in camera reference frame
	 * @param camZ 3D point in camera reference frame
	 * @param output Storage for projected point.
	 */
	public abstract void project(double camX , double camY , double camZ , Point2D_F64 output );

	/**
	 * Computes the gradient for the input (a.k.a. cam point) parameters as well as calibration parameters.
	 *
	 * @param camX 3D point in camera reference frame
	 * @param camY 3D point in camera reference frame
	 * @param camZ 3D point in camera reference frame
	 * @param inputX Array to store the gradient of X with respect to input point. length 3
	 * @param inputY Array to store the gradient of Y with respect to input point. length 3
	 * @param calibX Array to store the gradient of X with respect to calibration parameters. length N
	 * @param calibY Array to store the gradient of Y with respect to calibration parameters. length N
	 */
	public abstract void jacobian(double camX , double camY , double camZ,
								  double inputX[] , double inputY[] ,
								  double calibX[] , double calibY[] );

	/**
	 * Computes the gradient for input variables only. Used when camera parameters is assumed to be known
	 *
	 * @param camX 3D point in camera reference frame
	 * @param camY 3D point in camera reference frame
	 * @param camZ 3D point in camera reference frame
	 * @param inputX Array to store the gradient of X with respect to input point. length 3
	 * @param inputY Array to store the gradient of Y with respect to input point. length 3
	 */
	public abstract void jacobian(double camX , double camY , double camZ,
								  double inputX[] , double inputY[] );

	/**
	 * Returns the number of parameters in this model.
	 * @return number of parameters.
	 */
	public abstract int getParameterCount();
}
