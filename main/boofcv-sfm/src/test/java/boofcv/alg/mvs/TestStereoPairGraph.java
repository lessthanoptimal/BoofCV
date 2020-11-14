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

package boofcv.alg.mvs;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
class TestStereoPairGraph extends BoofStandardJUnit {
	@Test void addVertex() {
		var alg = new StereoPairGraph();
		StereoPairGraph.Vertex v = alg.addVertex("a", 2);
		assertSame(v, alg.vertexes.get("a"));
		assertEquals("a", v.id);
		assertEquals(2, v.indexSba);
		assertEquals(0, v.pairs.size());
	}

	@Test void connect() {
		var alg = new StereoPairGraph();
		StereoPairGraph.Vertex a = alg.addVertex("a", 2);
		StereoPairGraph.Vertex b = alg.addVertex("b", 2);

		StereoPairGraph.Edge e = alg.connect("a", "b", 0.2);
		assertSame(a, e.va);
		assertSame(b, e.vb);
		assertEquals(0.2, e.quality3D, UtilEjml.TEST_F64);
	}
}
