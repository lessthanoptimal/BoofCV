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

import boofcv.alg.geo.trifocal.CommonTrifocalChecks;
import boofcv.struct.geo.AssociatedTriple;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestDistanceTrifocalReprojectionSq extends CommonTrifocalChecks {
	@Test void perfect() {

		DistanceTrifocalReprojectionSq alg = new DistanceTrifocalReprojectionSq();
		alg.setModel(tensorPixels);

		for (int i = 0; i < observationsPixels.size(); i++) {
			assertEquals(0, alg.distance(observationsPixels.get(i)), UtilEjml.TEST_F64);
		}
	}

	@Test void noise() {
		DistanceTrifocalReprojectionSq alg = new DistanceTrifocalReprojectionSq();
		alg.setModel(tensorPixels);

		for (int i = 0; i < observationsPixels.size(); i++) {
			AssociatedTriple a = observationsPixels.get(i);
			a.p1.x += 0.5;
			assertEquals(0, alg.distance(observationsPixels.get(i)), 1.5*0.5*0.5);
			a.p1.x -= 0.5;
			a.p2.x += 0.5;
			assertEquals(0, alg.distance(observationsPixels.get(i)), 1.5*0.5*0.5);
			a.p2.x -= 0.5;
			a.p3.x += 0.5;
			assertEquals(0, alg.distance(observationsPixels.get(i)), 1.5*0.5*0.5);
		}
	}

	@Test void noise_refine() {
		DistanceTrifocalReprojectionSq alg = new DistanceTrifocalReprojectionSq(1e-8, 100);
		alg.setModel(tensorPixels);

		for (int i = 0; i < observationsPixels.size(); i++) {
			AssociatedTriple a = observationsPixels.get(i);
			a.p1.x += 0.5;
			assertEquals(0, alg.distance(observationsPixels.get(i)), 0.5*0.5);
			a.p1.x -= 0.5;
			a.p2.x += 0.5;
			assertEquals(0, alg.distance(observationsPixels.get(i)), 0.5*0.5);
			a.p2.x -= 0.5;
			a.p3.x += 0.5;
			assertEquals(0, alg.distance(observationsPixels.get(i)), 0.5*0.5);
		}
	}
}
