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

package boofcv.alg.distort.radtan;

import boofcv.struct.distort.PointTransform_F32;
import georegression.struct.point.Point2D_F32;

/**
 * Converts the observed distorted normalized image coordinates into undistorted normalized image coordinates.
 *
 * @author Peter Abeles
 */
public class RemoveRadialNtoN_F32 implements PointTransform_F32 {

	// distortion parameters
	protected RadialTangential_F32 params;

	// radial distortion magnitude
	protected float sum;
	// found tangential distortion
	protected float tx,ty;

	private float tol=1e-5f;

	public RemoveRadialNtoN_F32() {
	}

	public RemoveRadialNtoN_F32(float tol) {
		this.tol = tol;
	}

	public void setTolerance(float tol) {
		this.tol = tol;
	}

	public RemoveRadialNtoN_F32 setDistortion( float[] radial, float t1, float t2 ) {
		params = new RadialTangential_F32(radial,t1,t2);
		return this;
	}

	public RemoveRadialNtoN_F32 setDistortion( double[] radial, double t1, double t2 ) {
		params = new RadialTangential_F32().set(radial,t1,t2);
		return this;
	}

	/**
	 * Removes radial distortion
	 *
	 * @param x Distorted x-coordinate normalized image coordinate
	 * @param y Distorted y-coordinate normalized image coordinate
	 * @param out Undistorted normalized coordinate.
	 */
	@Override
	public void compute(float x, float y, Point2D_F32 out)
	{
		float radial[] = params.radial;
		float t1 = params.t1,t2 = params.t2;

		float origX = x;
		float origY = y;

		float prevSum = 0;

		for( int iter = 0; iter < 20; iter++ ) {

			// estimate the radial distance
			float r2 = x*x + y*y;
			float ri2 = r2;

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