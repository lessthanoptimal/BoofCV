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
 * TODO write
 * 
 * @author Peter Abeles
 */
public interface StereoVisualOdometry<T extends ImageBase> extends VisualOdometry{

	/**
	 * Specifies intrinsic and extrinsic parameters for the stereo camera system.
	 *
	 * @param parameters stereo calibration
	 */
	public void setCalibration( StereoParameters parameters );

	/**
	 * Forgets all past history and sets itself into its initial state.
	 */
	public void reset();

	/**
	 * TODO Update
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFatal}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @return If the motion estimate has been updated or not
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
