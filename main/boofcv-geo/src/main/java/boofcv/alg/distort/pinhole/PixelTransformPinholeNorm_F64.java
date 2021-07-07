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

package boofcv.alg.distort.pinhole;

import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PixelTransform;
import georegression.struct.point.Point2D_F64;

/**
 * Converts an image pixel coordinate into a normalized pixel coordinate using the
 * camera's intrinsic parameters. Lens distortion must have already been removed.
 *
 * @author Peter Abeles
 */
public class PixelTransformPinholeNorm_F64 implements PixelTransform<Point2D_F64> {

	// inverse of camera calibration matrix
	// These are the upper triangular elements in a 3x3 matrix
	private double a11, a12, a13, a22, a23;

	public PixelTransformPinholeNorm_F64( PixelTransformPinholeNorm_F64 original ) {
		this.a11 = original.a11;
		this.a12 = original.a12;
		this.a13 = original.a13;
		this.a22 = original.a22;
		this.a23 = original.a23;
	}

	public PixelTransformPinholeNorm_F64() {}

	public PixelTransformPinholeNorm_F64 fset( CameraPinhole pinhole ) {
		return fset(pinhole.fx, pinhole.fy, pinhole.skew, pinhole.cx, pinhole.cy);
	}

	public PixelTransformPinholeNorm_F64 fset(/**/double fx, /**/double fy, /**/double skew, /**/double cx, /**/double cy ) {
		// analytic solution to matrix inverse
		a11 = (double)(1.0/fx);
		a12 = (double)(-skew/(fx*fy));
		a13 = (double)((skew*cy - cx*fy)/(fx*fy));
		a22 = (double)(1.0/fy);
		a23 = (double)(-cy/fy);
		return this;
	}

	@Override public void compute( int x, int y, Point2D_F64 out ) {
		out.x = a11*x + a12*y + a13;
		out.y = a22*y + a23;
	}

	@Override
	public PixelTransformPinholeNorm_F64 copyConcurrent() {
		return new PixelTransformPinholeNorm_F64(this);
	}
}
