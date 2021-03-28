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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.*;
import boofcv.abst.geo.bundle.SceneStructureCommon.Point;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("RedundantCast")
class TestPruneStructureFromSceneMetric extends BoofStandardJUnit {
	SceneStructureMetric structure;
	SceneObservations observations;

	CameraPinholeBrown intrinsic = new CameraPinholeBrown(300, 300, 0, 250, 200, 500, 400);
	Point3D_F64 center = new Point3D_F64(0, 0, 4);

	@Test
	void pruneObservationsByErrorRank() {
		createPerfectScene();
		int N = observations.getObservationCount();
		addCorruptObservations((int)(N*0.05));

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure, observations);

		// 5% of the observations are bad. This should remove them all
		alg.pruneObservationsByErrorRank(0.95);

		// first see if the expected number of observations were prune
		assertEquals(N*95/100, observations.getObservationCount());

		// All bad observations should have been removed
		checkAllObservationsArePerfect();
	}

	/**
	 * Take this many observations and turn into garbage observations
	 */
	private void addCorruptObservations( int count ) {
		List<ObsId> list = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			for (int i = 0; i < observations.views.data[viewIdx].size(); i++) {
				list.add(new ObsId(viewIdx, i));
			}
		}

		for (int i = 0; i < count; i++) {
			int selected = rand.nextInt(list.size() - i);
			ObsId o = list.get(selected);

			// swap the last element with the select one. The last element will be unselectable in future iterations
			list.set(selected, list.get(list.size() - i - 1));
			list.set(list.size() - i - 1, o);

			observations.views.data[o.view].setPixel(o.point, 1000f, 1000f);
		}
		observations.checkOneObservationPerView();
	}

	@Test
	void pruneObservationsBehindCamera() {
		createPerfectScene();
		int N = observations.getObservationCount();
		movePointBehindCameras((int)(N*0.1));

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure, observations);

		// 5% of the observations are bad. This should remove them all
		alg.pruneObservationsBehindCamera();

		// A bunch of observations should have been pruned because 10% of the points are now behind the camera
		assertTrue(observations.getObservationCount() < N*0.901);

		// All bad observations should have been removed
		checkAllObservationsArePerfect();
	}

	/**
	 * Take this many observations and turn into garbage observations
	 */
	private void movePointBehindCameras( int count ) {
		DogArray<Point> points = structure.points;

		int[] indexes = new int[points.size];
		for (int i = 0; i < points.size; i++) {
			indexes[i] = i;
		}

		Point3D_F64 world = new Point3D_F64();
		for (int i = 0; i < count; i++) {
			int selected = rand.nextInt(points.size() - i);
			Point p = points.get(indexes[selected]);

			// swap the last element with the select one. The last element will be unselectable in future iterations
			int tmp = indexes[selected];
			indexes[selected] = indexes[points.size() - i - 1];
			indexes[points.size() - i - 1] = tmp;

			// all cameras lie along a line. This will move it behind all cameras
			p.get(world);
			world.z = -world.z;
			p.set(world.x, world.y, world.z);
		}
	}

	@Test
	void prunePoints_count() {
		createPerfectScene();
		int countPoints = structure.points.size;
		int countObservations = observations.getObservationCount();

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure, observations);

		// no change expected
		alg.prunePoints(1);
		assertEquals(countPoints, structure.points.size);
		assertEquals(countObservations, observations.getObservationCount());

		// this should prune a bunch
		int threshold = structure.views.size - 2;
		alg.prunePoints(threshold);
		assertTrue(countPoints > structure.points.size);
		assertTrue(countObservations > observations.getObservationCount());

		for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
			if (structure.points.data[pointIdx].views.size < threshold)
				fail("point with not enough views");
		}
		checkAllObservationsArePerfect();
	}

	/**
	 * Qualitative test of prune by nearest neighbor.
	 */
	@Test
	void prunePoints_neighbors() {
		createPerfectScene();
		int countPoints0 = structure.points.size;
		int countObservations0 = observations.getObservationCount();

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure, observations);

		// This should just prune the outliers far from the center
		alg.prunePoints(2, 0.5);

		int countPoints1 = structure.points.size;
		int countObservations1 = observations.getObservationCount();
		assertTrue(countPoints0 > countPoints1 && countPoints1 > 0.95*countPoints0);
		assertTrue(countObservations0 > countObservations1 && countObservations1 > 0.95*countObservations0);

		// If run a second time it should have very similar results
		alg.prunePoints(2, 0.5);
		assertEquals(countPoints1, structure.points.size, 5);
		assertEquals(countObservations1, observations.getObservationCount(), countObservations1*0.005);

		// sanity check the modifications
		checkAllObservationsArePerfect();
	}

	/**
	 * Prunes and makes sure the distance and count are correctly implemented
	 */
	@Test
	void prunePoints_neighbors_exact() {
		createPerfectScene(2, 5);

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure, observations);

		// no pruning should occur
		alg.prunePoints(1, 5.01);
		assertEquals(4, structure.points.size);

		// everything should be pruned
		alg.prunePoints(1, 4.99);
		assertEquals(0, structure.points.size);
		assertEquals(0, observations.getObservationCount());

		// Corners should get pruned but interior ones saved
		createPerfectScene(3, 5);
		alg = new PruneStructureFromSceneMetric(structure, observations);
		alg.prunePoints(3, 5.01);
		assertEquals(5, structure.points.size);
	}

	@Test
	void pruneViews() {
		createPerfectScene();

		// original point count
		int pointCount = structure.points.size;
		int observationCount = observations.getObservationCount();

		// figure out the view with the least number of observations
		int viewWithLeast = -1;
		int leastCount = Integer.MAX_VALUE;
		for (int viewIdx = 0; viewIdx < observations.views.size; viewIdx++) {
			if (leastCount > observations.views.data[viewIdx].size()) {
				leastCount = observations.views.data[viewIdx].size();
				viewWithLeast = viewIdx;
			}
		}

		// remove a point just in case there is a tie
		observations.views.data[viewWithLeast].remove(8);
		leastCount -= 1;

		assertEquals(10, observations.views.size);

		var alg = new PruneStructureFromSceneMetric(structure, observations);

		// no change
		assertFalse(alg.pruneViews(leastCount - 1));
		assertEquals(10, observations.views.size);
		assertEquals(structure.views.size, observations.views.size);

		// Now prune views. Only one should be removed
		assertTrue(alg.pruneViews(leastCount));
		assertEquals(9, observations.views.size);
		assertEquals(structure.views.size, observations.views.size);
		// Points are not removed even if there is no view that can see them now
		assertEquals(structure.points.size, pointCount);
		// However the number of observations will be decreased
		assertTrue(observations.getObservationCount() < observationCount);

		// sanity check the modifications
		checkAllObservationsArePerfect();
	}

	@Test
	void pruneUnusedCameras() {
		createPerfectScene();

		var alg = new PruneStructureFromSceneMetric(structure, observations);

		// no change
		assertFalse(alg.pruneUnusedCameras());
		assertEquals(2, structure.cameras.size);

		// remove all references to the first camera
		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureMetric.View v = structure.views.data[i];
			v.camera = 1;
		}

		// First camera is removed
		assertTrue(alg.pruneUnusedCameras());
		assertEquals(1, structure.cameras.size);
		// make sure references are updated
		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureMetric.View v = structure.views.data[i];
			assertEquals(0, v.camera);
		}
	}

	/**
	 * If nothing points to a motion then prune it
	 */
	@Test
	void pruneUnusedMotions() {
		createPerfectScene();

		var alg = new PruneStructureFromSceneMetric(structure, observations);

		// no change
		assertFalse(alg.pruneUnusedMotions());
		assertEquals(10, structure.motions.size);

		// remove all references to the first motion
		for (int i = 0; i < structure.views.size; i++) {
			SceneStructureMetric.View v = structure.views.data[i];
			v.parent_to_view = v.parent_to_view == 1 ? 2 : v.parent_to_view;
		}

		// First camera is removed
		assertTrue(alg.pruneUnusedMotions());
		assertEquals(9, structure.motions.size);
		structure.views.forIdx(( i, v ) -> assertEquals(i == 0 ? 0 : i == 1 ? 1 : i - 1, v.parent_to_view));
	}

	/**
	 * Make sure that if there are relative views in a chain those are handled currently when pruning
	 */
	@Test
	void handleRelativeViews_pruneObservationsBehindCamera() {
		structure = new SceneStructureMetric(false);
		structure.initialize(1, 2, 1);

		structure.setCamera(0, true, intrinsic);

		// If view[1] was absolute then the point would not be pruned since it would be infront of both cameras
		structure.setView(0, 0, true, eulerXyz(0, 0, -1, 0, 0, 0, null));
		structure.setView(1, 0, true, eulerXyz(0, 0, -1, 0, 0, 0, null), 0);

		structure.setPoint(0, 0, 0, 1.5);

		createObservationForAll();

		var alg = new PruneStructureFromSceneMetric(structure, observations);

		alg.pruneObservationsBehindCamera();
		assertEquals(1, observations.getView(0).size());
		assertEquals(0, observations.getView(1).size());
	}

	private void createObservationForAll() {
		observations = new SceneObservations();
		observations.initialize(structure.views.size);
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			observations.getView(viewIdx).resize(structure.points.size);
			for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
				observations.getView(viewIdx).set(pointIdx, pointIdx, 20, 20);
				structure.connectPointToView(pointIdx, viewIdx);
			}
		}
	}

	@Test
	void handleRelativeViews_pruneViews() {
		structure = new SceneStructureMetric(false);
		structure.initialize(1, 3, 3);

		structure.setCamera(0, true, intrinsic);

		structure.setView(0, 0, true, eulerXyz(0, 0, -1, 0, 0, 0, null));
		structure.setView(1, 0, false, eulerXyz(0, 0, -1, 0, 0, 0, null), 0);
		structure.setView(2, 0, true, eulerXyz(0, 0, -1, 0, 0, 0, null), 1);
		createObservationForAll();

		// View[1] should not be dropped since another is dependent on it
		observations.getView(1).resize(0);
		structure.getPoints().forIdx(( i, p ) -> p.removeView(1));

		var alg = new PruneStructureFromSceneMetric(structure, observations);
		assertFalse(alg.pruneViews(2));
		assertEquals(3, structure.views.size);

		// Now make the view[2] prune able, this should cause view[1] to be pruned also
		// View[1] should not be dropped since another is dependent on it
		observations.getView(2).resize(0);
		structure.getPoints().forIdx(( i, p ) -> p.removeView(2));
		assertTrue(alg.pruneViews(2));
		assertEquals(1, structure.views.size);
	}

	/**
	 * Creates a scene with points in a grid pattern. Useful when testing spacial filters
	 *
	 * @param grid Number of points wide the pattern is
	 * @param space Spacing between the points
	 */
	private void createPerfectScene( int grid, double space ) {
		structure = new SceneStructureMetric(false);
		structure.initialize(1, 1, grid*grid);

		structure.setCamera(0, true, intrinsic);

		List<Point3D_F64> points = new ArrayList<>();
		for (int i = 0; i < grid; i++) {
			for (int j = 0; j < grid; j++) {
				double x = (i - (int)(grid/2))*space;
				double y = (j - (int)(grid/2))*space;

				Point3D_F64 p = new Point3D_F64(center.x + x, center.y + y, center.z);
				points.add(p);
				structure.points.data[i*grid + j].set(p.x, p.y, p.z);
			}
		}

		createRestOfTheScene(points, false);
	}

	private void createPerfectScene() {
		structure = new SceneStructureMetric(false);
		structure.initialize(2, 10, 500);

		structure.setCamera(0, true, intrinsic);
		structure.setCamera(1, false, intrinsic);

		List<Point3D_F64> points = new ArrayList<>();
		for (int i = 0; i < structure.points.size; i++) {
			Point3D_F64 p = UtilPoint3D_F64.noiseNormal(center, 0.5, 0.5, 0.5, rand, null);
			points.add(p);
			structure.points.data[i].set(p.x, p.y, p.z);
		}

		createRestOfTheScene(points, true);
	}

	private void createRestOfTheScene( List<Point3D_F64> points, boolean sanityCheck ) {
		for (int i = 0; i < structure.views.size; i++) {
			double x = -1.5 + (int)(3*i/Math.max(1, (structure.views.size - 1)));
			structure.setView(i, i%2, false, eulerXyz(x, 0, 0, 0, 0, 0, null));
		}

		observations = new SceneObservations();
		observations.initialize(structure.views.size);

		Se3_F64 world_to_view = new Se3_F64();
		Se3_F64 tmp = new Se3_F64();

		// 3D point in camera coordinate system
		Point3D_F64 cameraX = new Point3D_F64();
		// observed pixel coordinate of 3D point
		Point2D_F64 pixel = new Point2D_F64();
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			SceneStructureMetric.View v = structure.views.data[viewIdx];
			BundleAdjustmentCamera camera = structure.cameras.get(v.camera).model;
			structure.getWorldToView(v, world_to_view, tmp);

			for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
				Point3D_F64 p = points.get(pointIdx);

				world_to_view.transform(p, cameraX);

				if (cameraX.z <= 0)
					continue;

				camera.project(cameraX.x, cameraX.y, cameraX.z, pixel);

				if (!intrinsic.isInside(pixel.x, pixel.y))
					continue;

				observations.views.data[viewIdx].add(pointIdx, (float)pixel.x, (float)pixel.y);
				structure.connectPointToView(pointIdx, viewIdx);
			}
		}

		if (!sanityCheck)
			return;

		// sanity checks
		for (int pointIdx = 0; pointIdx < structure.points.size; pointIdx++) {
			if (structure.points.data[pointIdx].views.size == 0) {
				Point3D_F64 p = new Point3D_F64();
				structure.points.data[pointIdx].get(p);
				throw new RuntimeException("Point with no views. " + p);
			}
		}

		for (int viewIdx = 0; viewIdx < observations.views.size; viewIdx++) {
			if (observations.views.data[viewIdx].size() == 0)
				throw new RuntimeException("View with no observations");
		}

		checkObservationAndStructureSync();
	}

	/**
	 * See if all the observations are perfect. This acts as a sanity check on the scenes structure after modification
	 */
	private void checkAllObservationsArePerfect() {
		Se3_F64 world_to_view = new Se3_F64();
		Se3_F64 tmp = new Se3_F64();

		Point3D_F64 worldX = new Point3D_F64();
		// 3D point in camera coordinate system
		Point3D_F64 cameraX = new Point3D_F64();
		// observed pixel coordinate of 3D point
		Point2D_F64 predicted = new Point2D_F64();
		Point2D_F64 found = new Point2D_F64();
		for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
			SceneStructureMetric.View v = structure.views.data[viewIdx];
			BundleAdjustmentCamera camera = structure.cameras.get(v.camera).model;
			structure.getWorldToView(v, world_to_view, tmp);

			for (int obsIdx = 0; obsIdx < observations.views.data[viewIdx].size(); obsIdx++) {
				Point f = structure.points.data[observations.views.data[viewIdx].point.get(obsIdx)];
				f.get(worldX);
				world_to_view.transform(worldX, cameraX);

				assertTrue(cameraX.z > 0);
				camera.project(cameraX.x, cameraX.y, cameraX.z, predicted);
				observations.views.data[viewIdx].getPixel(obsIdx, found);
				assertTrue(predicted.distance(found) < 1e-4);
			}
		}
	}

	private void checkObservationAndStructureSync() {
		for (int viewId = 0; viewId < structure.views.size; viewId++) {
			SceneObservations.View v = observations.views.data[viewId];
			for (int pointIdx = v.point.size - 1; pointIdx >= 0; pointIdx--) {
				SceneStructureCommon.Point structP = structure.points.data[v.getPointId(pointIdx)];
				if (!structP.views.contains(viewId))
					throw new RuntimeException("Miss match");
			}
		}
	}

	private static class ObsId {
		int view;
		int point;

		ObsId( int view, int point ) {
			this.view = view;
			this.point = point;
		}
	}
}
