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
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestSceneWorkingGraph extends BoofStandardJUnit {
	@Test void setTo() {
		var expected = new SceneWorkingGraph();
		var found = new SceneWorkingGraph();

		SceneWorkingGraph.Camera camera = expected.addCamera(2);

		expected.addView(new PairwiseImageGraph.View("1"), camera);
		expected.addView(new PairwiseImageGraph.View("2"), camera);
		expected.open.add(new PairwiseImageGraph.View("3"));
		expected.index = 2;
		expected.exploredViews.add("B");
		expected.numSeedViews = 6;

		found.setTo(expected);

		assertEquals(1, found.cameras.size());
		assertEquals(1, found.listCameras.size());
		assertEquals(2, found.listCameras.get(0).indexDB);
		assertEquals(2, found.views.size());
		assertEquals(2, found.listViews.size());
		assertSame(expected.listViews.get(0).pview, found.listViews.get(0).pview);
		assertSame(expected.listViews.get(1).pview, found.listViews.get(1).pview);
		assertEquals(expected.open.size, found.open.size);
		assertSame(expected.open.get(0), found.open.get(0));
		assertEquals(2, found.index);
		assertEquals(1, found.exploredViews.size());
		assertEquals(6, found.numSeedViews);
	}

	@Test void isKnown() {
		var alg = new SceneWorkingGraph();
		var viewA = new PairwiseImageGraph.View();
		viewA.id = "moo";
		var viewB = new PairwiseImageGraph.View();
		viewB.id = "boo";
		SceneWorkingGraph.Camera camera = alg.addCamera(2);

		assertFalse(alg.isKnown(viewA));
		assertFalse(alg.isKnown(viewB));
		alg.addView(viewB, camera);
		assertFalse(alg.isKnown(viewA));
		assertTrue(alg.isKnown(viewB));
		alg.addView(viewA, camera);
		assertTrue(alg.isKnown(viewA));
		assertTrue(alg.isKnown(viewB));
	}

	@Test void addView() {
		var alg = new SceneWorkingGraph();
		var pview = new PairwiseImageGraph.View();
		SceneWorkingGraph.Camera camera = alg.addCamera(2);
		pview.id = "moo";
		SceneWorkingGraph.View found = alg.addView(pview, camera);
		assertSame(found.pview, pview);
		assertSame(found, alg.views.get(pview.id));
	}

	@Test void View_setTo() {
		var expected = new SceneWorkingGraph.View();
		var found = new SceneWorkingGraph.View();

		expected.cameraIdx = 2;
		expected.world_to_view.T.setTo(1, 2, 3);
		expected.inliers.resize(2);
		expected.inliers.get(0).scoreGeometric = 5;
		expected.index = 2;
		expected.projective.set(0, 0, 2);
		expected.pview = new PairwiseImageGraph.View();

		found.setTo(expected);

		assertEquals(2, found.cameraIdx);
		assertEquals(0.0, expected.world_to_view.T.distance(found.world_to_view.T));
		assertEquals(2, found.inliers.size);
		assertEquals(5, found.inliers.get(0).scoreGeometric);
		assertEquals(2, found.index);
		assertTrue(MatrixFeatures_DDRM.isIdentical(expected.projective, found.projective, 1e-8));
		assertSame(expected.pview, found.pview);
	}

	@Test void InlierInfo_setTo() {
		var expected = new SceneWorkingGraph.InlierInfo();
		var found = new SceneWorkingGraph.InlierInfo();

		expected.views.add(new PairwiseImageGraph.View());
		expected.observations.resize(4, p -> p.setTo(1, 2, 3));
		expected.scoreGeometric = 1.5;

		found.setTo(expected);

		assertEquals(1, found.views.size);
		assertSame(expected.views.get(0), found.views.get(0));
		assertEquals(4, found.observations.size);
		found.observations.forEach(v -> assertEquals(3, v.size));
		assertEquals(expected.scoreGeometric, found.scoreGeometric);
	}
}
