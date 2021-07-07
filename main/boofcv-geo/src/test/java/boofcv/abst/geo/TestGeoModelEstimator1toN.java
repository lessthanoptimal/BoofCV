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

package boofcv.abst.geo;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.QueueMatrix;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestGeoModelEstimator1toN extends BoofStandardJUnit {

	List<AssociatedPair> points = new ArrayList<>();
	DogArray<DMatrixRMaj> solutions = new QueueMatrix(3, 3);

	@Test void basicTest() {
		GeoModelEstimator1toN<DMatrixRMaj,AssociatedPair> alg =
				new GeoModelEstimator1toN<>(new Dummy(true));

		assertTrue(alg.process(points,solutions));

		assertEquals(1, solutions.size());

		alg = new GeoModelEstimator1toN<>(new Dummy(false));

		assertFalse(alg.process(points,solutions));
		assertEquals(0,solutions.size);
	}

	/**
	 * Makes sure everything is reset properly on multiple calls
	 */
	@Test void multipleCalls() {
		GeoModelEstimator1toN<DMatrixRMaj,AssociatedPair> alg =
				new GeoModelEstimator1toN<>(new Dummy(true));

		assertTrue(alg.process(points,solutions));
		assertEquals(1, solutions.size());
		assertTrue(alg.process(points,solutions));
		assertEquals(1, solutions.size());

		alg = new GeoModelEstimator1toN<>(new Dummy(false));
		assertFalse(alg.process(points,solutions));
		assertFalse(alg.process(points,solutions));
	}

	private static class Dummy implements GeoModelEstimator1<DMatrixRMaj,AssociatedPair> {
		boolean hasSolution;

		private Dummy(boolean hasSolution) {
			this.hasSolution = hasSolution;
		}

		@Override
		public boolean process(List<AssociatedPair> points, DMatrixRMaj solution ) {
			return hasSolution;
		}


		@Override
		public int getMinimumPoints() {
			return 3;
		}
	}

}
