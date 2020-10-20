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

package boofcv.visualize;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestPeriodicColorizer extends BoofStandardJUnit {
	@Test
	void triangleWave() {
		Dummy alg = new Dummy();
		alg.setPeriod(2);

		assertEquals(0.0,alg.triangleWave(0), UtilEjml.TEST_F64);
		assertEquals(1.0,alg.triangleWave(1), UtilEjml.TEST_F64);
		assertEquals(0.0,alg.triangleWave(2), UtilEjml.TEST_F64);
		assertEquals(0.5,alg.triangleWave(2.5), UtilEjml.TEST_F64);
		assertEquals(0.5,alg.triangleWave(3.5), UtilEjml.TEST_F64);
		assertEquals(0.25,alg.triangleWave(3.75), UtilEjml.TEST_F64);
		assertEquals(0.25,alg.triangleWave(4.25), UtilEjml.TEST_F64);
	}

	private static class Dummy extends PeriodicColorizer {
		@Override
		public int color(int index, double x, double y, double z) {
			return 0;
		}
	}
}
