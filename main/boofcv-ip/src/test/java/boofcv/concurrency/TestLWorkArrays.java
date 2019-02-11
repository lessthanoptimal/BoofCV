/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.concurrency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestLWorkArrays {
	@Test
	void reset() {
		LWorkArrays alg = new LWorkArrays();
		alg.reset(20);
		assertEquals(20,alg.length);
		alg.recycle(new long[20]);
		alg.recycle(new long[20]);
		assertEquals(2,alg.storage.size());
		alg.reset(21);
		assertEquals(0,alg.storage.size());
		assertEquals(21,alg.length);
	}

	@Test
	void pop_recycle() {
		LWorkArrays alg = new LWorkArrays();
		alg.reset(20);
		assertEquals(20,alg.length);
		alg.recycle(new long[20]);
		alg.recycle(new long[20]);
		assertEquals(20,alg.pop().length);
		assertEquals(1,alg.storage.size());
		assertEquals(20,alg.pop().length);
		assertEquals(0,alg.storage.size());
		assertEquals(20,alg.pop().length);
		assertEquals(0,alg.storage.size());
	}

	@Test
	void length() {
		LWorkArrays alg = new LWorkArrays();
		assertEquals(0,alg.length());
		alg.reset(20);
		assertEquals(20,alg.length());
	}
}