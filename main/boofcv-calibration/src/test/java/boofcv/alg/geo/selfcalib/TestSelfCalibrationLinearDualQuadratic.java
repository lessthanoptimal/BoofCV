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

package boofcv.alg.geo.selfcalib;

import boofcv.struct.calib.CameraPinhole;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSelfCalibrationLinearDualQuadratic extends CommonAutoCalibrationChecks {

	@Test
	public void solve_ZeroCP() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			intrinsics.add(new CameraPinhole(400+i*5,420,0.1,0,0,0,0));
		}

		checkSolve(intrinsics, false,false,5);
	}

	@Test
	public void solve_ZeroCP_ZSkew() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			intrinsics.add(new CameraPinhole(400+i*5,420,0,0,0,0,0));
		}

		checkSolve(intrinsics, false,true,4);
	}

	@Test
	public void solve_ZeroCP_ZSkew_Aspect() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			intrinsics.add(new CameraPinhole((400+i*5)*1.05,400+i*5,0,0,0,0,0));
		}
		checkSolve(intrinsics, true,true,3);
	}

	private void checkSolve(List<CameraPinhole> intrinsics, boolean knownAspect, boolean zeroSkew, int numProjectives ) {
		renderGood(intrinsics);

		CameraPinhole a = intrinsics.get(0);

		double aspect = a.fy/a.fx;
		SelfCalibrationLinearDualQuadratic alg;
		if( zeroSkew ) {
			if( knownAspect ) {
				alg = new SelfCalibrationLinearDualQuadratic(aspect);
			} else {
				alg = new SelfCalibrationLinearDualQuadratic(true);
			}
		} else {
			alg = new SelfCalibrationLinearDualQuadratic(false);
		}

		assertEquals(numProjectives,alg.getMinimumProjectives());

		addProjectives(alg);

		assertEquals(SelfCalibrationLinearDualQuadratic.Result.SUCCESS,alg.solve());

		assertEquals(intrinsics.size()-1,alg.getSolutions().size());
		for (int i = 1; i < intrinsics.size(); i++) {
			CameraPinhole intrinsic = intrinsics.get(i);
			SelfCalibrationLinearDualQuadratic.Intrinsic found = alg.getSolutions().get(i-1);

			assertEquals(intrinsic.fx,   found.fx,   UtilEjml.TEST_F64_SQ);
			assertEquals(intrinsic.fy,   found.fy,   UtilEjml.TEST_F64_SQ);
			assertEquals(intrinsic.skew, found.skew, UtilEjml.TEST_F64_SQ);

		}
	}

	@Test
	public void geometry_no_rotation() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0.1,0,0,0,0);
		renderTranslationOnly(intrinsic);

		SelfCalibrationLinearDualQuadratic alg = new SelfCalibrationLinearDualQuadratic(false);
		addProjectives(alg);

		assertEquals(SelfCalibrationLinearDualQuadratic.Result.POOR_GEOMETRY,alg.solve());
	}

	@Test
	public void geometry_no_translation() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0.1,0,0,0,0);
		renderRotationOnly(intrinsic);

		SelfCalibrationLinearDualQuadratic alg = new SelfCalibrationLinearDualQuadratic(false);
		addProjectives(alg);

		assertEquals(SelfCalibrationLinearDualQuadratic.Result.POOR_GEOMETRY,alg.solve());
	}

	@Test
	public void constructMatrix() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			intrinsics.add(new CameraPinhole(400+i*5,420,0.1,0,0,0,0));
		}

		renderGood(intrinsics);

		SelfCalibrationLinearDualQuadratic alg = new SelfCalibrationLinearDualQuadratic(false);
		addProjectives(alg);

		DMatrixRMaj L = new DMatrixRMaj(3,3);
		alg.constructMatrix(L);

		DMatrixRMaj q = new DMatrixRMaj(10,1);
		q.data[0] = Q.get(0,0);
		q.data[1] = Q.get(0,1);
		q.data[2] = Q.get(0,2);
		q.data[3] = Q.get(0,3);
		q.data[4] = Q.get(1,1);
		q.data[5] = Q.get(1,2);
		q.data[6] = Q.get(1,3);
		q.data[7] = Q.get(2,2);
		q.data[8] = Q.get(2,3);
		q.data[9] = Q.get(3,3);

//		double[] sv = SingularOps_DDRM.singularValues(L);
//		for (int i = 0; i < sv.length; i++) {
//			System.out.println("sv["+i+"] = "+sv[i]);
//		}

		DMatrixRMaj found = new DMatrixRMaj(L.numRows,1);

		// See if it's the null space
		CommonOps_DDRM.mult(L,q,found);
		assertEquals(0,NormOps_DDRM.normF(found), 10*UtilEjml.TEST_F64);
	}
}