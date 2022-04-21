/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.distort.Point3Transform2_F64;
import georegression.struct.point.Point2D_F64;

/**
 * Converts a spherical coordinate into a pixel coordinate.
 *
 * @author Peter Abeles
 */
public class SphereToNarrowPixel_F64 implements Point3Transform2_F64 {

	Point2Transform2_F64 projToPixel;

	public SphereToNarrowPixel_F64( Point2Transform2_F64 projToPixel ) {
		this.projToPixel = projToPixel;
	}

	@Override
	public void compute( double x, double y, double z, Point2D_F64 out ) {
		// Handle the singularity. There is no good way to handle this.
		if (z == 0.0) {
			projToPixel.compute(x, y, out);
		} else {
			// ignore the whole z < 0 issue with it being behind the camera. No way to tell it that it failed
			projToPixel.compute(x/z, y/z, out);
		}
	}

	@Override
	public Point3Transform2_F64 copyConcurrent() {
		return new SphereToNarrowPixel_F64(projToPixel.copyConcurrent());
	}
}
