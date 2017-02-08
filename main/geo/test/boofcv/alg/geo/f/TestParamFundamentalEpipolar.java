/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.f;

import boofcv.alg.geo.MultiViewOps;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestParamFundamentalEpipolar {

	@Test
	public void backAndForth() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,1, 2, -0.5, null);
		Vector3D_F64 T = new Vector3D_F64(0.5,0.7,-0.3);

		DMatrixRMaj E = MultiViewOps.createEssential(R, T);
		
		double param[] = new double[7];
		
		ParamFundamentalEpipolar alg = new ParamFundamentalEpipolar();
		
		DMatrixRMaj found = new DMatrixRMaj(3,3);
		alg.encode(E, param);
		alg.decode(param, found);

		// normalize to take in account scale different when testing
		CommonOps_DDRM.divide(E.get(2,2),E);
		CommonOps_DDRM.divide(found.get(2, 2), found);
		
		assertTrue(MatrixFeatures_DDRM.isEquals(E, found, 1e-8));
	}
}
