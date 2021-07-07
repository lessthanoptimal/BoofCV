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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.TrifocalTensor;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * @author Peter Abeles
 */
class TestManagerTrifocalTensor extends BoofStandardJUnit {
	@Test void newInstance() {
		ManagerTrifocalTensor alg = new ManagerTrifocalTensor();
		TrifocalTensor a = alg.createModelInstance();
		TrifocalTensor b = alg.createModelInstance();

		assertNotSame(a, b);
	}

	@Test void copy() {
		ManagerTrifocalTensor alg = new ManagerTrifocalTensor();

		TrifocalTensor a = new TrifocalTensor();
		a.T1.set(0,0,1);
		a.T2.set(1,0,2);
		a.T3.set(0,2,3);

		TrifocalTensor b = new TrifocalTensor();
		alg.copyModel(a,b);

		assertEquals(1,b.T1.get(0,0), UtilEjml.TEST_F64);
		assertEquals(2,b.T2.get(1,0), UtilEjml.TEST_F64);
		assertEquals(3,b.T3.get(0,2), UtilEjml.TEST_F64);
	}
}
