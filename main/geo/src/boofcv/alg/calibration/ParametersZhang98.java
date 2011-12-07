/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.calibration;

import georegression.struct.point.Vector3D_F64;
import georegression.struct.so.Rodrigues;

/**
 * Parameters for batch optimization.
 *
 * @author Peter Abeles
 */
public class ParametersZhang98 {
	// camera calibration matrix
	double a,b,c,x0,y0;
	// radial distortion
	double distortion[];

	View[] views;

	public static class View
	{
		// TODO use 3D vector instead
		// description of rotation
		public Rodrigues rotation;
		// translation
		public Vector3D_F64 T;
	}
}
