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
	private float cx, cy;
	// other intrinsic parameters
	private float fx,fy,skew;

	// distortion parameters
	protected RadialTangential_F32 params;

	private DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
	private Point2D_F32 temp0 = new Point2D_F32();

	public AddRadialPtoP_F32() {
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
	public AddRadialPtoP_F32 setK(double fx, double fy, double skew, double cx, double cy) {

		this.fx = (float)fx;
		this.fy = (float)fy;
		this.skew = (float)skew;
		this.cx = (float)cx;
		this.cy = (float)cy;

		K_inv.zero();
		K_inv.set(0,0,fx);
		K_inv.set(1,1,fy);
		K_inv.set(0,1,skew);
		K_inv.set(0,2,cx);
		K_inv.set(1,2,cy);
		K_inv.set(2,2,1);

		CommonOps.invert(K_inv);

		return this;
	}

	/**
	 * Specify intrinsic camera parameters
	 *
	 * @param radial Radial distortion parameters
	 */
	public AddRadialPtoP_F32 setDistortion(double[] radial, double t1, double t2) {
		params = new RadialTangential_F32().set(radial,t1,t2);
		return this;
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

		float radial[] = params.radial;
		float t1 = params.t1,t2 = params.t2;

		temp0.x = x;
		temp0.y = y;

		GeometryMath_F32.mult(K_inv, temp0, out);

		float r2 = out.x*out.x + out.y*out.y;
		float ri2 = r2;

		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*ri2;
			ri2 *= r2;
		}

		float tx = 2*t1*out.x*out.y + t2*(r2 + 2*out.x*out.x);
		float ty = t1*(r2 + 2*out.y*out.y) + 2*t2*out.x*out.y;

		out.x = x + (x - cx)*sum + fx*tx + skew*ty;
		out.y = y + (y - cy)*sum + fy*ty;
	}
}
