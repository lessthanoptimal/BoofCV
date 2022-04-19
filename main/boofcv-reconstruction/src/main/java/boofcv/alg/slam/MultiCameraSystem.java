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

package boofcv.alg.slam;

import boofcv.alg.distort.LensDistortionWideFOV;
import georegression.struct.se.Se3_F64;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Describes a known multi camera system with intrinsics and extrinsics. Extrinsics are relative to a common
 * reference frame known as sensor frame.
 *
 * @author Peter Abeles
 */
public class MultiCameraSystem {
	public final List<Camera> cameras = new ArrayList<>();
	public final Map<String, Camera> nameToCameras = new HashMap<>();

	public void addCamera( String name, Se3_F64 cameraToSensor, LensDistortionWideFOV intrinsics ) {
		var cam = new Camera(name, cameraToSensor, intrinsics);
		cameras.add(cam);
		nameToCameras.put(name, cam);
	}

	/** Computes the extrinsics relationship between the two views from src to dst */
	public Se3_F64 computeSrcToDst( String src, String dst, @Nullable Se3_F64 src_to_dst ) {
		if (src_to_dst == null)
			src_to_dst = new Se3_F64();

		return src_to_dst;
	}

	public Camera lookupCamera( String cameraID ) {
		return Objects.requireNonNull(nameToCameras.get(cameraID));
	}

	public static class Camera {
		public final String name;
		public final LensDistortionWideFOV intrinsics;
		public final Se3_F64 cameraToSensor = new Se3_F64();

		public Camera( String name, Se3_F64 cameraToSensor, LensDistortionWideFOV intrinsics ) {
			this.name = name;
			this.cameraToSensor.setTo(cameraToSensor);
			this.intrinsics = intrinsics;
		}
	}
}
