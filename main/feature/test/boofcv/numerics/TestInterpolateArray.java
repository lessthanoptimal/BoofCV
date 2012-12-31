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

package boofcv.numerics;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestInterpolateArray {

	@Test
	public void linearCase() {
		double d[] = new double[]{1,3,5,7};

		InterpolateArray alg = new InterpolateArray(d);

		assertTrue(alg.interpolate(0.5));
		assertEquals(2, alg.value, 1e-8);

		assertTrue(alg.interpolate(1.1));
		assertEquals(3.2,alg.value,1e-8);
	}

	@Test
	public void checkBounds() {
		double d[] = new double[]{1,3,5,7};

		InterpolateArray alg = new InterpolateArray(d);

		assertTrue(alg.interpolate(0));
		assertTrue(alg.interpolate(2.9));
		assertFalse(alg.interpolate(3));
		assertFalse(alg.interpolate(-0.1));
	}

}
