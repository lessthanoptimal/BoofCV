/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import georegression.struct.se.Se3_F64;

/**
 * Location of the found fiducial.
 *
 * @author Peter Abeles
 */
public class FiducialFound {
	int id;
	double width;
	Se3_F64 pose;

	public FiducialFound(int id, double width, Se3_F64 pose) {
		this.id = id;
		this.pose = pose;
	}

	public double getWidth() {
		return width;
	}

	public int getId() {
		return id;
	}

	public Se3_F64 getFiducialToCamera() {
		return pose;
	}
}
