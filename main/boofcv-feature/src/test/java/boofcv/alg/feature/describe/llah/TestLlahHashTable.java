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

package boofcv.alg.feature.describe.llah;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestLlahHashTable extends BoofStandardJUnit {
	@Test
	void add_lookup() {
		var a = new LlahFeature(5);
		var b = new LlahFeature(5);
		var c = new LlahFeature(5);
		var d = new LlahFeature(5);

		a.hashCode = 1;
		b.hashCode = 1;
		c.hashCode = 1;
		d.hashCode = 10;

		var alg = new LlahHashTable();

		alg.add(a);
		assertEquals(1,alg.map.size());
		assertSame(a, alg.lookup(1));

		alg.add(b);
		assertEquals(1,alg.map.size());
		assertSame(a, alg.lookup(1));
		assertSame(a.next,b);

		alg.add(c);
		assertEquals(1,alg.map.size());
		assertSame(a, alg.lookup(1));
		assertSame(a.next,b);
		assertSame(b.next,c);

		d.next = d;
		alg.add(d);
		assertEquals(2,alg.map.size());
		assertSame(a, alg.lookup(1));
		assertSame(d, alg.lookup(10));
		assertNull(d.next);
	}
}
