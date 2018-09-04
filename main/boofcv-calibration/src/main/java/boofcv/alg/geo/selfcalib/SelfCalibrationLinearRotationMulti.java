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

package boofcv.alg.geo.selfcalib;

import boofcv.struct.calib.CameraPinhole;
import georegression.struct.homography.Homography2D_F64;

import java.util.List;

/**
 * Camera calibration for when the camera's motion is purely rotational and has no translational
 * component and camera parameters can change every frame. Linear constraints need to be specified
 * on camera parameters.
 *
 * @author Peter Abeles
 */
public class SelfCalibrationLinearRotationMulti {

	/**
	 * Assumes that the camera parameter are constant
	 * @param viewsI_to_view0 (Input) List of observed homographies
	 * @param calibration (Output) found calibration for each view.
	 * @return true if successful
	 */
	public boolean estimate(List<Homography2D_F64> viewsI_to_view0 , List<CameraPinhole> calibration ) {
		return true;
	}
}
