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

package boofcv.struct.kmeans;

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.clustering.PointDistance;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generic unit tests for implementations of {@link PointDistance}.
 *
 * @author Peter Abeles
 */
public abstract class GenericPointDistanceChecks<T> extends BoofStandardJUnit {

	/** Creates a new instance of the algorithm being tested */
	protected abstract PointDistance<T> createAlg();

	/** Creates a random point */
	protected abstract T createRandomPoint();

	/** Creates a new point which is this distance from the original point */
	protected abstract T addToPoint( T src, double magnitude );

	/** Sanity check situations where the point should have zero distance from another */
	@Test void zeroDistance() {
		PointDistance<T> alg = createAlg();

		// Same instance
		T a = createRandomPoint();
		assertEquals(0.0, alg.distance(a, a), UtilEjml.TEST_F64);

		// Two different but identical points
		T b = addToPoint(a, 0.0);
		assertEquals(0.0, alg.distance(a, b), UtilEjml.TEST_F64);
	}

	/** The point that's farther away should be farther away */
	@Test void distanceIncrease() {
		PointDistance<T> alg = createAlg();

		T a = createRandomPoint();
		T b = addToPoint(a, 0.5);
		T c = addToPoint(a, 1.0);

		assertTrue(alg.distance(a, b) < alg.distance(b, c));
	}

	/** This isn't much of a test. Just sees if the new instance works */
	@Test void newInstanceThread() {
		PointDistance<T> alg1 = createAlg();
		PointDistance<T> alg2 = alg1.newInstanceThread();

		for (int i = 0; i < 20; i++) {
			T a = createRandomPoint();
			T b = createRandomPoint();

			double dist1 = alg1.distance(a, b);
			double dist2 = alg2.distance(a, b);

			assertEquals(dist1, dist2);
		}
	}
}
