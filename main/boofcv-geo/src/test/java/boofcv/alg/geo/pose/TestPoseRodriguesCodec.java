/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPoseRodriguesCodec extends BoofStandardJUnit {

	@Test void encode_decode() {
		
		double []orig = new double[]{.1,.2,.3,4,5,6};
		double []found = new double[6];
		
		Se3_F64 encoded = new Se3_F64();
		
		PnPRodriguesCodec codec = new PnPRodriguesCodec();
		
		assertEquals(6,codec.getParamLength());
		
		codec.decode(orig,encoded);
		codec.encode(encoded,found);
		
		for( int i = 0; i < 6; i++ ) {
			assertEquals(orig[i],found[i],1e-6);
		}
	}
}
