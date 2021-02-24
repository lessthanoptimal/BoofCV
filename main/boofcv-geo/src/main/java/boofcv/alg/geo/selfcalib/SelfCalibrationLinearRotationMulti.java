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

import boofcv.alg.geo.GeometricResult;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.Matrix3x3_F64;
import georegression.struct.homography.Homography2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.List;

/**
 * Camera calibration for when the camera's motion is purely rotational and has no translational
 * component and camera parameters can change every frame. Linear constraints need to be specified
 * on camera parameters. Based off of variable calibration Algorithm 19.3 on page 482 in [1].
 *
 * <pre>
 * Steps:
 * 1) Compute homographies between view i and reference frame, i.e. x<sup>i</sup> = H<sup>i</sup>x, ensure det(H)=1
 * 2) Uses equation w<sup>i</sup>=(H<sup>i</sup>)<sup>-T</sup>w(<sup>i</sup>)<sup>-1</sup> to express linear constraints
 * 3) Compute K using Cholesky decomposition w = U*U<sup>T</sup>. Actually implemented as an algebraic formula.
 * </pre>
 *
 * <p>
 * There will be a sign ambiguity in the returned result for the translation vector. That can be resolved by checking
 * for positive depth of triangulated features.
 * </p>
 *
 * <ol>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class SelfCalibrationLinearRotationMulti {

	private SingularValueDecomposition_F64<DMatrixRMaj> svd =
			DecompositionFactory_DDRM.svd(10, 6, false, true, true);

	// specifies linear constraints
	boolean zeroSkew;
	boolean principlePointOrigin;
	boolean knownAspectRatio;

	// fy/fx
	double aspectRatio;

	//----------- Working space variables
	// linear constraint matrix A*x = 0
	DMatrixRMaj A = new DMatrixRMaj(1, 1);

	// W for reference frame
	Homography2D_F64 W0 = new Homography2D_F64();
	// W for all others
	Homography2D_F64 Wi = new Homography2D_F64();
	// storage for calibration matrix
	Matrix3x3_F64 K = new Matrix3x3_F64();
	Matrix3x3_F64 tmp = new Matrix3x3_F64();

	DogArray_I32 notZeros = new DogArray_I32();

	// storage for null vector
	DMatrixRMaj nv = new DMatrixRMaj(1, 1);

	DogArray<Homography2D_F64> listHInv = new DogArray<>(Homography2D_F64::new);

	DogArray<CameraPinhole> calibration = new DogArray<>(CameraPinhole::new);

	/**
	 * Specifies linear constraints
	 *
	 * Known aspect ratio constraint can only be used if zero skew is also assumped.
	 *
	 * @param zeroSkew Assume that skew is zero
	 * @param principlePointOrigin Principle point is at the origin
	 * @param knownAspect that the aspect ratio is known
	 * @param aspect If aspect is known then this is the aspect. ratio=fy/fx Ignored otherwise.
	 */
	public void setConstraints( boolean zeroSkew,
								boolean principlePointOrigin,
								boolean knownAspect,
								double aspect ) {
		if (knownAspect && !zeroSkew)
			throw new IllegalArgumentException("If aspect is known then skew must be zero");

		this.zeroSkew = zeroSkew;
		this.principlePointOrigin = principlePointOrigin;
		this.knownAspectRatio = knownAspect;
		this.aspectRatio = aspect;

		notZeros.resize(6);
		for (int i = 0; i < 6; i++) {
			notZeros.data[i] = i;
		}
		if (principlePointOrigin) {
			notZeros.remove(4);
			notZeros.remove(2);
		}

		if (zeroSkew) {
			notZeros.remove(1);
		}
	}

	/**
	 * Assumes that the camera parameter are constant
	 *
	 * @param viewsI_to_view0 (Input) List of observed homographies
	 * @return true if successful
	 */
	public GeometricResult estimate( List<Homography2D_F64> viewsI_to_view0 ) {
		calibration.reset();
		int N = viewsI_to_view0.size();

		if (!computeInverseH(viewsI_to_view0))
			return GeometricResult.SOLVE_FAILED;

		fillInConstraintMatrix();

		// Compute the SVD for its null space
		if (!svd.decompose(A)) {
			return GeometricResult.SOLVE_FAILED;
		}

		SingularOps_DDRM.nullVector(svd, true, nv);
		extractReferenceW(nv);
		convertW(W0, calibration.grow());
		for (int i = 0; i < N; i++) {
			extractCalibration(listHInv.get(i), calibration.grow());
		}

		return GeometricResult.SUCCESS;
	}

	/**
	 * Number of constraints which are applied by each view
	 */
	public int numberOfConstraints() {
		int total = 0;
		if (zeroSkew)
			total++;
		if (principlePointOrigin)
			total += 2;
		if (knownAspectRatio)
			total += 1;
		return total;
	}

	/**
	 * Fill in A using all the constraints
	 *
	 * Sage Math used for derivation
	 * <pre>
	 *  H = matrix(SR, 3, 3, var('H11,H12,H13,H21,H22,H23,H31,H32,H33'))
	 *  var('W11,W12,W13,W22,W23,W33')
	 *  W = matrix(SR, 3, 3, [W11,W12,W13,W12,W22,W23,W13,W23,W33])
	 *  a=H.transpose()*W*H
	 *  a[0,1].expand()
	 * </pre>
	 */
	void fillInConstraintMatrix() {
		int N = listHInv.size();
		A.reshape(N*numberOfConstraints(), 6);

		if (A.numRows < notZeros.size) {
			throw new IllegalArgumentException("More unknowns than equations");
		}

		double b = knownAspectRatio ? aspectRatio*aspectRatio : 1;
		for (int i = 0, idx = 0; i < N; i++) {
			Homography2D_F64 H = listHInv.get(i);

			if (zeroSkew) { // w12
				A.data[idx++] = H.a11*H.a12;
				A.data[idx++] = H.a12*H.a21 + H.a11*H.a22;
				A.data[idx++] = H.a12*H.a31 + H.a11*H.a32;
				A.data[idx++] = H.a21*H.a22;
				A.data[idx++] = H.a22*H.a31 + H.a21*H.a32;
				A.data[idx++] = H.a31*H.a32;
			}
			if (principlePointOrigin) { // w13 = 0, w23 = 0
				A.data[idx++] = H.a11*H.a13;
				A.data[idx++] = H.a13*H.a21 + H.a11*H.a23;
				A.data[idx++] = H.a13*H.a31 + H.a11*H.a33;
				A.data[idx++] = H.a21*H.a23;
				A.data[idx++] = H.a23*H.a31 + H.a21*H.a33;
				A.data[idx++] = H.a31*H.a33;

				A.data[idx++] = H.a12*H.a13;
				A.data[idx++] = H.a13*H.a22 + H.a12*H.a23;
				A.data[idx++] = H.a13*H.a32 + H.a12*H.a33;
				A.data[idx++] = H.a22*H.a23;
				A.data[idx++] = H.a23*H.a32 + H.a22*H.a33;
				A.data[idx++] = H.a32*H.a33;
			}

			if (knownAspectRatio) { // w11 - b*w22 = 0
				A.data[idx++] = H.a11*H.a11 - H.a12*H.a12*b;
				A.data[idx++] = 2*H.a11*H.a21 - 2*H.a12*H.a22*b;
				A.data[idx++] = 2*H.a11*H.a31 - 2*H.a12*H.a32*b;
				A.data[idx++] = H.a21*H.a21 - H.a22*H.a22*b;
				A.data[idx++] = 2*H.a21*H.a31 - 2*H.a22*H.a32*b;
				A.data[idx++] = H.a31*H.a31 - H.a32*H.a32*b;
			}
		}
	}

	/**
	 * Extracts calibration for the reference frame
	 */
	void extractReferenceW( DMatrixRMaj nv ) {
		W0.a11 = nv.data[0];
		W0.a12 = W0.a21 = nv.data[1];
		W0.a13 = W0.a31 = nv.data[2];
		W0.a22 = nv.data[3];
		W0.a23 = W0.a32 = nv.data[4];
		W0.a33 = nv.data[5];
	}

	/**
	 * Converts W into a pinhole camera model by finding the cholesky decomposition
	 */
	void convertW( Homography2D_F64 w, CameraPinhole c ) {
		// inv(w) = K*K'
		tmp.setTo(w);
		CommonOps_DDF3.divide(tmp, tmp.a33);
		CommonOps_DDF3.cholU(tmp);
		CommonOps_DDF3.invert(tmp, K);
		CommonOps_DDF3.divide(K, K.a33);

		c.fx = K.a11;
		c.fy = knownAspectRatio ? (K.a22 + c.fx*aspectRatio)/2.0 : K.a22;
		c.skew = zeroSkew ? 0 : K.a12;
		c.cx = principlePointOrigin ? 0 : K.a13;
		c.cy = principlePointOrigin ? 0 : K.a23;
	}

	/**
	 * Extracts calibration for the non-reference frames
	 *
	 * w = H^-T*w*H^-1
	 */
	void extractCalibration( Homography2D_F64 Hinv, CameraPinhole c ) {
		CommonOps_DDF3.multTransA(Hinv, W0, tmp);
		CommonOps_DDF3.mult(tmp, Hinv, Wi);

		convertW(Wi, c);
	}

	/**
	 * Ensures the determinant is one then inverts the homogrpahy
	 */
	public boolean computeInverseH( List<Homography2D_F64> homography0toI ) {
		listHInv.reset();
		int N = homography0toI.size();
		for (int i = 0; i < N; i++) {
			Homography2D_F64 H = homography0toI.get(i);
			Homography2D_F64 Hinv = listHInv.grow();

			// Ensure the determinant is one
			double d = CommonOps_DDF3.det(H);
			if (d < 0)
				CommonOps_DDF3.divide(H, -Math.pow(-d, 1.0/3), Hinv);
			else
				CommonOps_DDF3.divide(H, Math.pow(d, 1.0/3), Hinv);

			// Now invert the matrix
			if (!CommonOps_DDF3.invert(Hinv, Hinv)) {
				return false;
			}
		}
		return true;
	}

	public DogArray<CameraPinhole> getFound() {
		return calibration;
	}
}
