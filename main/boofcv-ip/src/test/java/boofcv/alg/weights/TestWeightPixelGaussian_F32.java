/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.weights;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestWeightPixelGaussian_F32 extends BoofStandardJUnit {

	/**
	 * Sees if changing the radius has the expected result
	 */
	@Test
	void changeRadius() {
		WeightPixelGaussian_F32 alg = new WeightPixelGaussian_F32();

		alg.setRadius(2,2, true);
		assertEquals(2,alg.getRadiusX());
		assertEquals(2,alg.getRadiusY());
		float middle = alg.weight(0,0);

		assertTrue(alg.isOdd());

		// see if it blows up
		alg.weight(-2,  0);alg.weight(2, 0);
		alg.weight( 0, -2);alg.weight(0, 2);

		// these should be equal
		assertEquals(alg.weight(-2,0),alg.weight(2,0), UtilEjml.TEST_F32);
		assertEquals(alg.weight(0,-2),alg.weight(0,2), UtilEjml.TEST_F32);

		// make it larger
		alg.setRadius(3,3, true);
		assertEquals(3,alg.getRadiusX());
		assertEquals(3,alg.getRadiusY());
		assertTrue( middle > alg.weight(0,0));
		alg.weight(-3,  0);alg.weight(3, 0);
		alg.weight( 0, -3);alg.weight(0, 3);

		// shouldn't declare a new kernel if the same size is requested
		Object before = alg.kernel;
		alg.setRadius(3,3, true);
		assertTrue( before == alg.kernel );
	}

	@Test
	void withEvenRadius() {
		WeightPixelGaussian_F32 alg = new WeightPixelGaussian_F32();

		alg.setRadius(2,2, false);
		assertFalse(alg.isOdd());

		// see if it blows up
		alg.weight(-2,0);alg.weight(1,0);
		alg.weight(0,-2);alg.weight(0,1);

		// make sure they are equal. they won't be off odd
		assertEquals(alg.weight(-2,0),alg.weight(1,0), UtilEjml.TEST_F32);
		assertEquals(alg.weight(0,-2),alg.weight(0,1), UtilEjml.TEST_F32);
	}
}
