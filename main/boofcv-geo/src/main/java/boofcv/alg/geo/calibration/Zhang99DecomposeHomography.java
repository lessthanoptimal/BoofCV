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

package boofcv.alg.geo.calibration;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilVector3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.SpecializedOps_DDRM;

/**
 * <p>
 * Decomposes a homography into rigid body motion (rotation and translation) utilizing specific
 * assumptions made inside the Zhang99 paper [1].
 * </p>
 *
 * <p>
 * Let K and R be the calibration matrix and rotation matrix.<br>
 * R = [ r<sub>1</sub> , r<sub>1</sub>  ,  r<sub>1</sub> ]<br>
 * r<sub>i</sub> is the i<sup>th</sup> column in R.<br>
 * Then compute R using the column of the homography matrix:
 * r<sub>1</sub> = &lambda;*inv(K)*h<sub>1</sub><br>
 * r<sub>2</sub> = &lambda;*inv(K)*h<sub>2</sub><br>
 * r<sub>3</sub> = cross(r<sub>1</sub>,r<sub>2</sub>)<br>
 * t =  &lambda;*inv(K)*h<sub>3</sub><br>
 * t is the translation vector. The R computed above is only approximate and needs to be turned into
 * a real rotation matrix.
 * </p>
 *
 * <p>
 * [1] Zhengyou Zhang, "Flexible Camera Calibration By Viewing a Plane From Unknown Orientations,",
 * International Conference on Computer Vision (ICCV'99), Corfu, Greece, pages 666-673, September 1999.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class Zhang99DecomposeHomography {

	// Rows in rotation matrix
	DMatrixRMaj r1 = new DMatrixRMaj(3, 1);
	DMatrixRMaj r2 = new DMatrixRMaj(3, 1);
	// storage for translation vector
	DMatrixRMaj t = new DMatrixRMaj(3, 1);
	DMatrixRMaj temp = new DMatrixRMaj(3, 1);
	// storage for rotation matrix
	DMatrixRMaj R = new DMatrixRMaj(3, 3);

	// calibration matrix and its inverse
	DMatrixRMaj K;
	DMatrixRMaj K_inv = new DMatrixRMaj(3, 3);

	/**
	 * Specifies the calibration matrix.
	 *
	 * @param K upper triangular calibration matrix.
	 */
	public void setCalibrationMatrix( DMatrixRMaj K ) {
		this.K = K;
		CommonOps_DDRM.invert(K, K_inv);
	}

	/**
	 * Compute the rigid body motion that composes the homography matrix H. It is assumed
	 * that H was computed using {@link Zhang99ComputeTargetHomography}.
	 *
	 * @param H homography matrix.
	 * @return Found camera motion.
	 */
	public Se3_F64 decompose( DMatrixRMaj H ) {
		// step through each calibration grid and compute its parameters
		DMatrixRMaj h[] = SpecializedOps_DDRM.splitIntoVectors(H, true);

		// lambda = 1/norm(inv(K)*h1) or 1/norm(inv(K)*h2)
		// use the average to attempt to reduce error
		CommonOps_DDRM.mult(K_inv, h[0], temp);
		double lambda = NormOps_DDRM.normF(temp);
		CommonOps_DDRM.mult(K_inv, h[1], temp);
		lambda += NormOps_DDRM.normF(temp);
		lambda = 2.0/lambda;

		// compute the column in the rotation matrix
		CommonOps_DDRM.mult(lambda, K_inv, h[0], r1);
		CommonOps_DDRM.mult(lambda, K_inv, h[1], r2);
		CommonOps_DDRM.mult(lambda, K_inv, h[2], t);

		Vector3D_F64 v1 = UtilVector3D_F64.convert(r1);
		Vector3D_F64 v2 = UtilVector3D_F64.convert(r2);
		Vector3D_F64 v3 = v1.crossWith(v2);

		UtilVector3D_F64.createMatrix(R, v1, v2, v3);

		Se3_F64 ret = new Se3_F64();
		// the R matrix is probably not a real rotation matrix. So find
		// the closest real rotation matrix
		ConvertRotation3D_F64.approximateRotationMatrix(R, ret.getR());
		ret.getT().setTo(t.data[0], t.data[1], t.data[2]);

		return ret;
	}
}
