/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.packed;

import boofcv.struct.PackedArray;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
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
			alg.append(createRandomPoint());
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
			alg.append(createRandomPoint());
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
		alg.append(a);
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
			alg.append(a);
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

		alg.append(a);
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

	@Test void forIdx() {
		PackedArray<T> array = createAlg();
		for (int i = 0; i < 26; i++) {
			array.append(createRandomPoint());
		}

		List<T> found = new ArrayList<>();
		DogArray_I32 indexes = new DogArray_I32();

		array.forIdx(2,array.size()-1, (idx, point)->{
			T copy = createRandomPoint();
			array.copy(point, copy);
			found.add(copy);
			indexes.add(idx);
		});

		assertEquals(23, indexes.size);
		for (int i = 0; i < indexes.size; i++) {
			assertEquals(i+2, indexes.get(i));
			checkEquals(array.getTemp(i+2), found.get(i));
		}
	}

	@Test void set_index() {
		PackedArray<T> alg = createAlg();

		alg.append(createRandomPoint());
		alg.append(createRandomPoint());
		alg.append(createRandomPoint());

		var p = createRandomPoint();
		alg.set(1, p);
		T found = alg.getTemp(1);
		checkEquals(p, found);
	}
}
