/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
 * Compute the inverse radial distortion using iteration.
 *
 * @author Peter Abeles
 */
public class RemoveRadialDistortionPixel implements PointTransform_F32 {

	// principle point / image center
	private float x_c,y_c;
	// radial distortion
	private float radial[];

	private DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
	private Point2D_F32 temp0 = new Point2D_F32();

	private float tol=1e-5f;
	
	public RemoveRadialDistortionPixel() {
	}

	public RemoveRadialDistortionPixel(double fx, double fy, double skew, double x_c, double y_c, double[] radial) {
		set(fx,fy,skew,x_c,y_c, radial);
	}

	public void setTolerance(float tol) {
		this.tol = tol;
	}

	/**
	 * Specify camera calibration parameters
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
	 * Removes radial distortion
	 *
	 * @param x Distorted x-coordinate pixel
	 * @param y Distorted y-coordinate pixel
	 * @param out Undistorted coordinate.
	 */
	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		float sum=0;

		temp0.x = x;
		temp0.y = y;

		// initial estimate of undistorted point
		GeometryMath_F32.mult(K_inv, temp0, out);

		float origX = out.x;
		float origY = out.y;
		
		float prevSum = 0;

		for( int iter = 0; iter < 20; iter++ ) {

			// estimate the radial distance
			float r2 = out.x*out.x + out.y*out.y;
			float r = r2;

			sum = 0;
			for( int i = 0; i < radial.length; i++ ) {
				sum += radial[i]*r;
				r *= r2;
			}

			out.x = origX/(1+sum);
			out.y = origY/(1+sum);
			
			if( Math.abs(prevSum-sum) <= tol ) {
				break;
			}
		}

		out.x = (x+x_c*sum)/(1+sum);
		out.y = (y+y_c*sum)/(1+sum);
	}
}
