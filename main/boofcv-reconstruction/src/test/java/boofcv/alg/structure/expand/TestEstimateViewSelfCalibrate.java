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

package boofcv.alg.structure.expand;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.structure.MockLookupSimilarImagesRealistic;
import boofcv.alg.structure.PairwiseGraphUtils;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEstimateViewSelfCalibrate extends BoofStandardJUnit {
	/**
	 * Check the calibrating homography computation by feeding it noise three data from 3 views
	 */
	@Test void computeCalibratingHomography() {
		var db = new MockLookupSimilarImagesRealistic().
				setIntrinsic(new CameraPinhole(400, 400, 0, 0, 0, 800, 800)).
				pathLine(5, 0.3, 1.5, 2);
		PairwiseImageGraph pairwise = db.createPairwise();

		var alg = new EstimateViewSelfCalibrate();
		alg.workGraph = db.createWorkingGraph(pairwise);
		alg.pairwiseUtils = new PairwiseGraphUtils();

		alg.pairwiseUtils.seed = pairwise.nodes.get(0);
		alg.pairwiseUtils.viewB = pairwise.nodes.get(1);
		alg.pairwiseUtils.viewC = pairwise.nodes.get(2);

		int[] viewIdx = new int[]{1, 0, 2};

		// P1 might not be identity
		DMatrixRMaj P1 = db.views.get(viewIdx[0]).camera;
		alg.pairwiseUtils.P2.setTo(db.views.get(viewIdx[1]).camera);
		alg.pairwiseUtils.P3.setTo(db.views.get(viewIdx[2]).camera);
		// make sure P1 is identity, which is what it would be coming out of the trifocal tensor
		List<DMatrixRMaj> cameras = BoofMiscOps.asList(P1.copy(), alg.pairwiseUtils.P2, alg.pairwiseUtils.P3);
		MultiViewOps.projectiveMakeFirstIdentity(cameras, null);

		// Create the pixel observations
		db.createTripleObs(viewIdx, alg.pairwiseUtils.matchesTriple, new DogArray_I32());

		// Compute the homography
		assertTrue(alg.computeCalibratingHomography());

		// Test it by seeing it it returns the expected camera matrix
		DMatrixRMaj H = alg.projectiveHomography.getCalibrationHomography();
		DMatrixRMaj foundK = new DMatrixRMaj(3, 3);
		Se3_F64 view_0_to_2 = new Se3_F64();
		MultiViewOps.projectiveToMetric(alg.pairwiseUtils.P3, H, view_0_to_2, foundK);

		assertEquals(db.intrinsic.fx, foundK.get(0, 0), 1e-7);
		assertEquals(db.intrinsic.fy, foundK.get(1, 1), 1e-7);
		assertEquals(db.intrinsic.cx, foundK.get(0, 2), 1e-7);
		assertEquals(db.intrinsic.cy, foundK.get(1, 2), 1e-7);
		assertEquals(db.intrinsic.skew, foundK.get(0, 1), 1e-7);
	}
}
