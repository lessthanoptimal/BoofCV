/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.autocalib;

import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.ConvertDMatrixStruct;

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
public class AutoCalibrationBase {

	FastQueue<Projective> projectives = new FastQueue<>(Projective.class,true);

	// Minimum number of projective views to estimate the parameters
	int minimumProjectives;

	/**
	 * Adds a projective transform which describes the relationship between a 3D point viewed in
	 * the view[i] and a projection viewed by a camera located at the origin.
	 *
	 * The projective is defined as P[i]=[A[i] | a[i]] where P is 3 by 4 matrix.
	 *
	 * @param viewI_to_view0 projective matrix representing the transform from the current camera
	 *                       to the coordinate system's origin. 3 x 4
	 */
	public void addProjective( DMatrixRMaj viewI_to_view0 ) {
		DMatrixRMaj P = viewI_to_view0;
		DMatrixRMaj A = new DMatrixRMaj(3,3);
		CommonOps_DDRM.extract(P,0,3,0,3,A);
		Projective pr = projectives.grow();
		ConvertDMatrixStruct.convert(A,pr.A);
		pr.a.set(P.get(0,3),P.get(1,3),P.get(2,3));
	}

	/**
	 * Minimum number of projectives required to estimate the parameters.
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
