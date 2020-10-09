/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe.llah;

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit5 blows up if these functions are in the same class as the nested class tests
 *
 * @author Peter Abeles
 */
abstract class GenericLlahHasherChecks extends BoofStandardJUnit {

	protected abstract LlahHasher create( int hashK, int hashSize );

	@Test
	void getInvariantSampleSize() {
		LlahHasher alg = create(5, 200);
		assertTrue(alg.getInvariantSampleSize() > 3);
	}

	@Test
	void getNumberOfInvariants() {
		LlahHasher alg = create(5, 200);
		int N = alg.getInvariantSampleSize();

		assertEquals(1, alg.getNumberOfInvariants(N));
		assertEquals(N + 1, alg.getNumberOfInvariants(N + 1));
	}

	@Test
	void computeHash() {
		LlahHasher alg = create(5, 200);
		int sampleSize = alg.getInvariantSampleSize();
		List<Point2D_F64> points = UtilPoint2D_F64.random(-5, 5, sampleSize, rand);

		// Test trivial case with just one invariant in the feature
		alg.setSamples(createSamples());
		var feature = new LlahFeature(1);
		alg.computeHash(points, feature);
		checkFeature(feature);

		// there should be more than one invariant now
		points = UtilPoint2D_F64.random(-5, 5, sampleSize + 3, rand);
		feature = new LlahFeature(alg.getNumberOfInvariants(points.size()));
		alg.computeHash(points, feature);
		checkFeature(feature);
	}

	private double[] createSamples() {
		double[] samples = new double[8];

		samples[0] = 0.05;
		for (int i = 1; i < samples.length; i++) {
			samples[i] = samples[i - 1] + rand.nextDouble();
		}
		return samples;
	}

	private void checkFeature( LlahFeature feature ) {
		assertTrue(feature.hashCode != 0);
		assertNull(feature.next);
		int totalNotZero = 0;
		for (int i = 0; i < feature.invariants.length; i++) {
			totalNotZero += feature.invariants[i] != 0 ? 1 : 0;
		}
		assertTrue(totalNotZero > feature.invariants.length*0.80);
	}

	@Test
	void discretize() {
		LlahHasher alg = create(5, 200);
		alg.samples = new double[]{0.2, 0.9, 5.0, 5.2};

		assertEquals(0, alg.discretize(0));
		assertEquals(0, alg.discretize(0.1));
		assertEquals(0, alg.discretize(0.2));
		assertEquals(1, alg.discretize(0.20001));
		assertEquals(1, alg.discretize(0.899));
		assertEquals(1, alg.discretize(0.9));
		assertEquals(2, alg.discretize(0.90001));
		assertEquals(2, alg.discretize(4.5));
		assertEquals(2, alg.discretize(5.0));
		assertEquals(3, alg.discretize(5.1));
		assertEquals(3, alg.discretize(5.2));
		assertEquals(4, alg.discretize(5.20001));
		assertEquals(4, alg.discretize(100.0));
	}

	@Test
	void learnDiscretization() {
		double maxValue = 25.0;
		var histogram = new int[1000];

		// high density region
		for (int i = 0; i < 50; i++) {
			histogram[i + 2] = 12;
		}

		// low density region
		for (int i = 100; i < histogram.length; i += 2) {
			histogram[i] = 1;
		}

		histogram[histogram.length - 1] = 20;

		LlahHasher alg = create(1, 1);

		// this total number of discrete values is larger than the actual number of unique values
		// and the look up table size is over kill. This should yield perfect results
		alg.learnDiscretization(histogram, histogram.length, maxValue, 150);
		check(alg, maxValue, 150);
	}

	private void check( LlahHasher alg, double maxValue, int numDiscrete ) {
		int[] counts = new int[numDiscrete];
		for (int i = 0; i < 1000; i++) {
			double value = maxValue*i/1000.0;
			int d = alg.discretize(value);
			counts[d]++;
		}
		int totalNotZero = 0;
		for (int i = 0; i < numDiscrete; i++) {
			if (counts[i] != 0)
				totalNotZero++;
		}
		assertTrue(totalNotZero > 15);

		// test edge cases
		assertEquals(0, alg.discretize(0));
		assertEquals(numDiscrete - 1, alg.discretize(maxValue*1.2));
	}
}
