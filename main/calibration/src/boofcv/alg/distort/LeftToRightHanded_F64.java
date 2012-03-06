/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.PointTransform_F64;
import georegression.struct.point.Point2D_F64;

/**
 * It is common practice to have the +y axis point downward in computer images.  However,
 * in 3D computer vision this creates a left handed coordinate system and +z points behind
 * the camera.  Many algorithms assume +z is in front of the camera so the way around this
 * problem is to invert the sign of y.
 *
 * @author Peter Abeles
 */
public class LeftToRightHanded_F64 implements PointTransform_F64 {

	PointTransform_F64 alg;

	public LeftToRightHanded_F64(PointTransform_F64 alg) {
		this.alg = alg;
	}

	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		alg.compute(x,y,out);
		out.y = -out.y;
	}
}
