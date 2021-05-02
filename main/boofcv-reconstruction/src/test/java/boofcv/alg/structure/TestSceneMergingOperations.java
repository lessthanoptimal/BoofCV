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

import boofcv.alg.structure.MetricFromUncalibratedPairwiseGraph.PairwiseViewScenes;
import boofcv.alg.structure.MetricFromUncalibratedPairwiseGraph.ViewScenes;
import boofcv.alg.structure.SceneMergingOperations.SceneCommonCounts;
import boofcv.alg.structure.SceneMergingOperations.SelectedScenes;
import boofcv.alg.structure.SceneMergingOperations.SelectedViews;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.util.PrimitiveArrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestSceneMergingOperations extends BoofStandardJUnit {
	@Test void initializeViewCounts() {
		var viewScenes = new PairwiseViewScenes();
		// Every view is seen by every Scene
		for (int viewIdx = 0; viewIdx < 6; viewIdx++) {
			ViewScenes v = viewScenes.views.grow();
			v.viewedBy.resize(4, ( idx ) -> idx + 1);
		}
		// Remove some scenes from view 1
		viewScenes.views.get(1).viewedBy.remove(2, 3);

		// Randomize the order
		viewScenes.views.forEach(v -> PrimitiveArrays.shuffle(v.viewedBy.data, 0, v.viewedBy.size, rand));

		// Process and compute how many views each scene has in common
		var alg = new SceneMergingOperations();
		alg.initializeViewCounts(viewScenes, 5);

		// All scenes should be enabled
		assertEquals(5, alg.enabledScenes.size);
		alg.enabledScenes.forEach(Assertions::assertTrue);

		assertEquals(5, alg.commonViewCounts.size);
		assertEquals(0, alg.commonViewCounts.get(0).size); // never added to any of the views
		assertEquals(3, alg.commonViewCounts.get(1).size);
		assertEquals(2, alg.commonViewCounts.get(2).size);
		assertEquals(1, alg.commonViewCounts.get(3).size);
		assertEquals(0, alg.commonViewCounts.get(4).size);

		// Check the scenes with common views
		alg.commonViewCounts.get(1).forIdx(( idx, v ) -> assertEquals(idx + 2, v.sceneIndex));
		alg.commonViewCounts.get(2).forIdx(( idx, v ) -> assertEquals(idx + 3, v.sceneIndex));
		alg.commonViewCounts.get(3).forIdx(( idx, v ) -> assertEquals(idx + 4, v.sceneIndex));

		// Check their counts
		assertEquals(6, alg.commonViewCounts.get(1).get(0).counts);
		assertEquals(5, alg.commonViewCounts.get(1).get(1).counts);
		assertEquals(5, alg.commonViewCounts.get(1).get(2).counts); // these were removed from view-1
	}

	@Test void selectViewsToMerge() {
		var alg = new SceneMergingOperations();
		var selected = new SelectedScenes();

		// Empty list so it should fail
		assertFalse(alg.selectViewsToMerge(selected));

		// create a set of observations with a known solution
		alg.commonViewCounts.resize(6);
		alg.enabledScenes.resize(6, true);
		alg.commonViewCounts.get(1).resize(4, ( idx, v ) -> v.sceneIndex = idx + 2);
		alg.commonViewCounts.get(3).resize(1, ( idx, v ) -> v.sceneIndex = idx + 4);

		alg.commonViewCounts.get(1).get(2).counts = 4;
		alg.commonViewCounts.get(3).get(0).counts = 8;

		assertTrue(alg.selectViewsToMerge(selected));
		assertEquals(3, selected.sceneA);
		assertEquals(4, selected.sceneB);
	}

	/**
	 * Make sure it ignores disabled scenes
	 */
	@Test void selectViewsToMerge_disabled() {
		var alg = new SceneMergingOperations();
		var selected = new SelectedScenes();

		// create a set of observations with a known solution
		alg.commonViewCounts.resize(6);
		alg.commonViewCounts.get(1).resize(4, ( idx, v ) -> v.sceneIndex = idx + 2);
		alg.commonViewCounts.get(3).resize(1, ( idx, v ) -> v.sceneIndex = idx + 4);
		alg.commonViewCounts.get(1).get(2).counts = 4;
		alg.commonViewCounts.get(3).get(0).counts = 8;

		// Disable every view. This should cause it to fail
		alg.enabledScenes.resize(6, false);
		assertFalse(alg.selectViewsToMerge(selected));

		// enable just one view. Should still fail since it can't merge with anything
		alg.enabledScenes.set(3, true);
		assertFalse(alg.selectViewsToMerge(selected));

		// enable one more scene. it should work now
		alg.enabledScenes.set(4, true);
		assertTrue(alg.selectViewsToMerge(selected));
		assertEquals(3, selected.sceneA);
		assertEquals(4, selected.sceneB);
	}

	@Test void decideFirstIntoSecond() {
		var scene1 = new SceneWorkingGraph();
		var scene2 = new SceneWorkingGraph();

		// First scene will have more and the second should be merged in to it
		for (int i = 0; i < 3; i++) {
			scene1.listViews.add(new SceneWorkingGraph.View());
		}
		var alg = new SceneMergingOperations();
		assertFalse(alg.decideFirstIntoSecond(scene1, scene2));

		// second scene will now have more
		for (int i = 0; i < 4; i++) {
			scene2.listViews.add(new SceneWorkingGraph.View());
		}
		assertTrue(alg.decideFirstIntoSecond(scene1, scene2));

		// If there are equal views that's undefined and doesn't matter which one wins
	}

	/**
	 * Handle nominal cases.
	 */
	@Test void mergeViews() {
		var alg = new SceneMergingOperations();
		var src = new SceneWorkingGraph();
		var dst = new SceneWorkingGraph();

		var src_to_dst = new ScaleSe3_F64();
		src_to_dst.scale = 2.5;
		src_to_dst.transform.T.x = 10;

		// Add views. Some will be common and some will not be
		for (int i = 0; i < 5; i++) {
			var a = new PairwiseImageGraph.View();
			var b = new PairwiseImageGraph.View();

			int j = i + 3;
			a.id = "" + i;
			b.id = "" + j; // 2 of the views will have the same ID

			SceneWorkingGraph.View wa = src.addView(a);
			SceneWorkingGraph.View wb = dst.addView(b);

			wa.imageDimension.setTo(i, i);
			wb.imageDimension.setTo(j, j);

			wa.intrinsic.f = i;
			wb.intrinsic.f = j;

			wa.world_to_view.T.setTo(i, 0, 0);
		}

		// Call the function being tested
		alg.mergeViews(src, dst, src_to_dst);

		// src has 3 views NOT in dst
		assertEquals(8, dst.listViews.size());

		// make sure the views which were the same were not modified
		assertEquals(0.0, dst.views.get("3").world_to_view.T.x);
		assertEquals(0.0, dst.views.get("4").world_to_view.T.x);

		// Views that were unique to src should have been converted
		for (int i = 0; i < 3; i++) {
			String id = "" + i;
			assertEquals(i*2.5 - 10.0, dst.views.get(id).world_to_view.T.x);
			assertEquals(i, dst.views.get(id).intrinsic.f);
		}
	}

	/**
	 * Test that makes sure the src is copied into the dst only if it has a better estimate
	 */
	@Test void mergeViews_CopySrcOnlyIfBetter() {
		var alg = new SceneMergingOperations();
		var src = new SceneWorkingGraph();
		var dst = new SceneWorkingGraph();

		// transform will do nothing
		var src_to_dst = new ScaleSe3_F64();

		// Add views. Some will be common and some will not be
		for (int i = 0; i < 5; i++) {
			var a = new PairwiseImageGraph.View();
			var b = new PairwiseImageGraph.View();

			// Every view will overlap
			a.id = "" + i;
			b.id = "" + i;

			SceneWorkingGraph.View wa = src.addView(a);
			SceneWorkingGraph.View wb = dst.addView(b);

			// give each view a distinctive value that makes it easy to see which one "won"
			wa.imageDimension.setTo(1, 1);
			wb.imageDimension.setTo(2, 2);

			wa.intrinsic.f = 1;
			wb.intrinsic.f = 2;

			wa.world_to_view.T.setTo(1, 0, 0);
			wb.world_to_view.T.setTo(2, 0, 0);

			// alternate which view will have a better score
			wa.inliers.grow().scoreGeometric = i%2 == 0 ? 100 : 50;
			wb.inliers.grow().scoreGeometric = 75;
		}

		// Call the function being tested
		alg.mergeViews(src, dst, src_to_dst);

		assertEquals(5, dst.listViews.size());

		for (int i = 0; i < 5; i++) {
			SceneWorkingGraph.View wb = dst.lookupView("" + i);

			assertEquals(2, wb.inliers.size);

			// src will dominate on even views
			if (i%2 == 0) {
				assertEquals(1, wb.imageDimension.width);
				assertEquals(1, wb.intrinsic.f);
				assertEquals(1, wb.world_to_view.T.x);
			} else {
				assertEquals(2, wb.imageDimension.width);
				assertEquals(2, wb.intrinsic.f);
				assertEquals(2, wb.world_to_view.T.x);
			}
		}
	}

	@Test void selectViewsToEstimateTransform() {
		var src = new SceneWorkingGraph();
		var dst = new SceneWorkingGraph();

		var src_to_dst = new Se3_F64();
		SpecialEuclideanOps_F64.eulerXyz(1, 2, -1, 0, 0, 0, src_to_dst);

		// All views are common with a noise free fixed transform between the two scenes
		for (int i = 0; i < 5; i++) {
			var a = new PairwiseImageGraph.View();
			var b = new PairwiseImageGraph.View();

			a.id = "" + i;
			b.id = a.id;

			SceneWorkingGraph.View wa = src.addView(a);
			SceneWorkingGraph.View wb = dst.addView(b);

			// The best pair will be the pair with the highest minimum score
			wa.inliers.grow().scoreGeometric = 11 - i;
			wb.inliers.grow().scoreGeometric = i;
		}

		var found = new SelectedViews();
		var alg = new SceneMergingOperations();
		alg.selectViewsToEstimateTransform(src, dst, found);

		assertSame(src.listViews.get(4), found.src);
		assertSame(dst.listViews.get(4), found.dst);
	}

	@Test void toggleViewEnabled() {
		int numScenes = 4;
		int numViews = 6;
		var viewScenes = new PairwiseViewScenes();
		// Every view is seen by every Scene
		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			ViewScenes v = viewScenes.views.grow();
			v.viewedBy.resize(numScenes, ( idx ) -> idx);
		}

		// Create the scene which will be adjusted
		var scene = new SceneWorkingGraph();
		scene.index = 1;
		for (int i = 0; i < viewScenes.views.size; i++) {
			var wv = new SceneWorkingGraph.View();
			wv.pview = new PairwiseImageGraph.View();
			wv.pview.index = i;
			scene.listViews.add(wv);
		}

		var alg = new SceneMergingOperations();
		alg.initializeViewCounts(viewScenes, numScenes);

		// Disable it
		alg.toggleViewEnabled(scene, viewScenes);

		// Verify that it has been disabled
		assertFalse(alg.enabledScenes.get(scene.index));

		// See if the counts were updated correctly
		alg.commonViewCounts.forIdx(( idx, v ) -> {
			if (idx == 0) {
				// scenes with a lower index should have zero counts for this scene
				assertEquals(numScenes - 1, v.size);
				assertEquals(1, v.get(0).sceneIndex);
				assertEquals(0, v.get(0).counts);
			} else if (idx == 1) {
				// The node which was adjusted should have zero counts for all the other scenes now
				v.forEach(c -> assertEquals(0, c.counts));
			} else {
				// there should be no change if the index is higher
				assertEquals(3 - idx, v.size);
			}
		});

		// Add it back. It will be visible in every scene again
		alg.toggleViewEnabled(scene, viewScenes);
		assertTrue(alg.enabledScenes.get(scene.index));

		alg.commonViewCounts.forIdx(( idx, v ) -> {
			if (idx == 0) {
				// scenes with a lower index should have zero counts for this scene
				assertEquals(numScenes - 1, v.size);
				assertEquals(1, v.get(0).sceneIndex);
				assertEquals(numViews, v.get(0).counts);
			} else if (idx == 1) {
				// The node which was adjusted should have zero counts for all the other scenes now
				v.forEach(c -> assertEquals(numViews, c.counts));
			} else {
				// there should be no change if the index is higher
				assertEquals(3 - idx, v.size);
			}
		});
	}

	@Test void findViewCounts() {
		var list = new DogArray<>(SceneCommonCounts::new, SceneCommonCounts::reset);

		// First two calls should add elements. The third all look up the first one again
		SceneCommonCounts found1 = SceneMergingOperations.findViewCounts(list, 5);
		SceneCommonCounts found2 = SceneMergingOperations.findViewCounts(list, 6);
		SceneCommonCounts found3 = SceneMergingOperations.findViewCounts(list, 5);

		assertEquals(2, list.size);
		assertSame(found1, found3);
		assertEquals(5, found1.sceneIndex);
		assertEquals(6, found2.sceneIndex);
	}
}
