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

package boofcv.alg.geo.structure;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.equation.Equation;
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.ops.MatrixFeatures_D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestDecomposeAbsoluteDualQuadratic extends BoofStandardJUnit {

	DMatrixRMaj K = new DMatrixRMaj(new double[][]{{400,1.1,450},{0,420,460},{0,0,1}});

	@Test void decompose() {

		Equation eq =  new Equation(K,"K");
		DMatrixRMaj Q = eq.process("I=diag([1,1,1,0])").
				process("p=[2.1;0.4;-0.3]").
				process("H=[K [0;0;0];-p'*K 1]").
				process("Q=H*I*H'").
				process("Q=Q*1e-3"). // change scale of Q to make it more interesting
				lookupDDRM("Q");

		DMatrixRMaj w = eq.process("w=Q(0:2,0:2)").lookupDDRM("w");
		DMatrixRMaj p = eq.process("p=-inv(w)*Q(0:2,3)").lookupDDRM("p");

		DMatrix4x4 _Q = new DMatrix4x4();
		DConvertMatrixStruct.convert(Q,_Q);

		DecomposeAbsoluteDualQuadratic alg = new DecomposeAbsoluteDualQuadratic();
		assertTrue(alg.decompose(_Q));


		CommonOps_DDRM.scale(1.0/w.get(2,2),w);

		assertTrue(MatrixFeatures_D.isIdentical(w,alg.getW(), UtilEjml.TEST_F64));
		assertTrue(MatrixFeatures_D.isIdentical(p,alg.getP(), UtilEjml.TEST_F64));
	}

	@Test void recomputeQ() {
		Equation eq = new Equation();
		eq.process("k = [300 3, 204;0 230 400; 0 0 1]").
				process("w = k*k'").
				process("p=[2.1;0.4;-0.3]").process("Q=[w , -w*p;-p'*w, p'*w*p]");


		DecomposeAbsoluteDualQuadratic alg = new DecomposeAbsoluteDualQuadratic();
		DConvertMatrixStruct.convert(eq.lookupDDRM("k"),alg.getK());
		DConvertMatrixStruct.convert(eq.lookupDDRM("p"),alg.getP());

		DMatrix4x4 found = new DMatrix4x4();
		alg.recomputeQ(found);

		DMatrixRMaj Q = eq.lookupDDRM("Q");

		assertTrue(MatrixFeatures_D.isIdentical(Q,found, UtilEjml.TEST_F64));
	}

	/**
	 * Create a dual quadratic from its definition and see if its correctly decomposed
	 */
	@Test void computeRectifyingHomography() {
		Equation eq =  new Equation(K,"K");
		DMatrixRMaj Q = eq.process("I=diag([1,1,1,0])").
				process("p=[2.1;0.4;-0.3]").
				process("H=[K [0;0;0];-p'*K 1]").
				process("Q=H*I*H'").
				process("Q=Q*1e-3"). // change scale of Q to make it more interesting
				lookupDDRM("Q");
		DMatrixRMaj H = eq.lookupDDRM("H");

		DMatrix4x4 _Q = new DMatrix4x4();
		DConvertMatrixStruct.convert(Q,_Q);

		DecomposeAbsoluteDualQuadratic alg = new DecomposeAbsoluteDualQuadratic();
		assertTrue(alg.decompose(_Q));

		DMatrixRMaj foundH = new DMatrixRMaj(4,4);
		assertTrue(alg.computeRectifyingHomography(foundH));
		assertTrue(MatrixFeatures_DDRM.isIdentical(H,foundH, UtilEjml.TEST_F64));
		assertTrue(MatrixFeatures_D.isIdentical(K,alg.getK(), UtilEjml.TEST_F64));
	}
}
