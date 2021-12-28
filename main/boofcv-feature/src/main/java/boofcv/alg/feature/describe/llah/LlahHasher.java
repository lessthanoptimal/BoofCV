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

package boofcv.alg.feature.describe.llah;

import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.combinatorics.Combinations;
import org.ddogleg.util.PrimitiveArrays;

import java.util.List;

/**
 * Functions related to computing the hash values of a LLAH feature. This is done by looking at the invariant
 * geometry between points and computing a hash function from their values.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class LlahHasher {
	/**
	 * Recommended K from the paper
	 */
	public static int DEFAULT_HASH_K = 25;
	/**
	 * The recommended hash size from the paper
	 */
	public static int DEFAULT_HASH_SIZE = 12_800_000;

	/**
	 * Defines the look up table. A binary search is used to effectively find the index of a value
	 */
	@Setter @Getter protected double[] samples;

	/**
	 * k^i in the hash function
	 */
	private final long hashK;
	/**
	 * The maximum value of the hash code
	 */
	private final int hashSize;

	// Used to compute all the combinations of a set
	private final Combinations<Point2D_F64> combinator = new Combinations<>();

	/**
	 * Configures the hash function. See JavaDoc for info on variables
	 */
	protected LlahHasher( long hashK, int hashSize ) {
		this.hashK = hashK;
		this.hashSize = hashSize;
	}

	/**
	 * Returns the number of invariants given the number of points.
	 *
	 * @param numPoints Number of points the hash function is computed from
	 * @return Number of invariants the feature will have
	 */
	public int getNumberOfInvariants( int numPoints ) {
		return (int)Combinations.computeTotalCombinations(numPoints, getInvariantSampleSize());
	}

	/**
	 * Computes the hashcode and invariant values. Stores result in output
	 *
	 * @param points Set of points. Must be &ge; 4.
	 */
	public void computeHash( List<Point2D_F64> points, LlahFeature output ) {
		int N = getInvariantSampleSize();
		if (points.size() < N)
			throw new IllegalArgumentException("Must be at least 5 points and not " + points.size());
		combinator.init(points, N);
		long hash = 0;
		int i = 0;
		long k = 1;
		do {
			double invariant = computeInvariant(combinator);
			int r = output.invariants[i++] = discretize(invariant);
			hash += r*k;
			k *= hashK;
		} while (combinator.next());

		output.hashCode = (int)(hash%hashSize);
	}

	/**
	 * Stores the computed invariants into an array
	 */
	public void computeInvariants( List<Point2D_F64> points, double[] invariants, int offset ) {
		int N = getInvariantSampleSize();
		combinator.init(points, N);
		int i = 0;
		do {
			invariants[offset + i++] = computeInvariant(combinator);
		} while (combinator.next());
	}

	/**
	 * Number of points required to compute the invariants
	 */
	protected abstract int getInvariantSampleSize();

	/**
	 * Computes the invariants given the set of points
	 */
	protected abstract double computeInvariant( Combinations<Point2D_F64> combinator );

	/**
	 * Computes the discrete value from the continuous valued invariant
	 */
	public int discretize( double invariant ) {
		return PrimitiveArrays.lowerBound(samples, 0, samples.length, invariant);
	}

	/**
	 * Create a lookup table by sorting then sampling the invariants. This will have the desired property of
	 * having a denser set of points where there is a higher density of values. A histogram is required instead
	 * of raw values because it becomes intractable quickly for even only a few documents if it's an array..
	 *
	 * @param histogram Histogram of invariant values from 0 to maxValue
	 * @param histLength Histogram length.
	 * @param histMaxValue The maximum value in the histogram
	 * @param numDiscrete Number of possible discrete values. Larger values indicate higher resolution in discretation
	 */
	public void learnDiscretization( int[] histogram, int histLength, double histMaxValue, int numDiscrete ) {
		this.samples = new double[numDiscrete - 1];

		// Number of hits in the histogram
		int total = 0;
		for (int i = 0; i < histLength; i++) {
			total += histogram[i];
		}

		// samples is designed so that any value form 0.0 to samples[0] will have a value of 0
		// then any value greater than samples[N-1] wil have a value of N
		int locHist = 0;
		for (int i = 1, j = 0; i < numDiscrete; i++) {
			int target = (total - 1)*i/numDiscrete;

			while (locHist < target) {
				locHist += histogram[j++];
			}
			samples[i - 1] = j*histMaxValue/histLength;
		}
	}

	/**
	 * Returns the number of possible values
	 */
	public int getNumValues() {
		return samples.length;
	}

	public static class Affine extends LlahHasher {

		public Affine( long hashK, int hashSize ) {
			super(hashK, hashSize);
			// computed from random data
			samples = new double[]{
					0.044, 0.0876, 0.1334, 0.1813, 0.2332,
					0.2885, 0.3465, 0.4099, 0.4779, 0.5522,
					0.6353, 0.7279, 0.8316, 0.9477, 1.0751,
					1.223, 1.3926, 1.5891, 1.8183, 2.0855,
					2.4067, 2.8084, 3.3036, 3.9727, 4.9149,
					6.2906, 8.5293, 13.0366, 25.6325};
		}

		@Override
		protected int getInvariantSampleSize() {
			return 4;
		}

		@Override
		protected double computeInvariant( Combinations<Point2D_F64> combinator ) {
			Point2D_F64 p1 = combinator.get(0);
			Point2D_F64 p2 = combinator.get(1);
			Point2D_F64 p3 = combinator.get(2);
			Point2D_F64 p4 = combinator.get(3);
			return PerspectiveOps.invariantAffine(p1, p2, p3, p4);
		}
	}

	public static class CrossRatio extends LlahHasher {

		public CrossRatio( long hashK, int hashSize ) {
			super(hashK, hashSize);
			// computed from random data
			samples = new double[]{
					0.01434, 0.03408, 0.05712, 0.08384, 0.11374,
					0.14714, 0.18358, 0.2239, 0.26832, 0.31724,
					0.37016, 0.4261, 0.48634, 0.5486, 0.6153,
					0.68642, 0.75742, 0.8274, 0.90072, 0.96994,
					1.0305, 1.12668, 1.26924, 1.48098, 1.80568,
					2.35426, 3.29888, 5.3724, 12.1995};
		}

		@Override
		protected int getInvariantSampleSize() {
			return 5;
		}

		@Override
		protected double computeInvariant( Combinations<Point2D_F64> combinator ) {
			Point2D_F64 p1 = combinator.get(0);
			Point2D_F64 p2 = combinator.get(1);
			Point2D_F64 p3 = combinator.get(2);
			Point2D_F64 p4 = combinator.get(3);
			Point2D_F64 p5 = combinator.get(4);
			return PerspectiveOps.invariantCrossRatio(p1, p2, p3, p4, p5);
		}
	}
}
