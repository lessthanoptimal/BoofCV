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

package boofcv.alg.structure.spawn;

import boofcv.alg.geo.MetricCameras;
import boofcv.alg.structure.*;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestMetricSpawnSceneFromView extends BoofStandardJUnit {

	/**
	 * Simple scenario that tests everything all together
	 */
	@Test void simple() {
		// Create a simple scene with the camera moving along a straight line
		var dbSimilar = new MockLookupSimilarImagesRealistic();
		dbSimilar.pathLine(10, 0.1, 0.9, 2);
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

		PairwiseImageGraph pairwise = dbSimilar.createPairwise();
		PairwiseImageGraph.View seed = pairwise.nodes.get(2);
		DogArray_I32 motions = DogArray_I32.array(0, 1, 2, 3);

		// Create the new scene
		var alg = new MetricSpawnSceneFromView(
				new RefineMetricWorkingGraph(),
				new PairwiseGraphUtils(new ConfigProjectiveReconstruction()));
		assertTrue(alg.process(dbSimilar, dbCams, pairwise, seed, motions));

		// DO some very basic tests
		SceneWorkingGraph found = alg.getScene();
		assertEquals(1, found.listCameras.size());
		assertEquals(5, found.listViews.size());
	}

	@Test void saveMetricSeed() {
		var graph = new PairwiseImageGraph();
		List<String> viewIds = BoofMiscOps.asList("A", "B", "C");
		var listInliers = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);
		// specify the seed view
		listInliers.grow().setTo(1, 3, 5, 7, 9);
		int numInliers = listInliers.get(0).size;

		// create distinctive sets of inlier indexes for each view
		for (int otherIdx = 0; otherIdx < viewIds.size() - 1; otherIdx++) {
			DogArray_I32 inliers = listInliers.grow();
			for (int i = 0; i < numInliers; i++) {
				inliers.add(listInliers.get(0).get(i) + 1 + otherIdx);
			}
		}

		// Create some arbitrary metric results that should be saved
		var results = new MetricCameras();
		for (int viewIdx = 0; viewIdx < viewIds.size(); viewIdx++) {
			// skip zero since it's implicit
			if (viewIdx > 0)
				results.motion_1_to_k.grow().T.setTo(1, viewIdx, 0);
			CameraPinhole pinhole = results.intrinsics.grow();
			pinhole.fx = pinhole.fy = 100 + viewIdx;
			graph.createNode(viewIds.get(viewIdx));
		}

		var alg = new MetricSpawnSceneFromView();
		alg.utils.dbCams = new MockLookUpCameraInfo(800, 800);
		var wgraph = new SceneWorkingGraph();
		alg.saveMetricSeed(graph, viewIds, listInliers, results, wgraph);

		// Check the cameras. There should only be one
		assertEquals(1, wgraph.cameras.size());
		assertEquals(0, wgraph.cameras.get(0).indexDB);
		assertEquals(100, wgraph.cameras.get(0).intrinsic.f);

		// See metric view info got saved correctly
		BoofMiscOps.forIdx(viewIds, ( idx, viewId ) -> {
			PairwiseImageGraph.View pview = graph.lookupNode(viewId);
			assertTrue(wgraph.isKnown(pview));
			SceneWorkingGraph.View wview = wgraph.lookupView(viewId);

			assertEquals(idx == 0 ? 0 : 1, wview.world_to_view.T.x, 1e-8);
			assertEquals(idx, wview.world_to_view.T.y, 1e-8);
		});

		// See if inliers got saved correctly
		BoofMiscOps.forIdx(viewIds, ( idx, viewId ) -> {
			SceneWorkingGraph.View wview = wgraph.lookupView(viewId);

			assertEquals(1, wview.inliers.size);
			SceneWorkingGraph.InlierInfo inlier = wview.inliers.get(0);

			assertEquals(0.0, inlier.scoreGeometric, "Score should be unassigned");

			assertEquals(viewIds.size(), inlier.views.size);
			assertEquals(viewIds.size(), inlier.observations.size);
			assertEquals(viewId, inlier.views.get(0).id);
			for (int inlierIdx = 0; inlierIdx < viewIds.size(); inlierIdx++) {
				int offset = (idx + inlierIdx)%viewIds.size();
				final int c = offset;
				DogArray_I32 obs = inlier.observations.get(inlierIdx);
				obs.forIdx(( i, value ) -> assertEquals(i*2 + 1 + c, value));
			}
		});
	}
}
