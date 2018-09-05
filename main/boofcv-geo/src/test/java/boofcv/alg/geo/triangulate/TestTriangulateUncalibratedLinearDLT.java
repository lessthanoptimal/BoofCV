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

package boofcv.alg.geo.triangulate;

import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTriangulateUncalibratedLinearDLT extends CommonTriangulationChecks {

	/**
	 * Create 3 perfect observations and solve for the position. Everything is in metric instead of an arbtirary
	 * projective frame for ease of testing.
	 */
	@Test
	public void triangulate_metric_N() {
		createScene();

		TriangulateUncalibratedLinearDLT alg = new TriangulateUncalibratedLinearDLT();

		Point4D_F64 found = new Point4D_F64();

		alg.triangulate(obsPts, metricToProjective(motionWorldToCamera),found);

		found.x /= found.w;
		found.y /= found.w;
		found.z /= found.w;

		assertEquals(worldPoint.x,found.x,1e-8);
		assertEquals(worldPoint.y,found.y,1e-8);
		assertEquals(worldPoint.z,found.z,1e-8);
	}

	private List<DMatrixRMaj> metricToProjective(List<Se3_F64> motions ) {
		List<DMatrixRMaj> output = new ArrayList<>();

		for( Se3_F64 M : motions ) {
			DMatrixRMaj P = new DMatrixRMaj(3,4);
			CommonOps_DDRM.insert(M.R,P,0,0);
			P.set(0,3, M.T.x);
			P.set(1,3, M.T.y);
			P.set(2,3, M.T.z);

			output.add(P);
		}
		return output;
	}
}