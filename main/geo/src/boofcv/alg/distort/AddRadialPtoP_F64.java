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

import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Given an undistorted pixel coordinate, compute the distorted coordinate.
 *
 * @author Peter Abeles
 */
public class AddRadialPtoP_F64 implements PointTransform_F64 {

	// principle point / image center
	private double cx, cy;
	// radial distortion
	private double radial[];

	private DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
	private Point2D_F64 temp0 = new Point2D_F64();

	public AddRadialPtoP_F64() {
	}

	public AddRadialPtoP_F64(double fx, double fy, double skew, double cx, double cy, double... radial) {
		set(fx,fy,skew, cx, cy, radial);
	}

	/**
	 * Specify camera calibration parameters
	 *
	 * @param fx Focal length x-axis in pixels
	 * @param fy Focal length y-axis in pixels
	 * @param skew skew in pixels
	 * @param cx camera center x-axis in pixels
	 * @param cy center center y-axis in pixels
	 * @param radial Radial distortion parameters
	 */
	public void set(double fx, double fy, double skew, double cx, double cy, double[] radial) {

		K_inv.set(0, 0, fx);
		K_inv.set(1,1,fy);
		K_inv.set(0,1,skew);
		K_inv.set(0,2,cx);
		K_inv.set(1,2,cy);
		K_inv.set(2,2,1);

		CommonOps.invert(K_inv);

		this.cx = cx;
		this.cy = cy;

		this.radial = new double[radial.length];
		for( int i = 0; i < radial.length; i++ ) {
			this.radial[i] = radial[i];
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
	public void compute(double x, double y, Point2D_F64 out) {
		float sum = 0;

		temp0.x = x;
		temp0.y = y;

		GeometryMath_F64.mult(K_inv, temp0, out);

		double r2 = out.x*out.x + out.y*out.y;

		double r = r2;

		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*r;
			r *= r2;
		}

		out.x = x + (x- cx)*sum;
		out.y = y + (y- cy)*sum;
	}
}
