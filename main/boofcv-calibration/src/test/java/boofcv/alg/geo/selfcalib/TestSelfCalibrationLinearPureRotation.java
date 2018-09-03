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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.homography.Homography2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.equation.Equation;
import org.ejml.ops.ConvertDMatrixStruct;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSelfCalibrationLinearPureRotation extends CommonAutoCalibrationChecks
{
	@Test
	public void perfect() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0.1,450,475,0,0);
		renderRotationOnly(intrinsic);

		// compute planes at infinity
		List<Homography2D_F64> homographies = new ArrayList<>();
		for (int i = 0; i < listP.size(); i++) {
			DMatrixRMaj P = listP.get(i);

			Equation eq = new Equation();
			eq.alias(P,"P",p,"p");
			eq.process("H = P(:,0:2) - P(:,3)*p'");
			Homography2D_F64 H = new Homography2D_F64();
			ConvertDMatrixStruct.convert(eq.lookupDDRM("H"),H);
			homographies.add(H);
		}

		SelfCalibrationLinearPureRotation alg = new SelfCalibrationLinearPureRotation();
		CameraPinhole found = new CameraPinhole();
		assertTrue(alg.estimate(homographies,found));

		found.print();
		System.out.println("Egads");
	}

	@Test
	public void checkLinearSystem() {
		CameraPinhole intrinsic = new CameraPinhole(400,420,0.1,450,475,0,0);
		renderRotationOnly(intrinsic);

		DMatrixRMaj K = new DMatrixRMaj(3,3);
		PerspectiveOps.pinholeToMatrix(intrinsic,K);

		// compute planes at infinity
		List<Homography2D_F64> homographies = new ArrayList<>();
		for (int i = 0; i < listP.size(); i++) {
			DMatrixRMaj P = listP.get(i);

			Equation eq = new Equation();
			eq.alias(P,"P",p,"p",K,"K");
			eq.process("H = P(:,0:2) - P(:,3)*p'");
			eq.process("w = K*K'");
			eq.process("w2 = H*w*H'");

//			System.out.println("w");
//			eq.lookupDDRM("w").print();
//			System.out.println("w2");
//			eq.lookupDDRM("w2").print();

			Homography2D_F64 H = new Homography2D_F64();
			ConvertDMatrixStruct.convert(eq.lookupDDRM("H"),H);
			homographies.add(H);
			H.print();
		}

		SelfCalibrationLinearPureRotation alg = new SelfCalibrationLinearPureRotation();

		int N = homographies.size();
		DMatrixRMaj A = new DMatrixRMaj(5*N,5);
		DMatrixRMaj B = new DMatrixRMaj(A.numRows,1);
		DMatrixRMaj X = new DMatrixRMaj(5,1);
		DMatrixRMaj found = new DMatrixRMaj(A.numRows,1);

		for (int i = 0; i < homographies.size(); i++) {
			alg.add(i,homographies.get(i),A,B);
		}

		X.data[0] = intrinsic.fx;
		X.data[1] = intrinsic.skew;
		X.data[2] = intrinsic.cx;
		X.data[3] = intrinsic.fy;
		X.data[4] = intrinsic.cy;

		CommonOps_DDRM.mult(A,X,found);
		found.print();
	}
}