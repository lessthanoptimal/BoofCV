/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.PointTransform_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Given an undistorted pixel coordinate, compute the distorted coordinate.
 *
 * @author Peter Abeles
 */
public class AddRadialPtoP_F32 implements PointTransform_F32 {

	// principle point / image center
	private float x_c,y_c;
	// radial distortion
	private float radial[];

	private DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
	private Point2D_F32 temp0 = new Point2D_F32();

	public AddRadialPtoP_F32() {
	}

	public AddRadialPtoP_F32(double fx, double fy, double skew, double x_c, double y_c, double... radial) {
		set(fx,fy,skew,x_c,y_c, radial);
	}

	/**
	 * Specify camera calibration parameters
	 *
	 * @param fx Focal length x-axis in pixels
	 * @param fy Focal length y-axis in pixels
	 * @param skew skew in pixels
	 * @param x_c camera center x-axis in pixels
	 * @param y_c center center y-axis in pixels
	 * @param radial Radial distortion parameters
	 */
	public void set(double fx, double fy, double skew, double x_c, double y_c, double[] radial) {

		K_inv.set(0,0,fx);
		K_inv.set(1,1,fy);
		K_inv.set(0,1,skew);
		K_inv.set(0,2,x_c);
		K_inv.set(1,2,y_c);
		K_inv.set(2,2,1);

		CommonOps.invert(K_inv);

		this.x_c = (float)x_c;
		this.y_c = (float)y_c;

		this.radial = new float[radial.length];
		for( int i = 0; i < radial.length; i++ ) {
			this.radial[i] = (float)radial[i];
		}
	}

	/**
	 * Adds radial distortion
	 *
	 * @param x Undistorted x-coordinate pixel
	 * @param y Undistorted y-coordinate pixel
	 * @param out Distorted pixel coordinate.
	 */
	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		float sum = 0;

		temp0.x = x;
		temp0.y = y;

		GeometryMath_F32.mult(K_inv, temp0, out);

		float r2 = out.x*out.x + out.y*out.y;

		float r = r2;

		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*r;
			r *= r2;
		}

		out.x = x + (x-x_c)*sum;
		out.y = y + (y-y_c)*sum;
	}
}
