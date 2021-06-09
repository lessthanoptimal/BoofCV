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

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestSelectNeighborsAroundView extends BoofStandardJUnit {

	/**
	 * Test everything all at once. This just checks (crudely) to see if it's a valid sub graph and doesn't check
	 * to see if it's a good sub graph
	 */
	@Test void process() {
		var db = new MockLookupSimilarImagesRealistic().setFeatures(50).pathLine(15, 0.1, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph working = db.createWorkingGraph(pairwise);

		// there needs to be some inlier sets for this to work. Intentionally make this complex and force it
		// to handle situations where the seed has no inliers, some views have no inliers in the candidate set
		createInliers(working.listViews.get(2), 1, 4, working);
		createInliers(working.listViews.get(4), 4, 5, working);
		createInliers(working.listViews.get(7), 6, 8, working);
		createInliers(working.listViews.get(11), 9, 12, working);

		var alg = new SelectNeighborsAroundView();
		alg.maxViews = 6;
		alg.minNeighbors = 3;
		SceneWorkingGraph.View seed = working.listViews.get(6);
		alg.process(seed, working);

		// see if found local graph meets our request
		checkLocalGraph(6, 3, alg, seed);

		// test multiple calls and a different configuration
		alg.maxViews = 7;
		alg.minNeighbors = 1;
		alg.process(seed, working);
		checkLocalGraph(7, 1, alg, seed);
	}

	private void checkLocalGraph( int numViews, int minNeighbors,
								  SelectNeighborsAroundView alg, SceneWorkingGraph.View seed ) {
		SceneWorkingGraph found = alg.getLocalWorking();
		assertEquals(numViews, found.listViews.size());
		int totalNeighbors = 0;
		for (SceneWorkingGraph.View v : found.listViews) {
			if (v.pview.findMotion(seed.pview) != null) {
				totalNeighbors++;
			}
		}
		assertTrue(totalNeighbors >= minNeighbors);
	}

	/**
	 * See if the situation where there is no work view for a pairwise view is handled
	 */
	@Test void process_NullWorkViews() {
		var db = new MockLookupSimilarImagesRealistic().setFeatures(50).
				pathLine(15, 0.1, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph working = db.createWorkingGraph(pairwise);

		// needs to have inliers
		createInliers(working.listViews.get(6), 1, 12, working);

		// remove one view
		SceneWorkingGraph.View removed = working.listViews.get(8);
		working.views.remove(removed.pview.id);

		// if it doesn't blow up that's a success
		var alg = new SelectNeighborsAroundView();
		SceneWorkingGraph.View seed = working.listViews.get(6);
		alg.process(seed, working);
	}

	@Test void addNeighbors2() {
		var db = new MockLookupSimilarImagesRealistic().setFeatures(50).
				pathLine(15, 0.1, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph working = db.createWorkingGraph(pairwise);

		var alg = new SelectNeighborsAroundView();

		// Start with a literal edge case that won't have the max number
		alg.addNeighbors2(working.listViews.get(0), working);
		assertEquals(4, alg.candidates.size());
		assertEquals(5, alg.lookup.size());
		assertEquals(7, alg.edges.size);
		assertFalse(alg.candidates.contains(working.listViews.get(0)));
		for (int id : new int[]{1, 2, 3, 4}) {
			SceneWorkingGraph.View v = working.listViews.get(id);
			assertTrue(alg.lookup.containsKey(v.pview.id));
			assertTrue(alg.candidates.contains(v));
		}

		// Try another in the middle
		alg.initialize();
		alg.addNeighbors2(working.listViews.get(4), working);
		assertEquals(8, alg.candidates.size());
		assertEquals(9, alg.lookup.size());
		assertEquals(15, alg.edges.size);
		assertFalse(alg.candidates.contains(working.listViews.get(4)));
		for (int id : new int[]{0, 1, 2, 3, 5, 6, 7, 8}) {
			SceneWorkingGraph.View v = working.listViews.get(id);
			assertTrue(alg.lookup.containsKey(v.pview.id));
			assertTrue(alg.candidates.contains(v));
		}
	}

	@Test void pruneViews() {
		var db = new MockLookupSimilarImagesRealistic().setFeatures(50).
				pathLine(15, 0.1, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph working = db.createWorkingGraph(pairwise);

		SceneWorkingGraph.View seed = working.listViews.get(4);
		seed.pview.connections.forEach(m -> m.score3D += 1000); // make it not want to select immediate neighbors
		var alg = new SelectNeighborsAroundView();

		// for the first test we want to prune NOTHING
		{
			// initialize internal data structures. Easier to use existing code
			alg.initialize();
			alg.addNeighbors2(seed, working);

			alg.maxViews = alg.lookup.size();
			alg.pruneViews(seed);
			assertEquals(alg.maxViews - 1, alg.candidates.size());
		}

		// prune one element, but mark a motion as having a very low score so we know which views it should be
		{
			alg.maxViews -= 1;
			PairwiseImageGraph.Motion m = seed.pview.connections.get(0);
			m.score3D = 1.0; // make it a prime target to be removed
			SceneWorkingGraph.View expected = alg.lookup.get(m.other(seed.pview).id);
			alg.initialize();
			alg.addNeighbors2(seed, working);
			// prune and see if the expected one was removed
			alg.pruneViews(seed);
			// we know expected got prune because it directly touches the seed
			assertEquals(alg.maxViews - 1, alg.candidates.size());
			assertFalse(alg.candidates.contains(expected));
		}

		// Prune several elements. It might not hit the target exactly since orphans can be pruned
		{
			seed.pview.connections.get(0).score3D = 1003;
			alg.maxViews = 5;
			alg.initialize();
			alg.addNeighbors2(seed, working);
			alg.pruneViews(seed);
			assertTrue(alg.maxViews - 1 - alg.candidates.size() <= 1);
		}

		// Prune until the point we hit minNeighbors, meaning only neighbors should remain
		// NOTE: There are conditions that can arise where this will not be the case. These unit tests
		// should be made more robust.
		{
			alg.maxViews = alg.minNeighbors;
			alg.initialize();
			alg.addNeighbors2(seed, working);
			alg.pruneViews(seed);
			assertEquals(alg.maxViews - 1, alg.candidates.size());
			for (var v : alg.candidates) {
				assertNotNull(v.pview.findMotion(seed.pview));
			}
		}
	}

	@Test void scoreForRemoval() {
		var alg = new SelectNeighborsAroundView();
		// It will return the score of the second best
		alg.worstOfTop = 2;

		// First case there will be no motions that can be scored and make sure it ignores the ignore motion
		var v = new PairwiseImageGraph.View();
		var m0 = new PairwiseImageGraph.Motion();
		m0.score3D = 999;
		v.connections.add(m0);
		assertEquals(0.0, alg.scoreForRemoval(v, m0));

		// There will be only one motion which is valid
		addViewForRemoval("1", v, 25, alg);
		assertEquals(25.0, alg.scoreForRemoval(v, m0));

		addViewForRemoval("2", v, 90, alg);
		assertEquals(25.0, alg.scoreForRemoval(v, m0));

		addViewForRemoval("3", v, 15, alg);
		assertEquals(25.0, alg.scoreForRemoval(v, m0));

		addViewForRemoval("3", v, 100, alg);
		assertEquals(90.0, alg.scoreForRemoval(v, m0));
	}

	private void addViewForRemoval( String id, PairwiseImageGraph.View src, double motionScore,
									SelectNeighborsAroundView alg ) {
		var workView = new SceneWorkingGraph.View();
		workView.pview = new PairwiseImageGraph.View();
		workView.pview.id = id;
		var m = new PairwiseImageGraph.Motion();
		m.score3D = motionScore;
		m.src = src;
		m.dst = workView.pview;
		alg.addCandidateView(workView);
		src.connections.add(m);
		workView.pview.connections.add(m);
	}

	@Test void removeCandidateNode() {
		var db = new MockLookupSimilarImagesRealistic().setFeatures(50).
				pathLine(15, 0.1, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph working = db.createWorkingGraph(pairwise);

		var alg = new SelectNeighborsAroundView();
		alg.minNeighbors = 2;
		var seed = working.listViews.get(5);
		var target = working.listViews.get(4);
		// easier to construct internal data structures this way
		alg.addNeighbors2(seed, working);

		// Remove the specified ID
		alg.removeCandidateNode(target.pview.id, seed);
		assertEquals(7, alg.candidates.size());
		assertEquals(8, alg.lookup.size());
		//  make sure no edges are connected to it
		for (var e : alg.edges.toList()) {
			assertFalse(e.m.isConnected(target.pview));
		}

		// Now let's trigger the orphan removal by removing everything that would be connected to 1
		for (int i = 2; i < 4; i++) {
			alg.removeCandidateNode(working.listViews.get(i).pview.id, seed);
		}
		assertEquals(4, alg.candidates.size());
		assertEquals(5, alg.lookup.size());
	}

	@Test void isOrphan() {
		var db = new MockLookupSimilarImagesRealistic().setFeatures(50).
				pathLine(15, 0.1, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph working = db.createWorkingGraph(pairwise);

		var alg = new SelectNeighborsAroundView();

		// The 'lookup' map isn't populated so everything is an orphan
		SceneWorkingGraph.View v1 = working.listViews.get(6);
		assertTrue(alg.isOrphan(v1));

		// Adding v1 to the known list shouldn't make it not an orphan
		alg.lookup.put(v1.pview.id, v1);
		assertTrue(alg.isOrphan(v1));

		// Adding v2 will make them both not orphans
		SceneWorkingGraph.View v2 = working.listViews.get(7);
		assertFalse(alg.isOrphan(v2));
		alg.lookup.put(v2.pview.id, v2);
		assertFalse(alg.isOrphan(v1));
	}

	@Test void createLocalGraph() {
		var db = new MockLookupSimilarImagesRealistic().setFeatures(50).
				pathLine(15, 0.1, 1.5, 2);

		PairwiseImageGraph pairwise = db.createPairwise();
		SceneWorkingGraph working = db.createWorkingGraph(pairwise);
		// Fill in arbitrary values for everything that has not already been filled in and will be checked
		working.listCameras.forIdx(( idx, c ) -> c.prior.width = 97 + idx);

		SceneWorkingGraph.View seed = working.listViews.get(6);

		var alg = new SelectNeighborsAroundView();
		alg.initialize();
		alg.addNeighbors2(seed, working);

		// it should fail here since there are no inliers
		try {
			alg.createLocalGraph(seed, working);
			fail("Should have failed");
		} catch (RuntimeException ignore) {
		}

		// Create an inlier set for all views in the local graph
		// put ever view into this inlier set, except for 10. For that it will need to go outside
		createInliers(seed, 2, 9, working);
		// Inliers for 10 come from 11
		createInliers(working.listViews.get(11), 10, 12, working);
		// Create the local graph
		alg.initialize();
		alg.addNeighbors2(seed, working);
		alg.createLocalGraph(seed, working);
		// examine the results
		SceneWorkingGraph localGraph = alg.localWorking;
		assertEquals(alg.candidates.size() + 1, localGraph.listViews.size());

		assertEquals(localGraph.cameras.size(), working.cameras.size());
		for (int i = 0; i < working.listCameras.size; i++) {
			SceneWorkingGraph.Camera c = working.listCameras.get(i);
			SceneWorkingGraph.Camera lc = localGraph.cameras.get(c.indexDB);
			assertEquals(lc.prior.width, c.prior.width);
		}

		for (int i = 2; i <= 10; i++) {
			SceneWorkingGraph.View v = working.listViews.get(i);
			SceneWorkingGraph.View lv = localGraph.views.get(v.pview.id);
			assertNotNull(lv);

			assertEquals(lv.world_to_view.T.x, v.world_to_view.T.x);

			if (i == 6) {
				assertEquals(8, lv.inliers.get(0).views.size);
			} else if (i == 10) {
				assertEquals(1, lv.inliers.get(0).views.size);
			} else {
				assertTrue(lv.inliers.isEmpty());
			}
		}
	}

	private void createInliers( SceneWorkingGraph.View v, int idx0, int idx1,
								SceneWorkingGraph working ) {
		// put ever view into this inlier set, except for 10. For that it will need to go outside
		for (int i = idx0; i <= idx1; i++) {
			PairwiseImageGraph.View pv = working.listViews.get(i).pview;
			SceneWorkingGraph.InlierInfo inlier = v.inliers.isEmpty() ? v.inliers.grow() : v.inliers.get(0);
			inlier.views.add(pv);
			DogArray_I32 obs = inlier.observations.grow();
			for (int j = 0; j < 5 + i; j++) {
				obs.add(j);
			}
		}
	}
}
