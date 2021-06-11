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

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.equation.Equation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelfCalibrationGuessAndCheckFocus extends BoofStandardJUnit {
	double cx = 500, cy = 490, skew = 0.1, fx = 600;
	int width = 1000, height = 800;

	@Test void perfect_data_oneK() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(fx, fx, skew, cx, cy);

		Se3_F64 view0_to_view0 = new Se3_F64();
		Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(1, 0.1, -0.6, 0.1, -0.05, -0.2, null);

		DMatrixRMaj P1a = PerspectiveOps.createCameraMatrix(view0_to_view0.R, view0_to_view0.T, K, null);
		DMatrixRMaj P2a = PerspectiveOps.createCameraMatrix(view0_to_view1.R, view0_to_view1.T, K, null);

		// Convert it into the canonical projection matrices with P1 = [I|0]
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		MultiViewOps.projectiveToIdentityH(P1a, H);

		DMatrixRMaj P1 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(P1a, H, P1);
		CommonOps_DDRM.mult(P2a, H, P2);

		SelfCalibrationPraticalGuessAndCheckFocus alg = new SelfCalibrationPraticalGuessAndCheckFocus();
//		alg.setVerbose(System.out,0);
		alg.setSampling(0.1, 3, 200);
		alg.setSingleCamera(true);
		alg.setCamera(skew, cx, cy, width, height);

		List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
		cameraMatrices.add(P2);

		assertTrue(alg.process(cameraMatrices));

		H = alg.getRectifyingHomography();

		// Can't expect perfect results due to how the focal lengths are sampled
		DMatrixRMaj KF = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.mult(P1, H, P1a);
		MultiViewOps.decomposeMetricCamera(P1a, KF, new Se3_F64());
//		KF.print();
		assertEquals(K.get(0, 0), KF.get(0, 0), 12);
		assertEquals(K.get(1, 1), KF.get(1, 1), 12);
		assertEquals(K.get(0, 1), KF.get(0, 1), 0.01);
		assertEquals(K.get(0, 2), KF.get(0, 2), UtilEjml.TEST_F64);
		assertEquals(K.get(1, 2), KF.get(1, 2), UtilEjml.TEST_F64);

		CommonOps_DDRM.mult(P2, H, P1a);
		MultiViewOps.decomposeMetricCamera(P1a, KF, new Se3_F64());
//		KF.print();
		assertEquals(K.get(0, 0), KF.get(0, 0), 12);
		assertEquals(K.get(1, 1), KF.get(1, 1), 12);
		assertEquals(K.get(0, 1), KF.get(0, 1), 1); // skew estimate tends to be very bad
		assertEquals(K.get(0, 2), KF.get(0, 2), 0.1);
		assertEquals(K.get(1, 2), KF.get(1, 2), 0.1);
	}

	@Test void perfect_data_twoK() {

		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(fx, fx, skew, cx, cy);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(fx + 150, fx + 150, skew, cx, cy);

		Se3_F64 view0_to_view0 = new Se3_F64();
		Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(1, 0.1, -0.6, 0.1, -0.05, -0.2, null);

		DMatrixRMaj P1a = PerspectiveOps.createCameraMatrix(view0_to_view0.R, view0_to_view0.T, K1, null);
		DMatrixRMaj P2a = PerspectiveOps.createCameraMatrix(view0_to_view1.R, view0_to_view1.T, K2, null);

		// Conver it into the canonical projection matrices with P1 = [I|0]
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		MultiViewOps.projectiveToIdentityH(P1a, H);

		DMatrixRMaj P1 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(P1a, H, P1);
		CommonOps_DDRM.mult(P2a, H, P2);

		SelfCalibrationPraticalGuessAndCheckFocus alg = new SelfCalibrationPraticalGuessAndCheckFocus();
//		alg.setVerbose(System.out,0);
		alg.setSampling(0.1, 3, 200);
		alg.setCamera(skew, cx, cy, width, height);

		List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
		cameraMatrices.add(P2);

		assertTrue(alg.process(cameraMatrices));
		H = alg.getRectifyingHomography();

		// Can't expect perfect results due to how the focal lengths are sampled
		DMatrixRMaj KF = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.mult(P1, H, P1a);
		MultiViewOps.decomposeMetricCamera(P1a, KF, new Se3_F64());
//		KF.print();
		assertEquals(K1.get(0, 0), KF.get(0, 0), 12);
		assertEquals(K1.get(1, 1), KF.get(1, 1), 12);
		assertEquals(K1.get(0, 1), KF.get(0, 1), 0.01);
		assertEquals(K1.get(0, 2), KF.get(0, 2), UtilEjml.TEST_F64);
		assertEquals(K1.get(1, 2), KF.get(1, 2), UtilEjml.TEST_F64);

		CommonOps_DDRM.mult(P2, H, P1a);
		MultiViewOps.decomposeMetricCamera(P1a, KF, new Se3_F64());
//		KF.print();
		assertEquals(K2.get(0, 0), KF.get(0, 0), 12);
		assertEquals(K2.get(1, 1), KF.get(1, 1), 12);
		assertEquals(K2.get(0, 1), KF.get(0, 1), 1); // skew estimate tends to be very bad
		assertEquals(K2.get(0, 2), KF.get(0, 2), 10);
		assertEquals(K2.get(1, 2), KF.get(1, 2), 10);
	}

	@Test void perfect_data_three_views() {
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(fx, fx, skew, cx, cy);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(fx + 150, fx + 150, skew, cx, cy);
		DMatrixRMaj K3 = PerspectiveOps.pinholeToMatrix(fx + 50, fx + 50, skew, cx, cy);

		Se3_F64 view0_to_view0 = new Se3_F64();
		Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(1, 0.1, -0.6, 0.1, -0.05, -0.2, null);
		Se3_F64 view0_to_view2 = SpecialEuclideanOps_F64.eulerXyz(0.5, -0.15, -0.4, 0.2, 0, -0.1, null);

		DMatrixRMaj P1a = PerspectiveOps.createCameraMatrix(view0_to_view0.R, view0_to_view0.T, K1, null);
		DMatrixRMaj P2a = PerspectiveOps.createCameraMatrix(view0_to_view1.R, view0_to_view1.T, K2, null);
		DMatrixRMaj P3a = PerspectiveOps.createCameraMatrix(view0_to_view2.R, view0_to_view2.T, K3, null);

		// Conver it into the canonical projection matrices with P1 = [I|0]
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		MultiViewOps.projectiveToIdentityH(P1a, H);

		DMatrixRMaj P1 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(P1a, H, P1);
		CommonOps_DDRM.mult(P2a, H, P2);
		CommonOps_DDRM.mult(P3a, H, P3);

		// make sure it is scale invariant
		CommonOps_DDRM.scale(2.6, P2);

		SelfCalibrationPraticalGuessAndCheckFocus alg = new SelfCalibrationPraticalGuessAndCheckFocus();
//		alg.setSingleCamera(true);
		alg.setSampling(0.1, 3, 200);
		alg.setCamera(skew, cx, cy, width, height);

		List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
		cameraMatrices.add(P2);
		cameraMatrices.add(P3);

		assertTrue(alg.process(cameraMatrices));
		H = alg.getRectifyingHomography();

		// Can't expect perfect results due to how the focal lengths are sampled
		DMatrixRMaj KF = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.mult(P1, H, P1a);
		MultiViewOps.decomposeMetricCamera(P1a, KF, new Se3_F64());
//		KF.print();
		assertEquals(K1.get(0, 0), KF.get(0, 0), 12);
		assertEquals(K1.get(1, 1), KF.get(1, 1), 12);
		assertEquals(K1.get(0, 1), KF.get(0, 1), 0.01);
		assertEquals(K1.get(0, 2), KF.get(0, 2), UtilEjml.TEST_F64);
		assertEquals(K1.get(1, 2), KF.get(1, 2), UtilEjml.TEST_F64);

		CommonOps_DDRM.mult(P2, H, P1a);
		MultiViewOps.decomposeMetricCamera(P1a, KF, new Se3_F64());
//		KF.print();
		assertEquals(K2.get(0, 0), KF.get(0, 0), 12);
		assertEquals(K2.get(1, 1), KF.get(1, 1), 12);
		assertEquals(K2.get(0, 1), KF.get(0, 1), 1); // skew estimate tends to be very bad
		assertEquals(K2.get(0, 2), KF.get(0, 2), 10);
		assertEquals(K2.get(1, 2), KF.get(1, 2), 10);

		CommonOps_DDRM.mult(P3, H, P1a);
		MultiViewOps.decomposeMetricCamera(P1a, KF, new Se3_F64());
//		KF.print();
		assertEquals(K3.get(0, 0), KF.get(0, 0), 12);
		assertEquals(K3.get(1, 1), KF.get(1, 1), 12);
		assertEquals(K3.get(0, 1), KF.get(0, 1), 1); // skew estimate tends to be very bad
		assertEquals(K3.get(0, 2), KF.get(0, 2), 10);
		assertEquals(K3.get(1, 2), KF.get(1, 2), 10);
	}

	/**
	 * See if the solution has a few expected properties
	 */
	@Test void setCamera_properties() {
		SelfCalibrationPraticalGuessAndCheckFocus alg = new SelfCalibrationPraticalGuessAndCheckFocus();
		alg.setCamera(skew, cx, cy, width, height);

		// a point on the center should be zero
		Point2D_F64 x = new Point2D_F64(500, 490);
		Point2D_F64 y = new Point2D_F64();
		GeometryMath_F64.mult(alg.Vinv, x, y);
		assertEquals(0, y.x, UtilEjml.TEST_F64);
		assertEquals(0, y.y, UtilEjml.TEST_F64);

		// this should be negative
		x.setTo(0, 0);
		GeometryMath_F64.mult(alg.Vinv, x, y);
		assertTrue(y.x < 0 && -y.x < 1);
		assertTrue(y.y < 0 && -y.y < 1);

		// this should be positive
		x.setTo(999, 799);
		GeometryMath_F64.mult(alg.Vinv, x, y);
		assertTrue(y.x > 0 && y.x < 1);
		assertTrue(y.y > 0 && y.y < 1);
	}

	/**
	 * Compare to an equation written in easy to read code
	 */
	@Test void setCamera_equation() {
		SelfCalibrationPraticalGuessAndCheckFocus alg = new SelfCalibrationPraticalGuessAndCheckFocus();
		alg.setCamera(skew, cx, cy, width, height);

		Equation eq = new Equation(skew, "sk", cx, "cx", cy, "cy");
		eq.process("d = sqrt(1000^2 + 800^2)/2");
		eq.process("V=[d sk cx;0 d cy;0 0 1]");
		eq.process("A = inv(V)");

		DMatrixRMaj Vinv = eq.lookupDDRM("A");

		Point2D_F64 a0 = new Point2D_F64(756, 45);
		Point2D_F64 b0 = new Point2D_F64();
		GeometryMath_F64.mult(alg.Vinv, a0, b0);
		Point2D_F64 b1 = new Point2D_F64();
		GeometryMath_F64.mult(Vinv, a0, b1);

		assertEquals(0, b0.distance(b1), UtilEjml.TEST_F64);
	}
}
