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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestRefineMetricGraphSubset extends BoofStandardJUnit {
	/**
	 * Test everything all at once in a simple scenario
	 */
	@Test void allTogether() {
		var db = new MockLookupSimilarImagesRealistic().
				setFeatures(100).
				setIntrinsic(new CameraPinhole(400, 400, 0, 250, 250, 500, 500)).
				pathLine(7, 0.1, 0.6, 2);
		var pairwise = db.createPairwise();
		var graph = db.createWorkingGraph(pairwise);

		// Need the pairwise info so give it a few sets of triplets
		db.addInlierInfo(pairwise, graph.listViews.get(0), 1, 2);
		for (int i = 1; i < db.views.size() - 1; i++) {
			db.addInlierInfo(pairwise, graph.listViews.get(i), i - 1, i + 1);
		}
		db.addInlierInfo(pairwise, graph.listViews.get(6), 4, 5);

		// Save a copy of the poses to test for changes
		var expectedSE3 = new ArrayList<Se3_F64>();
		graph.listViews.forEach(v -> expectedSE3.add(v.world_to_view.copy()));

		// leave out the first and last images
		var subsetViews = new ArrayList<SceneWorkingGraph.View>();
		for (int i = 1; i < 6; i++) {
			subsetViews.add(graph.listViews.get(i));
		}

		var alg = new RefineMetricGraphSubset();
		alg.setSubset(graph, subsetViews);

		// set one view to known
		alg.setViewKnown(subsetViews.get(1).pview.id);

		// There should be no change since the data is perfect right now
		assertTrue(alg.process(db));
		BoofMiscOps.forIdx(expectedSE3, ( idx, v ) -> {
			assertEquals(0.0, v.T.distance(graph.listViews.get(idx).world_to_view.T), 1e-6);
		});

		// modify a view, it should fix it
		alg.subgraph.listViews.get(2).world_to_view.T.x += 0.1;
		assertTrue(alg.process(db));
		assertEquals(expectedSE3.get(3).T.x, graph.listViews.get(3).world_to_view.T.x, 1e-6);

		// modify the known view, it should remain broken
		graph.listViews.get(2).world_to_view.T.x = 2.5;
		alg.setSubset(graph, subsetViews);
		alg.setViewKnown(subsetViews.get(1).pview.id);
		assertTrue(alg.process(db));
		assertEquals(2.5, graph.listViews.get(2).world_to_view.T.x, 1e-6);
		assertNotEquals(expectedSE3.get(3).T.x, graph.listViews.get(3).world_to_view.T.x, 1e-6);

		// Sanity check that added referenced views are set as fixed
		for (int i = 5; i < 7; i++) {
			assertTrue(alg.viewsFixed.get(i));
		}

		// Check cameras now. Only one camera in this scenario
		assertEquals(1, alg.camerasFixed.size);
		assertFalse(alg.camerasFixed.get(0));
	}

	/**
	 * Checks that the conversion to a scaled local coordinate system is done correctly
	 */
	@Test void localCoordinateTransform() {
		var alg = new RefineMetricGraphSubset();
		var cameraDummy = new SceneWorkingGraph.Camera();
		for (int i = 0; i < 10; i++) {
			var pview = new PairwiseImageGraph.View();
			pview.id = "" + i;
			SceneWorkingGraph.View wv = alg.subgraph.addView(pview, cameraDummy);
			wv.world_to_view.T.setTo(i, 0.1, -0.1*i);
		}

		alg.rescaleLocalCoordinateSystem();
		double maxNorm = 0.0;
		for (var wv : alg.subgraph.listViews) {
			maxNorm = Math.max(maxNorm, wv.world_to_view.T.norm());
		}

		assertEquals(1.0, maxNorm, 1e-8);

		Se3_F64 found = new Se3_F64();
		for (int i = 0; i < 10; i++) {
			alg.localToGlobal(alg.subgraph.listViews.get(i).world_to_view, found);
			assertEquals(i, found.T.x, UtilEjml.TEST_F64);
			assertEquals(0.1, found.T.y, UtilEjml.TEST_F64);
			assertEquals(-0.1*i, found.T.z, UtilEjml.TEST_F64);
		}
	}
}
