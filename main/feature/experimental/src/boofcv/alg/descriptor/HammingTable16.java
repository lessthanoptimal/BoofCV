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

/**
 * Lookup table for hamming distance from 16-bit variables
 *
 * @author Peter Abeles
 */
public class HammingTable16 {

	// about 10% faster if int[] is used instead of byte[]
	public int score[] = new int[65536];

	public HammingTable16() {
		int index = 0;
		for( int i = 0; i < 65536; i++ ) {
			score[index++] = DescriptorDistance.hamming(i);
		}
	}

	/**
	 * Looks up the hamming distance from a table
	 *
	 * @param a First feature vector
	 * @param b Second feature vector
	 * @return Hamming score
	 */
	public int lookup( short a , short b ) {
		return score[ (a ^ b) & 0xFFFF ];
	}
}
