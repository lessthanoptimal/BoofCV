/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.abst.geo.bundle.PruneStructureFromSceneMetric;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon.Point;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestPruneStructureFromSceneMetric {
	SceneStructureMetric structure;
	SceneObservations observations;

	Random rand = new Random(234);
	CameraPinholeRadial intrinsic = new CameraPinholeRadial(300,300,0,250,200,500,400);
	Point3D_F64 center = new Point3D_F64(0,0,4);

	@Test
	public void pruneObservationsByErrorRank() {
		createPerfectScene();
		int N = observations.getObservationCount(false);
		addCorruptObservations((int)(N*0.05));

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure,observations);

		// 5% of the observations are bad. This should remove them all
		alg.pruneObservationsByErrorRank(0.95);

		// first see if the expected number of observations were prune
		assertEquals(N*95/100,observations.getObservationCount(false));

		// All bad observations should have been removed
		checkAllObservationsArePerfect();
	}

	/**
	 * Take this many observations and turn into garbage observations
	 */
	private void addCorruptObservations( int count ) {
		List<ObsId> list = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < structure.views.length; viewIdx++) {
			for (int i = 0; i < observations.views[viewIdx].size(); i++) {
				list.add( new ObsId(viewIdx,i));
			}
		}

		for (int i = 0; i < count; i++) {
			int selected = rand.nextInt(list.size()-i);
			ObsId o = list.get(selected);

			// swap the last element with the select one. The last element will be unselectable in future iterations
			list.set(selected,list.get(list.size()-i-1));
			list.set(list.size()-i-1,o);

			observations.views[o.view].set(o.point,1000f,1000f);
		}
		observations.checkOneObservationPerView();
	}

	@Test
	public void pruneObservationsBehindCamera() {
		createPerfectScene();
		int N = observations.getObservationCount(false);
		movePointBehindCameras((int)(N*0.1));

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure,observations);

		// 5% of the observations are bad. This should remove them all
		alg.pruneObservationsBehindCamera();

		// A bunch of observations should have been pruned because 10% of the points are now behind the camera
		assertTrue( observations.getObservationCount(false) < N*0.901);

		// All bad observations should have been removed
		checkAllObservationsArePerfect();
	}

	/**
	 * Take this many observations and turn into garbage observations
	 */
	private void movePointBehindCameras( int count ) {
		List<Point> list = new ArrayList<>(Arrays.asList(structure.points));

		Point3D_F64 world = new Point3D_F64();
		for (int i = 0; i < count; i++) {
			int selected = rand.nextInt(list.size()-i);
			Point p = list.get(selected);

			// swap the last element with the select one. The last element will be unselectable in future iterations
			list.set(selected,list.get(list.size()-i-1));
			list.set(list.size()-i-1,p);

			// all cameras lie along a line. This will move it behind all cameras
			p.get(world);
			world.z = -world.z;
			p.set(world.x,world.y,world.z);
		}
	}

	@Test
	public void prunePoints_count() {
		createPerfectScene();
		int countPoints = structure.points.length;
		int countObservations = observations.getObservationCount(false);

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure,observations);

		// no change expected
		alg.prunePoints(1);
		assertEquals(countPoints,structure.points.length);
		assertEquals(countObservations,observations.getObservationCount(false));

		// this should prune a bunch
		int threshold = structure.views.length-2;
		alg.prunePoints(threshold);
		assertTrue(countPoints>structure.points.length);
		assertTrue(countObservations>observations.getObservationCount(false));

		for (int pointIdx = 0; pointIdx < structure.points.length; pointIdx++) {
			if( structure.points[pointIdx].views.size < threshold )
				fail("point with not enough views");
		}
		checkAllObservationsArePerfect();
	}

	/**
	 * Qualitative test of prune by nearest neighbor.
	 */
	@Test
	public void prunePoints_neighbors() {
		createPerfectScene();
		int countPoints0 = structure.points.length;
		int countObservations0 = observations.getObservationCount(false);

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure,observations);

		// This should just prune the outliers far from the center
		alg.prunePoints(2,0.5);

		int countPoints1 = structure.points.length;
		int countObservations1 = observations.getObservationCount(false);
		assertTrue(countPoints0>countPoints1 && countPoints1>0.95*countPoints0);
		assertTrue(countObservations0>countObservations1 && countObservations1>0.95*countObservations0);

		// If run a second time it should have very similar results
		alg.prunePoints(2,0.5);
		assertEquals(countPoints1, structure.points.length,5);
		assertEquals(countObservations1, observations.getObservationCount(false),countObservations1*0.005);

		// sanity check the modifications
		checkAllObservationsArePerfect();
	}

	/**
	 * Prunes and makes sure the distance and count are correctly implemented
	 */
	@Test
	public void prunePoints_neighbors_exact() {
		createPerfectScene(2,5);

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure,observations);

		// no pruning should occur
		alg.prunePoints(1,5.01);
		assertEquals(4, structure.points.length);

		// everything should be pruned
		alg.prunePoints(1,4.99);
		assertEquals(0, structure.points.length);
		assertEquals(0, observations.getObservationCount(false));

		// Corners should get pruned but interior ones saved
		createPerfectScene(3,5);
		alg = new PruneStructureFromSceneMetric(structure,observations);
		alg.prunePoints(3,5.01);
		assertEquals(5, structure.points.length);

	}

	@Test
	public void pruneViews() {
		createPerfectScene();

		// original point count
		int pointCount = structure.points.length;
		int observationCount = observations.getObservationCount(false);

		// figure out the view with the least number of observations
		int viewWithLeast = -1;
		int leastCount = Integer.MAX_VALUE;
		for (int viewIdx = 0; viewIdx < observations.views.length; viewIdx++) {
			if( leastCount > observations.views[viewIdx].size()) {
				leastCount = observations.views[viewIdx].size();
				viewWithLeast = viewIdx;
			}
		}

		// remove a point just in case there is a tie
		observations.views[viewWithLeast].remove(8);
		leastCount -= 1;

		assertEquals(10,observations.views.length);

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure,observations);

		// no change
		alg.pruneViews(leastCount-1);
		assertEquals(10,observations.views.length);
		assertEquals(structure.views.length,observations.views.length);

		// Now prune views. Only one should be removed
		alg.pruneViews(leastCount);
		assertEquals(9,observations.views.length);
		assertEquals(structure.views.length,observations.views.length);
		// Points are not removed even if there is no view that can see them now
		assertEquals(structure.points.length,pointCount);
		// However the number of observations will be decreased
		assertTrue( observations.getObservationCount(false) < observationCount);

		// sanity check the modifications
		checkAllObservationsArePerfect();
	}

	@Test
	public void pruneUnusedCameras() {
		createPerfectScene();

		PruneStructureFromSceneMetric alg = new PruneStructureFromSceneMetric(structure,observations);

		// no change
		alg.pruneUnusedCameras();
		assertEquals(2,structure.cameras.length);

		// remove all references to the first camera
		for (int i = 0; i < structure.views.length; i++) {
			SceneStructureMetric.View v = structure.views[i];
			v.camera = 1;
		}

		// First camera is removed
		alg.pruneUnusedCameras();
		assertEquals(1,structure.cameras.length);
		// make sure references are updated
		for (int i = 0; i < structure.views.length; i++) {
			SceneStructureMetric.View v = structure.views[i];
			assertEquals(0,v.camera);
		}
	}

	/**
	 * Creates a scene with points in a grid pattern. Useful when testing spacial filters
	 * @param grid Number of points wide the pattern is
	 * @param space Spacing between the points
	 */
	private void createPerfectScene( int grid , double space ) {
		structure = new SceneStructureMetric(false);
		structure.initialize(1,1,grid*grid);

		structure.setCamera(0,true,intrinsic);

		List<Point3D_F64> points = new ArrayList<>();
		for (int i = 0; i < grid; i++) {
			for (int j = 0; j < grid; j++) {
				double x = (i-grid/2)*space;
				double y = (j-grid/2)*space;

				Point3D_F64 p = new Point3D_F64(center.x+x,center.y+y,center.z);
				points.add(p);
				structure.points[i*grid+j].set(p.x,p.y,p.z);
			}
		}

		createRestOfTheScene(points, false);
	}

	private void createPerfectScene() {
		structure = new SceneStructureMetric(false);
		structure.initialize(2,10,500);

		structure.setCamera(0,true,intrinsic);
		structure.setCamera(1,false,intrinsic);

		List<Point3D_F64> points = new ArrayList<>();
		for (int i = 0; i < structure.points.length; i++) {
			Point3D_F64 p = UtilPoint3D_F64.noiseNormal(center,0.5,0.5,1,rand,null);
			points.add(p);
			structure.points[i].set(p.x,p.y,p.z);
		}

		createRestOfTheScene(points, true);
	}

	private void createRestOfTheScene(List<Point3D_F64> points , boolean sanityCheck ) {
		for (int i = 0; i < structure.views.length; i++) {
			double x = -1.5 + 3*i/Math.max(1,(structure.views.length-1));
			structure.setView(i,false,SpecialEuclideanOps_F64.eulerXyz(x,0,0,0,0,0,null));
			structure.connectViewToCamera(i,i%2);
		}

		observations = new SceneObservations(structure.views.length);

		// 3D point in camera coordinate system
		Point3D_F64 cameraX = new Point3D_F64();
		// observed pixel coordinate of 3D point
		Point2D_F64 pixel = new Point2D_F64();
		for (int viewIdx = 0; viewIdx < structure.views.length; viewIdx++) {
			BundleAdjustmentCamera camera = structure.cameras[structure.views[viewIdx].camera].model;
			Se3_F64 worldToView = structure.views[viewIdx].worldToView;

			for (int pointIdx = 0; pointIdx < structure.points.length; pointIdx++) {
				Point3D_F64 p = points.get(pointIdx);

				worldToView.transform(p,cameraX);

				if( cameraX.z <= 0)
					continue;

				camera.project(cameraX.x,cameraX.y,cameraX.z, pixel);

				if( !intrinsic.inside(pixel.x,pixel.y) )
					continue;


				observations.views[viewIdx].add(pointIdx,(float)pixel.x,(float)pixel.y);
				structure.connectPointToView(pointIdx,viewIdx);
			}
		}

		if( !sanityCheck )
			return;

		// sanity checks
		for (int pointIdx = 0; pointIdx < structure.points.length; pointIdx++) {
			if( structure.points[pointIdx].views.size == 0 ) {
				Point3D_F64 p = new Point3D_F64();
				structure.points[pointIdx].get(p);
				throw new RuntimeException("Point with no views. "+p);
			}
		}

		for (int viewIdx = 0; viewIdx < observations.views.length; viewIdx++) {
			if( observations.views[viewIdx].size() == 0 )
				throw new RuntimeException("View with no observations");
		}

		checkObservationAndStructureSync();
	}


	/**
	 * See if all the observations are perfect. This acts as a sanity check on the scenes structure after modification
	 */
	private void checkAllObservationsArePerfect() {
		Point3D_F64 worldX = new Point3D_F64();
		// 3D point in camera coordinate system
		Point3D_F64 cameraX = new Point3D_F64();
		// observed pixel coordinate of 3D point
		Point2D_F64 predicted = new Point2D_F64();
		Point2D_F64 found = new Point2D_F64();
		for (int viewIdx = 0; viewIdx < structure.views.length; viewIdx++) {
			BundleAdjustmentCamera camera = structure.cameras[structure.views[viewIdx].camera].model;
			Se3_F64 worldToView = structure.views[viewIdx].worldToView;

			for (int obsIdx = 0; obsIdx < observations.views[viewIdx].size(); obsIdx++) {
				Point f = structure.points[observations.views[viewIdx].point.get(obsIdx)];
				f.get(worldX);
				worldToView.transform(worldX,cameraX);

				assertTrue( cameraX.z > 0);
				camera.project(cameraX.x,cameraX.y,cameraX.z,predicted);
				observations.views[viewIdx].get(obsIdx,found);
				assertTrue( predicted.distance(found) < 1e-4 );
			}
		}
	}

	private void checkObservationAndStructureSync() {
		for (int viewId = 0; viewId < structure.views.length; viewId++) {
			SceneObservations.View v = observations.views[viewId];
			for(int pointIdx = v.point.size-1; pointIdx >= 0; pointIdx-- ) {
				SceneStructureMetric.Point structP = structure.points[ v.getPointId(pointIdx) ];
				if( !structP.views.contains(viewId))
					throw new RuntimeException("Miss match");
			}
		}
	}

	private static class ObsId {
		int view;
		int point;

		public ObsId(int view, int point) {
			this.view = view;
			this.point = point;
		}
	}
}