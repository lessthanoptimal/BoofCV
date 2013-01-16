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

package boofcv.struct.sfm;

import georegression.struct.se.Se3_F64;

/**
 * Specifies the pose of a stereo camera system as a kinematic chain relative to camera 0.
 *
 * @author Peter Abeles
 */
public class StereoPose {
	public Se3_F64 worldToCam0;
	public Se3_F64 cam0ToCam1;

	public StereoPose(Se3_F64 worldToCam0, Se3_F64 cam0ToCam1) {
		this.worldToCam0 = worldToCam0;
		this.cam0ToCam1 = cam0ToCam1;
	}

	public StereoPose() {
		worldToCam0 = new Se3_F64();
		cam0ToCam1 = new Se3_F64();
	}
}
