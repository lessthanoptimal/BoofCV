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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform3_F32;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F32;
import boofcv.struct.distort.Point3Transform2_F64;

/**
 * Interface for creating transform between distorted and undistorted pixel/unit sphere
 * coordinates for camera models that supports FOV more than 180 degrees.  In this situation
 * the entire undistorted image can't be rendered onto a flat plane.  Thus there are no functions
 * for working in undistorted pixels.
 *
 * @author Peter Abeles
 */
public interface LensDistortionWideFOV {

	/**
	 * Applies lens distortion.
	 * <pre>
	 * (Input) Undistorted unit sphere coordinate
	 * (Output) Distorted pixel coordinates
	 * </pre>
	 * @return Transform
	 */
	Point3Transform2_F64 distortStoP_F64();

	/**
	 * Applies lens distortion.
	 * <pre>
	 * (Input) Undistorted unit sphere coordinate
	 * (Output) Distorted pixel coordinates
	 * </pre>
	 * @return Transform
	 */
	Point3Transform2_F32 distortStoP_F32();

	/**
	 * Removes lens distortion.
	 * <pre>
	 * (Input) Distorted pixel coordinate
	 * (Output) Undistorted unit sphere coordinates
	 * </pre>
	 * @return Transform
	 */
	Point2Transform3_F64 undistortPtoS_F64();

	/**
	 * Removes lens distortion.
	 * <pre>
	 * (Input) Distorted pixel coordinate
	 * (Output) Undistorted unit sphere coordinates
	 * </pre>
	 * @return Transform
	 */
	Point2Transform3_F32 undistortPtoS_F32();


}
