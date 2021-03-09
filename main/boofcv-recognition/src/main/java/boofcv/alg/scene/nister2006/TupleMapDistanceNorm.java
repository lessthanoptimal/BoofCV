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

import boofcv.misc.BoofMiscOps;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.ddogleg.struct.DogArray_I32;

import java.util.List;

/**
 * Generalized way for normalizing and computing the distance between two sparse descriptors in a map
 * format. Intended for use with {@link RecognitionVocabularyTreeNister2006}.
 *
 * @author Peter Abeles
 */
public interface TupleMapDistanceNorm {
	/**
	 * Normalizes the descriptor. Computes the norm then divides each element by the norm.
	 */
	void normalize( TIntFloatMap descriptor );

	/**
	 * Computes the distance between these two descriptors. Default values for the map must be zero
	 */
	float distance( TIntFloatMap descA, TIntFloatMap descB );

	/**
	 * Computes the distance between these two descriptors given the common keys and values.
	 * This produces the same output as {@link #distance(TIntFloatMap, TIntFloatMap)}, but can
	 * potentially be substantially faster since common elements are known.
	 */
	float distance( TIntFloatMap descA, TIntFloatMap descB, List<CommonWords> common );

	/** Create a new instance that is thread safe, i.e. read only settings can be shared */
	TupleMapDistanceNorm newInstanceThread();

	/** Distance functions that are supported */
	enum Types {L1, L2}

	/**
	 * Set of words which are common between the two maps
	 */
	class CommonWords {
		// The key which is common
		public int key;
		// value in descriptor A
		public float valueA;
		// value in descriptor B
		public float valueB;

		public CommonWords() {}

		public CommonWords( int key, float valueA, float valueB ) {
			setTo(key, valueA, valueB);
		}

		public void setTo( int key, float valueA, float valueB) {
			this.key = key;
			this.valueA = valueA;
			this.valueB = valueB;
		}
	}

	/**
	 * Base class for norms which need to know which keys are common between the two
	 */
	abstract class CommonKeys implements TupleMapDistanceNorm {
		// Temporary storage for keys in a map
		final DogArray_I32 keys = new DogArray_I32();
		// Storage for keys which are common between the two descriptors
		final TIntSet commonKeys = new TIntHashSet();

		/**
		 * Finds common keys between the two descriptors
		 */
		protected void findCommonKeys( TIntFloatMap descA, TIntFloatMap descB, TIntSet commonKeys) {
			commonKeys.clear();

			// Add keys from descA
			keys.resize(descA.size());
			descA.keys(keys.data);
			for (int i = 0; i < keys.size; i++) {
				commonKeys.add(keys.get(i));
			}

			// Add keys from descB
			keys.resize(descB.size());
			descB.keys(keys.data);
			for (int i = 0; i < keys.size; i++) {
				commonKeys.add(keys.get(i));
			}
		}
	}

	class L1 extends CommonKeys {
		@Override public void normalize( TIntFloatMap descriptor ) {
			keys.resize(descriptor.size());
			descriptor.keys(keys.data);
			float norm = 0;
			for (int i = 0; i < keys.size; i++) {
				norm += descriptor.get(keys.data[i]);
			}
			BoofMiscOps.checkTrue(norm != 0.0f, "Norm value is zero. Something went very wrong");

			float _norm = norm;
			descriptor.transformValues((v)->v/_norm);
		}

		@Override public float distance( TIntFloatMap descA, TIntFloatMap descB ) {
			// Find the set of common keys between the two descriptors
			findCommonKeys(descA, descB, commonKeys);

			// Look up the common keys
			keys.resize(commonKeys.size());
			commonKeys.toArray(keys.data);

			// L1-norm is the sum of the difference magnitude of each element
			float sum = 0.0f;
			for (int keyIdx = 0; keyIdx < keys.size; keyIdx++) {
				int key = keys.data[keyIdx];

				// takes advantage of default value being 0.0f
				float valueA = descA.get(key);
				float valueB = descB.get(key);
				sum += Math.abs(valueA - valueB);
			}

			return sum;
		}

		@Override public float distance( TIntFloatMap descA, TIntFloatMap descB, List<CommonWords> common ) {
			// TODO optimize this
			return distance(descA, descB);
		}

		@Override public TupleMapDistanceNorm newInstanceThread() {
			return new L1();
		}
	}

	class L2 implements TupleMapDistanceNorm {
		final DogArray_I32 keys = new DogArray_I32();

		@Override public void normalize( TIntFloatMap descriptor ) {
			keys.resize(descriptor.size());
			descriptor.keys(keys.data);
			float norm = 0;
			for (int i = 0; i < keys.size; i++) {
				float value = descriptor.get(keys.data[i]);
				norm += value*value;
			}
			norm = (float)Math.sqrt(norm);
			BoofMiscOps.checkTrue(norm != 0.0, "Sum of weights is zero. Something went very wrong");

			float _norm = norm;
			descriptor.transformValues((v)->v/_norm);
		}

		@Override public float distance( TIntFloatMap descA, TIntFloatMap descB ) {
			keys.resize(descA.size());
			descA.keys(keys.data);

			float sum = 0.0f;

			for (int i = 0; i < keys.size(); i++) {
				int key = keys.get(i);
				float valueA = descA.get(key);
				float valueB = descB.get(key);
				sum += valueA*valueB;
			}

			return 2.0f*(1.0f - sum);
		}

		@Override public float distance( TIntFloatMap descA, TIntFloatMap descB, List<CommonWords> common ) {
			float sum = 0.0f;

			for (int i = 0; i < common.size(); i++) {
				CommonWords c = common.get(i);
				sum += c.valueA*c.valueB;
			}

			return 2.0f*(1.0f - sum);
		}

		@Override public TupleMapDistanceNorm newInstanceThread() {
			return new L2();
		}
	}
}
