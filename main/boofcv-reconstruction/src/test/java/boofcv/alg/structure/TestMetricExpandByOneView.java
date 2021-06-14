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
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMetricExpandByOneView extends BoofStandardJUnit {
	/**
	 * Perfect inputs that should yield perfect results. Camera is unknown.
	 */
	@Test void perfect_SelfCalibrate() {
		// make sure (cx,cy) = (width/2, height/2) or else unit test will fail because it doesn't perfectly
		// match the model
		var dbSimilar = new MockLookupSimilarImagesRealistic().
				setIntrinsic(new CameraPinhole(400, 400, 0, 400, 400, 800, 800)).
				pathLine(5, 0.3, 1.5, 2);
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);
		var alg = new MetricExpandByOneView();

		// Perfect without SBA - Will normally not be done this way, but with perfect data it should return
		// perfect results and will highlight bugs that might be hidden by SBA
		alg.utils.configConvergeSBA.maxIterations = 0;
		for (int i = 0; i < 5; i++) {
			checkPerfect(dbSimilar, dbCams, alg, false, i);
		}

		// Turn SBA back on and estimate each of the views after leaving them out one at a time
		alg.utils.configConvergeSBA.maxIterations = 50;
		for (int i = 0; i < 5; i++) {
			checkPerfect(dbSimilar, dbCams, alg, false, i);
		}
	}

	/**
	 * Check the code path that deals with calibrated scenes
	 */
	@Test void perfect_KnownCameras() {
		// make sure (cx,cy) = (width/2, height/2) or else unit test will fail because it doesn't perfectly
		// match the model
		var dbSimilar = new MockLookupSimilarImagesRealistic().
				setIntrinsic(new CameraPinhole(400, 400, 0, 400, 400, 800, 800)).
				pathLine(5, 0.3, 1.5, 2);
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);
		var alg = new MetricExpandByOneView();

		checkPerfect(dbSimilar, dbCams, alg, true, 0);
	}

	private void checkPerfect( MockLookupSimilarImagesRealistic dbSimilar,
							   MockLookUpCameraInfo dbCams,
							   MetricExpandByOneView alg, boolean cameraKnown, int targetViewIdx ) {
		PairwiseImageGraph pairwise = dbSimilar.createPairwise();
		SceneWorkingGraph workGraph = dbSimilar.createWorkingGraph(pairwise);

		if (cameraKnown) {
			SceneWorkingGraph.Camera c = workGraph.addCamera(0);
			BundleAdjustmentOps.convert(dbSimilar.intrinsic, c.intrinsic);
			c.prior.setTo(dbSimilar.intrinsic);
		}

		// Decide which view will be estimated
		PairwiseImageGraph.View target = pairwise.nodes.get(targetViewIdx);

		// remove the view from the work graph
		workGraph.listViews.remove(workGraph.views.remove(target.id));

		// add the target view
		assertTrue(alg.process(dbSimilar, dbCams, workGraph, target));

		// Check calibration
		workGraph.listCameras.forEach(c -> {
			assertEquals(dbSimilar.intrinsic.fx, c.intrinsic.f, 1e-4);
			assertEquals(dbSimilar.intrinsic.fy, c.intrinsic.f, 1e-4);
		});

		// Check pose
		SceneWorkingGraph.View found = workGraph.views.get(target.id);
		BoofTesting.assertEquals(dbSimilar.views.get(targetViewIdx).world_to_view, found.world_to_view, 0.01, 0.01);
	}

	/**
	 * Perfect inputs that should yield perfect results. Camera is known/calibrated.
	 */
	@Test void perfect_Calibrated() {
		fail("Implement");
	}

	/**
	 * When it fails to find the metric upgrade make sure it doesn't add it to th work graph
	 */
	@Test void fail_and_doNotAdd() {
		var dbSimilar = new MockLookupSimilarImagesRealistic().
				setIntrinsic(new CameraPinhole(400, 420, 0, 400, 400, 800, 800)).
				pathLine(5, 0.3, 1.5, 2);
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

		// force it to fail at these two different points
		var alg1 = new MetricExpandByOneView() {
			@Override
			public boolean selectTwoConnections( PairwiseImageGraph.View target,
												 List<PairwiseImageGraph.Motion> connections ) {
				return false;
			}
		};

		fail_and_doNotAdd(dbSimilar, dbCams, alg1, 0);
	}

	private void fail_and_doNotAdd( MockLookupSimilarImagesRealistic dbSimilar,
									MockLookUpCameraInfo dbCams,
									MetricExpandByOneView alg, int targetViewIdx ) {
		PairwiseImageGraph pairwise = dbSimilar.createPairwise();
		SceneWorkingGraph workGraph = dbSimilar.createWorkingGraph(pairwise);

		// Decide which view will be estimated
		PairwiseImageGraph.View target = pairwise.nodes.get(targetViewIdx);

		workGraph.listViews.remove(workGraph.views.remove(target.id));

		// This should fail and not add it to the work graph
		assertFalse(alg.process(dbSimilar, dbCams, workGraph, target));
		assertFalse(workGraph.isKnown(target));
	}

	/**
	 * Make sure the local optimization doesn't update any global intrinsic parameters
	 */
	@Test void globalIntrinsicsNotModified() {
		fail("Implement");
	}
}
