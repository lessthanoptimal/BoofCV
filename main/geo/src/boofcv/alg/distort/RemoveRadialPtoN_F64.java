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
 * Converts the observed distorted pixels into normalized image coordinates.
 *
 * @author Peter Abeles
 */
public class RemoveRadialPtoN_F64 implements PointTransform_F64 {

	// principle point / image center
	protected double x_c,y_c;
	// radial distortion
	protected double radial[];

	// radial distortion magnitude
	protected double sum;

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
	 * @param x_c camera center x-axis in pixels
	 * @param y_c center center y-axis in pixels
	 * @param radial Radial distortion parameters
	 */
	public void set(double fx, double fy, double skew, double x_c, double y_c, double... radial) {

		K_inv.set(0,0,fx);
		K_inv.set(1,1,fy);
		K_inv.set(0,1,skew);
		K_inv.set(0,2,x_c);
		K_inv.set(1,2,y_c);
		K_inv.set(2,2,1);

		CommonOps.invert(K_inv);

		this.x_c = x_c;
		this.y_c = y_c;

		this.radial = new double[radial.length];
		System.arraycopy(radial, 0, this.radial, 0, radial.length);
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

		// initial estimate of undistorted point
		GeometryMath_F64.mult(K_inv, out, out);

		double origX = out.x;
		double origY = out.y;

		double prevSum = 0;

		for( int iter = 0; iter < 20; iter++ ) {

			// estimate the radial distance
			double r2 = out.x*out.x + out.y*out.y;
			double r = r2;

			sum = 0;
			for( int i = 0; i < radial.length; i++ ) {
				sum += radial[i]*r;
				r *= r2;
			}

			out.x = origX/(1+sum);
			out.y = origY/(1+sum);

			if( Math.abs(prevSum-sum) <= tol ) {
				break;
			} else {
				prevSum = sum;
			}
		}
	}
}