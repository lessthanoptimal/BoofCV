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

import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageBase;

/**
 * <p>
 * Stereo visual odometry algorithms that estimate the camera's ego-motion in Euclidean space using a pair of
 * stereo images.  Camera motion is estimated relative to the first frame in the left camera's point of view.
 * </p>
 * <p>
 * The following is a set of assumptions and behaviors that all implementations of this interface must follow:
 * <ul>
 * <li>Stereo images must be captured simultaneously</li>
 * <li>Cameras must have a global shutter</li>
 * <li>Calibration parameters can be changed at any time, but must be set at least once before processing an image.</li>
 * <li>If process returns false then the motion could not be estimated and isFault() should be checked</li>
 * <li>If isFault() is true then the reset() should be called since it can't estimate motion any more</li>
 * <li>reset() puts back into its initial state</li>
 * </ul>
 * </p>
 *
 * <p>
 * Optional interfaces are provided for accessing internal features.
 * <ul>
 *     <li>{@link boofcv.abst.sfm.AccessPointTracks3D}</li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public interface StereoVisualOdometry<T extends ImageBase> extends VisualOdometry {

	/**
	 * Specifies intrinsic and extrinsic parameters for the stereo camera system. Can be called
	 * at any time, but must be called at least once before {@link #process) can be called.
	 *
	 * @param parameters stereo calibration
	 */
	public void setCalibration( StereoParameters parameters );

	/**
	 * Forgets all past history and sets itself into its initial state.
	 */
	public void reset();

	/**
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFatal}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @return true if the motion estimate has been updated and false if not
	 */
	public boolean process(T leftImage , T rightImage );

	/**
	 * True if a major fault has occurred and localization was lost. This value only needs to be
	 * checked when process() returns false.
	 *
	 * @return true if a major fault occurred.
	 */
	public boolean isFault();

	/**
	 * Type of input images it can process.
	 *
	 * @return The image type
	 */
	public Class<T> getImageType();

}
