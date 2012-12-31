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

package boofcv.abst.sfm;

import boofcv.struct.image.ImageBase;

/**
 * <P>
 * Interface for visual odometry from a single camera that provides 6-DOF pose.  The camera is assumed 
 * to be calibrated so that the "true" Euclidean motion can be extracted, up to a scale factor. 
 * Motion can only be found up to a scale factor because it is impossible to distinguish between a 
 * large motion and distant objects against a small motion and close objects when using a single camera.
 * </p>
 * 
 * <p>
 * Each time a new image arrives the function {@link #process} should be invoked and its return value
 * checked.  If false is returned then {@link #isFatal()} needs to be called to see if a fatal error
 * occurred.  If a fatal error occurred then the motion estimate has been reset relative to the first
 * frame in which {@Link isFatal} returns false.
 * </p>
 * 
 * @author Peter Abeles
 */
public interface MonocularVisualOdometry<T extends ImageBase> extends VisualOdometry{

	/**
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFatal}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @param input Next image in the sequence.
	 * @return If the motion estimate has been updated or not
	 */
	public boolean process( T input );

}
