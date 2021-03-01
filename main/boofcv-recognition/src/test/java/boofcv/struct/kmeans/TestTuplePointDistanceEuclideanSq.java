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
import boofcv.struct.feature.TupleDesc_S8;
import boofcv.struct.feature.TupleDesc_U8;
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
				desc.data[i] = rand.nextGaussian();
			}
			return desc;
		}

		@Override protected TupleDesc_F64 addToPoint( TupleDesc_F64 src, double magnitude ) {
			TupleDesc_F64 ret = src.copy();
			ret.data[0] += magnitude;
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
				desc.data[i] = (float)rand.nextGaussian();
			}
			return desc;
		}

		@Override protected TupleDesc_F32 addToPoint( TupleDesc_F32 src, double magnitude ) {
			TupleDesc_F32 ret = src.copy();
			ret.data[0] += (float)magnitude;
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

	@Nested class U8 extends GenericPointDistanceChecks<TupleDesc_U8> {
		@Override protected PointDistance<TupleDesc_U8> createAlg() {
			return new TuplePointDistanceEuclideanSq.U8();
		}

		@Override protected TupleDesc_U8 createRandomPoint() {
			var desc = new TupleDesc_U8(DOF);
			for (int i = 0; i < DOF; i++) {
				desc.data[i] = (byte)rand.nextInt(256);
			}
			return desc;
		}

		@Override protected TupleDesc_U8 addToPoint( TupleDesc_U8 src, double magnitude ) {
			TupleDesc_U8 ret = src.copy();
			for (int i = 0; i < DOF; i++) {
				ret.data[i] = (byte)((ret.data[i]&0xFF) + (int)magnitude);
			}
			return ret;
		}

		@Test void knownSolution() {
			PointDistance<TupleDesc_U8> alg = createAlg();
			var a = new TupleDesc_U8((byte)1,(byte)2,(byte)190);
			var b = new TupleDesc_U8((byte)4,(byte)1,(byte)200);

			float expected = 3*3 + 1 + 10*10;

			assertEquals(expected, alg.distance(a,b), UtilEjml.TEST_F32);
		}
	}

	@Nested class S8 extends GenericPointDistanceChecks<TupleDesc_S8> {
		@Override protected PointDistance<TupleDesc_S8> createAlg() {
			return new TuplePointDistanceEuclideanSq.S8();
		}

		@Override protected TupleDesc_S8 createRandomPoint() {
			var desc = new TupleDesc_S8(DOF);
			for (int i = 0; i < DOF; i++) {
				desc.data[i] = (byte)rand.nextInt(256);
			}
			return desc;
		}

		@Override protected TupleDesc_S8 addToPoint( TupleDesc_S8 src, double magnitude ) {
			TupleDesc_S8 ret = src.copy();
			for (int i = 0; i < DOF; i++) {
				ret.data[i] = (byte)(ret.data[i] + (int)magnitude);
			}
			return ret;
		}

		@Test void knownSolution() {
			PointDistance<TupleDesc_S8> alg = createAlg();
			var a = new TupleDesc_S8((byte)1,(byte)2,(byte)-2);
			var b = new TupleDesc_S8((byte)4,(byte)1,(byte)3);

			float expected = 3*3 + 1 + 5*5;

			assertEquals(expected, alg.distance(a,b), UtilEjml.TEST_F32);
		}
	}
}
