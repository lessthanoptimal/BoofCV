/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
 * Converts the observed distorted pixels into normalized image coordinates.
 *
 * @author Peter Abeles
 */
public class RemoveRadialPtoN_F64 implements PointTransform_F64 {

	// principle point / image center
	protected double cx, cy;
	// other intrinsic parameters
	protected double fx,fy,skew;

	// distortion parameters
	protected RadialTangential_F64 params;

	// radial distortion magnitude
	protected double sum;
	// found tangential distortion
	protected double tx,ty;

	// inverse of camera calibration matrix
	protected DenseMatrix64F K_inv = new DenseMatrix64F(3,3);

	private double tol=1e-10;

	public RemoveRadialPtoN_F64() {
	}

	public RemoveRadialPtoN_F64( double tol ) {
		this.tol = tol;
	}

	public void setTolerance(double tol) {
		this.tol = tol;
	}

	/**
	 * Specify camera calibration parameters
	 *
	 * @param fx Focal length x-axis in pixels
	 * @param fy Focal length y-axis in pixels
	 * @param skew skew in pixels
	 * @param cx camera center x-axis in pixels
	 * @param cy center center y-axis in pixels
	 */
	public RemoveRadialPtoN_F64 setK(double fx, double fy, double skew, double cx, double cy ) {

		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
		this.cx = cx;
		this.cy = cy;

		K_inv.set(0,0,fx);
		K_inv.set(1,1,fy);
		K_inv.set(0,1,skew);
		K_inv.set(0,2,cx);
		K_inv.set(1,2,cy);
		K_inv.set(2,2,1);

		CommonOps.invert(K_inv);

		return this;
	}

	public RemoveRadialPtoN_F64 setDistortion( double[] radial, double t1, double t2 ) {
		params = new RadialTangential_F64(radial,t1,t2);
		return this;
	}

	/**
	 * Removes radial distortion
	 *
	 * @param x Distorted x-coordinate pixel
	 * @param y Distorted y-coordinate pixel
	 * @param out Undistorted normalized coordinate.
	 */
	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		out.x = x;
		out.y = y;

		double radial[] = params.radial;
		double t1 = params.t1,t2 = params.t2;

		// initial estimate of undistorted point
		GeometryMath_F64.mult(K_inv, out, out);

		double origX = x = out.x;
		double origY = y = out.y;

		double prevSum = 0;

		for( int iter = 0; iter < 20; iter++ ) {

			// estimate the radial distance
			double r2 = x*x + y*y;
			double ri2 = r2;

			sum = 0;
			for( int i = 0; i < radial.length; i++ ) {
				sum += radial[i]*ri2;
				ri2 *= r2;
			}

			tx = 2*t1*x*y + t2*(r2 + 2*x*x);
			ty = t1*(r2 + 2*y*y) + 2*t2*x*y;

			x = (origX - tx)/(1+sum);
			y = (origY - ty)/(1+sum);

			if( Math.abs(prevSum-sum) <= tol ) {
				break;
			} else {
				prevSum = sum;
			}
		}
		out.set(x,y);
	}
}