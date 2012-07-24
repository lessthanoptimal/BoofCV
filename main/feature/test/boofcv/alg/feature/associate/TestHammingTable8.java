/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.associate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHammingTable8 {

	@Test
	public void exhaustive() {
		HammingTable8 alg = new HammingTable8();

		for( int i = 0; i < 256; i++ ) {
			for( int j = 0; j < 256; j++ ) {
				int expected = TestDescriptorDistance.hamming(i,j);
				int found = alg.lookup((byte)i,(byte)j);

				assertEquals(expected,found);
			}
		}
	}
}
