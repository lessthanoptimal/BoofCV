/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestSceneWorkingGraph extends BoofStandardJUnit {

	@Test void setTo() {
		fail("Implement");
	}

	@Test void isKnown() {
		var alg = new SceneWorkingGraph();
		var viewA = new PairwiseImageGraph.View();
		viewA.id = "moo";
		var viewB = new PairwiseImageGraph.View();
		viewB.id = "boo";

		assertFalse(alg.isKnown(viewA));
		assertFalse(alg.isKnown(viewB));
		alg.addView(viewB);
		assertFalse(alg.isKnown(viewA));
		assertTrue(alg.isKnown(viewB));
		alg.addView(viewA);
		assertTrue(alg.isKnown(viewA));
		assertTrue(alg.isKnown(viewB));
	}

	@Test void addView() {
		var alg = new SceneWorkingGraph();
		var pview = new PairwiseImageGraph.View();
		pview.id = "moo";
		SceneWorkingGraph.View found = alg.addView(pview);
		assertSame(found.pview, pview);
		assertSame(found, alg.views.get(pview.id));
	}

	@Test void InlierInfo_setTo() {
		var expected = new SceneWorkingGraph.InlierInfo();
		var found = new SceneWorkingGraph.InlierInfo();

		expected.views.add(new PairwiseImageGraph.View());
		expected.observations.resize(4,p->p.setTo(1,2,3));
		expected.scoreGeometric = 1.5;

		found.setTo(expected);

		assertEquals(1, found.views.size);
		assertSame(expected.views.get(0), found.views.get(0));
		assertEquals(4, found.observations.size);
		found.observations.forEach(v->assertEquals(3, v.size));
		assertEquals(expected.scoreGeometric, found.scoreGeometric);
	}
}
