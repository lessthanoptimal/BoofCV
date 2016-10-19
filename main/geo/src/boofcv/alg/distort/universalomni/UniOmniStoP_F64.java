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

package boofcv.alg.distort.universalomni;

import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point3Transform2_F64;
import georegression.struct.point.Point2D_F64;

/**
 * Forward projection model for {@link CameraUniversalOmni}.  Takes a 3D point in camera unit sphere
 * coordinates and converts it into a distorted pixel coordinate.  There are no checks to see if
 * it is physically possible to perform the forward projection, e.g. point could be outside the FOV.
 *
 * @author Peter Abeles
 */
public class UniOmniStoP_F64 implements Point3Transform2_F64 {

	public CameraUniversalOmni params;

	public UniOmniStoP_F64(CameraUniversalOmni params) {
		this.params = params;
	}

	public UniOmniStoP_F64() {
	}

	public CameraUniversalOmni getParams() {
		return params;
	}

	public void setParams(CameraUniversalOmni params) {
		this.params = params;
	}

	@Override
	public void compute(double x, double y, double z, Point2D_F64 out) {

		double[] radial = params.radial;
		double t1 = params.t1;
		double t2 = params.t2;

		double r2 = x*x + y*y;
		double ri2 = r2;

		double sum = 0;
		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*ri2;
			ri2 *= r2;
		}

		// apply mirror offset
		z += params.mirrorOffset;

		// compute normalized image coordinates
		x /= z;
		y /= z;

		// compute distorted normalized image coordinates
		x = x*( 1 + sum);
		y = y*( 1 + sum);

		x += 2*t1*x*y + t2*(r2 + 2*x*x);
		y += t1*(r2 + 2*y*y) + 2*t2*x*y;

		// project into pixels
		out.x = params.fx * x + params.skew * y + params.cx;
		out.y = params.fy * y + params.cy;
	}
}
