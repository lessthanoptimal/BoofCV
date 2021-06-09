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

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import joptsimple.internal.Objects;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRefineMetricWorkingGraph extends BoofStandardJUnit {
	public CameraPinhole intrinsic = new CameraPinhole(400, 400, 0, 400, 350, 800, 700);
	public CameraPinhole intrinsicZ = new CameraPinhole(400, 400, 0, 0, 0, 800, 700);

	/**
	 * Run everything together with and without noise
	 */
	@Test void process() {
		process_perfect(false);
		process_perfect(true);
	}

	void process_perfect( boolean addNoise ) {
		var dbSimilar = new MockLookupSimilarImagesRealistic().
				setFeatures(500).
				setIntrinsic(intrinsic).
				pathLine(5, 0.1, 0.6, 2);
		var pairwise = dbSimilar.createPairwise();
		var graph = dbSimilar.createWorkingGraph(pairwise);

		// Create two views with inliers
		SceneWorkingGraph.InlierInfo inlier0 = graph.listViews.get(0).inliers.grow();
		inlier0.views.add(pairwise.nodes.get(0));
		inlier0.views.add(pairwise.nodes.get(1));
		inlier0.views.add(pairwise.nodes.get(2));
		inlier0.views.add(pairwise.nodes.get(3));
		selectObservations(dbSimilar, inlier0);
		SceneWorkingGraph.InlierInfo inlier3 = graph.listViews.get(3).inliers.grow();
		inlier3.views.add(pairwise.nodes.get(2));
		inlier3.views.add(pairwise.nodes.get(3));
		inlier3.views.add(pairwise.nodes.get(4));
		selectObservations(dbSimilar, inlier3);

		if (addNoise) {
			// Make a few of the parameters in correct and see if it can recover from it. Observations are perfect
			// so there is still a "perfect" global minimum
			graph.listViews.get(2).world_to_view.T.x += 0.05;
			graph.listCameras.get(0).intrinsic.f += -100;
		}
		var alg = new RefineMetricWorkingGraph();
		assertTrue(alg.process(dbSimilar, graph));

		// The scale is not locked and the first view is not at the origin. Let's just use reprojection error as
		// a test since comparing translation is more complex
		assertEquals(1, graph.listCameras.size);
		assertEquals(400.0, graph.listCameras.get(0).intrinsic.f, 1e-3);
		assertEquals(0.0, alg.metricSba.sba.getFitScore(), 1e-6);
	}

	private void selectObservations( MockLookupSimilarImagesRealistic dbSimilar, SceneWorkingGraph.InlierInfo inliers ) {
		// Find the set of features visible in all the inliers
		List<MockLookupSimilarImagesRealistic.Feature> features = new ArrayList<>(dbSimilar.points);
		List<MockLookupSimilarImagesRealistic.View> dbViews = new ArrayList<>();

		for (PairwiseImageGraph.View v : inliers.views.toList()) {
			dbViews.add(dbSimilar.views.stream().filter(it -> it.id.equals(v.id)).findFirst().get());
		}

		for (MockLookupSimilarImagesRealistic.View v : dbViews) {
			for (int i = features.size() - 1; i >= 0; i--) {
				MockLookupSimilarImagesRealistic.Feature f = features.get(i);
				boolean found = v.observations.stream().anyMatch(it -> it.feature == f);
				if (!found) {
					features.remove(i);
				}
			}
		}

		// Create the inlier sets for each feature
		inliers.observations.resize(inliers.views.size);
		for (MockLookupSimilarImagesRealistic.Feature f : features) {
			for (int viewIdx = 0; viewIdx < inliers.views.size; viewIdx++) {
				int obsID = dbViews.get(viewIdx).findIndex(f);
				inliers.observations.get(viewIdx).add(obsID);
			}
		}
	}

	/**
	 * make sure it can be called two or more times without issues
	 */
	@Test
	void process_CallMultipleTimes() {
		var dbSimilar = new MockLookupSimilarImagesRealistic().
				setFeatures(500).
				setIntrinsic(intrinsic).
				pathLine(5, 0.1, 0.6, 2);
		var pairwise = dbSimilar.createPairwise();
		var graph = dbSimilar.createWorkingGraph(pairwise);

		// Create two views with inliers
		SceneWorkingGraph.InlierInfo inlier0 = graph.listViews.get(0).inliers.grow();
		inlier0.views.add(pairwise.nodes.get(0));
		inlier0.views.add(pairwise.nodes.get(1));
		inlier0.views.add(pairwise.nodes.get(2));
		inlier0.views.add(pairwise.nodes.get(3));
		selectObservations(dbSimilar, inlier0);
		SceneWorkingGraph.InlierInfo inlier3 = graph.listViews.get(3).inliers.grow();
		inlier3.views.add(pairwise.nodes.get(2));
		inlier3.views.add(pairwise.nodes.get(3));
		inlier3.views.add(pairwise.nodes.get(4));
		selectObservations(dbSimilar, inlier3);

		var alg = new RefineMetricWorkingGraph();
		assertTrue(alg.process(dbSimilar, graph));
		assertTrue(alg.process(dbSimilar, graph));

		// The scale is not locked and the first view is not at the origin. Let's just use reprojection error as
		// a test since comparing translation is more complex
		assertEquals(1, graph.listCameras.size);
		assertEquals(400.0, graph.listCameras.get(0).intrinsic.f, 1e-3);
		assertEquals(0.0, alg.metricSba.sba.getFitScore(), 1e-6);
	}

	@Test
	void findUnassignedObsAndKnown3D() {
		var dbSimilar = new MockLookupSimilarImagesRealistic().pathLine(5, 0.3, 1.5, 2);
		var pairwise = dbSimilar.createPairwise();
		var graph = dbSimilar.createWorkingGraph(pairwise);

		// create an inlier set composed of observations from 3 views
		var inliers = new SceneWorkingGraph.InlierInfo();
		for (int viewIdx : new int[]{1, 2, 3}) {
			inliers.views.add(pairwise.nodes.get(viewIdx));
			inliers.observations.grow().setTo(DogArray_I32.array(1, 2, 3, 5, 6));
		}

		var alg = new RefineMetricWorkingGraph();
		alg.initializeDataStructures(dbSimilar, graph);
		alg.initLookUpTablesForInlierSet(graph, inliers.views);

		int inlierIdx = 1;

		// have one observation point to a 3D feature
		alg.metricSba.structure.points.grow().set(1, 2, 3, 4);
		alg.metricSba.observations.getView(3).point.set(2, 0);

		alg.findUnassignedObsAndKnown3D(inliers, inlierIdx);

		assertEquals(1, alg.featureIdx3D.size);
		assertEquals(0, alg.featureIdx3D.get(0));
		assertEquals(2, alg.unassigned.size);
		DogArray_I32.array(0, 1).forIdx(( i, v ) -> assertTrue(alg.unassigned.contains(v)));
	}

	/**
	 * Test two situations. 1) Where the unassigned observations should be added to a known feature. 2) where they
	 * should not be added.
	 */
	@Test
	void assignKnown3DToUnassignedObs() {
		assignKnown3DToUnassignedObs(false);
		assignKnown3DToUnassignedObs(true);
	}

	void assignKnown3DToUnassignedObs( boolean shouldReject ) {
		var dbSimilar = new MockLookupSimilarImagesRealistic().pathLine(5, 0.3, 1.5, 2);
		var pairwise = dbSimilar.createPairwise();
		var graph = dbSimilar.createWorkingGraph(pairwise);

		var alg = new RefineMetricWorkingGraph() {
			// Override so that it can return an error for which all should be accepted or rejected
			@Override
			double computeReprojectionError( Se3_F64 world_to_view, Point2Transform2_F64 normToPixels, Point2D_F64 pixelObs, Point4D_F64 world3D ) {
				// this is also a sanity check on the used error being squared
				double error = maxReprojectionErrorPixel*maxReprojectionErrorPixel;
				return error + (shouldReject ? 0.001 : -0.001);
			}
		};
		alg.maxReprojectionErrorPixel = 10;
		alg.initializeDataStructures(dbSimilar, graph);
		alg.metricSba.structure.points.resize(20);

		// create an inlier set composed of observations from 3 views
		var inliers = new SceneWorkingGraph.InlierInfo();
		for (int viewIdx : new int[]{1, 2, 3}) {
			inliers.views.add(pairwise.nodes.get(viewIdx));
			inliers.observations.grow().setTo(DogArray_I32.array(1, 2, 3, 5, 6));
		}

		// Specific which of the observations in the inlier set are currently unassigned
		var unassignedOrig = DogArray_I32.array(0, 2);
		alg.unassigned.setTo(unassignedOrig);
		// There is only one 3D feature they can be matched with
		alg.featureIdx3D.add(3);

		int inlierFeatIdx = 4; // this is the inlier set that's going to be inspected

		alg.initLookUpTablesForInlierSet(graph, inliers.views);
		alg.assignKnown3DToUnassignedObs(graph, inliers, inlierFeatIdx);
		// it keeps tracks of what was actually assigned by removing it
		assertEquals(shouldReject ? unassignedOrig.size : 0, alg.unassigned.size);

		BoofMiscOps.forIdx(graph.listViews, ( i, v ) -> {
			// See if the observation were added to the inliers and nothing changed for others
			boolean isInlier = inliers.views.contains(v.pview);
			int inlierViewIdx = inliers.views.indexOf(v.pview);
			boolean shouldBeAssigned = !shouldReject && isInlier && unassignedOrig.contains(inlierViewIdx);

			if (shouldBeAssigned) {
				// Make sure the point in the inlier set which is being inspected was assigned a value
				int pointID = inliers.observations.get(inlierViewIdx).get(inlierFeatIdx);
				DogArray_I32 point = alg.metricSba.observations.views.get(i).point;
				assertEquals(3, point.get(pointID));
				// Set it to -1 make the next test easier since everything should now be -1
				point.set(pointID, -1);
			}

			alg.metricSba.observations.views.get(i).point.forIdx(( j, value ) -> assertEquals(-1, value));
		});
	}

	/**
	 * Render a known object and see if expected reprojection error is returned.
	 */
	@Test
	void computeReprojectionError() {
		var X = new Point4D_F64(0.01, 0.02, 0.2, 1.0);
		var intrinsic = new CameraPinhole(400, 410, 0, 420, 420, 800, 800);
		var world_to_vew = SpecialEuclideanOps_F64.eulerXyz(0.1, -0.2, 2, 0, 0.05, -0.05, null);
		var normToPixels = new LensDistortionPinhole(intrinsic).distort_F64(false, true);
		var observed = PerspectiveOps.renderPixel(world_to_vew, intrinsic, X, null);
		Objects.ensureNotNull(observed);
		// homogenous coordinates are scale invariant
		X.scale(-5.0);

		var alg = new RefineMetricWorkingGraph();
		assertEquals(0.0, alg.computeReprojectionError(world_to_vew, normToPixels, observed, X), UtilEjml.TEST_F64);
		observed.x += 2.0;
		assertEquals(4.0, alg.computeReprojectionError(world_to_vew, normToPixels, observed, X), UtilEjml.TEST_F64);
	}

	@Test
	void triangulateAndSave() {
		var dbSimilar = new MockLookupSimilarImagesRealistic().pathLine(5, 0.3, 1.5, 2);
		var pairwise = dbSimilar.createPairwise();
		var graph = dbSimilar.createWorkingGraph(pairwise);

		var alg = new RefineMetricWorkingGraph();
		alg.initializeDataStructures(dbSimilar, graph);

		// create an inlier set composed of observations from 3 views
		var inliers = new SceneWorkingGraph.InlierInfo();
		for (int viewIdx : new int[]{1, 2, 3}) {
			inliers.views.add(pairwise.nodes.get(viewIdx));
			inliers.observations.grow().setTo(DogArray_I32.array(1, 2, 3, 5, 6));
		}
		alg.sceneViewIntIds.setTo(DogArray_I32.array(1, 2, 3));
		alg.unassigned.setTo(DogArray_I32.array(1, 2));

		alg.initLookUpTablesForInlierSet(graph, inliers.views);
		alg.triangulateAndSave(inliers, 4);

		// see if it added the point to the structure correctly
		Point3D_F64 expectedX = dbSimilar.points.get(5).world;
		Point4D_F64 foundX = new Point4D_F64();
		assertEquals(1, alg.metricSba.structure.points.size);
		alg.metricSba.structure.points.get(0).get(foundX);
		foundX.divideIP(foundX.w);
		assertEquals(0.0, expectedX.distance(expectedX.x, expectedX.y, expectedX.z), 1e-6);

		// observations should now point to this new 3D feature
		BoofMiscOps.forIdx(inliers.views.toList(), ( i, v ) -> {
			int expected = alg.unassigned.contains(i) ? 0 : -1;
			int viewID = alg.sceneViewIntIds.get(i);
			assertEquals(expected, alg.metricSba.observations.getView(viewID).point.get(6));
		});
	}

	/**
	 * Adds observations and sets a few of them to be removed with a consistent pattern. Checks results to see if
	 * they were removed and that expected ones remain.
	 */
	@Test
	void pruneUnassignedObservations() {
		var alg = new RefineMetricWorkingGraph();
		alg.metricSba.observations.initialize(6);
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < i; j++) {
				alg.metricSba.observations.getView(i).resize(j + 1);
				for (int k = 0; k <= j; k++) {
					alg.metricSba.observations.getView(i).set(k, k%2 == 0 ? k : -1, 1, 2 + k);
				}
			}
		}

		alg.pruneUnassignedObservations();

		for (int i = 0; i < 6; i++) {
			// first see if the size has changed after having some removed
			int expectedCount = i - i/2;
			assertEquals(expectedCount, alg.metricSba.observations.getView(i).size());
			assertEquals(2*expectedCount, alg.metricSba.observations.getView(i).observations.size);
			// the order might have changed. So check to see if it contains the expected reference to the 3D point
			for (int j = 0; j + 1 < i; j += 2) {
				assertTrue(alg.metricSba.observations.getView(i).point.contains(j));
			}
		}
	}
}
