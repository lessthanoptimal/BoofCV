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

import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.MultiViewOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofTesting;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestMetricFromUncalibratedPairwiseGraph {

	// testing tolerance for focal length on perfect input data
	double focalTol = 1e-2;
	// testing tolernace for location on perfect data
	double locationTol = 1e-3;

	/**
	 * Random scene with fairly linear motion. Everything is randomized and physical constraints on camera are enforced
	 */
	@Test
	void process_perfect() {
		// NOTE: Self calibration assumes image center is the principle point and that fx==fy and zero skew. Make
		// sure truth matches that assumption

		var alg = new MetricFromUncalibratedPairwiseGraph();
//		alg.setVerbose(System.out, null);
		for (int numViews = 3; numViews <= 23; numViews += 5) {
			// Need to increase the number of features to ensure everything is connected properly and that there is
			// enough info for a good estimate
			var db = new MockLookupSimilarImagesRealistic().setLoop(false).
					setIntrinsic(new CameraPinhole(410,410,0,400,400,800,800)).
					setSeed(numViews).setFeatures(Math.max(400,50*numViews)).pathLine(numViews,0.30,6.0,2);
			PairwiseImageGraph2 graph = db.createPairwise();
			assertTrue(alg.process(db,graph));
			checkReconstruction(alg,db);
		}
	}

	/**
	 * Compare found camera matrices against truth by converting them into the same projective scale
	 */
	private void checkReconstruction(MetricFromUncalibratedPairwiseGraph alg, MockLookupSimilarImagesRealistic db) {
		List<SceneWorkingGraph.View> foundViews = alg.workGraph.getAllViews();
		assertEquals(db.views.size(), foundViews.size());

		//------------- Check intrinsic parameters
		for (SceneWorkingGraph.View v : foundViews) {
			assertEquals(db.intrinsic.fx, v.pinhole.fx, focalTol);
			assertEquals(db.intrinsic.fy, v.pinhole.fy, focalTol);
			assertEquals(0.0, v.pinhole.skew, 1e-8);
			assertEquals(0.0, v.pinhole.cx, 1e-8);
			assertEquals(0.0, v.pinhole.cy, 1e-8);
		}

		//------------- Check camera motion up to scale
		// The the origin can float in truth and found so let's pick a view arbitrarily and make that the origin
		String originID = db.views.get(0).id;
		Se3_F64 fndWorld_to_origin = alg.workGraph.lookupView(originID).world_to_view;
		Se3_F64 expWorld_to_origin = db.views.get(0).world_to_view;

		for( var trueView : db.views ) {
			SceneWorkingGraph.View wview = alg.workGraph.lookupView(trueView.id);
			Se3_F64 found    = wview.world_to_view.invert(null).concat(fndWorld_to_origin,null);
			Se3_F64 expected = trueView.world_to_view.invert(null).concat(expWorld_to_origin,null);

			// These are only equal up to a scale + sign ambiguity
			double scale = MultiViewOps.findScale(found.T,expected.T);
			found.T.scale(scale);

			BoofTesting.assertEquals(expected,found,0.001,locationTol);
		}

		// Should also check the inliers to see if they make sense.
	}

	@Test
	void saveMetricSeed() {
		var graph = new PairwiseImageGraph2();
		List<String> viewIds = BoofMiscOps.asList("A","B","C");
		var inlierToSeed = GrowQueue_I32.array(1,3,5,7,9);
		var inlierToOther = new FastQueue<>(GrowQueue_I32::new,GrowQueue_I32::reset);

		// create distintive sets of inlier indexes for each view
		for (int otherIdx = 0; otherIdx < viewIds.size()-1; otherIdx++) {
			GrowQueue_I32 inliers = inlierToOther.grow();
			for (int i = 0; i < inlierToSeed.size; i++) {
				inliers.add(inlierToSeed.get(i)+1+otherIdx);
			}
		}

		// Create some arbitrary metric results that should be saved
		var results = new MetricCameras();
		for (int viewIdx = 0; viewIdx < viewIds.size(); viewIdx++) {
			// skip zero since it's implicit
			if( viewIdx > 0 )
				results.motion_1_to_k.grow().T.set(1,viewIdx,0);
			results.intrinsics.grow().fx = 100+viewIdx;
			graph.createNode(viewIds.get(viewIdx));
		}

		var alg = new MetricFromUncalibratedPairwiseGraph();
		alg.saveMetricSeed(graph,viewIds,inlierToSeed,inlierToOther,results);
		SceneWorkingGraph wgraph = alg.getWorkGraph();

		// See metric view info got saved correctly
		BoofMiscOps.forIdx(viewIds,(idx,viewId)->{
			PairwiseImageGraph2.View pview = graph.lookupNode(viewId);
			assertTrue(wgraph.isKnown(pview));
			SceneWorkingGraph.View wview = wgraph.lookupView(viewId);

			assertEquals(100+idx,wview.pinhole.fx, 1e-8);
			assertEquals(idx==0?0:1,wview.world_to_view.T.x, 1e-8);
			assertEquals(idx,wview.world_to_view.T.y, 1e-8);
		});

		// See if inliers got saved correctly
		BoofMiscOps.forIdx(viewIds,(idx,viewId)->{
			SceneWorkingGraph.View wview = wgraph.lookupView(viewId);

			if( idx != 0 ) {
				// only the first view (the seed) should have inliers saved
				assertEquals(0,wview.inliers.views.size);
				assertEquals(0,wview.inliers.observations.size);
				return;
			}

			assertEquals(viewIds.size(),wview.inliers.views.size);
			assertEquals(viewIds.size(),wview.inliers.observations.size);
			assertEquals(viewId,wview.inliers.views.get(0).id);
			for (int checkIdx = 0; checkIdx < viewIds.size(); checkIdx++) {
				final int c = checkIdx;
				GrowQueue_I32 obs = wview.inliers.observations.get(checkIdx);
				obs.forIdx((i,value)-> assertEquals(i*2+1+c,value));
			}
		});
	}
}