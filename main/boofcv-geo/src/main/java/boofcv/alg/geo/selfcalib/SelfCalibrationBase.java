/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.ConvertDMatrixStruct;

import java.util.List;

/**
 * view[0] is assumed to the located at coordinate system's origin.
 *
 *<p>
 * w<sup>*</sup> = P Q<sup>*</sup><sub>&infin;</sub>P<sup>T</sup>
 * </p>
 *
 * TODO Describe
 *
 * TODO constant internal parameters
 * TODO aspect ratio and known skew
 * TODO
 *
 * @author Peter Abeles
 */
public class SelfCalibrationBase {

	// 3x4 camera matrices
	FastQueue<Projective> cameras = new FastQueue<>(Projective::new);

	// Minimum number of projective views/camera matrices to estimate the parameters
	int minimumProjectives;

	// workspace
	DMatrixRMaj _P = new DMatrixRMaj(3,4);
	DMatrixRMaj _Q = new DMatrixRMaj(4,4);
	DMatrixRMaj tmp = new DMatrixRMaj(3,4);

	/**
	 * Adds a projective transform which describes the relationship between a 3D point viewed in
	 * the view[i] and a projection viewed by a camera located at the origin.
	 *
	 * The projective is defined as P[i]=[A[i] | a[i]] where P is 3 by 4 matrix.
	 *
	 * @param viewI projective matrix representing the transform from the current camera
	 *                       to the coordinate system's origin. 3 x 4. A copy is saved internally.
	 */
	public void addCameraMatrix(DMatrixRMaj viewI ) {
		Projective pr = cameras.grow();
		PerspectiveOps.projectionSplit(viewI,pr.A,pr.a);
	}

	public void addCameraMatrix(List<DMatrixRMaj> viewI_to_view0 ) {
		for (int i = 0; i < viewI_to_view0.size(); i++) {
			addCameraMatrix(viewI_to_view0.get(i));
		}
	}

	public static void encodeQ( DMatrix4x4 Q , double param[] ) {
		Q.a11 = param[0];
		Q.a12 = Q.a21 = param[1];
		Q.a13 = Q.a31 = param[2];
		Q.a14 = Q.a41 = param[3];
		Q.a22 = param[4];
		Q.a23 = Q.a32 = param[5];
		Q.a24 = Q.a42 = param[6];
		Q.a33 = param[7];
		Q.a34 = Q.a43 = param[8];
		Q.a44 = param[9];
	}

	public void computeW( Projective P , DMatrix4x4 Q , DMatrixRMaj w_i) {
		ConvertDMatrixStruct.convert(Q,_Q);

		convert(P, _P);
		CommonOps_DDRM.mult(_P,_Q,tmp);
		CommonOps_DDRM.multTransB(tmp,_P,w_i);
		CommonOps_DDRM.divide(w_i,w_i.get(2,2));
	}

	static void convert( Projective P , DMatrixRMaj D ) {
		D.data[0] = P.A.a11;
		D.data[1] = P.A.a12;
		D.data[2] = P.A.a13;
		D.data[3] = P.a.a1;
		D.data[4] = P.A.a21;
		D.data[5] = P.A.a22;
		D.data[6] = P.A.a23;
		D.data[7] = P.a.a2;
		D.data[8] = P.A.a31;
		D.data[9] = P.A.a32;
		D.data[10] = P.A.a33;
		D.data[11] = P.a.a3;
	}

	/**
	 * Minimum number of cameras required to estimate the parameters.
	 */
	public int getMinimumProjectives() {
		return minimumProjectives;
	}

	public static class Projective {
		protected DMatrix3x3 A = new DMatrix3x3();
		protected DMatrix3 a = new DMatrix3();

		public Projective() {
		}
	}
}
