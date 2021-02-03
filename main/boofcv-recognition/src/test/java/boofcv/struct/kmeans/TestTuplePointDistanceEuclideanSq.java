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

import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.clustering.PointDistance;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTuplePointDistanceEuclideanSq extends BoofStandardJUnit {
	int DOF = 3;

	@Nested class F64 extends GenericPointDistanceChecks<TupleDesc_F64> {
		@Override protected PointDistance<TupleDesc_F64> createAlg() {
			return new TuplePointDistanceEuclideanSq.F64();
		}

		@Override protected TupleDesc_F64 createRandomPoint() {
			var desc = new TupleDesc_F64(DOF);
			for (int i = 0; i < DOF; i++) {
				desc.value[i] = rand.nextGaussian();
			}
			return desc;
		}

		@Override protected TupleDesc_F64 addToPoint( TupleDesc_F64 src, double magnitude ) {
			TupleDesc_F64 ret = src.copy();
			ret.value[0] += magnitude;
			return ret;
		}

		@Test void knownSolution() {
			PointDistance<TupleDesc_F64> alg = createAlg();
			TupleDesc_F64 a = new TupleDesc_F64(new double[]{1,2,3});
			TupleDesc_F64 b = new TupleDesc_F64(new double[]{4,1,-1});

			double expected = 3*3 + 1 + 4*4;

			assertEquals(expected, alg.distance(a,b), UtilEjml.TEST_F64);
		}
	}

	@Nested class F32 extends GenericPointDistanceChecks<TupleDesc_F32> {
		@Override protected PointDistance<TupleDesc_F32> createAlg() {
			return new TuplePointDistanceEuclideanSq.F32();
		}

		@Override protected TupleDesc_F32 createRandomPoint() {
			var desc = new TupleDesc_F32(DOF);
			for (int i = 0; i < DOF; i++) {
				desc.value[i] = (float)rand.nextGaussian();
			}
			return desc;
		}

		@Override protected TupleDesc_F32 addToPoint( TupleDesc_F32 src, double magnitude ) {
			TupleDesc_F32 ret = src.copy();
			ret.value[0] += (float)magnitude;
			return ret;
		}

		@Test void knownSolution() {
			PointDistance<TupleDesc_F32> alg = createAlg();
			var a = new TupleDesc_F32(1,2,3);
			var b = new TupleDesc_F32(4,1,-1);

			float expected = 3*3 + 1 + 4*4;

			assertEquals(expected, alg.distance(a,b), UtilEjml.TEST_F32);
		}
	}
}