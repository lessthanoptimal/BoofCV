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
import georegression.struct.se.SpecialEuclideanOps_F64;
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
public class MultiCameraCalibParams implements Serializable {
	// serialization version
	public static final long serialVersionUID = 1L;

	/**
	 * Intrinsic camera parameters and model for each camera.
	 */
	@Getter public final List<CameraModel> intrinsics = new ArrayList<>();

	/**
	 * Extrinsics for each camera. Camera to the common sensor frame. Typically the common sensor frame is
	 * defined as camera[0], but not always.
	 */
	@Getter public final List<Se3_F64> camerasToSensor = new ArrayList<>();

	public void reset() {
		intrinsics.clear();
		camerasToSensor.clear();
	}

	public <Cam extends CameraModel> Cam getIntrinsics( int index ) {
		return (Cam)intrinsics.get(index);
	}

	public Se3_F64 getCameraToSensor( int index ) {
		return camerasToSensor.get(index);
	}

	/**
	 * Returns the distance between two cameras
	 */
	public double getBaseline( int cam0, int cam1 ) {
		return computeExtrinsics(cam0, cam1, null).T.norm();
	}

	/**
	 * Returns the transform from cam0 to cam1 reference frame
	 *
	 * @param cam0 Index of camera 0
	 * @param cam1 Index of camera 1
	 * @param cam0_to_cam1 (Output) Storage for transform from cam[0] to cam[1]. Can be null.
	 * @return The transform.
	 */
	public Se3_F64 computeExtrinsics( int cam0, int cam1, @Nullable Se3_F64 cam0_to_cam1 ) {
		if (cam0_to_cam1 == null)
			cam0_to_cam1 = new Se3_F64();

		Se3_F64 camera0_to_sensor = camerasToSensor.get(cam0);
		Se3_F64 camera1_to_sensor = camerasToSensor.get(cam1);

		camera0_to_sensor.concat(camera1_to_sensor.invert(null), cam0_to_cam1);

		return cam0_to_cam1;
	}

	/**
	 * Compares 'this' to the passed in parameters and returns true if they have identical values. This
	 * function is only partially implemented because all the camera models don't implement isIdentical() yet.
	 */
	public boolean isIdentical_WARNING( MultiCameraCalibParams o ) {
		if (intrinsics.size() != o.intrinsics.size())
			return false;
		if (camerasToSensor.size() != o.camerasToSensor.size())
			return false;

		for (int i = 0; i < intrinsics.size(); i++) {
			CameraModel a = intrinsics.get(i);
			CameraModel b = o.intrinsics.get(i);
			if (a.getClass() != b.getClass())
				return false;

			// NOTE: This is where it should compare for equality by model
		}

		for (int i = 0; i < camerasToSensor.size(); i++) {
			Se3_F64 a = camerasToSensor.get(i);
			Se3_F64 b = o.camerasToSensor.get(i);

			if (!SpecialEuclideanOps_F64.isIdentical(a, b, 0, 0))
				return false;
		}

		return true;
	}
}
