/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Intrinsic and extrinsic calibration for a multi-camera calibration system. Extrinsics for each camera is
 * in reference to a global reference frame that is typically camera 0, but not always. Each camera can
 * have its own different camera model, but typically they will all be the same.
 *
 * @author Peter Abeles
 */
public class MultiCameraCalib implements Serializable {
	// serialization version
	public static final long serialVersionUID = 1L;

	/**
	 * Intrinsic camera parameters and model for each camera.
	 */
	@Getter final List<CameraModel> intrinsics = new ArrayList<>();

	/**
	 * Extrinsics for each camera from camera to the world frame. World frame is typically camera[0] but not always
	 */
	@Getter final List<Se3_F64> extrinsicsCamToWorld = new ArrayList<>();

	public <Cam extends CameraModel>Cam getIntrinsics( int index ) {
		return (Cam)intrinsics.get(index);
	}

	public Se3_F64 getExtrinsics( int index ) {
		return extrinsicsCamToWorld.get(index);
	}

	/**
	 * Returns the distance between two cameras
	 */
	public double getBaseline(int cam0, int cam1) {
		return computeExtrinsics(cam0, cam1, null).T.norm();
	}

	/**
	 * Returns the transform from cam0 to cam1 reference frame
	 * @param cam0 Index of camera 0
	 * @param cam1 Index of camera 1
	 * @param cam0_to_cam1 (Output) Transform from cam0 to cam 1. Can be null.
	 * @return The transform.
	 */
	public Se3_F64 computeExtrinsics( int cam0, int cam1, @Nullable Se3_F64 cam0_to_cam1 ) {
		if (cam0_to_cam1 == null)
			cam0_to_cam1 = new Se3_F64();


		return cam0_to_cam1;
	}
}
