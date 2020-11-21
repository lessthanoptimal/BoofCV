/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.GeometryMath_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SpecializedOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.QRDecomposition;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

/**
 * Decomposes metric camera matrices as well as projective with known intrinsic parameters.
 *
 * @author Peter Abeles
 */
public class DecomposeProjectiveToMetric {

	// Need to do an RQ decomposition, but we only have QR
	// by permuting the rows in KR we can get the desired result
	protected QRDecomposition<DMatrixRMaj> qr = DecompositionFactory_DDRM.qr(3, 3);
	// Pivot matrix
	protected DMatrixRMaj Pv = SpecializedOps_DDRM.pivotMatrix(null, new int[]{2, 1, 0}, 3, false);
	// Storage for A after pivots have been applied to it
	protected DMatrixRMaj A_p = new DMatrixRMaj(3, 3);

	// P = [A|a]  The left side 3x3 sub matrix in camera matrix P
	protected DMatrixRMaj A = new DMatrixRMaj(3, 3);

	protected SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(true, true, true);
	// P_metric = P*H
	protected DMatrixRMaj P_metric = new DMatrixRMaj(3, 4);
	// metric camera after being multiplied by inv(K)
	protected DMatrixRMaj P_rt = new DMatrixRMaj(3, 4);
	// Storage for the inverse of the intrinic matrix K
	protected DMatrixRMaj K_inv = new DMatrixRMaj(3, 3);

	/** Indicates how far the singular values deviated from their expected value. zero means perfect match */
	public @Getter double singularError;

	/**
	 * <p>
	 * Convert the projective camera matrix into a metric transform given the rectifying homography and a
	 * known calibration matrix. This simplifies the math compared to {@link #projectiveToMetric} where it needs
	 * to extract `K`.
	 * </p>
	 * {@code P = K*[R|T]*H} where H is the inverse of the rectifying homography.
	 *
	 * A goodness of fit error can be accessed using {@link #singularError}.
	 *
	 * @param cameraMatrix (Input) camera matrix. 3x4
	 * @param H (Input) Rectifying homography. 4x4
	 * @param K (Input) Known calibration matrix
	 * @param worldToView (Output) transform from world to camera view
	 * @return true if the decomposition was successful
	 */
	public boolean projectiveToMetricKnownK( DMatrixRMaj cameraMatrix,
											 DMatrixRMaj H, DMatrixRMaj K,
											 Se3_F64 worldToView ) {
		// Reset internal data structures
		singularError = 0;

		// Elevate the projective camera into a metric camera matrix
		CommonOps_DDRM.mult(cameraMatrix, H, P_metric);
		// "Remove" K from the metric camera, e.g. P= [K*R | K*T] then inv(K)P = [R | T]
		CommonOps_DDRM.invert(K, K_inv);
		CommonOps_DDRM.mult(K_inv, P_metric, P_rt);

		// Remove R and T
		CommonOps_DDRM.extract(P_rt, 0, 0, worldToView.R);
		worldToView.T.x = P_rt.get(0, 3);
		worldToView.T.y = P_rt.get(1, 3);
		worldToView.T.z = P_rt.get(2, 3);

		// Turn R into a true rotation matrix which is orthogonal and has a determinant of +1
		DMatrixRMaj R = worldToView.R;
		if (!svd.decompose(R))
			return false;

		CommonOps_DDRM.multTransB(svd.getU(null, false), svd.getV(null, false), R);

		// determinant should be +1
		double det = CommonOps_DDRM.det(R);
		if (det < 0) {
			CommonOps_DDRM.scale(-1, R);
			worldToView.T.scale(-1);
		}

		// recover the scale of T. This is important when trying to construct a common metric frame from a common
		// projective frame
		double[] sv = svd.getSingularValues();
		double sv_mag = (sv[0] + sv[1] + sv[2])/3.0;
		worldToView.T.divideIP(sv_mag);

		// if the input preconditions are false and K was not a good fit to this metric transform then the singular
		// values will not all be identical
		for (int i = 0; i < 3; i++) {
			singularError += Math.abs(sv[i] - sv_mag);
		}

		return true;
	}

	/**
	 * Elevates a projective camera matrix into a metric one using the rectifying homography.
	 * Extracts calibration and Se3 pose.
	 *
	 * <pre>
	 * P'=P*H
	 * K,R,t = decompose(P')
	 * </pre>
	 * where P is the camera matrix, H is the homography, (K,R,t) are the intrinsic calibration matrix, rotation,
	 * and translation
	 *
	 * @param cameraMatrix (Input) camera matrix. 3x4
	 * @param H (Input) Rectifying homography. 4x4
	 * @param worldToView (Output) Transform from world to camera view
	 * @param K (Output) Camera calibration matrix
	 * @see MultiViewOps#absoluteQuadraticToH
	 * @see #decomposeMetricCamera(DMatrixRMaj, DMatrixRMaj, Se3_F64)
	 */
	public boolean projectiveToMetric( DMatrixRMaj cameraMatrix, DMatrixRMaj H, Se3_F64 worldToView, DMatrixRMaj K ) {
		CommonOps_DDRM.mult(cameraMatrix, H, P_metric);
		return decomposeMetricCamera(P_metric, K, worldToView);
	}

	/**
	 * <p>
	 * Decomposes a metric camera matrix P=K*[R|T], where A is an upper triangular camera calibration
	 * matrix, R is a rotation matrix, and T is a translation vector. If {@link PerspectiveOps#createCameraMatrix}
	 * is called using the returned value you will get an equivalent camera matrix.
	 * </p>
	 *
	 * <ul>
	 * <li> NOTE: There are multiple valid solutions to this problem and only one solution is returned.
	 * <li> NOTE: The camera center will be on the plane at infinity.
	 * </ul>
	 *
	 * @param cameraMatrix Input: Camera matrix, 3 by 4
	 * @param K Output: Camera calibration matrix, 3 by 3.
	 * @param worldToView Output: The rotation and translation.
	 * @return true if decompose was successful
	 */
	public boolean decomposeMetricCamera( DMatrixRMaj cameraMatrix, DMatrixRMaj K, Se3_F64 worldToView ) {
		CommonOps_DDRM.extract(cameraMatrix, 0, 3, 0, 3, A, 0, 0);
		worldToView.T.setTo(cameraMatrix.get(0, 3), cameraMatrix.get(1, 3), cameraMatrix.get(2, 3));

		CommonOps_DDRM.mult(Pv, A, A_p);
		CommonOps_DDRM.transpose(A_p);
		if (!qr.decompose(A_p))
			return false;

		// extract the rotation using RQ decomposition (via a pivoted QR)
		qr.getQ(A, false);
		CommonOps_DDRM.multTransB(Pv, A, worldToView.R);

		// extract the calibration matrix
		qr.getR(K, false);
		CommonOps_DDRM.multTransB(Pv, K, A);
		CommonOps_DDRM.mult(A, Pv, K);

		// there are four solutions, massage it so that it's the correct one.
		// each of these row/column negations produces the same camera matrix
		for (int i = 0; i < 3; i++) {
			if (K.get(i, i) < 0) {
				CommonOps_DDRM.scaleCol(-1, K, i);
				CommonOps_DDRM.scaleRow(-1, worldToView.R, i);
			}
		}

		// rotation matrices have det() == 1
		if (CommonOps_DDRM.det(worldToView.R) < 0) {
			CommonOps_DDRM.scale(-1, worldToView.R);
			worldToView.T.scale(-1);
		}

		// save the scale so that T is scaled correctly. This is important when upgrading common projective cameras
		double scale = K.get(2, 2);

		// make sure it's a proper camera matrix and this is more numerically stable to invert
		CommonOps_DDRM.divide(K, scale);

		// could do a very fast triangulate inverse. EJML doesn't have one for upper triangle, yet.
		if (!CommonOps_DDRM.invert(K, A))
			return false;

		GeometryMath_F64.mult(A, worldToView.T, worldToView.T);
		worldToView.T.divide(scale);

		return true;
	}
}
