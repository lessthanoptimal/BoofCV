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

package boofcv.struct.sparse;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestGradientValue_I32 extends BoofStandardJUnit {
	@Test
	void set_get() {
		var alg = new GradientValue_I32();
		alg.setTo(2.3,4.8);
		assertEquals(2, alg.x);
		assertEquals(4, alg.y);
		assertEquals(2, alg.getX(), UtilEjml.TEST_F64);
		assertEquals(4, alg.getY(), UtilEjml.TEST_F64);
	}
}
