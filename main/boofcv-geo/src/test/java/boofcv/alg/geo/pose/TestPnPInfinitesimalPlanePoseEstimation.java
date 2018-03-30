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

package boofcv.alg.geo.pose;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix2x2;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.equation.Equation;
import org.ejml.equation.VariableMatrix;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPnPInfinitesimalPlanePoseEstimation {
	@Test
	public void simpleTest() {
		fail("Implement");
	}

	@Test
	public void estimateTranslation() {
		fail("Implement");
	}

	@Test
	public void IPPE() {
		fail("Implement");
	}

	@Test
	public void constructR() {
		Equation eq = new Equation();
		eq.getFunctions().add("cross", (inputs, manager) -> {
			DMatrixRMaj output = manager.createMatrix().matrix;
			DMatrixRMaj m1 = ((VariableMatrix)inputs.get(0)).matrix;
			DMatrixRMaj m2 = ((VariableMatrix)inputs.get(1)).matrix;

			output.reshape(3,1);
			Vector3D_F64 v1 = new Vector3D_F64(m1.data[0],m1.data[1],m1.data[2]);
			Vector3D_F64 v2 = new Vector3D_F64(m2.data[0],m2.data[1],m2.data[2]);
			Vector3D_F64 c = new Vector3D_F64();

			GeometryMath_F64.cross(v1,v2,c);
			output.data[0] = c.x;
			output.data[1] = c.y;
			output.data[2] = c.z;
		});
		eq.process("R_v=randn(3,3)");
		eq.process("R22=[1 2;3 4]");
		eq.process("b=[5;6]");
		eq.process("ca=cross([R22;b']*[1;0] , [R22;b']*[0;1])");

		eq.print("ca");

//		PnPInfinitesimalPlanePoseEstimation.constructR();

		fail("Implement");
	}

	@Test
	public void compute_B() {
		Equation eq = new Equation();
		eq.process("v=[1.1,0.5]'");
		eq.process("R_v=[1,2,3;4,5,6;7,8,9]'");
		eq.process("B=[eye(2),-v]*R_v");

		DMatrixRMaj v = eq.lookupDDRM("v");
		DMatrixRMaj R_v = eq.lookupDDRM("R_v");
		DMatrixRMaj expected = eq.lookupDDRM("B");

		double v1 = v.get(0);
		double v2 = v.get(1);
		DMatrix2x2 B = new DMatrix2x2();

		PnPInfinitesimalPlanePoseEstimation.compute_B(B,R_v,v1,v2);
		assertEquals(expected.get(0,0),B.a11, UtilEjml.TEST_F64);
		assertEquals(expected.get(0,1),B.a12, UtilEjml.TEST_F64);
		assertEquals(expected.get(1,0),B.a21, UtilEjml.TEST_F64);
		assertEquals(expected.get(1,1),B.a22, UtilEjml.TEST_F64);
	}

	@Test
	public void largestSingularValue2x2() {
		DMatrix2x2 M = new DMatrix2x2(1,-1.5,0.5,1.8);

		SimpleMatrix A = new SimpleMatrix(new double[][]{{M.a11,M.a12},{M.a21,M.a22}});

		double[] s = A.svd().getSingularValues();

		PnPInfinitesimalPlanePoseEstimation alg = new PnPInfinitesimalPlanePoseEstimation();

		double found = alg.largestSingularValue2x2(M);
		assertEquals(s[0],found,UtilEjml.TEST_F64);
	}

	@Test
	public void compute_Rv() {
		Equation eq = new Equation();
		eq.process("v=[1.1,0.5]'");
		eq.process("t=normF(v)");
		eq.process("s=normF([v',1]')");
		eq.process("cosT=1.0/s");
		eq.process("sinT=sqrt(1-1.0/s^2)");
		eq.process("Kx=(1.0/t)*[[0 0;0 0] v;-v' 0]");
		eq.process("R_v = eye(3) + sinT*Kx + (1.0-cosT)*Kx*Kx");
		PnPInfinitesimalPlanePoseEstimation alg = new PnPInfinitesimalPlanePoseEstimation();

		DMatrixRMaj V = eq.lookupDDRM("v");
		alg.v1 = V.get(0);
		alg.v2 = V.get(1);
		alg.compute_Rv();

		DMatrixRMaj expected_R_v = eq.lookupDDRM("R_v");

		assertTrue(MatrixFeatures_DDRM.isEquals(expected_R_v,alg.R_v, UtilEjml.TEST_F64));
	}
}