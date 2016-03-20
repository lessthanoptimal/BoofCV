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

import boofcv.struct.feature.TupleDesc_B;

/**
 * @author Peter Abeles
 */
public class ExperimentalDescriptorDistance {
	/**
	 * Computes the hamming distance between two binary feature descriptors
	 *
	 * @param a First variable
	 * @param b Second variable
	 * @return The hamming distance
	 */
	public static int hamming(TupleDesc_B a, TupleDesc_B b ) {
		int score = 0;
		final int N = a.data.length;
		for( int i = 0; i < N; i++ ) {
			score += hamming(a.data[i] ^ b.data[i]);
		}
		return score;
	}

	public static int hamming( int val ) {
		int distance = 0;

		while( val != 0 ) {
			val &= val - 1;
			distance++;
		}
		return distance;
	}
}
