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
import org.ddogleg.struct.DogArray_F32;

import java.util.List;

/**
 * Generalized way for normalizing and computing the distance between two sparse descriptors in a map
 * format. Intended for use with {@link RecognitionVocabularyTreeNister2006}. Uses efficient distance
 * formula from [1].
 *
 * <p>
 * [1] Nister, David, and Henrik Stewenius. "Scalable recognition with a vocabulary tree."
 * 2006 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'06). Vol. 2. Ieee, 2006.
 * </p>
 * @author Peter Abeles
 */
public interface TupleMapDistanceNorm {
	/**
	 * Normalizes the descriptor. Computes the norm then divides each element by the norm.
	 */
	void normalize( DogArray_F32 weights );

	/**
	 * Computes the distance between two vectors using common elements only. This only works for a subclass of
	 * normalizations. See [1] for details.
	 */
	float distance( List<CommonWords> common );

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

		public void setTo( int key, float valueA, float valueB ) {
			this.key = key;
			this.valueA = valueA;
			this.valueB = valueB;
		}
	}

	/**
	 * L1-norm for scoring
	 */
	class L1 implements TupleMapDistanceNorm {
		@Override public void normalize( DogArray_F32 weights ) {
			float norm = 0;
			for (int i = 0; i < weights.size; i++) {
				norm += weights.get(i);
			}
			BoofMiscOps.checkTrue(norm != 0.0f, "Norm value is zero. Something went very wrong");

			for (int i = 0; i < weights.size; i++) {
				weights.data[i] /= norm;
			}
		}

		@Override public float distance( List<CommonWords> common ) {
			float sum = 2.0f;

			for (int i = 0; i < common.size(); i++) {
				CommonWords c = common.get(i);
				sum += Math.abs(c.valueA - c.valueB) - c.valueA - c.valueB;
			}

			return sum;
		}

		@Override public TupleMapDistanceNorm newInstanceThread() {
			return new L1();
		}
	}

	/**
	 * L2-norm for scoring
	 */
	class L2 implements TupleMapDistanceNorm {
		@Override public void normalize( DogArray_F32 weights ) {
			float norm = 0;
			for (int i = 0; i < weights.size; i++) {
				float value = weights.data[i];
				norm += value*value;
			}
			norm = (float)Math.sqrt(norm);
			BoofMiscOps.checkTrue(norm != 0.0, "Sum of weights is zero. Something went very wrong");

			for (int i = 0; i < weights.size; i++) {
				weights.data[i]/=norm;
			}
		}

		@Override public float distance( List<CommonWords> common ) {
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
