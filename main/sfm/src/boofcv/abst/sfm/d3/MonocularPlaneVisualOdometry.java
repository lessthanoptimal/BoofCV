/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.sfm.d3;

import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

/**
 * Estimates the camera's motion relative to the ground plane.  The world being viewed is assumed to be planar.
 * The transform from the ground plane to the camera view is assumed to be known.   Both the intrinsic and
 * extrinsic parameters are allowed to change between image frames.  Since the transform between the ground and
 * camera is known, there is no scale ambiguity.
 *
 * @author Peter Abeles
 */
public interface MonocularPlaneVisualOdometry<T extends ImageBase> extends VisualOdometry<Se3_F64> {

	/**
	 * Specifies the camera's intrinsic and extrinsic parameters.  Can be changed at any time.
	 *
	 * @param param Camera calibration parameters
	 */
	public void setCalibration( MonoPlaneParameters param );

	/**
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFault}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @param input Next image in the sequence.
	 * @return If the motion estimate has been updated or not
	 */
	public boolean process( T input );

	/**
	 * Type of input images it can process.
	 *
	 * @return The image type
	 */
	public ImageType<T> getImageType();

}
