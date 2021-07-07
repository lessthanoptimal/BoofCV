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

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestGenerateTrifocalTensor extends BoofStandardJUnit {
	@Test void generate() {
		Estimator estimator = new Estimator();
		GenerateTrifocalTensor alg = new GenerateTrifocalTensor(estimator);

		assertTrue(alg.generate(new ArrayList<>(),new TrifocalTensor()));

		assertTrue(estimator.called);
	}

	@Test void getMinimumPoints() {
		Estimator estimator = new Estimator();
		GenerateTrifocalTensor alg = new GenerateTrifocalTensor(estimator);
		assertEquals(9,alg.getMinimumPoints());
	}

	public class Estimator implements Estimate1ofTrifocalTensor {

		boolean called= false;

		@Override
		public boolean process(List<AssociatedTriple> points, TrifocalTensor estimatedModel) {
			called = true;
			return true;
		}

		@Override
		public int getMinimumPoints() {
			return 9;
		}
	}
}
