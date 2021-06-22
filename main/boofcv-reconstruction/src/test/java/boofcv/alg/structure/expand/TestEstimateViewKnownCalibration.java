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

import boofcv.alg.structure.MockLookupSimilarImagesRealistic;
import boofcv.alg.structure.PairwiseGraphUtils;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEstimateViewKnownCalibration extends BoofStandardJUnit {
	/**
	 * Generate a known scene and pass in everything but the location of the 3rd view. Make sure
	 * it's able to estimate it. A couple of outliers will be added and checked to make sure they are removed.
	 */
	@Test void simpleKnownScenario() {
		var db = new MockLookupSimilarImagesRealistic().
				pathLine(5, 0.3, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph scene = db.createWorkingGraph(pairwise);

		var pairwiseUtils = new PairwiseGraphUtils();
		pairwiseUtils.dbSimilar = db;
		pairwiseUtils.dbCams = db.createLookUpCams();

		// Specify the 3-views it should use. viewC = target that's estimated
		pairwiseUtils.seed = pairwise.nodes.get(1);
		pairwiseUtils.viewB = pairwise.nodes.get(2);
		pairwiseUtils.viewC = pairwise.nodes.get(3);

		// Use truth for the priors
		pairwiseUtils.priorCamA.setTo(db.intrinsic);
		pairwiseUtils.priorCamB.setTo(db.intrinsic);
		pairwiseUtils.priorCamC.setTo(db.intrinsic);

		// Specifies the observations for the 3-views
		SceneWorkingGraph.View wviewA = scene.listViews.get(1);
		db.addInlierInfo(pairwise, wviewA, 2,3);

		// Create the triplet of pixel observations using the "inlier" set created above
		SceneWorkingGraph.InlierInfo info = Objects.requireNonNull(wviewA.getBestInliers());
		int numInliers = info.getInlierCount();
		pairwiseUtils.inlierIdx.resize(numInliers, (idx)->idx);
		assertTrue(numInliers > 20);
		for (int i = 0; i < numInliers; i++) {
			int obs1 = info.observations.get(0).get(i);
			int obs2 = info.observations.get(1).get(i);
			int obs3 = info.observations.get(2).get(i);

			AssociatedTriple a = new AssociatedTriple();
			a.p1.setTo(db.views.get(1).observations.get(obs1).pixel);
			a.p2.setTo(db.views.get(2).observations.get(obs2).pixel);
			a.p3.setTo(db.views.get(3).observations.get(obs3).pixel);

			pairwiseUtils.inliersThreeView.add(a);
		}

		// Pixels must be centered at (0,0)
		for (int i = 0; i < numInliers; i++) {
			AssociatedTriple a = pairwiseUtils.inliersThreeView.get(i);
			a.p1.x -= db.intrinsic.cx;
			a.p1.y -= db.intrinsic.cy;
			a.p2.x -= db.intrinsic.cx;
			a.p2.y -= db.intrinsic.cy;
			a.p3.x -= db.intrinsic.cx;
			a.p3.y -= db.intrinsic.cy;
		}

		var alg = new EstimateViewKnownCalibration();
//		alg.setVerbose(System.out, BoofMiscOps.hashSet(BoofVerbose.RECURSIVE));
		var solution = new MetricExpandByOneView.Solution();
		assertTrue(alg.process(pairwiseUtils, scene, solution));

		// See if the transforms are the same
		Se3_F64 expected = scene.listViews.get(3).world_to_view;
		assertEquals(0.0, expected.T.distance(solution.world_to_target.T), 1e-2);
		// NOTE: The tolerance does seem a bit high for noise free observations. Could there be numeric issues?
	}
}
