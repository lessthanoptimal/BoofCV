/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestSceneWorkingGraph {
	@Test
	void isKnown() {
		var alg = new SceneWorkingGraph();
		var viewA = new PairwiseImageGraph2.View();
		viewA.id = "moo";
		var  viewB = new PairwiseImageGraph2.View();
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

	@Test
	void addView() {
		var alg = new SceneWorkingGraph();
		var pview = new PairwiseImageGraph2.View();
		pview.id = "moo";
		SceneWorkingGraph.View found = alg.addView(pview);
		assertSame(found.pview,pview);
		assertSame(found,alg.views.get(pview.id));
	}

	@Test
	void createFeature() {
		var alg = new SceneWorkingGraph();
		SceneWorkingGraph.Feature found = alg.createFeature();
		assertEquals(1, alg.features.size());
		assertSame(alg.features.get(0), found);
	}
}