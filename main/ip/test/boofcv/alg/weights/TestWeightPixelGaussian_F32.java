/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWeightPixelGaussian_F32 {

	/**
	 * Sees if changing the radius has the expected result
	 */
	@Test
	public void changeRadius() {
		WeightPixelGaussian_F32 alg = new WeightPixelGaussian_F32();

		alg.setRadius(2,2);
		assertEquals(2,alg.getRadiusX());
		assertEquals(2,alg.getRadiusY());
		float middle = alg.weight(0,0);

		// see if it blows up
		alg.weight(-2,  0);alg.weight(2, 0);
		alg.weight( 0, -2);alg.weight(0, 2);

		// make it larger
		alg.setRadius(3,3);
		assertEquals(3,alg.getRadiusX());
		assertEquals(3,alg.getRadiusY());
		assertTrue( middle > alg.weight(0,0));
		alg.weight(-3,  0);alg.weight(3, 0);
		alg.weight( 0, -3);alg.weight(0, 3);

		// shouldn't declare a new kernel if the same size is requested
		Object before = alg.kernel;
		alg.setRadius(3,3);
		assertTrue( before == alg.kernel );
	}

}
