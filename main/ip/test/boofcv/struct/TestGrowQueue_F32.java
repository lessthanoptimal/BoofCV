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

package boofcv.struct;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestGrowQueue_F32 {

	@Test
	public void auto_grow() {
		GrowQueue_F32 alg = new GrowQueue_F32(3);

		assertEquals(3,alg.data.length);

		for( int i = 0; i < 10; i++ )
			alg.push(i);

		assertEquals(10,alg.size);

		for( int i = 0; i < 10; i++ )
			assertEquals(i,alg.get(i),1e-8);
	}

	@Test
	public void reset() {
		GrowQueue_F32 alg = new GrowQueue_F32(10);

		alg.push(1);
		alg.push(3);
		alg.push(-2);

		assertTrue(1.0f == alg.get(0));
		assertEquals(3,alg.size);

		alg.reset();

		assertEquals(0, alg.size);
	}

	@Test
	public void push_pop() {
		GrowQueue_F32 alg = new GrowQueue_F32(10);

		alg.push(1);
		alg.push(3);

		assertEquals(2,alg.size);
		assertTrue(3==alg.pop());
		assertTrue(1==alg.pop());
		assertEquals(0,alg.size);
	}

}
