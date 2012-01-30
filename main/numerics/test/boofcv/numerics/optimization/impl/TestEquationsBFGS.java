/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization.impl;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.ejml.ops.RandomMatrices;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEquationsBFGS {
	
	Random rand = new Random(234);

	@Test
	public void inverseUpdate() {
		int N = 6;
		DenseMatrix64F H = RandomMatrices.createSymmetric(N,-1,1,rand);
		DenseMatrix64F s = RandomMatrices.createRandom(N,1,-1,1,rand);
		DenseMatrix64F y = RandomMatrices.createRandom(N,1,-1,1,rand);
		DenseMatrix64F tempV0 = new DenseMatrix64F(N,1);
		DenseMatrix64F tempV1 = new DenseMatrix64F(N,1);

		DenseMatrix64F expected = H.copy();
		DenseMatrix64F found = H.copy();

		EquationsBFGS.naiveInverseUpdate(expected, s, y);
		EquationsBFGS.inverseUpdate(found,s,y.copy(),tempV0,tempV1);

		assertTrue(MatrixFeatures.isIdentical(expected, found, 1e-8));
	}


}
