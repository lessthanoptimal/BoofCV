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

package boofcv.alg.geo;

import org.ddogleg.fitting.modelset.DistanceFromModel;

/**
 * Compute the distance between a model and an observation for a stereo camera configuration in pixels
 * when the observations are in normalized image coordinates.
 *
 * @author Peter Abeles
 */
public interface DistanceModelStereoPixels<Model,Point> extends DistanceFromModel<Model,Point>
{

	/**
	 * Specifies intrinsic parameters
	 *
	 * @param cam1_fx intrinsic parameter: focal length x for camera 1
	 * @param cam1_fy intrinsic parameter: focal length y for  camera 1
	 * @param cam1_skew intrinsic parameter: skew for camera 1 (usually zero)
	 * @param cam2_fx intrinsic parameter: focal length x for camera 2
	 * @param cam2_fy intrinsic parameter: focal length y for camera 2
	 * @param cam2_skew intrinsic parameter: skew for camera 2 (usually zero)
	 */
	public void setIntrinsic(double cam1_fx, double cam1_fy , double cam1_skew ,
							 double cam2_fx, double cam2_fy , double cam2_skew );
}
