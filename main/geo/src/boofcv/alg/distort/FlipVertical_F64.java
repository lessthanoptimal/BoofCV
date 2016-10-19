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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;

/**
 * <p>
 * Flips the image along the vertical axis.  Equivalent to applying this transform, y = height - y - 1.
 * Useful when the image coordinate system is left handed and needs to be right handed.
 * </p>
 *
 * @author Peter Abeles
 */
public class FlipVertical_F64 implements Point2Transform2_F64 {

	int height;

	public FlipVertical_F64(int imageHeight) {
		this.height = imageHeight - 1;
	}

	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		out.x = x;
		out.y = height - y;
	}
}
