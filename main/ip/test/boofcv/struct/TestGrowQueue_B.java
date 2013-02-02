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

package boofcv.struct;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGrowQueue_B {

	@Test
	public void auto_grow() {
		GrowQueue_B alg = new GrowQueue_B(3);

		assertEquals(3,alg.data.length);

		for( int i = 0; i < 10; i++ )
			alg.push((i%2)==0);

		assertEquals(10,alg.size);

		for( int i = 0; i < 10; i++ )
			assertEquals((i%2)==0,alg.get(i));
	}

	@Test
	public void reset() {
		GrowQueue_B alg = new GrowQueue_B(10);

		alg.push(true);
		alg.push(false);
		alg.push(false);

		assertTrue(true == alg.get(0));
		assertEquals(3,alg.size);

		alg.reset();

		assertEquals(0, alg.size);
	}

	@Test
	public void push_pop() {
		GrowQueue_B alg = new GrowQueue_B(10);

		alg.push(false);
		alg.push(true);

		assertEquals(2,alg.size);
		assertTrue(alg.pop());
		assertTrue(!alg.pop());
		assertEquals(0, alg.size);
	}
}
