/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort.radtan;

import boofcv.struct.distort.Point2Transform2_F64;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;

import static boofcv.alg.distort.radtan.RemoveRadialNtoN_F64.removeRadial;

/**
 * Converts the observed distorted pixels into normalized image coordinates.
 *
 * @author Peter Abeles
 */
public class RemoveRadialPtoN_F64 implements Point2Transform2_F64 {

	// principle point / image center
	protected double cx, cy;
	// other intrinsic parameters
	protected double fx,fy,skew;

	// distortion parameters
	protected RadialTangential_F64 params;

	// inverse of camera calibration matrix
	// These are the upper triangular elements in a 3x3 matrix
	private double a11,a12,a13,a22,a23;

	private double tol = GrlConstants.DCONV_TOL_A;

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
	public RemoveRadialPtoN_F64 setK( /**/double fx, /**/double fy, /**/double skew, /**/double cx, /**/double cy ) {

		this.fx = (double)fx;
		this.fy = (double)fy;
		this.skew = (double)skew;
		this.cx = (double)cx;
		this.cy = (double)cy;

		// analytic solution to matrix inverse
		a11 = (double)(1.0/fx);
		a12 = (double)(-skew/(fx*fy));
		a13 = (double)((skew*cy - cx*fy)/(fx*fy));
		a22 = (double)(1.0/fy);
		a23 = (double)(-cy/fy);

		return this;
	}

	public RemoveRadialPtoN_F64 setDistortion( /**/double[] radial, /**/double t1, /**/double t2 ) {
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
		// initial estimate of undistorted point
		out.x = a11*x + a12*y + a13;
		out.y = a22*y + a23;

		removeRadial(out.x, out.y, params.radial, params.t1, params.t2, out, tol );
	}
}