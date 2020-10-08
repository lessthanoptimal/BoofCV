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
class TestWeightPixelUniform_F32 extends BoofStandardJUnit {

	@Test
	void inside() {
		WeightPixelUniform_F32 alg = new WeightPixelUniform_F32();
		alg.setRadius(2,3,true);

		float expected = 1.0f/(5f*7f);

		int index = 0;
		for( int y = -3; y <= 3; y++ )
			for( int x = -2; x <= 2; x++ , index++) {
				assertEquals(expected,alg.weight(x,y),1e-4);
				assertEquals(expected,alg.weightIndex(index),1e-4);
			}
	}

	/**
	 * Well if it is outside the square region it should be zero, but the class specifies that it doesn't actually
	 * check to see if that's the case.
	 */
	@Test
	void outside() {
		WeightPixelUniform_F32 alg = new WeightPixelUniform_F32();
		alg.setRadius(2,2,true);

		float expected = 1.0f/25.0f;

		assertEquals(expected,alg.weight(-3,0),1e-4);
	}

	@Test
	void withEvenRadius() {
		WeightPixelUniform_F32 alg = new WeightPixelUniform_F32();

		alg.setRadius(2,2, false);
		assertFalse(alg.isOdd());

		// see if it blows up
		alg.weight(-2,0);alg.weight(1,0);
		alg.weight(0,-2);alg.weight(0,1);

		// make sure they are equal. they won't be off odd
		for (int i = 0; i < 4; i++) {
			assertEquals(1.0/16.0,alg.weight(i-2,0), UtilEjml.TEST_F32);
			assertEquals(1.0/16.0,alg.weight(0,i-2), UtilEjml.TEST_F32);
		}
	}

	@Test
	void withOddRadius() {
		WeightPixelUniform_F32 alg = new WeightPixelUniform_F32();

		alg.setRadius(2,2, true);
		assertTrue(alg.isOdd());

		// see if it blows up
		alg.weight(-2,0);alg.weight(2,0);
		alg.weight(0,-2);alg.weight(0,2);

		// make sure they are equal. they won't be off odd
		for (int i = 0; i < 5; i++) {
			assertEquals(1.0/25.0,alg.weight(i-2,0), UtilEjml.TEST_F32);
			assertEquals(1.0/25.0,alg.weight(0,i-2), UtilEjml.TEST_F32);
		}
	}
}
