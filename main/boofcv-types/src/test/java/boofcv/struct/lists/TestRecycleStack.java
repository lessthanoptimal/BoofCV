/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.lists;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestRecycleStack extends BoofStandardJUnit {
	@Test
	void pop() {
		RecycleStack<Moo> alg = new RecycleStack<>(Moo::new);

		Moo m = alg.pop();
		assertNotNull(m);
		assertEquals(0,alg.list.size());

		alg.list.add( new Moo());
		m = alg.pop();
		assertNotNull(m);
		assertEquals(0,alg.list.size());
	}

	@Test
	void recycle() {
		RecycleStack<Moo> alg = new RecycleStack<>(Moo::new);

		alg.recycle(new Moo());
		assertEquals(1,alg.list.size());
	}

	@Test
	void purge() {
		RecycleStack<Moo> alg = new RecycleStack<>(Moo::new);

		List<Moo> original = alg.list;
		alg.purge();
		assertNotSame(original, alg.list);
	}

	static class Moo {
	}
}
