/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.descriptor;

import boofcv.struct.feature.*;

/**
 * Series of simple functions for computing difference distance measures between two descriptors.
 *
 * @author Peter Abeles
 */
public class DescriptorDistance {

	/**
	 * Returns the Euclidean distance (L2-norm) between the two descriptors.
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return Euclidean distance
	 */
	public static double euclidean(TupleDesc_F64 a, TupleDesc_F64 b) {
		final int N = a.value.length;
		double total = 0;
		for( int i = 0; i < N; i++ ) {
			double d = a.value[i]-b.value[i];
			total += d*d;
		}

		return Math.sqrt(total);
	}

	/**
	 * Returns the Euclidean distance squared between the two descriptors.
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return Euclidean distance squared
	 */
	public static double euclideanSq(TupleDesc_F64 a, TupleDesc_F64 b) {
		final int N = a.value.length;
		double total = 0;
		for( int i = 0; i < N; i++ ) {
			double d = a.value[i]-b.value[i];
			total += d*d;
		}

		return total;
	}

	/**
	 * Returns the Euclidean distance squared between the two descriptors.
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return Euclidean distance squared
	 */
	public static double euclideanSq(TupleDesc_F32 a, TupleDesc_F32 b) {
		final int N = a.value.length;
		float total = 0;
		for( int i = 0; i < N; i++ ) {
			double d = a.value[i]-b.value[i];
			total += d*d;
		}

		return total;
	}

	/**
	 * Correlation score
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return Correlation score
	 */
	public static double correlation( TupleDesc_F64 a, TupleDesc_F64 b) {
		final int N = a.value.length;
		double total = 0;
		for( int i = 0; i < N; i++ ) {
			total += a.value[i]*b.value[i];
		}

		return total;
	}

	/**
	 * <p>
	 * Normalized cross correlation (NCC) computed using a faster technique.<br>
	 * <br>
	 * NCC = sum(a[i]*b[i]) / (N*sigma_a * sigma_b)<br>
	 * where a[i] = I[i]-mean(a), I[i] is the image pixel intensity around the feature, and N is the number of
	 * elements.
	 * </p>
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return NCC score
	 */
	public static double ncc(NccFeature a, NccFeature b) {
		double top = 0;

		final int N = a.value.length;
		for( int i = 0; i < N; i++ ) {
			top += a.value[i]*b.value[i];
		}

		return top/(N*a.sigma * b.sigma);
	}

	/**
	 * Sum of absolute difference (SAD) score
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return SAD score
	 */
	public static int sad(TupleDesc_U8 a, TupleDesc_U8 b) {

		int total = 0;
		for( int i = 0; i < a.value.length; i++ ) {
			total += Math.abs( (a.value[i] & 0xFF) - (b.value[i] & 0xFF));
		}
		return total;
	}

	/**
	 * Sum of absolute difference (SAD) score
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return SAD score
	 */
	public static int sad(TupleDesc_S8 a, TupleDesc_S8 b) {

		int total = 0;
		for( int i = 0; i < a.value.length; i++ ) {
			total += Math.abs( a.value[i] - b.value[i]);
		}
		return total;
	}

	/**
	 * Sum of absolute difference (SAD) score
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return SAD score
	 */
	public static float sad(TupleDesc_F32 a, TupleDesc_F32 b) {

		float total = 0;
		for( int i = 0; i < a.value.length; i++ ) {
			total += Math.abs( a.value[i] - b.value[i]);
		}
		return total;
	}

	/**
	 * Sum of absolute difference (SAD) score
	 *
	 * @param a First descriptor
	 * @param b Second descriptor
	 * @return SAD score
	 */
	public static double sad(TupleDesc_F64 a, TupleDesc_F64 b) {

		double total = 0;
		for( int i = 0; i < a.value.length; i++ ) {
			total += Math.abs( a.value[i] - b.value[i]);
		}
		return total;
	}

	/**
	 * Computes the hamming distance between two binary feature descriptors
	 *
	 * @param a First variable
	 * @param b Second variable
	 * @return The hamming distance
	 */
	public static int hamming( TupleDesc_B a, TupleDesc_B b ) {
		int score = 0;
		final int N = a.data.length;
		for( int i = 0; i < N; i++ ) {
			score += hamming(a.data[i] ^ b.data[i]);
		}
		return score;
	}

	/**
	 * <p>Computes the hamming distance.  A bit = 0 is a match and 1 is not match<p>
	 *
	 * Based on code snippet from <a href="http://graphics.stanford.edu/~seander/bithacks.html">Sean Eron Anderson Bit Twiddling Hacks</a>.
	 *
	 * @param val Hamming encoding
	 * @return The hamming distance
	 */
	public static int hamming( int val ) {
		int c;
		int v = val;
		v = v - ((v >> 1) & 0x55555555);
		v = (v & 0x33333333) + ((v >> 2) & 0x33333333);
		c = ((v + (v >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
		return c;
	}
}
