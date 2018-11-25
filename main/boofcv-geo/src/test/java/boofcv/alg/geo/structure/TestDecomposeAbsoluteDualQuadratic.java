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

package boofcv.alg.geo.structure;

import org.ejml.UtilEjml;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.equation.Equation;
import org.ejml.ops.ConvertDMatrixStruct;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestDecomposeAbsoluteDualQuadratic {

	@Test
	public void decompose() {
		fail("Implement");
	}

	@Test
	public void recomputeQ() {
		fail("Implement");
	}

	/**
	 * Create a dual quadratic from its definition and see if its correctly decomposed
	 */
	@Test
	public void computeRectifyingHomography() {
		DMatrixRMaj K = new DMatrixRMaj(new double[][]{{400,1.1,450},{0,420,460},{0,0,1}});

		Equation eq =  new Equation(K,"K");
		DMatrixRMaj Q = eq.process("I=diag([1,1,1,0])").
				process("p=[2.1;0.4;-0.3]").
				process("H=[K [0;0;0];-p'*K 1]").
				process("Q=H*I*H'").
				process("Q=Q*1e-3"). // change scale of Q to make it more interesting
				lookupDDRM("Q");
		DMatrixRMaj H = eq.lookupDDRM("H");

		DMatrix4x4 _Q = new DMatrix4x4();
		ConvertDMatrixStruct.convert(Q,_Q);

		DecomposeAbsoluteDualQuadratic alg = new DecomposeAbsoluteDualQuadratic();
		assertTrue(alg.decompose(_Q));

		DMatrixRMaj foundH = new DMatrixRMaj(4,4);
		assertTrue(alg.computeRectifyingHomography(foundH));
		assertTrue(MatrixFeatures_DDRM.isIdentical(H,foundH, UtilEjml.TEST_F64));
	}
}