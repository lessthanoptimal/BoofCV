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

package boofcv.abst.sfm.d3;

/**
 * @author Peter Abeles
 */

import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

/**
 * <p>
 * Visual odometry that estimate the camera's ego-motion in Euclidean space using a camera image and
 * a depth image.  Camera motion is estimated relative to the first frame in the left camera's point of view.
 * </p>
 *
 * <p>
 * The following is a set of assumptions and behaviors that all implementations of this interface must follow:
 * <ul>
 * <li>Visual and depth images must be captured simultaneously</li>
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
 * @param <Vis> Visual camera sensor
 * @param <Depth> Depth camera sensor
 */

// DEVELOPMENT NOTE: This right now assumes that the depth image contains depth.  A transform could be added which
//                   would convert it from the internal value into depth. This would allow a sparse depth calculation
//                   right now the depth of the whole image must be computed.
public interface DepthVisualOdometry<Vis extends ImageBase, Depth extends ImageGray>
		extends VisualOdometry<Se3_F64>
{
	/**
	 * Specifies the intrinsic parameters for the visual camera and the transform from visual to depth pixels.
	 *
	 * @param paramVisual Intrinsic parameters for visual camera
	 * @param visToDepth Transform from visual camera pixels into depth camera pixels
	 */
	public void setCalibration(CameraPinholeRadial paramVisual , PixelTransform2_F32 visToDepth );

	/**
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFault()}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @param visual Image from visual camera
	 * @param depth Image from depth sensor
	 * @return true if the motion estimate has been updated and false if not
	 */
	public boolean process(Vis visual , Depth depth );

	/**
	 * Type of visual images it can process.
	 *
	 * @return The image type
	 */
	public ImageType<Vis> getVisualType();

	/**
	 * Type of depth images it can process.
	 *
	 * @return The image type
	 */
	public Class<Depth> getDepthType();

}
