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

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic.Intrinsic;
import boofcv.factory.geo.ConfigTrifocal;
import boofcv.factory.geo.EnumTrifocal;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastAccess;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelfCalibrationLinearDualQuadratic extends CommonAutoCalibrationChecks {

	@Test void solve_ZeroCP() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			intrinsics.add(new CameraPinhole(400 + i*5, 420, 0.1, 0, 0, 0, 0));
		}

		checkSolve(intrinsics, false, false, 5);
	}

	@Test void solve_ZeroCP_ZSkew() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			intrinsics.add(new CameraPinhole(400 + i*5, 420, 0, 0, 0, 0, 0));
		}

		checkSolve(intrinsics, false, true, 4);
	}

	@Test void solve_ZeroCP_ZSkew_Aspect() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			intrinsics.add(new CameraPinhole((400 + i*5)*1.05, 400 + i*5, 0, 0, 0, 0, 0));
		}
		checkSolve(intrinsics, true, true, 3);
	}

	/**
	 * Create a trifocal tensor, extract camera matrices, and see if it can find the solution
	 */
	@Test void solveWithTrificalInput() {
		CameraPinhole intrinsic = new CameraPinhole(500, 500, 0, 0, 0, 0, 0);

		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			intrinsics.add(intrinsic);
		}

		renderGood(intrinsics);
		List<AssociatedTriple> obs = new ArrayList<>();
		for (int i = 0; i < cloud.size(); i++) {
			Point3D_F64 X = cloud.get(i);

			AssociatedTriple t = new AssociatedTriple();
			t.p1 = PerspectiveOps.renderPixel(listCameraToWorld.get(0), intrinsic, X, null);
			t.p2 = PerspectiveOps.renderPixel(listCameraToWorld.get(1), intrinsic, X, null);
			t.p3 = PerspectiveOps.renderPixel(listCameraToWorld.get(2), intrinsic, X, null);

			obs.add(t);
		}

		ConfigTrifocal config = new ConfigTrifocal();
		config.which = EnumTrifocal.LINEAR_7;
		Estimate1ofTrifocalTensor estimate = FactoryMultiView.trifocal_1(config);
		TrifocalTensor tensor = new TrifocalTensor();
		assertTrue(estimate.process(obs, tensor));

		DMatrixRMaj P1 = CommonOps_DDRM.identity(3, 4);
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);

		MultiViewOps.trifocalToCameraMatrices(tensor, P2, P3);

		SelfCalibrationLinearDualQuadratic alg = new SelfCalibrationLinearDualQuadratic(1.0);
		alg.addCameraMatrix(P1);
		alg.addCameraMatrix(P2);
		alg.addCameraMatrix(P3);

		assertEquals(GeometricResult.SUCCESS, alg.solve());

		FastAccess<Intrinsic> found = alg.getIntrinsics();
		assertEquals(3, found.size());
		for (int i = 0; i < found.size(); i++) {
			Intrinsic f = found.get(i);
			assertEquals(500, f.fx, UtilEjml.TEST_F64_SQ);
			assertEquals(500, f.fy, UtilEjml.TEST_F64_SQ);
			assertEquals(0, f.skew, UtilEjml.TEST_F64_SQ);
		}
	}

	private void checkSolve( List<CameraPinhole> intrinsics, boolean knownAspect, boolean zeroSkew, int numProjectives ) {
		renderGood(intrinsics);

		CameraPinhole a = intrinsics.get(0);

		double aspect = a.fy/a.fx;
		SelfCalibrationLinearDualQuadratic alg;
		if (zeroSkew) {
			if (knownAspect) {
				alg = new SelfCalibrationLinearDualQuadratic(aspect);
			} else {
				alg = new SelfCalibrationLinearDualQuadratic(true);
			}
		} else {
			alg = new SelfCalibrationLinearDualQuadratic(false);
		}

		assertEquals(numProjectives, alg.getMinimumProjectives());

		addProjectives(alg);

		assertEquals(GeometricResult.SUCCESS, alg.solve());

		assertEquals(intrinsics.size(), alg.getIntrinsics().size());
		for (int i = 0; i < intrinsics.size(); i++) {
			CameraPinhole intrinsic = intrinsics.get(i);
			Intrinsic found = alg.getIntrinsics().get(i);

			assertEquals(intrinsic.fx, found.fx, UtilEjml.TEST_F64_SQ);
			assertEquals(intrinsic.fy, found.fy, UtilEjml.TEST_F64_SQ);
			assertEquals(intrinsic.skew, found.skew, UtilEjml.TEST_F64_SQ);
		}
	}

	@Test void geometry_no_rotation() {
		CameraPinhole intrinsic = new CameraPinhole(400, 420, 0.1, 0, 0, 0, 0);
		renderTranslationOnly(intrinsic);

		SelfCalibrationLinearDualQuadratic alg = new SelfCalibrationLinearDualQuadratic(false);
		addProjectives(alg);

		assertEquals(GeometricResult.GEOMETRY_POOR, alg.solve());
	}

	@Test void geometry_no_translation() {
		CameraPinhole intrinsic = new CameraPinhole(400, 420, 0.1, 0, 0, 0, 0);
		renderRotationOnly(intrinsic);

		SelfCalibrationLinearDualQuadratic alg = new SelfCalibrationLinearDualQuadratic(false);
		addProjectives(alg);

		assertEquals(GeometricResult.GEOMETRY_POOR, alg.solve());
	}

	@Test void constructMatrix() {
		List<CameraPinhole> intrinsics = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			intrinsics.add(new CameraPinhole(400 + i*5, 420, 0.1, 0, 0, 0, 0));
		}

		renderGood(intrinsics);

		SelfCalibrationLinearDualQuadratic alg = new SelfCalibrationLinearDualQuadratic(false);
		addProjectives(alg);

		DMatrixRMaj L = new DMatrixRMaj(3, 3);
		alg.constructMatrix(L);

		DMatrixRMaj q = new DMatrixRMaj(10, 1);
		q.data[0] = Q.get(0, 0);
		q.data[1] = Q.get(0, 1);
		q.data[2] = Q.get(0, 2);
		q.data[3] = Q.get(0, 3);
		q.data[4] = Q.get(1, 1);
		q.data[5] = Q.get(1, 2);
		q.data[6] = Q.get(1, 3);
		q.data[7] = Q.get(2, 2);
		q.data[8] = Q.get(2, 3);
		q.data[9] = Q.get(3, 3);

//		double[] sv = SingularOps_DDRM.singularValues(L);
//		for (int i = 0; i < sv.length; i++) {
//			System.out.println("sv["+i+"] = "+sv[i]);
//		}

		DMatrixRMaj found = new DMatrixRMaj(L.numRows, 1);

		// See if it's the null space
		CommonOps_DDRM.mult(L, q, found);
		assertEquals(0, NormOps_DDRM.normF(found), 10*UtilEjml.TEST_F64);
	}
}
