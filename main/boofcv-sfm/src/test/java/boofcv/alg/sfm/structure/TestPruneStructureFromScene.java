/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.abst.geo.bundle.BundleAdjustmentObservations;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure.Point;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPruneStructureFromScene {
	BundleAdjustmentSceneStructure structure;
	BundleAdjustmentObservations observations;

	Random rand = new Random(234);
	CameraPinholeRadial intrinsic = new CameraPinholeRadial(300,300,0,250,200,500,400);
	Point3D_F64 center = new Point3D_F64(0,0,4);

	@Test
	public void pruneObservationsByErrorRank() {
		createPerfectScene();
		int N = observations.getObservationCount();
		addCorruptObservations((int)(N*0.05));

		PruneStructureFromScene alg = new PruneStructureFromScene(structure,observations);

		// 5% of the observations are bad. This should remove them all
		alg.pruneObservationsByErrorRank(0.95);

		// first see if the expected number of observations were prune
		assertEquals(N*95/100,observations.getObservationCount());

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
		int N = observations.getObservationCount();
		movePointBehindCameras((int)(N*0.1));

		PruneStructureFromScene alg = new PruneStructureFromScene(structure,observations);

		// 5% of the observations are bad. This should remove them all
		alg.pruneObservationsBehindCamera();

		// A bunch of observations should have been pruned because 10% of the points are now behind the camera
		assertTrue( observations.getObservationCount() < N*0.901);

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
		int countObservations = observations.getObservationCount();

		PruneStructureFromScene alg = new PruneStructureFromScene(structure,observations);

		// no change expected
		alg.prunePoints(1);
		assertEquals(countPoints,structure.points.length);
		assertEquals(countObservations,observations.getObservationCount());

		// this should prune a bunch
		int threshold = structure.views.length-2;
		alg.prunePoints(threshold);
		assertTrue(countPoints>structure.points.length);
		assertTrue(countObservations>observations.getObservationCount());

		for (int pointIdx = 0; pointIdx < structure.points.length; pointIdx++) {
			if( structure.points[pointIdx].views.size < threshold )
				fail("point with not enough views");
		}
		checkAllObservationsArePerfect();
	}

	@Test
	public void prunePoints_neighbors() {
		fail("Implement");
	}

	@Test
	public void pruneViews() {
		fail("Implement");
	}

	@Test
	public void pruneUnusedCameras() {
		fail("Implement");
	}

	private void createPerfectScene() {
		structure = new BundleAdjustmentSceneStructure(false);
		structure.initialize(2,10,500);

		structure.setCamera(0,true,intrinsic);
		structure.setCamera(1,false,intrinsic);

		List<Point3D_F64> points = new ArrayList<>();
		for (int i = 0; i < structure.points.length; i++) {
			Point3D_F64 p = UtilPoint3D_F64.noiseNormal(center,0.5,0.5,1,rand,null);
			points.add(p);
			structure.points[i].set(p.x,p.y,p.z);
		}

		for (int i = 0; i < structure.views.length; i++) {
			double x = -1.5 + 3*i/(structure.views.length-1);
			structure.setView(i,false,SpecialEuclideanOps_F64.setEulerXYZ(0,0,0,x,0,0,null));
			structure.connectViewToCamera(i,i%2);
		}

		observations = new BundleAdjustmentObservations(structure.views.length);

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
		for (int viewIdx = 0; viewIdx < structure.views.length; viewIdx++) {
			BundleAdjustmentObservations.View v = observations.views[viewIdx];
			for(int pointIndex = v.point.size-1; pointIndex >= 0; pointIndex-- ) {
				BundleAdjustmentSceneStructure.Point structP = structure.points[ v.getPointId(pointIndex)];
				if( !structP.views.contains(viewIdx))
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