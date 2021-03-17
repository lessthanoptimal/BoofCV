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

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_F32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common tests for TupleMapDistanceNorm
 *
 * @author Peter Abeles
 */
abstract class GenericTupleMapDistanceNormChecks extends BoofStandardJUnit {

	public abstract TupleMapDistanceNorm createAlg();

	/** Compute the error using a brute force approach that's easy to visually verify */
	public abstract float computeError( DogArray_F32 descA, DogArray_F32 descB );

	/** Compute the norm using a brute force approach that's easy to visually verify */
	public abstract float computeNorm( DogArray_F32 desc );

	/**
	 * Compare the normalization to the same method computed other ways
	 */
	@Test void normalize_CompareToManual() {
		TupleMapDistanceNorm alg = createAlg();

		for (int i = 0; i < 20; i++) {
			DogArray_F32 weights = new DogArray_F32();
			DogArray_F32 copy = new DogArray_F32();

			for (int j = 0; j < 5; j++) {
				float value = rand.nextFloat();
				weights.add(value);
				copy.add(value);
			}

			float norm = computeNorm(weights);

			alg.normalize(weights);

			for (int weightIdx = 0; weightIdx < weights.size; weightIdx++) {
				assertEquals(copy.get(weightIdx)/norm, weights.get(weightIdx), UtilEjml.TEST_F32);
			}
		}
	}

	/**
	 * Compare the normalization to the same method computed other ways
	 */
	@Test void distanceUpdate_CompareToManual() {
		TupleMapDistanceNorm alg = createAlg();

		for (int i = 0; i < 20; i++) {
			DogArray_F32 descA = new DogArray_F32();
			DogArray_F32 descB = new DogArray_F32();
			descA.resize(10);
			descB.resize(10);

			int uniqueA = rand.nextInt(3);
			int uniqueB = rand.nextInt(3);

			// these might be unique
			for (int idx = 0; idx < uniqueA; idx++) {
				descA.set(rand.nextInt(10), rand.nextFloat());
			}
			for (int idx = 0; idx < uniqueB; idx++) {
				descB.set(rand.nextInt(10), rand.nextFloat());
			}

			// these will be common
			for (int commonI = 0; commonI < 5; commonI++) {
				int index = rand.nextInt(10);
				descA.set(index, rand.nextFloat());
				descB.set(index, rand.nextFloat());
			}

			// Need to normalize for distance functions to work correctly
			alg.normalize(descA);
			alg.normalize(descB);

			float found = 2.0f;
			for (int j = 0; j < 10; j++) {
				if (descA.get(j) != 0 && descB.get(j) != 0)
					found += alg.distanceUpdate(descA.get(j), descB.get(j));
			}

			assertEquals(computeError(descA, descB), found, UtilEjml.TEST_F32);
		}
	}

	/**
	 * Basic check to makes sure the returned function produces
	 * the same results as the original
	 */
	@Test void newInstanceThread_SameAnswer() {
		TupleMapDistanceNorm alg1 = createAlg();
		TupleMapDistanceNorm alg2 = alg1.newInstanceThread();

		DogArray_F32 descA = new DogArray_F32();
		DogArray_F32 descB = new DogArray_F32();

		for (int j = 0; j < 20; j++) {
			float value = rand.nextFloat();
			descA.add(value);
			descB.add(value);
		}

		// Need to normalize for distance functions to work correctly
		alg1.normalize(descA);
		alg2.normalize(descB);

		for (int i = 0; i < descA.size; i++) {
			assertEquals(descA.get(i), descB.get(i), UtilEjml.TEST_F32);
		}
	}
}
