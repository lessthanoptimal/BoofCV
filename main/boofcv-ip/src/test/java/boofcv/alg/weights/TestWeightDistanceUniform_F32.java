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

package boofcv.alg.weights;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestWeightDistanceUniform_F32 extends BoofStandardJUnit {

	@Test void basic() {
		WeightDistanceUniform_F32 alg = new WeightDistanceUniform_F32(1.5f);

		float expected = 1.0f/1.5f;

		assertEquals(expected, alg.weight(0), 1e-4f);
		assertEquals(expected, alg.weight(0.6f), 1e-4f);
		assertEquals(expected, alg.weight(1.5f), 1e-4f);
		assertEquals(0, alg.weight(1.50001f), 1e-4f);
		assertEquals(0, alg.weight(2f), 1e-4f);
	}

}
