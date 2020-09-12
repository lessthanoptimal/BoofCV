/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import org.jetbrains.annotations.Nullable;

/**
 * Generalized camera model for bundle adjustment. By implementing this function you can swap in and out
 * arbitrary camera models.
 *
 * @author Peter Abeles
 */
public interface BundleAdjustmentCamera {

	/**
	 * Specifies the intrinsic camera parameters.
	 *
	 * @param parameters Array containing the parameters
	 * @param offset Location of first index in the array which the parameters are stored
	 */
	void setIntrinsic( double[] parameters, int offset );

	/**
	 * Copies the intrinsic camera into the array.
	 *
	 * @param parameters Array containing the parameters
	 * @param offset Location of first index in the array which the parameters are stored
	 */
	void getIntrinsic( double[] parameters, int offset );

	/**
	 * Project the 3D point in the camera reference frame onto the camera's image plane.
	 *
	 * @param camX 3D point in camera reference frame
	 * @param camY 3D point in camera reference frame
	 * @param camZ 3D point in camera reference frame
	 * @param output Storage for projected point.
	 */
	void project( double camX, double camY, double camZ, Point2D_F64 output );

	/**
	 * Computes the gradient for the projected pixel coordinate with partials for the input 3D point in camera
	 * reference frame and camera intrinsic parameters. <code>[x',y'] </code> is the projected pixel coordinate of
	 * the 3D point in camera reference frame.
	 *
	 * @param camX (Input) 3D point in camera reference frame
	 * @param camY (Input) 3D point in camera reference frame
	 * @param camZ (Input) 3D point in camera reference frame
	 * @param pointX (Output) Partial of projected x' relative to input camera point.<code>[@x'/@camX, @ x' / @ camY, @ x' / @ camZ]</code> length 3
	 * @param pointY (Output) Partial of projected y' relative to input camera point.<code>[@y'/@camX, @ y' / @ camY, @ y' / @ camZ]</code> length 3
	 * @param computeIntrinsic If true the calibX and calibY is computed. Otherwise they are ignored and can be null
	 * @param calibX (Output) Partial of projected x' relative to calibration parameters. length N
	 * @param calibY (Output) Partial of projected y' relative to calibration parameters. length N
	 */
	void jacobian( double camX, double camY, double camZ,
				   double[] pointX, double[] pointY,
				   boolean computeIntrinsic,
				   @Nullable double[] calibX, @Nullable double[] calibY );

	/**
	 * Returns the number of intrinsic parameters for this model. If the camera is known then the number of parameters
	 * is zero
	 *
	 * @return number of intrinsic parameters.
	 */
	int getIntrinsicCount();
}
