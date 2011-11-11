/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d3.epipolar;

import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.RandomMatrices;
import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestEpipolarConstraintMatricesLinear {

	Random rand = new Random(234);

	/**
	 * Provide a random matrix and see if it sets the last singular valuye to be zero
	 */
	@Test
	public void enforceSmallZeroSingularValue() {
		Dummy d = new Dummy();
		RandomMatrices.addRandom(d.M,0,10,rand);

		d.enforceSmallZeroSingularValue();

		SimpleMatrix m = SimpleMatrix.wrap(d.M);

		double smallestSingularValue = m.svd().getSingleValue(2);
		assertEquals(smallestSingularValue,0,1e-5);

		// make sure the other singular values are not zero
		assertTrue(m.svd().getSingleValue(1) > 1e-5);
	}

	/**
	 * Test it against a simple test case
	 */
	@Test
	public void normalize() {
		DenseMatrix64F N = new DenseMatrix64F(3,3,true,1,2,3,4,5,6,7,8,9);

		Point2D_F64 a = new Point2D_F64(3,4);
		Point2D_F64 found = new Point2D_F64(3,4);
		Point2D_F64 expected = new Point2D_F64(3,4);

		expected.x = a.x * N.get(0,0) + N.get(0,2);
		expected.y = a.y * N.get(1,1) + N.get(1,2);

		// use the actual matrix from Dummy to make sure its the appropriate size
		Dummy d = new Dummy();
		d.N1.set(N);

		EpipolarConstraintMatricesLinear.normalize(a,found,d.N1);

		assertEquals(found.x,expected.x,1e-8);
		assertEquals(found.y,expected.y,1e-8);
	}

	public static class Dummy extends EpipolarConstraintMatricesLinear
	{

	}
}
