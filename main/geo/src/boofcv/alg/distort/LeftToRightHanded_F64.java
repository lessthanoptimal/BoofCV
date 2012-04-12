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
 * <p>
 * Changes the input for a {@link boofcv.struct.distort.PointTransform_F64} such that the pixel coordinate system has its
 * origin at the lower left hand corner and that positive y-axis is pointed up, thus making it right handed.
 * This transform is done using the following equation: y = height - y - 1
 * </p>
 *
 * <p>
 * WARNING: If using a calibrated camera distortion model make sure that the camera was calibrated using
 * the same adjusted pixels!
 * </p>
 *
 * @author Peter Abeles
 */
public class LeftToRightHanded_F64 implements PointTransform_F64 {

	int height;

	public LeftToRightHanded_F64(int imageHeight) {
		this.height = imageHeight - 1;
	}

	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		out.x = x;
		out.y = height - y;
	}
}
