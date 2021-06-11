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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.homography.Homography2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.equation.Equation;
import org.ejml.ops.DConvertMatrixStruct;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestSelfCalibrationLinearRotationSingle extends CommonAutoCalibrationChecks {
	@Test void perfect() {
		CameraPinhole intrinsic = new CameraPinhole(400, 420, 0.1, 450, 475, 0, 0);
		renderRotationOnly(intrinsic);
		performTest(intrinsic);
	}

	@Test void two_axis() {
		CameraPinhole intrinsic = new CameraPinhole(400, 420, 0.1, 450, 475, 0, 0);
		renderRotateTwoAxis(intrinsic);
		performTest(intrinsic);
	}

	private void performTest( CameraPinhole intrinsic ) {
		// compute planes at infinity
		List<Homography2D_F64> homographies = new ArrayList<>();
		for (int i = 0; i < listP.size(); i++) {
			DMatrixRMaj P = listP.get(i);

			Equation eq = new Equation();
			eq.alias(P, "P", planeAtInfinity, "p");
			eq.process("H = P(:,0:2) - P(:,3)*p'");
			Homography2D_F64 H = new Homography2D_F64();
			DConvertMatrixStruct.convert(eq.lookupDDRM("H"), H);
			homographies.add(H);
		}

		SelfCalibrationLinearRotationSingle alg = new SelfCalibrationLinearRotationSingle();
		CameraPinhole found = new CameraPinhole();
		assertTrue(alg.estimate(homographies, found));

		assertEquals(intrinsic.fx, found.fx, UtilEjml.TEST_F64_SQ);
		assertEquals(intrinsic.fy, found.fy, UtilEjml.TEST_F64_SQ);
		assertEquals(intrinsic.cx, found.cx, UtilEjml.TEST_F64_SQ);
		assertEquals(intrinsic.cy, found.cy, UtilEjml.TEST_F64_SQ);
		assertEquals(intrinsic.skew, found.skew, UtilEjml.TEST_F64_SQ);
	}

	/**
	 * This should fail because there isn't enough visual motion
	 */
	@Test void stationary() {
		CameraPinhole intrinsic = new CameraPinhole(400, 420, 0.1, 450, 475, 0, 0);
		renderStationary(intrinsic);
		checkFailure();
	}

	/**
	 * One axis rotation will fail
	 */
	@Test void one_axis() {
		CameraPinhole intrinsic = new CameraPinhole(400, 420, 0.1, 450, 475, 0, 0);
		renderRotateOneAxis(intrinsic);
		checkFailure();
	}

	private void checkFailure() {
		// compute planes at infinity
		List<Homography2D_F64> homographies = new ArrayList<>();
		for (int i = 0; i < listP.size(); i++) {
			DMatrixRMaj P = listP.get(i);

			Equation eq = new Equation();
			eq.alias(P, "P", planeAtInfinity, "p");
			eq.process("H = P(:,0:2) - P(:,3)*p'");
			Homography2D_F64 H = new Homography2D_F64();
			DConvertMatrixStruct.convert(eq.lookupDDRM("H"), H);
			homographies.add(H);
		}

		SelfCalibrationLinearRotationSingle alg = new SelfCalibrationLinearRotationSingle();
		CameraPinhole found = new CameraPinhole();
		assertFalse(alg.estimate(homographies, found));
	}

	@Test void checkLinearSystem() {
		CameraPinhole intrinsic = new CameraPinhole(400, 420, 0.1, 450, 475, 0, 0);
		renderRotationOnly(intrinsic);

		DMatrixRMaj K = new DMatrixRMaj(3, 3);
		PerspectiveOps.pinholeToMatrix(intrinsic, K);
		DMatrixRMaj w = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.multTransB(K, K, w);

		// compute planes at infinity
		List<Homography2D_F64> homographies = new ArrayList<>();
		for (int i = 0; i < listP.size(); i++) {
//			System.out.println("projective "+i);
			DMatrixRMaj P = listP.get(i);

			Equation eq = new Equation();
			eq.alias(P, "P", planeAtInfinity, "p", K, "K", w, "w");
			eq.process("H = P(:,0:2) - P(:,3)*p'");
//			eq.process("w2 = H*w*H'");
//			eq.process("w2 = w2 - w");

//			System.out.println("w");
//			eq.lookupDDRM("w").print();
//			System.out.println("w2");
//			eq.lookupDDRM("w2").print();

			Homography2D_F64 H = new Homography2D_F64();
			DConvertMatrixStruct.convert(eq.lookupDDRM("H"), H);
			homographies.add(H);
//			H.print();
		}

		SelfCalibrationLinearRotationSingle alg = new SelfCalibrationLinearRotationSingle();
		alg.ensureDeterminantOfOne(homographies);

		int N = homographies.size();
		DMatrixRMaj A = new DMatrixRMaj(6*N, 6);
		DMatrixRMaj X = new DMatrixRMaj(6, 1);
		DMatrixRMaj found = new DMatrixRMaj(A.numRows, 1);

		for (int i = 0; i < homographies.size(); i++) {
			alg.add(i, homographies.get(i), A);
		}

		X.data[0] = w.data[0];
		X.data[1] = w.data[1];
		X.data[2] = w.data[2];
		X.data[3] = w.data[4];
		X.data[4] = w.data[5];
		X.data[5] = w.data[8];

		CommonOps_DDRM.mult(A, X, found);
		assertEquals(0, NormOps_DDRM.normF(found), UtilEjml.TEST_F64);
	}
}
