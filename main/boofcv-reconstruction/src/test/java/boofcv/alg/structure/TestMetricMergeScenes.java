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

import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestMetricMergeScenes extends BoofStandardJUnit {
	/**
	 * Create a scene that should be merged without issues and make sure everything passes
	 */
	@Test void simpleCase() {
		var db = new MockLookupSimilarImagesRealistic();
		// true camera will match the model used exactly. Default does not causing the scale to not be quite accurate
		db.setIntrinsic(new CameraPinhole(410, 410, 0, 400, 400, 800, 800));
		db.pathLine(10, 0.1, 0.9, 1);

		PairwiseImageGraph pairwise = db.createPairwise();

		double scale = 0.5;

		// Create two initially disconnected scales. Provide location using truth.
		var src = new SceneWorkingGraph();
		var dst = new SceneWorkingGraph();

		for (int i = 0; i < 5; i++) {
			int j = i + 5;
			src.addView(pairwise.nodes.get(i)).world_to_view.setTo(db.views.get(i).world_to_view);
			dst.addView(pairwise.nodes.get(j)).world_to_view.setTo(db.views.get(j).world_to_view);

			// Change the scale
			src.listViews.get(i).world_to_view.T.scale(scale);

			BundleAdjustmentOps.convert(db.intrinsic, src.listViews.get(i).intrinsic);
			BundleAdjustmentOps.convert(db.intrinsic, dst.listViews.get(i).intrinsic);
			src.listViews.get(i).imageDimension.setTo(db.intrinsic.width, db.intrinsic.height);
			dst.listViews.get(i).imageDimension.setTo(db.intrinsic.width, db.intrinsic.height);
		}
		// Add one overlapping view between src and dst
		db.addInlierInfo(pairwise, dst.addView(pairwise.nodes.get(4)), 5, 6);
		dst.lookupView(pairwise.nodes.get(4).id).world_to_view.setTo(db.views.get(4).world_to_view);
		BundleAdjustmentOps.convert(db.intrinsic, dst.listViews.get(5).intrinsic);
		dst.listViews.get(5).imageDimension.setTo(db.intrinsic.width, db.intrinsic.height);

		// Connect views to each other
		for (int i = 0; i < 5; i++) {
			int[] connected;
			if (i == 0)
				connected = new int[]{1, 2};
			else if (i == 4)
				connected = new int[]{2, 3};
			else
				connected = new int[]{i - 1, i + 1};

			db.addInlierInfo(pairwise, src.listViews.get(i), connected);
			connected[0] += 5;
			connected[1] += 5;
			db.addInlierInfo(pairwise, dst.listViews.get(i), connected);
		}

		// merge the two scenes now
		var alg = new MetricMergeScenes();
		assertTrue(alg.merge(db, src, dst));

		// See if 'dst' contains all the views now and that they are correctly scaled
		assertEquals(10, dst.listViews.size());
		for (int viewIdx = 0; viewIdx < db.views.size(); viewIdx++) {
			Se3_F64 expected = db.views.get(viewIdx).world_to_view;
			SceneWorkingGraph.View wview = dst.lookupView("" + viewIdx);
			assertTrue(SpecialEuclideanOps_F64.isIdentical(expected, wview.world_to_view, 1e-4, 1e-4));
		}
	}

	/**
	 * No common views. Should return false
	 */
	@Test void merge_NoCommon() {
		var src = new SceneWorkingGraph();
		var dst = new SceneWorkingGraph();
		var db = new MockLookupSimilarImagesCircleAround();

		var alg = new MetricMergeScenes();
		assertFalse(alg.merge(db, src, dst));
	}

	@Test void removeBadFeatures() {
		var alg = new MetricMergeScenes();
		alg.fractionBadFeaturesRecover = 1.0; // it should never fail
		int numInliers = 100;

		// Mark the 20% of the features as bad
		alg.checks.badFeatures.resize(numInliers, false);
		for (int i = 0; i < 20; i++) {
			alg.checks.badFeatures.set(i + 2, true);
		}

		// This needs to be filled in too since it will be modified
		var info = new SceneWorkingGraph.InlierInfo();
		info.observations.grow().resize(numInliers, ( idx ) -> idx);

		assertTrue(alg.removeBadFeatures(info));

		// Make sure it removed the observations
		DogArray_I32 o = info.observations.get(0);
		assertEquals(80, o.size);
		for (int i = 0; i < 80; i++) {
			if (i < 2)
				assertTrue(o.contains(i));
			else
				assertTrue(o.contains(i + 20));
		}
	}

	/**
	 * Makes sure fractionBadFeaturesRecover is respected
	 */
	@Test void removeBadFeatures_fraction() {
		int numInliers = 100;

		// Mark exactly 20% of the features as bad. This should pass
		var alg = new MetricMergeScenes();
		alg.checks.badFeatures.resize(numInliers, false);
		for (int i = 0; i < 20; i++) {
			alg.checks.badFeatures.set(i + 2, true);
		}

		// This needs to be filled in too since it will be modified
		var info = new SceneWorkingGraph.InlierInfo();
		info.observations.grow().resize(numInliers);

		// This should fail since the threshold is 19% but > 19% are bad
		alg.fractionBadFeaturesRecover = 0.19;
		assertFalse(alg.removeBadFeatures(info));

		// It should remove bad features now
		alg.fractionBadFeaturesRecover = 0.20;
		assertTrue(alg.removeBadFeatures(info));
	}

	@Test void convertNewViewCoordinateSystem() {
		var alg = new MetricMergeScenes();
		alg.src_to_dst.scale = 2.0;
		SpecialEuclideanOps_F64.axisXyz(1, 2, 3, 0.1, 0.2, 0.3, alg.src_to_view);

		SceneWorkingGraph.View wview = new SceneWorkingGraph.View();
		SpecialEuclideanOps_F64.axisXyz(1, 1, 1, 0, 0, 0, alg.src_to_view);

		Se3_F64 expected = new Se3_F64();
		SpecialEuclideanOps_F64.axisXyz(2, 2, 2, 0, 0, 0, alg.src_to_view);
		expected.copy().concat(alg.src_to_dst.transform, expected);

		alg.convertNewViewCoordinateSystem(wview);

		assertTrue(SpecialEuclideanOps_F64.isIdentical(expected, wview.world_to_view, 1e-4, 1e-4));
	}

	@Test void createWorkScene() {
		var src = new SceneWorkingGraph();
		var dst = new SceneWorkingGraph();

		createTwoScenesWithSomeCommon(src, dst);

		// Make one of the inliers reference a view that's not in 'src'
		dst.listViews.get(0).inliers.get(0).views.add(dst.listViews.get(5).pview);

		var alg = new MetricMergeScenes();
		MetricMergeScenes.findCommonViews(src, dst, alg.commonViews, null);
		alg.src_to_dst.scale = 2.0;
		alg.createWorkScene(src, dst);

		// Should be the size of 'src' plus the extra view
		assertEquals(src.listViews.size() + 1, alg.workScene.listViews.size());

		for (int viewIdx = 0; viewIdx < alg.workScene.listViews.size(); viewIdx++) {
			SceneWorkingGraph.View v = alg.workScene.listViews.get(viewIdx);

			if (src.isKnown(v.pview) && !dst.isKnown(v.pview)) {
				SceneWorkingGraph.View viewSrc = src.lookupView(v.pview.id);
				// make sure the coordinate system was converted into 'dst'
				assertEquals(2.0*viewSrc.world_to_view.T.x, v.world_to_view.T.x, UtilEjml.TEST_F64);
			}

			// There should be two sets of inliers in the common views
			if (src.isKnown(v.pview) && dst.isKnown(v.pview)) {
				assertEquals(2, v.inliers.size);
			} else if (src.isKnown(v.pview)) {
				assertEquals(1, v.inliers.size);
			} else {
				assertEquals(0, v.inliers.size);
			}
		}
	}

	@Test void mergeWorkIntoDst() {
		var alg = new MetricMergeScenes();
		var src = alg.workScene;
		var dst = new SceneWorkingGraph();

		createTwoScenesWithSomeCommon(src, dst);

		// iterate through common views in 'src'
		for (int i = 4; i < 8; i++) {
			src.listViews.get(i).inliers.grow().scoreGeometric = 10;
			// two inlier sets are added to src since the last inlier set is assumed to be from 'dst'
			// as 'src' is really a working scene
		}

		alg.mergeWorkIntoDst(dst);

		// Makes sure the exact number of views are found
		assertEquals(10, dst.listViews.size());
		assertEquals(10, dst.views.size());

		// Make sure index has been set correctly
		for (int i = 0; i < 10; i++) {
			assertEquals(i, dst.listViews.get(i).index);
		}

		// Makes sure the correct view info is contained in the results. 'dst' should be dominant
		for (int i = 0; i < 10; i++) {
			SceneWorkingGraph.View wview = dst.views.get(i + "");

			if (i > 3) {
				assertEquals(i*2 + 1, wview.world_to_view.T.x, UtilEjml.TEST_F64);
				assertEquals(200, wview.imageDimension.width);
				assertEquals(5, wview.intrinsic.f, UtilEjml.TEST_F64);
			} else {
				assertEquals(i*2, wview.world_to_view.T.x, UtilEjml.TEST_F64);
				assertEquals(400, wview.imageDimension.width);
				assertEquals(10, wview.intrinsic.f, UtilEjml.TEST_F64);
			}

			// In overlapping views there should be two inlier sets
			int inlierSets = i > 3 && i < 8 ? 2 : 1;
			assertEquals(inlierSets, wview.inliers.size);
		}
	}

	@Test void findCommonViews() {
		var src = new SceneWorkingGraph();
		var dst = new SceneWorkingGraph();

		// Create two scenes with overlapping and non-overlapping views
		for (int i = 0; i < 10; i++) {
			var pview = new PairwiseImageGraph.View(i + "");

			if (i < 8)
				src.addView(pview);

			if (i > 3)
				dst.addView(pview);
		}

		// Find common views
		var commonViews = new DogArray<>(MetricMergeScenes.CommonView::new);
		MetricMergeScenes.findCommonViews(src, dst, commonViews, null);

		// See if there's the expected number and expected views
		assertEquals(4, commonViews.size);
		commonViews.forIdx(( idx, c ) -> {
			assertSame(idx + 4, c.src.index);
			assertSame(idx, c.dst.index);
		});
	}

	/**
	 * Create two scenes with only some common and some overlapping views
	 */
	private void createTwoScenesWithSomeCommon( SceneWorkingGraph src, SceneWorkingGraph dst ) {
		for (int i = 0; i < 10; i++) {
			var pview = new PairwiseImageGraph.View(i + "");

			if (i < 8) {
				SceneWorkingGraph.View viewSrc = src.addView(pview);
				viewSrc.world_to_view.T.x = i*2;
				viewSrc.imageDimension.setTo(400, 300);
				viewSrc.intrinsic.f = 10;
				viewSrc.inliers.grow().scoreGeometric = 10;
			}

			if (i > 3) {
				SceneWorkingGraph.View viewDst = dst.addView(pview);
				viewDst.world_to_view.T.x = 1 + i*2;
				viewDst.imageDimension.setTo(200, 0);
				viewDst.intrinsic.f = 5;
				viewDst.inliers.grow().scoreGeometric = 8;
			}
		}
	}
}
