/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.distort;

import georegression.struct.point.Point2D_F64;

/**
 * A transform which applies no transform. Can be used to avoid checking for null
 *
 * @author Peter Abeles
 */
public class DoNothing2Transform2_F64 implements Point2Transform2_F64 {
	@Override
	public void compute( double x, double y, Point2D_F64 out ) {
		out.x = x;
		out.y = y;
	}

	@Override
	public DoNothing2Transform2_F64 copyConcurrent() {
		return new DoNothing2Transform2_F64();
	}
}
