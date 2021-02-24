/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Vector3D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;

/**
 * If the camera calibration is known for two views then given canonical camera projection matrices (P1 = [I|0])
 * it is possible to compute the plane a infinity and from that elevate the views from projective to metric.
 * The solution returned is only approximate.
 *
 * <p>
 * <li>Gherardi, Riccardo, and Andrea Fusiello. "Practical autocalibration."
 * European Conference on Computer Vision. Springer, Berlin, Heidelberg, 2010.</li>
 * </p>
 *
 * @author Peter Abeles
 */
public class EstimatePlaneAtInfinityGivenK {

	// Second view's projective camera matrix
	DMatrix3x3 Q2 = new DMatrix3x3();
	DMatrix3 q2 = new DMatrix3();

	// Known calibration for view 1 and view 2
	DMatrix3x3 K1 = new DMatrix3x3();
	DMatrix3x3 K2 = new DMatrix3x3();
	DMatrix3x3 K2_inv = new DMatrix3x3();

	// Workspace variables. Follows notation in paper when possible
	DMatrix3 t2 = new DMatrix3();

	// Rotation matrix that makes t2 all in x-axis
	DMatrix3x3 RR = new DMatrix3x3();

	DMatrix3x3 tmpA = new DMatrix3x3();
	DMatrix3x3 tmpB = new DMatrix3x3();
	DMatrix3x3 W = new DMatrix3x3();

	Vector3D_F64 w2 = new Vector3D_F64();
	Vector3D_F64 w3 = new Vector3D_F64();

	/**
	 * Specifies known intrinsic parameters for view 1
	 */
	public void setCamera1( double fx, double fy, double skew, double cx, double cy ) {
		PerspectiveOps.pinholeToMatrix(fx, fy, skew, cx, cy, K1);
	}

	/**
	 * Specifies known intrinsic parameters for view 2
	 */
	public void setCamera2( double fx, double fy, double skew, double cx, double cy ) {
		PerspectiveOps.pinholeToMatrix(fx, fy, skew, cx, cy, K2);
		PerspectiveOps.invertPinhole(K2, K2_inv);
	}

	/**
	 * Computes the plane at infinity
	 *
	 * @param P2 (Input) projective camera matrix for view 2. Not modified.
	 * @param v (Output) plane at infinity
	 * @return true if successful or false if it failed
	 */
	public boolean estimatePlaneAtInfinity( DMatrixRMaj P2, Vector3D_F64 v ) {
		PerspectiveOps.projectionSplit(P2, Q2, q2);

		// inv(K2)*(Q2*K1 + q2*v')
		CommonOps_DDF3.mult(K2_inv, q2, t2);
		CommonOps_DDF3.mult(K2_inv, Q2, tmpA);
		CommonOps_DDF3.mult(tmpA, K1, tmpB);

		// Find the rotation matrix R*t2 = [||t2||,0,0]^T
		computeRotation(t2, RR);

		// Compute W
		CommonOps_DDF3.mult(RR, tmpB, W);

		// Compute v, the plane at infinity
		// v = (w2 cross w3 / ||w3|| - w1 ) / ||t2||
		w2.setTo(W.a21, W.a22, W.a23);
		w3.setTo(W.a31, W.a32, W.a33);
		double n3 = w3.norm();
		v.crossSetTo(w2, w3); // approximation here, w2 and w3 might not be orthogonal
		v.divideIP(n3);
		v.x -= W.a11;
		v.y -= W.a12;
		v.z -= W.a13;
		v.divideIP(t2.a1);

		// really just a sanity check for bad input
		return !(UtilEjml.isUncountable(v.x) || UtilEjml.isUncountable(v.y) || UtilEjml.isUncountable(v.z));
	}

	/**
	 * Computes rotators which rotate t into [|t|,0,0]
	 *
	 * @param t Input the vector, Output vector after rotator has been applied
	 */
	static void computeRotation( DMatrix3 t, DMatrix3x3 R ) {
		for (int i = 1; i >= 0; i--) {
			double a = t.get(i, 0);
			double b = t.get(i + 1, 0);

			// compute the rotator such that [a,b] = [||X||,0]
			double r = Math.sqrt(a*a + b*b);
			double q11 = a/r;
			double q21 = b/r;

			// apply rotator to t and R
			t.set(i, 0, r);
			t.set(i + 1, 0, 0);

			// @formatter:off
			if( i == 1 ) {
				R.a11 = 1; R.a12 =  0;   R.a13 =  0;
				R.a21 = 0; R.a22 =  q11; R.a23 = q21;
				R.a31 = 0; R.a32 = -q21; R.a33 = q11;
			} else {
				R.a11 = q11;  R.a12 = R.a22*q21; R.a13 = R.a23*q21;
				R.a21 = -q21; R.a22 = R.a22*q11; R.a23 = R.a23*q11;
			}
			// @formatter:on
		}
	}
}
