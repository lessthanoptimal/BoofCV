/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.feature;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestCachedSineCosine_F32 {

	/**
	 * Compare solution against a hand generated one
	 */
	@Test
	public void knowCase() {
		CachedSineCosine_F32 alg = new CachedSineCosine_F32(-2,1,5);

		assertEquals(-2,alg.minAngle,1e-4);
		assertEquals(1,alg.maxAngle,1e-4);
		assertEquals(0.75,alg.delta,1e-4);

		assertEquals(-0.41615,alg.c[0],0.2);
		assertEquals(-0.90930,alg.s[0],0.2);

		assertEquals(0.87758,alg.c[2],0.2);
		assertEquals(-0.47943,alg.s[2],0.2);

		assertEquals(0.54030,alg.c[4],0.2);
		assertEquals(0.84147,alg.s[4],0.2);

		assertEquals(0,alg.computeIndex(-2));
		assertEquals(2,alg.computeIndex(0));
		assertEquals(4,alg.computeIndex(1));
	}
}
