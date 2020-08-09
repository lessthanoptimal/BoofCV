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

import boofcv.alg.geo.MultiViewOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofTesting;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMetricExpandByOneView {
	/**
	 * Perfect inputs that should yield perfect results.
	 */
	@Test
	void perfect() {
		// make sure (cx,cy) = (width/2, height/2) or else unit test will fail because it doesn't perfectly
		// match the model
		var db = new MockLookupSimilarImagesRealistic().
				setIntrinsic(new CameraPinhole(400,420,0,400,400,800,800)).
				pathLine(5,0.3,1.5,2);
		var alg = new MetricExpandByOneView();

		// Perfect without SBA - Will normally not be done this way, but with perfect data it should return
		// perfect results and will highlight bugs that might be hidden by SBA
		alg.utils.configConvergeSBA.maxIterations = 0;
		for (int i = 0; i < 5; i++) {
			checkPerfect(db, alg, i);
		}

		// Turn SBA back on and estimate each of the views after leaving them out one at a time
		alg.utils.configConvergeSBA.maxIterations = 50;
		for (int i = 0; i < 5; i++) {
			checkPerfect(db, alg, i);
		}
	}

	private void checkPerfect(MockLookupSimilarImagesRealistic db,
							  MetricExpandByOneView alg, int targetViewIdx) {
		PairwiseImageGraph2 pairwise = db.createPairwise();
		SceneWorkingGraph workGraph = db.createWorkingGraph(pairwise);
		// make the estimated intrinsic have zero principle point
		workGraph.viewList.forEach(v-> v.pinhole.cx = v.pinhole.cy = 0.0);

		// Decide which view will be estimated
		PairwiseImageGraph2.View target = pairwise.nodes.get(targetViewIdx);

		// remove the view from the work graph
		workGraph.viewList.remove(workGraph.views.remove(target.id));

		// add the target view
		assertTrue(alg.process(db,workGraph,target));

		SceneWorkingGraph.View found = workGraph.views.get(target.id);

		// Check calibration
		assertEquals(db.intrinsic.fx, found.pinhole.fx, 1e-4);
		assertEquals(db.intrinsic.fy, found.pinhole.fy, 1e-4);
		assertEquals(0.0, found.pinhole.cx, 1e-4);
		assertEquals(0.0, found.pinhole.cy, 1e-4);

		// Check pose
		BoofTesting.assertEquals(db.views.get(targetViewIdx).world_to_view,found.world_to_view,0.01,0.01);
	}

	/**
	 * When it fails to find the metric upgrade make sure it doesn't add it to th work graph
	 */
	@Test
	void fail_and_doNotAdd() {
		var db = new MockLookupSimilarImagesRealistic().
				setIntrinsic(new CameraPinhole(400,420,0,400,400,800,800)).
				pathLine(5,0.3,1.5,2);

		// force it to fail at these two different points
		var alg1 = new MetricExpandByOneView() {
			public boolean selectTwoConnections(PairwiseImageGraph2.View target ,
												List<PairwiseImageGraph2.Motion> connections ) {
				return false;
			}
		};

		var alg2 = new MetricExpandByOneView() {
			boolean computeCalibratingHomography() { {
				return false;
			}}
		};

		fail_and_doNotAdd(db,alg1,0);
		fail_and_doNotAdd(db,alg2,0);
	}

	private void fail_and_doNotAdd(MockLookupSimilarImagesRealistic db,
							  MetricExpandByOneView alg, int targetViewIdx) {
		PairwiseImageGraph2 pairwise = db.createPairwise();
		SceneWorkingGraph workGraph = db.createWorkingGraph(pairwise);
		// make the estimated intrinsic have zero principle point
		workGraph.viewList.forEach(v-> v.pinhole.cx = v.pinhole.cy = 0.0);

		// Decide which view will be estimated
		PairwiseImageGraph2.View target = pairwise.nodes.get(targetViewIdx);

		workGraph.viewList.remove(workGraph.views.remove(target.id));

		// This should fail and not add it to the work graph
		assertFalse(alg.process(db,workGraph,target));
		assertFalse(workGraph.isKnown(target));
	}

	/**
	 * Check the calibrating homography computation by feeding it noise three data from 3 views
	 */
	@Test
	void computeCalibratingHomography() {
		var db = new MockLookupSimilarImagesRealistic().pathLine(5,0.3,1.5,2);
		PairwiseImageGraph2 pairwise = db.createPairwise();

		var alg = new MetricExpandByOneView();
		alg.workGraph = db.createWorkingGraph(pairwise);

		alg.utils.seed = pairwise.nodes.get(0);
		alg.utils.viewB = pairwise.nodes.get(1);
		alg.utils.viewC = pairwise.nodes.get(2);

		int[] viewIdx = new int[]{1,0,2};

		// P1 might not be identity
		DMatrixRMaj P1 = db.views.get(viewIdx[0]).camera;
		alg.utils.P2.set(db.views.get(viewIdx[1]).camera);
		alg.utils.P3.set(db.views.get(viewIdx[2]).camera);
		// make sure P1 is identity, which is what it would be coming out of the trifocal tensor
		List<DMatrixRMaj> cameras = BoofMiscOps.asList(P1.copy(),alg.utils.P2,alg.utils.P3);
		MultiViewOps.projectiveMakeFirstIdentity(cameras,null);

		// Create the pixel observations
		db.createTripleObs(viewIdx,alg.utils.matchesTriple,new GrowQueue_I32());

		// Compute the homogrpahy
		assertTrue(alg.computeCalibratingHomography());

		// Test it by seeing it it returns the expected camera matrix
		DMatrixRMaj H = alg.projectiveHomography.getCalibrationHomography();
		DMatrixRMaj foundK = new DMatrixRMaj(3,3);
		Se3_F64 view_0_to_2 = new Se3_F64();
		MultiViewOps.projectiveToMetric(alg.utils.P3,H,view_0_to_2,foundK);

		assertEquals(db.intrinsic.fx, foundK.get(0,0), 1e-7);
		assertEquals(db.intrinsic.fy, foundK.get(1,1), 1e-7);
		assertEquals(db.intrinsic.cx, foundK.get(0,2), 1e-7);
		assertEquals(db.intrinsic.cy, foundK.get(1,2), 1e-7);
		assertEquals(db.intrinsic.skew, foundK.get(0,1), 1e-7);
	}
}