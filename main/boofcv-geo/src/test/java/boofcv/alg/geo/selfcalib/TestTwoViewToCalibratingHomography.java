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

import boofcv.BoofTesting;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestTwoViewToCalibratingHomography extends CommonThreeViewSelfCalibration {

	/**
	 * Give it cameras with various structures and see if it blows up
	 */
	@Test void perfect_cameras() {
		List<CameraPinhole> cameras = new ArrayList<>();
		cameras.add(new CameraPinhole(600, 600, 0.1, 400, 410, 800, 600));
		cameras.add(new CameraPinhole(600, 650, 0, 400, 410, 800, 600));
		cameras.add(new CameraPinhole(600, 650, 0, 0, 0, 800, 600));
		cameras.add(new CameraPinhole(1200, 1250, 0, 0, 0, 800, 600));
		cameras.add(new CameraPinhole(250, 220, 0, 30, 100, 800, 600));

		for (CameraPinhole camera : cameras) {
			standardScene();
			setCameras(camera, camera, camera);
			simulateScene(0);

			DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(camera, (DMatrixRMaj)null);
			var alg = new TwoViewToCalibratingHomography();

			alg.initialize(F21, P2);
			assertTrue(alg.process(K, K, observations2));

			checkSolution(alg);
		}
	}

	/**
	 * Have 3 different cameras render the scene
	 */
	@Test void perfect_different_cameras() {
		standardScene();
		cameraA = new CameraPinhole(600, 600, 0.1, 400, 410, 800, 600);
		cameraB = new CameraPinhole(600, 650, 0, 400, 410, 800, 600);
		cameraC = new CameraPinhole(1200, 1250, 0, 0, 0, 800, 600);

		simulateScene(0);

		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(cameraA, (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(cameraB, (DMatrixRMaj)null);

		var alg = new TwoViewToCalibratingHomography();

		alg.initialize(F21, P2);
		assertTrue(alg.process(K1, K2, observations2));

		checkSolution(alg);
	}

	private void checkSolution( TwoViewToCalibratingHomography alg ) {
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(cameraB, (DMatrixRMaj)null);

		Se3_F64 found_1_to_2 = new Se3_F64();
		DMatrixRMaj H = alg.getCalibrationHomography();

		DMatrixRMaj foundK2 = new DMatrixRMaj(3, 3);

		MultiViewOps.projectiveToMetric(alg.P2, H, found_1_to_2, foundK2);

		assertEquals(1.0, found_1_to_2.T.norm(), 1e-4);
		BoofTesting.assertEqualsToScaleS(truthView_1_to_i(1), found_1_to_2, 0.01, 1e-4);

		assertTrue(MatrixFeatures_DDRM.isIdentical(K2, foundK2, UtilEjml.TEST_F64));
	}

	/**
	 * Give it hand selected degenerate motions and see if it blows up
	 */
	@Test void perfect_motions() {
		perfect_motions(eulerXyz(1, 0.1, -2, -0.1, 0, 0.05, null).invert(null));
		perfect_motions(eulerXyz(1, 0.1, -2, -0.1, 0, 0.05, null).invert(null));

		// and it blows up with the ones below
//		perfect_motions(eulerXyz(1,0,-2,0,0,0.10,null).invert(null));
//		perfect_motions(eulerXyz(1,0,-2,-0.1,0,0.05,null).invert(null));
//		perfect_motions(eulerXyz(1,0,-2,0,0,0.0,null).invert(null));
	}

	void perfect_motions( Se3_F64 truth_world_to_2 ) {
		standardScene();
		// select a camera with the most zeros
//		CameraPinhole camera = new CameraPinhole(600,650,0,0,0,800,600);
//		setCameras(camera,camera,camera);
		list_world_to_cameras.set(1, truth_world_to_2);
		simulateScene(0);

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(cameraA, (DMatrixRMaj)null);
		var alg = new TwoViewToCalibratingHomography();

		alg.initialize(F21, P2);
		assertTrue(alg.process(K, K, observations2));

		checkSolution(alg);
	}

//	/**
//	 * Hand crafted scenario with lots of zeros in the input matrices
//	 */
//	@Test
//	void perfect_sadistic() {
//		standardScene();
//		// camera with lots of zeros
//		CameraPinhole camera = new CameraPinhole(600,650,0,0,0,800,600);
//		setCameras(camera,camera,camera);
//		// motion with lots of zeros
//		list_world_to_cameras.set(1,eulerXyz(1,0,-2,0,0,0.0,null).invert(null));
//		list_world_to_cameras.set(2,eulerXyz(0,1,-2,0,0,0.0,null).invert(null));
//
//		simulateScene(0.0);
//
//		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(cameraA, (DMatrixRMaj) null);
//		var alg = new TwoViewToCalibratingHomography();
//
//		alg.initialize(F21,P2);
//		assertTrue(alg.process(K, K, observations2));
//
//		checkSolution(alg);
//	}

	/**
	 * Add a little bit of noise and see if it blows up
	 */
	@Test void noisy() {
		standardScene();
		simulateScene(0.4);

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(cameraA, (DMatrixRMaj)null);
		var alg = new TwoViewToCalibratingHomography();

		alg.initialize(F21, P2);
		alg.process(K, K, observations2);

		Se3_F64 found_1_to_2 = new Se3_F64();
		DMatrixRMaj K2 = new DMatrixRMaj(3, 3);
		DMatrixRMaj H = alg.getCalibrationHomography();

		MultiViewOps.projectiveToMetric(alg.P2, H, found_1_to_2, K2);

		assertEquals(1.0, found_1_to_2.T.norm(), 1e-2);
		BoofTesting.assertEqualsToScaleS(truthView_1_to_i(1), found_1_to_2, 0.01, 1e-2);
		assertTrue(MatrixFeatures_DDRM.isIdentical(K, K2, 2));
	}

	/**
	 * Tests the computeHypothesesForH by giving it its inputs directly
	 */
	@Test void computeHypothesesForH() {
		standardScene();
		simulateScene(0);

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(cameraA, (DMatrixRMaj)null);

		var alg = new TwoViewToCalibratingHomography();

		Se3_F64 view_1_to_2 = new Se3_F64();
		list_world_to_cameras.get(0).invert(null).concat(list_world_to_cameras.get(1), view_1_to_2);
		// zap the scale
		view_1_to_2.T.divide(view_1_to_2.T.norm());

		alg.initialize(F21, P2);
		alg.computeHypothesesForH(K, K, BoofMiscOps.asList(view_1_to_2));
		assertEquals(1, alg.hypothesesH.size);
		Se3_F64 found_1_to_2 = new Se3_F64();
//		Se3_F64 found_1_to_3 = new Se3_F64();
		DMatrixRMaj K2 = new DMatrixRMaj(3, 3);
//		DMatrixRMaj K3 = new DMatrixRMaj(3,3);
		for (DMatrixRMaj H : alg.hypothesesH.toList()) {
			MultiViewOps.projectiveToMetric(alg.P2, H, found_1_to_2, K2);
			assertTrue(MatrixFeatures_DDRM.isIdentical(K, K2, UtilEjml.TEST_F64));
		}
	}
}
