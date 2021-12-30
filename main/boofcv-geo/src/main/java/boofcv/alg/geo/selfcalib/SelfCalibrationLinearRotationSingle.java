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

import boofcv.struct.calib.CameraPinhole;
import georegression.struct.homography.Homography2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.Arrays;
import java.util.List;

/**
 * Camera calibration for when the camera's motion is purely rotational and has no translational
 * component and camera parameters are assumed to be constant. All five camera parameters are estimated and no
 * constraints can be specified. Based off of constant calibration Algorithm 19.3 on page 482 in [1].
 *
 * <pre>
 * Steps:
 * 1) Compute homographies between view i and reference frame, i.e. x<sup>i</sup> = H<sup>i</sup>x, ensure det(H)=1
 * 2) Write equation w=(H<sup>i</sup>)<sup>-T</sup>w(<sup>i</sup>)<sup>-1</sup> as A*x=0, where A = 6m by 6 matrix.
 * 3) Compute K using Cholesky decomposition w = U*U<sup>T</sup>. Actually implemented as an algebraic formula.
 * </pre>
 *
 * <ol>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class SelfCalibrationLinearRotationSingle {

	SingularValueDecomposition_F64<DMatrixRMaj> svd = DecompositionFactory_DDRM.svd(10, 10,
			false, true, true);

	double singularThreshold = 1e-5;

	boolean singular;

	DMatrixRMaj A = new DMatrixRMaj(6, 6);

	/**
	 * Assumes that the camera parameter are constant
	 *
	 * @param viewsI_to_view0 (Input) List of observed homographies. Modified so that determinant is one.
	 * @param calibration (Output) found calibration
	 * @return true if successful
	 */
	public boolean estimate( List<Homography2D_F64> viewsI_to_view0, CameraPinhole calibration ) {
		singular = false;
		int N = viewsI_to_view0.size();

		ensureDeterminantOfOne(viewsI_to_view0);

		A.reshape(N*6, 6);

		for (int i = 0; i < N; i++) {
			add(i, viewsI_to_view0.get(i), A);
		}

		// Compute the SVD for its null space
		if (!svd.decompose(A)) {
			return false;
		}

		DMatrixRMaj nv = new DMatrixRMaj(6, 1);
		SingularOps_DDRM.nullVector(svd, true, nv);

		if (!extractCalibration(nv, calibration))
			return false;

		// determine if the solution is good by looking at two smallest singular values
		// If there isn't a steep drop it either isn't singular or more there is more than 1 singular value
		double[] sv = svd.getSingularValues();
		Arrays.sort(sv);
		if (singularThreshold*sv[1] <= sv[0]) {
			return false;
		}
		return true;
	}

	/**
	 * Scales all homographies so that their determinants are equal to one
	 */
	public static void ensureDeterminantOfOne( List<Homography2D_F64> homography0toI ) {
		int N = homography0toI.size();
		for (int i = 0; i < N; i++) {
			Homography2D_F64 H = homography0toI.get(i);
			double d = CommonOps_DDF3.det(H);
//			System.out.println("Before = "+d);
			if (d < 0)
				CommonOps_DDF3.divide(H, -Math.pow(-d, 1.0/3));
			else
				CommonOps_DDF3.divide(H, Math.pow(d, 1.0/3));
//			System.out.println("determinant = "+ CommonOps_DDF3.det(H));
		}
	}

	/**
	 * Extracts camera parameters from the solution. Checks for errors
	 */
	private boolean extractCalibration( DMatrixRMaj x, CameraPinhole calibration ) {
		double s = x.data[5];

		double cx = calibration.cx = x.data[2]/s;
		double cy = calibration.cy = x.data[4]/s;
		double fy = calibration.fy = Math.sqrt(x.data[3]/s - cy*cy);
		double sk = calibration.skew = (x.data[1]/s - cx*cy)/fy;
		calibration.fx = Math.sqrt(x.data[0]/s - sk*sk - cx*cx);

		if (calibration.fx < 0 || calibration.fy < 0)
			return false;
		if (UtilEjml.isUncountable(fy) || UtilEjml.isUncountable(calibration.fx))
			return false;
		if (UtilEjml.isUncountable(sk))
			return false;
		return true;
	}

	/**
	 * Adds the linear system defined by H into A and B
	 *
	 * @param which index of H in the list
	 */
	protected void add( int which, Homography2D_F64 H, DMatrixRMaj A ) {
		int idx = which*6*6;

		// Row 0
		A.data[idx++] = H.a11*H.a11 - 1;
		A.data[idx++] = 2*H.a11*H.a12;
		A.data[idx++] = 2*H.a11*H.a13;
		A.data[idx++] = H.a12*H.a12;
		A.data[idx++] = 2*H.a12*H.a13;
		A.data[idx++] = H.a13*H.a13;
		// Row 1
		A.data[idx++] = H.a11*H.a21;
		A.data[idx++] = H.a12*H.a21 + H.a11*H.a22 - 1;
		A.data[idx++] = H.a13*H.a21 + H.a11*H.a23;
		A.data[idx++] = H.a12*H.a22;
		A.data[idx++] = H.a13*H.a22 + H.a12*H.a23;
		A.data[idx++] = H.a13*H.a23;
		// Row 2
		A.data[idx++] = H.a11*H.a31;
		A.data[idx++] = H.a12*H.a31 + H.a11*H.a32;
		A.data[idx++] = H.a13*H.a31 + H.a11*H.a33 - 1;
		A.data[idx++] = H.a12*H.a32;
		A.data[idx++] = H.a13*H.a32 + H.a12*H.a33;
		A.data[idx++] = H.a13*H.a33;
		// Row 3
		A.data[idx++] = H.a21*H.a21;
		A.data[idx++] = 2*H.a21*H.a22;
		A.data[idx++] = 2*H.a21*H.a23;
		A.data[idx++] = H.a22*H.a22 - 1;
		A.data[idx++] = 2*H.a22*H.a23;
		A.data[idx++] = H.a23*H.a23;
		// Row 4
		A.data[idx++] = H.a21*H.a31;
		A.data[idx++] = H.a22*H.a31 + H.a21*H.a32;
		A.data[idx++] = H.a23*H.a31 + H.a21*H.a33;
		A.data[idx++] = H.a22*H.a32;
		A.data[idx++] = H.a23*H.a32 + H.a22*H.a33 - 1;
		A.data[idx++] = H.a23*H.a33;
		// Row 5
		A.data[idx++] = H.a31*H.a31;
		A.data[idx++] = 2*H.a31*H.a32;
		A.data[idx++] = 2*H.a31*H.a33;
		A.data[idx++] = H.a32*H.a32;
		A.data[idx++] = 2*H.a32*H.a33;
		A.data[idx] = H.a33*H.a33 - 1;
	}
}
