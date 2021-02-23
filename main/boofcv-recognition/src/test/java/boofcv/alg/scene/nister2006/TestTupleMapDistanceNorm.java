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
import gnu.trove.map.TIntFloatMap;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestTupleMapDistanceNorm extends BoofStandardJUnit {

	/**
	 * Common function for computing most norms
	 */
	public static float computeError( TIntFloatMap descA, TIntFloatMap descB,
									  Operator2 op ) {
		float sum = 0.0f;

		for (int key : descA.keys()) {
			float valA = descA.get(key);
			float valB = descB.containsKey(key) ? descB.get(key) : 0.0f;
			sum += op.process(valA, valB);
		}

		for (int key : descB.keys()) {
			// see if it has already been processed
			if (descA.containsKey(key))
				continue;
			float valA = 0.0f;
			float valB = descB.get(key);
			sum += op.process(valA, valB);
		}

		return sum;
	}

	/**
	 * Common function for computing most norms
	 */
	public static float computeNorm( TIntFloatMap descA, Operator1 op, Operator1 post ) {
		float sum = 0.0f;

		for (int key : descA.keys()) {
			float valA = descA.get(key);
			sum += op.process(valA);
		}

		return post.process(sum);
	}

	@Nested
	public class L1 extends GenericTupleMapDistanceNormChecks {
		@Override public TupleMapDistanceNorm createAlg() {
			return new TupleMapDistanceNorm.L1();
		}

		@Override public float computeError( TIntFloatMap descA, TIntFloatMap descB ) {
			return TestTupleMapDistanceNorm.computeError(descA, descB, ( a, b ) -> Math.abs(a - b));
		}

		@Override public float computeNorm( TIntFloatMap desc ) {
			return TestTupleMapDistanceNorm.computeNorm(desc, Math::abs, ( a ) -> a);
		}
	}

	@Nested
	public class L2 extends GenericTupleMapDistanceNormChecks {
		@Override public TupleMapDistanceNorm createAlg() {
			return new TupleMapDistanceNorm.L2();
		}

		@Override public float computeError( TIntFloatMap descA, TIntFloatMap descB ) {
			return TestTupleMapDistanceNorm.computeError(descA, descB, ( a, b ) -> (a - b)*(a - b));
		}

		@Override public float computeNorm( TIntFloatMap desc ) {
			return TestTupleMapDistanceNorm.computeNorm(desc, ( a ) -> a*a, ( a ) -> (float)Math.sqrt(a));
		}
	}

	@FunctionalInterface interface Operator2 {
		float process( float valA, float valB );
	}

	@FunctionalInterface interface Operator1 {
		float process( float val );
	}
}
