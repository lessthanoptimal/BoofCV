/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.calib;

import georegression.struct.se.Se3_F64;

/**
 * Calibration parameters when the intrinsic parameters for a single camera is known and the location
 * of the camera relative to the ground plane.  In the plane's reference frame, the plane is parallel
 * to the x-z plane and contains point (0,0,0).
 *
 * @author Peter Abeles
 */
public class MonoPlaneParameters {
	/**
	 * Intrinsic parameters for the camera
	 */
	public CameraPinholeRadial intrinsic;
	/**
	 * Extrinsic parameters for the camera.  Transform from plane to camera coordinate system.  This is technically
	 * over specified since only orientation (3-DOF) and distance from plane (1-DOF) is needed.  The extra
	 * degrees of freedom can be used to specify the local coordinate (e.g. robot) completely and simplify
	 * various camera transforms.
	 */
	public Se3_F64 planeToCamera;

	public MonoPlaneParameters(CameraPinholeRadial intrinsic, Se3_F64 planeToCamera) {
		this.intrinsic = intrinsic;
		this.planeToCamera = planeToCamera;
	}

	public MonoPlaneParameters() {
	}

	public CameraPinholeRadial getIntrinsic() {
		return intrinsic;
	}

	public void setIntrinsic(CameraPinholeRadial intrinsic) {
		this.intrinsic = intrinsic;
	}

	public Se3_F64 getPlaneToCamera() {
		return planeToCamera;
	}

	public void setPlaneToCamera(Se3_F64 planeToCamera) {
		this.planeToCamera = planeToCamera;
	}
}
