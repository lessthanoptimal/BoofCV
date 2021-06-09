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
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.MultiViewOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMetricExpandByOneView extends BoofStandardJUnit {
	/**
	 * Perfect inputs that should yield perfect results.
	 */
	@Test void perfect() {
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
			checkPerfect(dbSimilar, dbCams, alg, i);
		}

		// Turn SBA back on and estimate each of the views after leaving them out one at a time
		alg.utils.configConvergeSBA.maxIterations = 50;
		for (int i = 0; i < 5; i++) {
			checkPerfect(dbSimilar, dbCams, alg, i);
		}
	}

	private void checkPerfect( MockLookupSimilarImagesRealistic dbSimilar,
							   MockLookUpCameraInfo dbCams,
							   MetricExpandByOneView alg, int targetViewIdx ) {
		PairwiseImageGraph pairwise = dbSimilar.createPairwise();
		SceneWorkingGraph workGraph = dbSimilar.createWorkingGraph(pairwise);

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

		var alg2 = new MetricExpandByOneView() {
			@Override
			boolean computeCalibratingHomography() {
				return false;
			}
		};

		fail_and_doNotAdd(dbSimilar, dbCams, alg1, 0);
		fail_and_doNotAdd(dbSimilar, dbCams, alg2, 0);
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
	 * Check the calibrating homography computation by feeding it noise three data from 3 views
	 */
	@Test void computeCalibratingHomography() {
		var db = new MockLookupSimilarImagesRealistic().
				setIntrinsic(new CameraPinhole(400, 400, 0, 0, 0, 800, 800)).
				pathLine(5, 0.3, 1.5, 2);
		PairwiseImageGraph pairwise = db.createPairwise();

		var alg = new MetricExpandByOneView();
		alg.workGraph = db.createWorkingGraph(pairwise);

		alg.utils.seed = pairwise.nodes.get(0);
		alg.utils.viewB = pairwise.nodes.get(1);
		alg.utils.viewC = pairwise.nodes.get(2);

		int[] viewIdx = new int[]{1, 0, 2};

		// P1 might not be identity
		DMatrixRMaj P1 = db.views.get(viewIdx[0]).camera;
		alg.utils.P2.setTo(db.views.get(viewIdx[1]).camera);
		alg.utils.P3.setTo(db.views.get(viewIdx[2]).camera);
		// make sure P1 is identity, which is what it would be coming out of the trifocal tensor
		List<DMatrixRMaj> cameras = BoofMiscOps.asList(P1.copy(), alg.utils.P2, alg.utils.P3);
		MultiViewOps.projectiveMakeFirstIdentity(cameras, null);

		// Create the pixel observations
		db.createTripleObs(viewIdx, alg.utils.matchesTriple, new DogArray_I32());

		// Compute the homogrpahy
		assertTrue(alg.computeCalibratingHomography());

		// Test it by seeing it it returns the expected camera matrix
		DMatrixRMaj H = alg.projectiveHomography.getCalibrationHomography();
		DMatrixRMaj foundK = new DMatrixRMaj(3, 3);
		Se3_F64 view_0_to_2 = new Se3_F64();
		MultiViewOps.projectiveToMetric(alg.utils.P3, H, view_0_to_2, foundK);

		assertEquals(db.intrinsic.fx, foundK.get(0, 0), 1e-7);
		assertEquals(db.intrinsic.fy, foundK.get(1, 1), 1e-7);
		assertEquals(db.intrinsic.cx, foundK.get(0, 2), 1e-7);
		assertEquals(db.intrinsic.cy, foundK.get(1, 2), 1e-7);
		assertEquals(db.intrinsic.skew, foundK.get(0, 1), 1e-7);
	}

	/**
	 * Checks return value and handles a fatal error
	 */
	@Test void removedBadFeatures_FatalError() {
		// This will always have a fatal error
		var checks = new DummySanityChecks();
		checks.success = false;

		var alg = new MetricExpandByOneView();
		alg.checks = checks;

		var workGraph = new SceneWorkingGraph();

		// The fatal error should be detected early on, processing stop, and the function return false
		assertFalse(alg.removedBadFeatures(workGraph));
	}

	/**
	 * Correctly applies the bad feature threshold that defines an unrecoverable error
	 */
	@Test void removedBadFeatures_BadFeaturesThreshold() {
		var scene = new SceneWorkingGraph();
		var checks = new DummySanityChecks();
		var alg = new MetricExpandByOneView() {
			@Override boolean performBundleAdjustment( SceneWorkingGraph workGraph ) {
				return true; // need this to do nothing
			}
		};

		// If 10% of the features are bad it will try to fix the situation
		alg.fractionBadFeaturesRecover = 0.1;
		alg.checks = checks;

		// need to make the number of inliers 100 so that internal checks pass
		alg.utils.inliersThreeView.resize(100);
		alg.utils.inlierIdx.resize(100);

		// First test will have too many, above the threshold. Second time will have just the right number to pass
		checks.numFeatures = 100;
		checks.numBad = 11;
		assertFalse(alg.removedBadFeatures(scene));
		assertEquals(100, alg.utils.inliersThreeView.size);

		checks.counter = 0;
		checks.numBad = 10;
		assertTrue(alg.removedBadFeatures(scene));
		assertEquals(90, alg.utils.inliersThreeView.size);
		assertEquals(90, alg.utils.inlierIdx.size);

		// The second pass will always be perfect and isn't checked by this function
	}

	private static class DummySanityChecks extends MetricSanityChecks {
		boolean success = true;
		int numFeatures = 100;
		int numBad = 0;

		int counter = 0;

		@Override
		public boolean checkPhysicalConstraints( SceneStructureMetric structure,
												 SceneObservations observations,
												 List<CameraPinholeBrown> listPriors ) {
			if (counter == 0) {
				badFeatures.resetResize(numFeatures, false);
				for (int i = 0; i < numBad; i++) {
					badFeatures.set(i, true);
				}
			} else {
				// second pass will be good
				badFeatures.fill(false);
			}
			counter++;
			return success;
		}
	}
}
