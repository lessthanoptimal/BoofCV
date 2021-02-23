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

package boofcv.alg.scene.nister2006;

import boofcv.alg.scene.nister2006.TupleMapDistanceNorm.CommonWords;
import boofcv.testing.BoofStandardJUnit;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common tests for TupleMapDistanceNorm
 *
 * @author Peter Abeles
 */
abstract class GenericTupleMapDistanceNormChecks extends BoofStandardJUnit {

	public abstract TupleMapDistanceNorm createAlg();

	/** Compute the error using a brute force approach that's easy to visually verify */
	public abstract float computeError( TIntFloatMap descA, TIntFloatMap descB );

	/** Compute the norm using a brute force approach that's easy to visually verify */
	public abstract float computeNorm( TIntFloatMap desc );

	/**
	 * Compare the normalization to the same method computed other ways
	 */
	@Test void normalize_CompareToManual() {
		TupleMapDistanceNorm alg = createAlg();

		for (int i = 0; i < 20; i++) {
			TIntFloatMap desc = new TIntFloatHashMap();
			TIntFloatMap copy = new TIntFloatHashMap();

			for (int j = 0; j < 5; j++) {
				int key = rand.nextInt(10);
				float value = rand.nextFloat();
				desc.put(key, value);
				copy.put(key, value);
			}

			float norm = computeNorm(desc);

			alg.normalize(desc);

			for (int keys : desc.keys()) {
				assertEquals(copy.get(keys)/norm, desc.get(keys), UtilEjml.TEST_F32);
			}
		}
	}

	/**
	 * Compare the normalization to the same method computed other ways
	 */
	@Test void distance_CompareToManual() {
		TupleMapDistanceNorm alg = createAlg();

		for (int i = 0; i < 20; i++) {
			TIntFloatMap descA = new TIntFloatHashMap();
			TIntFloatMap descB = new TIntFloatHashMap();

			int uniqueA = rand.nextInt(3);
			int uniqueB = rand.nextInt(3);

			// these might be unique
			for (int idx = 0; idx < uniqueA; idx++) {
				descA.put(rand.nextInt(10), rand.nextFloat());
			}
			for (int idx = 0; idx < uniqueB; idx++) {
				descB.put(rand.nextInt(10), rand.nextFloat());
			}

			// these will be common
			for (int commonI = 0; commonI < 5; commonI++) {
				descA.put(rand.nextInt(10), rand.nextFloat());
				descB.put(rand.nextInt(10), rand.nextFloat());
			}

			// Need to normalize for distance functions to work correctly
			alg.normalize(descA);
			alg.normalize(descB);

			assertEquals(computeError(descA, descB), alg.distance(descA, descB), UtilEjml.TEST_F32);
		}
	}

	/**
	 * The two descriptors have no non-zero elements in common
	 */
	@Test void distance_NoOverLap() {
		TupleMapDistanceNorm alg = createAlg();

		TIntFloatMap descA = new TIntFloatHashMap();
		TIntFloatMap descB = new TIntFloatHashMap();

		descA.put(1, 1.0f);
		descA.put(5, 2.0f);
		descA.put(10, 1.0f);

		descB.put(0, 1.0f);
		descB.put(7, 2.0f);
		descB.put(123, 1.0f);

		// Need to normalize for distance functions to work correctly
		alg.normalize(descA);
		alg.normalize(descB);

		assertEquals(computeError(descA, descB), alg.distance(descA, descB), UtilEjml.TEST_F32);
		assertEquals(computeError(descA, descB), alg.distance(descA, descB, new ArrayList<>()), UtilEjml.TEST_F32);
	}

	/**
	 * Makes sure the two distance functions return the same result
	 */
	@Test void distance_SameAnswer() {
		TupleMapDistanceNorm alg = createAlg();

		TIntFloatMap descA = new TIntFloatHashMap();
		TIntFloatMap descB = new TIntFloatHashMap();

		descA.put(1, 1.0f);
		descA.put(5, 2.0f);
		descA.put(10, 1.0f);
		descA.put(72, 0.9f);

		descB.put(1, 0.5f);
		descB.put(2, 0.9f);
		descB.put(5, 0.01f);
		descB.put(10, 0.9f);

		// Need to normalize for distance functions to work correctly
		alg.normalize(descA);
		alg.normalize(descB);

		var common = new ArrayList<CommonWords>();
		common.add(new CommonWords(1, descA.get(1), descB.get(1)));
		common.add(new CommonWords(5, descA.get(5), descB.get(5)));
		common.add(new CommonWords(10, descA.get(10), descB.get(10)));

		assertEquals(alg.distance(descA, descB, common),
				alg.distance(descA, descB), UtilEjml.TEST_F32);
	}

	/**
	 * Basic check to makes sure the returned function produces
	 * the same results as the original
	 */
	@Test void newInstanceThread_SameAnswer() {
		TupleMapDistanceNorm alg1 = createAlg();
		TupleMapDistanceNorm alg2 = alg1.newInstanceThread();

		TIntFloatMap descA = new TIntFloatHashMap();
		TIntFloatMap descB = new TIntFloatHashMap();

		descA.put(1, 1.0f);
		descA.put(5, 2.0f);
		descA.put(10, 1.0f);

		descB.put(0, 1.0f);
		descB.put(7, 2.0f);
		descB.put(123, 1.0f);

		// Need to normalize for distance functions to work correctly
		alg1.normalize(descA);
		alg1.normalize(descB);

		assertEquals(alg1.distance(descA, descB), alg2.distance(descA, descB), UtilEjml.TEST_F32);
	}
}
