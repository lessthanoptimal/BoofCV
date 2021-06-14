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
import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.junit.jupiter.api.Test;

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
		// attempt to speed things up by telling it to not try as hard
		var config = new ConfigProjectiveReconstruction();
		config.ransac.iterations = 1;
		config.sbaConverge.maxIterations = 0;
		config.ransacTrifocal.converge.maxIterations = 0;
		// Can this be speed up any more? Should profile and see what the slow down is

		// Add extra internal checks
		var alg = new MetricFromUncalibratedPairwiseGraph(config) {
			@Override
			protected boolean spawnSceneFromSeed( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams,
												  PairwiseImageGraph pairwise, SeedInfo info ) {
				if (!super.spawnSceneFromSeed(dbSimilar, dbCams, pairwise, info))
					return false;
				sanityCheckScenesInEachView();
				return true;
			}

			@Override
			boolean expandIntoView( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams,
									SceneWorkingGraph scene, PairwiseImageGraph.View selected ) {
				if (!super.expandIntoView(dbSimilar, dbCams, scene, selected))
					return false;
				sanityCheckScenesInEachView();
				return true;
			}

			@Override void mergeScenes( LookUpSimilarImages db ) {
				// TODO this test isn't triggering merge logic. Create a new scene which will trigger it?
				super.mergeScenes(db);
				mergeOps.sanityCheckTable(scenes.toList());
			}
		};
		// Attempting to speed up the test. It has perfect data so it shouldn't need to iterate so many times
		alg.getRefineWorking().metricSba.configConverge.maxIterations = 4;
		alg.getExpandMetric().expandUnknown.estimateUtils.metricSba.configConverge.maxIterations = 4;
		alg.getMergeScenes().refiner.metricSba.configConverge.maxIterations = 4;

//		alg.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
//		alg.getRefineWorking().setVerbose(System.out, null);
		for (int numViews = 3; numViews <= 23; numViews += 5) {
//			System.out.println("Number of views " + numViews);
			// Need to increase the number of features to ensure everything is connected properly and that there is
			// enough info for a good estimate
			var dbSimilar = new MockLookupSimilarImagesRealistic().setLoop(false).
					setIntrinsic(new CameraPinhole(410, 410, 0, 400, 400, 800, 800)).
					setSeed(numViews).setFeatures(Math.max(400, 50*numViews)).pathLine(numViews, 0.30, 6.0, 2);
			var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

			PairwiseImageGraph graph = dbSimilar.createPairwise();
			assertTrue(alg.process(dbSimilar, dbCams, graph));
			assertEquals(1, alg.getScenes().size);
			checkReconstruction(alg, dbSimilar);
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
		for (SceneWorkingGraph.Camera c : workGraph.listCameras.toList()) {
			assertEquals(db.intrinsic.fx, c.intrinsic.f, focalTol);
			assertEquals(db.intrinsic.fy, c.intrinsic.f, focalTol);
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

	@Test void getLargestScene() {
		var alg = new MetricFromUncalibratedPairwiseGraph();
		// Create scenes with increasing number of views
		alg.scenes.resize(4, ( idx, scene ) -> {
			SceneWorkingGraph.Camera camera = scene.addCamera(1);
			for (int i = 0; i < idx; i++) {
				var v = new PairwiseImageGraph.View();
				v.init(i, i + "");
				scene.addView(v, camera);
			}
		});

		// Test it against the known solution
		assertSame(alg.scenes.getTail(), alg.getLargestScene());
	}
}
