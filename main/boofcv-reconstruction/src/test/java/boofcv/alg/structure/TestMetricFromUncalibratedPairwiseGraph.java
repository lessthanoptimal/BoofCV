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

import boofcv.BoofTesting;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.MultiViewOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.ImageDimension;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMetricFromUncalibratedPairwiseGraph extends BoofStandardJUnit {

	// testing tolerance for focal length on perfect input data
	double focalTol = 1e-2;
	// testing tolernace for location on perfect data
	double locationTol = 1e-3;

	/**
	 * Random scene with fairly linear motion. Everything is randomized and physical constraints on camera are enforced
	 */
	@Test void process_perfect() {
		// NOTE: Self calibration assumes image center is the principle point and that fx==fy and zero skew. Make
		// sure truth matches that assumption

		// TODO make this test run faster
		var alg = new MetricFromUncalibratedPairwiseGraph();
//		alg.setVerbose(System.out, null);
//		alg.getRefineWorking().setVerbose(System.out, null);
		for (int numViews = 3; numViews <= 23; numViews += 5) {
//			System.out.println("Number of views "+numViews);
			// Need to increase the number of features to ensure everything is connected properly and that there is
			// enough info for a good estimate
			var db = new MockLookupSimilarImagesRealistic().setLoop(false).
					setIntrinsic(new CameraPinhole(410, 410, 0, 400, 400, 800, 800)).
					setSeed(numViews).setFeatures(Math.max(400, 50*numViews)).pathLine(numViews, 0.30, 6.0, 2);
			PairwiseImageGraph graph = db.createPairwise();
			assertTrue(alg.process(db, graph));
			assertEquals(1, alg.getScenes().size);
			checkReconstruction(alg, db);
		}
	}

	/**
	 * Compare found camera matrices against truth by converting them into the same projective scale
	 */
	private void checkReconstruction( MetricFromUncalibratedPairwiseGraph alg, MockLookupSimilarImagesRealistic db ) {
		SceneWorkingGraph workGraph = alg.getLargestScene();
		List<SceneWorkingGraph.View> foundViews = workGraph.getAllViews();
		assertEquals(db.views.size(), foundViews.size());

		//------------- Check intrinsic parameters
		for (SceneWorkingGraph.View v : foundViews) {
			assertEquals(db.intrinsic.fx, v.intrinsic.f, focalTol);
			assertEquals(db.intrinsic.fy, v.intrinsic.f, focalTol);
		}

		//------------- Check camera motion up to scale
		// The the origin can float in truth and found so let's pick a view arbitrarily and make that the origin
		String originID = db.views.get(0).id;
		Se3_F64 fndWorld_to_origin = workGraph.lookupView(originID).world_to_view;
		Se3_F64 expWorld_to_origin = db.views.get(0).world_to_view;

		for (var trueView : db.views) {
			SceneWorkingGraph.View wview = workGraph.lookupView(trueView.id);
			Se3_F64 found = wview.world_to_view.invert(null).concat(fndWorld_to_origin, null);
			Se3_F64 expected = trueView.world_to_view.invert(null).concat(expWorld_to_origin, null);

			// These are only equal up to a scale + sign ambiguity
			double scale = MultiViewOps.findScale(found.T, expected.T);
			found.T.scale(scale);

			BoofTesting.assertEquals(expected, found, 0.001, locationTol);
		}

		// Should also check the inliers to see if they make sense.
	}

	@Test void saveMetricSeed() {
		var graph = new PairwiseImageGraph();
		List<String> viewIds = BoofMiscOps.asList("A", "B", "C");
		List<ImageDimension> dimensions = new ArrayList<>();
		var inlierToSeed = DogArray_I32.array(1, 3, 5, 7, 9);
		var inlierToOther = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);

		// create distinctive sets of inlier indexes for each view
		for (int otherIdx = 0; otherIdx < viewIds.size() - 1; otherIdx++) {
			DogArray_I32 inliers = inlierToOther.grow();
			for (int i = 0; i < inlierToSeed.size; i++) {
				inliers.add(inlierToSeed.get(i) + 1 + otherIdx);
			}
			dimensions.add(new ImageDimension(800, 800));
		}
		dimensions.add(new ImageDimension(800, 800));

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

		var alg = new MetricFromUncalibratedPairwiseGraph();
		var wgraph = new SceneWorkingGraph();
		alg.saveMetricSeed(graph, viewIds, dimensions, inlierToSeed, inlierToOther, results, wgraph);

		// See metric view info got saved correctly
		BoofMiscOps.forIdx(viewIds, ( idx, viewId ) -> {
			PairwiseImageGraph.View pview = graph.lookupNode(viewId);
			assertTrue(wgraph.isKnown(pview));
			SceneWorkingGraph.View wview = wgraph.lookupView(viewId);

			assertEquals(100 + idx, wview.intrinsic.f, 1e-8);
			assertEquals(idx == 0 ? 0 : 1, wview.world_to_view.T.x, 1e-8);
			assertEquals(idx, wview.world_to_view.T.y, 1e-8);
		});

		// See if inliers got saved correctly
		BoofMiscOps.forIdx(viewIds, ( idx, viewId ) -> {
			SceneWorkingGraph.View wview = wgraph.lookupView(viewId);

			if (idx != 0) {
				// only the first view (the seed) should have inliers saved
				assertEquals(0, wview.inliers.size);
				return;
			}

			assertEquals(1, wview.inliers.size);
			SceneWorkingGraph.InlierInfo inlier = wview.inliers.get(0);

			assertTrue(inlier.scoreGeometric > 0.0);

			assertEquals(viewIds.size(), inlier.views.size);
			assertEquals(viewIds.size(), inlier.observations.size);
			assertEquals(viewId, inlier.views.get(0).id);
			for (int checkIdx = 0; checkIdx < viewIds.size(); checkIdx++) {
				final int c = checkIdx;
				DogArray_I32 obs = inlier.observations.get(checkIdx);
				obs.forIdx(( i, value ) -> assertEquals(i*2 + 1 + c, value));
			}
		});
	}

	@Test void mergeScenes() {
		fail("Implement");
	}

	@Test void getLargestScene() {
		var alg = new MetricFromUncalibratedPairwiseGraph();
		// Create scenes with increasing number of views
		alg.scenes.resize(4, ( idx, scene ) -> {
			for (int i = 0; i < idx; i++) {
				var v = new PairwiseImageGraph.View();
				v.init(i, i + "");
				scene.addView(v);
			}
		});

		// Test it against the known solution
		assertSame(alg.scenes.getTail(), alg.getLargestScene());
	}
}
