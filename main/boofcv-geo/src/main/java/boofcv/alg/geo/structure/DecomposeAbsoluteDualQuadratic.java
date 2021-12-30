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

package boofcv.alg.geo.structure;

import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.fixed.CommonOps_DDF4;

/**
 * Decomposes the absolute quadratic to extract the rectifying homogrpahy H. This is used to go from
 * a projective to metric (calibrated) geometry. See pg 464 in [1].
 *
 * <p>Q = H*diag([1 1 1 0])*H<sup>T</sup> and H = [K 0; -p'*K 1]</p>
 * <ol>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class DecomposeAbsoluteDualQuadratic {
	// work space variables
	DMatrix3x3 k = new DMatrix3x3();
	DMatrix3x3 w = new DMatrix3x3();
	DMatrix3x3 w_inv = new DMatrix3x3();

	DMatrix3 t = new DMatrix3();
	// coordinate of plane at infinity [p,1]
	DMatrix3 p = new DMatrix3();

	/**
	 * Decomposes the passed in absolute quadratic
	 *
	 * @param Q Absolute quadratic
	 * @return true if successful or false if it failed
	 */
	public boolean decompose( DMatrix4x4 Q ) {

		// scale Q so that Q(3,3) = 1 to provide a uniform scaling
		CommonOps_DDF4.scale(1.0/Q.a33, Q);

		// TODO Consider using eigen decomposition like it was suggested.
		//      Then the smallest eigenvalue can be forced to be zero. Having two classes that can be
		//      compared against would probably be best.

		// Directly extract from the definition of Q
		// Q = [w -w*p;-p'*w p'*w*p]
		// w = k*k'
		// @formatter:off
		k.a11 = Q.a11; k.a12 = Q.a12; k.a13 = Q.a13;
		k.a21 = Q.a21; k.a22 = Q.a22; k.a23 = Q.a23;
		k.a31 = Q.a31; k.a32 = Q.a32; k.a33 = Q.a33;
		// @formatter:on

		if (!CommonOps_DDF3.invert(k, w_inv))
			return false;
		// force it to be positive definite. Solution will be of dubious value if this condition is triggered, but it
		// seems to help much more often than it hurts
		// I'm not sure if I flip these variables if others along the same row/col should be flipped too or not
		k.setTo(w_inv);
		k.a11 = Math.abs(k.a11);
		k.a22 = Math.abs(k.a22);
		k.a33 = Math.abs(k.a33);

		if (!CommonOps_DDF3.cholU(k))
			return false;
		if (!CommonOps_DDF3.invert(k, k))
			return false;
		CommonOps_DDF3.divide(k, k.a33);

		t.a1 = Q.a14; t.a2 = Q.a24; t.a3 = Q.a34;

		CommonOps_DDF3.mult(w_inv, t, p);
		CommonOps_DDF3.scale(-1, p);

		CommonOps_DDF3.multTransB(k, k, w);

		return true;
	}

	/**
	 * Recomputes Q from w and p.
	 *
	 * @param Q Storage for the recomputed Q
	 */
	public void recomputeQ( DMatrix4x4 Q ) {
		// @formatter:off
		CommonOps_DDF3.multTransB(k, k, w);

		Q.a11 = w.a11; Q.a12 = w.a12; Q.a13 = w.a13;
		Q.a21 = w.a21; Q.a22 = w.a22; Q.a23 = w.a23;
		Q.a31 = w.a31; Q.a32 = w.a32; Q.a33 = w.a33;

		CommonOps_DDF3.mult(w, p, t);
		CommonOps_DDF3.scale(-1, t);

		Q.a14 = t.a1; Q.a24 = t.a2; Q.a34 = t.a3;
		Q.a41 = t.a1; Q.a42 = t.a2; Q.a43 = t.a3;

		Q.a44 = -CommonOps_DDF3.dot(t, p);
		// @formatter:on
	}

	/**
	 * Computes the rectifying homography from the decomposed Q
	 *
	 * H = [K 0; -p'*K 1]  see Pg 460
	 */
	public boolean computeRectifyingHomography( DMatrixRMaj H ) {
		H.reshape(4, 4);

		// insert the results into H
		// H = [K 0;-p'*K 1 ]
		H.zero();
		for (int i = 0; i < 3; i++) {
			for (int j = i; j < 3; j++) {
				H.set(i, j, k.get(i, j));
			}
		}
		// p and k have different scales, fix that
		H.set(3, 0, -(p.a1*k.a11 + p.a2*k.a21 + p.a3*k.a31));
		H.set(3, 1, -(p.a1*k.a12 + p.a2*k.a22 + p.a3*k.a32));
		H.set(3, 2, -(p.a1*k.a13 + p.a2*k.a23 + p.a3*k.a33));
		H.set(3, 3, 1);

		return true;
	}

	public DMatrix3x3 getW() {
		return w;
	}

	public DMatrix3x3 getK() {
		return k;
	}

	/**
	 * Coordinate of plane at infinity = pi_inf = (p,1)
	 *
	 * @return Coordinate of plane at infinity
	 */
	public DMatrix3 getP() {
		return p;
	}
}
