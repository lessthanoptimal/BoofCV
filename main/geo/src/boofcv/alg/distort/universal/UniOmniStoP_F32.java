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

package boofcv.alg.distort.universal;

import boofcv.alg.distort.radtan.RadialTangential_F32;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.Point3Transform2_F32;
import georegression.struct.point.Point2D_F32;

/**
 * Forward projection model for {@link CameraUniversalOmni}.  Takes a 3D point in camera unit sphere
 * coordinates and converts it into a distorted pixel coordinate.  There are no checks to see if
 * it is physically possible to perform the forward projection, e.g. point could be outside the FOV.
 *
 * @author Peter Abeles
 */
public class UniOmniStoP_F32 implements Point3Transform2_F32 {
	float mirrorOffset;
	// principle point / image center
	protected float cx, cy;
	// other intrinsic parameters
	protected float fx,fy,skew;

	// storage for distortion terms
	protected RadialTangential_F32 distortion = new RadialTangential_F32();

	public UniOmniStoP_F32(CameraUniversalOmni model) {
		setModel(model);
	}

	public UniOmniStoP_F32() {
	}

	public void setModel(CameraUniversalOmni model) {
		this.mirrorOffset = (float)model.mirrorOffset;

		distortion.set(model.radial,model.t1,model.t2);

		this.cx = (float)model.cx;
		this.cy = (float)model.cy;
		this.fx = (float)model.fx;
		this.fy = (float)model.fy;
		this.skew = (float)model.skew;
	}

	@Override
	public void compute(float x, float y, float z, Point2D_F32 out) {

		float[] radial = distortion.radial;
		float t1 = distortion.t1;
		float t2 = distortion.t2;

		// apply mirror offset
		z += mirrorOffset;

		// compute normalized image coordinates
		x /= z;
		y /= z;

		float r2 = x*x + y*y;
		float ri2 = r2;

		float sum = 0;
		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*ri2;
			ri2 *= r2;
		}

		// compute distorted normalized image coordinates
		x = x*( 1.0f + sum);
		y = y*( 1.0f + sum);

		x += 2.0f*t1*x*y + t2*(r2 + 2.0f*x*x);
		y += t1*(r2 + 2.0f*y*y) + 2.0f*t2*x*y;

		// project into pixels
		out.x = fx * x + skew * y + cx;
		out.y = fy * y + cy;
	}
}
