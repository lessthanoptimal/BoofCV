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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Common tests for implementations of {@link PackedArray}.
 */
public abstract class GenericPackedArrayChecks<T> extends BoofStandardJUnit {
	/** Creates a new instance of the algorithm being tested */
	protected abstract PackedArray<T> createAlg();

	/** Creates a random point */
	protected abstract T createRandomPoint();

	/** Checks to see if the two elements are identical */
	protected abstract void checkEquals( T a, T b );

	protected abstract void checkNotEquals( T a, T b );

	@Test void reset() {
		PackedArray<T> alg = createAlg();

		assertEquals(0, alg.size());
		for (int i = 0; i < 3; i++) {
			alg.addCopy(createRandomPoint());
		}
		assertEquals(3, alg.size());

		alg.reset();
		assertEquals(0, alg.size());
	}

	@Test void reserve() {
		PackedArray<T> alg = createAlg();
		alg.reserve(12);
		assertEquals(0, alg.size());

		for (int i = 0; i < 3; i++) {
			alg.addCopy(createRandomPoint());
		}
		assertEquals(3, alg.size());
	}

	@Test void addCopy_getCopy() {
		PackedArray<T> alg = createAlg();

		// Create two different points
		T a = createRandomPoint();
		T b = createRandomPoint();

		// Add the first point
		assertEquals(0, alg.size());
		alg.addCopy(a);
		assertEquals(1, alg.size());

		// Copy the first point into the second
		checkNotEquals(a, b);
		alg.getCopy(0, b);
		checkEquals(a, b);
	}

	@Test void addCopy_getCopy_multiple() {
		PackedArray<T> alg = createAlg();
		List<T> expected = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			T a = createRandomPoint();
			expected.add(a);
			alg.addCopy(a);
		}

		assertEquals(5, alg.size());

		T b = createRandomPoint();
		for (int i = 0; i < 5; i++) {
			alg.getCopy(i, b);
			checkEquals(expected.get(i), b);
		}
	}

	@Test void getTemp() {
		PackedArray<T> alg = createAlg();

		T a = createRandomPoint();

		alg.addCopy(a);
		// The returned item should not be the same instance
		assertNotSame(a, alg.getTemp(0));

		// They should have an identical value
		checkEquals(a, alg.getTemp(0));
	}

	@Test void copy() {
		T a = createRandomPoint();
		T b = createRandomPoint();

		PackedArray<T> alg = createAlg();
		alg.copy(a, b);
		checkEquals(a, b);
	}
}
