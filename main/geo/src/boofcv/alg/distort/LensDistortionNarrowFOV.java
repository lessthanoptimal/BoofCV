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

import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;

/**
 * HInterface for creating transform between distorted and undistorted pixel/normalized-2D image
 * cordinates for camera models that supports FOV less than 180 degrees.
 *
 * @author Peter Abeles
 */
public interface LensDistortionNarrowFOV {

	/**
	 * Adds lens distortion
	 * @param pixelIn true if input is pixel coordinates or false if in  normalized image coordinates
	 * @param pixelOut true if output is pixel coordinates or false if in  normalized image coordinates
	 * @return Specified transform
	 */
	Point2Transform2_F64 distort_F64(boolean pixelIn, boolean pixelOut);

	/**
	 * Adds lens distortion
	 * @param pixelIn true if input is pixel coordinates or false if in  normalized image coordinates
	 * @param pixelOut true if output is pixel coordinates or false if in  normalized image coordinates
	 * @return Specified transform
	 */
	Point2Transform2_F32 distort_F32(boolean pixelIn, boolean pixelOut);

	/**
	 * Removes lens distortion
	 * @param pixelIn true if input is pixel coordinates or false if in  normalized image coordinates
	 * @param pixelOut true if output is pixel coordinates or false if in  normalized image coordinates
	 * @return Specified transform
	 */
	Point2Transform2_F64 undistort_F64(boolean pixelIn, boolean pixelOut);

	/**
	 * Removes lens distortion
	 * @param pixelIn true if input is pixel coordinates or false if in  normalized image coordinates
	 * @param pixelOut true if output is pixel coordinates or false if in  normalized image coordinates
	 * @return Specified transform
	 */
	Point2Transform2_F32 undistort_F32(boolean pixelIn, boolean pixelOut);

}
