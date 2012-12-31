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

package boofcv.alg.geo;

import org.ejml.data.DenseMatrix64F;

/**
 * Computes the rotation matrix derivative for Rodrigues coordinates
 * which have been parameterized by a 3 vector.  The rotation is equal to
 * the vector's magnitude and the rotation vector is the unit vector.
 *
 * @author Peter Abeles
 */
public class RodriguesRotationJacobian {
	// partial for vector x component
	public DenseMatrix64F Rx = new DenseMatrix64F(3,3);
	// partial for vector x component
	public DenseMatrix64F Ry = new DenseMatrix64F(3,3);
	// partial for vector y component
	public DenseMatrix64F Rz = new DenseMatrix64F(3,3);

	/**
	 * Computes the Rodrigues coordinate Jacobian
	 *
	 * @param x x-component of Rodrigues parametrization.
	 * @param y y-component of Rodrigues parametrization.
	 * @param z z-component of Rodrigues parametrization.
	 */
	public void process( double x , double y , double z ) {

		double theta2 = x*x + y*y + z*z;
		double theta = Math.sqrt(theta2);
		double theta4 = theta2*theta2;
		double theta3 = theta2*theta;

		if( theta4 == 0 ) {
			Rx.zero();Ry.zero();Rz.zero();

			Rx.set(1,2,1);
			Rx.set(2,1,-1);

			Ry.set(0,2,-1);
			Ry.set(2,0,1);

			Rz.set(0,1,1);
			Rz.set(1,0,-1);
		} else {
			// computed using sage by differentiating:
			// R = I + (hat(w)/theta)*sin(theta) + ((hat(w)/theta)^2)*(1-cos(theta))
			// theta = sqrt(x*x + y*y + z*z)
			// Then the equations were further simplified by hand

			double s = Math.sin(theta);
			double c = Math.cos(theta);
			double cm = c-1;

			double xxx = x*x*x*s/theta3 + 2*cm*x*x*x/theta4;
			double xxy = x*x*y*s/theta3 + 2*cm*x*x*y/theta4;
			double xxz = x*x*z*s/theta3 + 2*cm*x*x*z/theta4;
			double xyy = x*y*y*s/theta3 + 2*cm*x*y*y/theta4;
			double xyz = x*y*z*s/theta3 + 2*cm*x*y*z/theta4;
			double xzz = x*z*z*s/theta3 + 2*cm*x*z*z/theta4;
			double yyy = y*y*y*s/theta3 + 2*cm*y*y*y/theta4;
			double yyz = y*y*z*s/theta3 + 2*cm*y*y*z/theta4;
			double yzz = y*z*z*s/theta3 + 2*cm*y*z*z/theta4;
			double zzz = z*z*z*s/theta3 + 2*cm*z*z*z/theta4;

			Rx.data[0] = xxx - x*s/theta - 2*cm*x/theta2;
			Rx.data[1] = xxy - x*z*c/theta2 + x*z*s/theta3 - cm*y/theta2;
			Rx.data[2] = xxz + x*y*c/theta2 - x*y*s/theta3 - cm*z/theta2;
			Rx.data[3] = xxy + x*z*c/theta2 - x*z*s/theta3 - cm*y/theta2;
			Rx.data[4] = xyy - x*s/theta;
			Rx.data[5] = xyz - x*x*c/theta2 + x*x*s/theta3 - s/theta;
			Rx.data[6] = xxz - x*y*c/theta2 + x*y*s/theta3 - cm*z/theta2;
			Rx.data[7] = xyz + x*x*c/theta2 - x*x*s/theta3 + s/theta;
			Rx.data[8] = xzz - x*s/theta;

			Ry.data[0] = xxy - y*s/theta;
			Ry.data[1] = xyy - y*z*c/theta2 + y*z*s/theta3 - cm*x/theta2;
			Ry.data[2] = xyz + y*y*c/theta2 - y*y*s/theta3 + s/theta;
			Ry.data[3] = xyy + y*z*c/theta2 - y*z*s/theta3 - cm*x/theta2;
			Ry.data[4] = yyy - y*s/theta - 2*cm*y/theta2;
			Ry.data[5] = yyz - x*y*c/theta2 + x*y*s/theta3 - cm*z/theta2;
			Ry.data[6] = xyz - y*y*c/theta2 + y*y*s/theta3 - s/theta;
			Ry.data[7] = yyz + x*y*c/theta2 - x*y*s/theta3 - cm*z/theta2;
			Ry.data[8] = yzz - y*s/theta;

			Rz.data[0] = xxz - z*s/theta;
			Rz.data[1] = xyz - z*z*c/theta2 + z*z*s/theta3 - s/theta;
			Rz.data[2] = xzz + y*z*c/theta2 - y*z*s/theta3 - cm*x/theta2;
			Rz.data[3] = xyz + z*z*c/theta2 - z*z*s/theta3 + s/theta;
			Rz.data[4] = yyz - z*s/theta;
			Rz.data[5] = yzz - x*z*c/theta2 + x*z*s/theta3 - cm*y/theta2;
			Rz.data[6] = xzz - y*z*c/theta2 + y*z*s/theta3 - cm*x/theta2;
			Rz.data[7] = yzz + x*z*c/theta2 - x*z*s/theta3 - cm*y/theta2;
			Rz.data[8] = zzz - z*s/theta - 2*cm*z/theta2;
		}
	}
}
