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

package boofcv.alg.geo;

import boofcv.struct.distort.Point3Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import lombok.Setter;

/**
 * Simple function for converting error in pointing vector coordinates to pixels using
 * intrinsic camera parameters. Better to use tested code than cut and pasting.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PointingToProjectedPixelError {
	@Getter @Setter Point3Transform2_F64 camera;

	// Storage for projected pixels in both views
	Point2D_F64 pixelA = new Point2D_F64();
	Point2D_F64 pixelB = new Point2D_F64();

	public PointingToProjectedPixelError( Point3Transform2_F64 camera ) {
		this.camera = camera;
	}

	public PointingToProjectedPixelError() {}

	public double errorSq( Point3D_F64 a, Point3D_F64 b ) {
		return errorSq(a.x, a.y, a.z, b.x, b.y, b.z);
	}

	public double errorSq( double a_x, double a_y, double a_z, double b_x, double b_y, double b_z ) {
		camera.compute(a_x, a_y, a_z, pixelA);
		camera.compute(b_x, b_y, b_z, pixelB);

		return pixelA.distance2(pixelB);
	}
}
