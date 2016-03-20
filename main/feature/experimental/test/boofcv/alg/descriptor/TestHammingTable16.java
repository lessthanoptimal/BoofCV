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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHammingTable16 {

	@Test
	public void exhaustive() {
		HammingTable16 alg = new HammingTable16();

		for( int i = 0; i < 256; i++ ) {
			for( int j = 0; j < 256; j++ ) {
				int expected = TestDescriptorDistance.hamming(i,j);
				int found = alg.lookup((short)i,(short)j);

				assertEquals(expected,found);
			}
		}

		int expected = TestDescriptorDistance.hamming(65533,62003);
		int found = alg.lookup((short)65533,(short)62003);
		assertEquals(expected,found);
	}
}
