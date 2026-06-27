/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.distort.Point2Transform3_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

public class PixelToNormUsingSphere_F64 implements Point2Transform2_F64 {
	Point2Transform3_F64 pixelToPointing;

	Point3D_F64 pointing = new Point3D_F64();

	public PixelToNormUsingSphere_F64( Point2Transform3_F64 pixelToPointing ) {
		this.pixelToPointing = pixelToPointing;
	}

	@Override public void compute( double x, double y, Point2D_F64 out ) {
		pixelToPointing.compute(x, y, pointing);
		out.x = pointing.x/pointing.z;
		out.y = pointing.y/pointing.z;
	}

	@Override public Point2Transform2_F64 copyConcurrent() {
		return new PixelToNormUsingSphere_F64(pixelToPointing.copyConcurrent());
	}
}
