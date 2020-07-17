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

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.alg.geo.GeoTestingOps;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestProjectiveToMetricReconstruction {
	// use a camera model where the principle point (cx,cy) is exactly the image center to make testing easier
	// since this is a fundamental assumption/approximation
	CameraPinhole intrinsic = new CameraPinhole(400,400,0,400,300,800,600);
	// The found intrinsic will have the principle point be the origin
	CameraPinhole intrinsicZero = new CameraPinhole(400,400,0,0,0,800,600);

	/**
	 * Put it all together and give it very good data. See if it can produce a consistent scene with low reprojection
	 * error
	 */
	@Test
	void process_almost_perfect_scene() {
		var db = new MockLookupSimilarImagesCircleAround().setIntrinsic(intrinsic).init(10,1);
		ProjectiveToMetricReconstruction alg = createAlg();

		SceneWorkingGraph working = db.createWorkingGraph();

		// Add a "tiny" bit of error
		working.views.values().iterator().next().projective.data[1] += 0.01;

		assertTrue(alg.process(db,working));

		double scaleFactor = 0.0;
		for (int truthIdx = 0; truthIdx < db.viewIds.size(); truthIdx++) {
			// won't be perfect since it is optimizing more degrees of freedom that the input model had, e.g. radial distortion
			String id = db.viewIds.get(truthIdx);
			SceneWorkingGraph.View v = working.views.get(id);
			// check the cameras out
			assertEquals(intrinsic.fx, v.pinhole.fx, 0.1);
			assertEquals(intrinsic.fy, v.pinhole.fy, 0.1);
			// principle point should be recentered
			assertEquals(intrinsic.cx, v.pinhole.cx, 1e-7);
			assertEquals(intrinsic.cy, v.pinhole.cy, 1e-7);
			BundlePinholeSimplified bundle = (BundlePinholeSimplified)alg.getRefinedCamera(id);
			assertEquals(0.0,bundle.k1, 0.001);
			assertEquals(0.0,bundle.k2, 0.001);

			// See if the ofund transform is equal up to a scale factor
			Se3_F64 truth_world_to_view = db.listOriginToView.get(truthIdx);
			assertTrue(GeoTestingOps.isEqualsScale(v.world_to_view,truth_world_to_view,0.01,0.01));

			if( truthIdx == 0 ) {
				scaleFactor = v.world_to_view.T.norm()/truth_world_to_view.T.norm();
			} else {
				// scale factor should not change
				double found = v.world_to_view.T.norm()/truth_world_to_view.T.norm();;
				assertEquals(scaleFactor,found,1e-3);
			}
		}

		// Go through every observation and if it has low reprojection error
		working.features.forEach(f-> f.observations.forEach(o->{
			Point2D_F64 found = new Point2D_F64();
			BundleAdjustmentCamera camera = alg.structure.getCameras().get(o.view.index).model;
			Point3D_F64 cameraX = new Point3D_F64();
			SePointOps_F64.transform(o.view.world_to_view,f.location,cameraX);
			camera.project(cameraX.x,cameraX.y,cameraX.z,found);
			found.x += o.view.pinhole.width/2;
			found.y += o.view.pinhole.height/2;
			assertEquals(0.0,o.pixel.distance(found), 1e-5);
		}));
	}

	@Test
	void upgradeViewsToMetric() {
		var db = new MockLookupSimilarImagesCircleAround().setIntrinsic(intrinsic).init(6,1);
		var alg = new ProjectiveToMetricReconstruction();
		alg.config = new ConfigProjectiveToMetric();

		alg.initialize(db,db.createWorkingGraph());
		assertTrue(alg.upgradeViewsToMetric());

		assertEquals(6,alg.workViews.size());
		for( SceneWorkingGraph.View v : alg.workViews ) {
			assertEquals(intrinsic.fx, v.pinhole.fx, UtilEjml.TEST_F64_SQ);
			assertEquals(intrinsic.fy, v.pinhole.fy, UtilEjml.TEST_F64_SQ);
			assertEquals(intrinsic.width, v.pinhole.width);
			assertEquals(intrinsic.height, v.pinhole.height);
		}
	}

	@Test
	void createFeaturesFromInliers() {
		var db = new MockLookupSimilarImagesCircleAround().setIntrinsic(intrinsic).init(6,1);
		ProjectiveToMetricReconstruction alg = createAlg();

		SceneWorkingGraph working = db.createWorkingGraph();

		alg.initialize(db,working);
		// set metric transform up using ground truth
		db.listOriginToView.forEach((i,o)-> working.views.get(db.viewIds.get(i)).world_to_view.set(o));
		alg.workViews.forEach(o->o.pinhole.set(intrinsicZero));

		alg.createFeaturesFromInliers();

		assertEquals(db.numFeatures, working.features.size());
		working.features.forEach(o-> assertEquals(6,o.observations.size()));
		working.views.values().forEach(o->assertEquals(db.numFeatures,o.obs_to_feat.values().length));

		// Since every feature is visible in every frame and the inlier was set was constructed so that index 'i' in
		// the inlier set matches with feature 'i' this test will work
		for (int featCnt = 0; featCnt < db.numFeatures; featCnt++) {
			assertEquals(0.0,
					working.features.get(featCnt).location.distance(db.feats3D.get(featCnt)),1e-7);
			SceneWorkingGraph.Feature f = working.features.get(featCnt);
			f.observations.forEach( o->assertSame(f,o.view.obs_to_feat.get(o.observationIdx)));
		}
	}

	/**
	 * It should not create a new feature if triangulation failed. Make sure that is handled gracefully
	 */
	@Test
	void createFeaturesFromInliers_failed_triangulation() {
		var db = new MockLookupSimilarImagesCircleAround().init(6,1);
		ProjectiveToMetricReconstruction alg = createAlg();

		SceneWorkingGraph working = db.createWorkingGraph();

		alg.initialize(db,working);
		alg.createFeaturesFromInliers();

		assertEquals(0, working.features.size());
	}

	@Test
	void mergeFeatures() {
		var db = new MockLookupSimilarImagesCircleAround().setIntrinsic(intrinsic).init(6,1);
		ProjectiveToMetricReconstruction alg = createAlg();
		SceneWorkingGraph working = db.createWorkingGraph();
		alg.initialize(db,working);

		// Pick a view and a couple of other views it's connected to
		SceneWorkingGraph.View v0 = alg.workViews.get(0);
		SceneWorkingGraph.View v1 = working.lookupView(v0.pview.connections.get(0).other(v0.pview).id);
		SceneWorkingGraph.View v2 = working.lookupView(v0.pview.connections.get(1).other(v0.pview).id);

		SceneWorkingGraph.Feature src = new SceneWorkingGraph.Feature();
		SceneWorkingGraph.Feature dst = new SceneWorkingGraph.Feature();

		// A bunch of boilerplate to set up the data structures
		List<SceneWorkingGraph.Observation> obsList = new ArrayList<>();
		obsList.add( new SceneWorkingGraph.Observation(v0,3) );
		obsList.add( new SceneWorkingGraph.Observation(v0,4) );
		obsList.add( new SceneWorkingGraph.Observation(v1,0) );
		obsList.add( new SceneWorkingGraph.Observation(v2,0) );

		src.observations.add(obsList.get(0));
		src.observations.add(obsList.get(2));
		src.observations.add(obsList.get(1));
		dst.observations.add(obsList.get(3));

		for( SceneWorkingGraph.Observation o : src.observations ) {
			o.view.obs_to_feat.put(o.observationIdx,src);
		}

		for( SceneWorkingGraph.Observation o : dst.observations ) {
			o.view.obs_to_feat.put(o.observationIdx,dst);
		}

		working.features.add(src);
		working.features.add(dst);

		alg.mergeFeatures(src,dst);

		// see if the src was removed from everything
		assertFalse(working.features.contains(src));
		for( var f : v0.obs_to_feat.values() ) {
			assertNotSame(src, f);
		}
		assertEquals(4,dst.observations.size());
		dst.observations.forEach(o->assertTrue(obsList.contains(o)));
		dst.observations.forEach(o->assertSame(dst,o.view.obs_to_feat.get(o.observationIdx)));
	}

	@Test
	void triangulateFeature() {
		var db = new MockLookupSimilarImagesCircleAround().setIntrinsic(intrinsic).init(6,1);

		ProjectiveToMetricReconstruction alg = createAlg();
		SceneWorkingGraph working = db.createWorkingGraph();
		alg.initialize(db,working);
		// set metric transform up using ground truth
		db.listOriginToView.forEach((i,o)-> working.views.get(db.viewIds.get(i)).world_to_view.set(o));
		alg.workViews.forEach(o->o.pinhole.set(intrinsicZero));

		Point3D_F64 X = new Point3D_F64();
		SceneWorkingGraph.View view0 = alg.workViews.get(0);

		alg.loadInlierObservations(view0.projectiveInliers);

		// Since every feature is visible in every frame and the inlier was set was constructed so that index 'i' in
		// the inlier set matches with feature 'i' this test will work
		for (int featCnt = 0; featCnt < db.numFeatures; featCnt++) {
			alg.triangulateFeature(view0.projectiveInliers,featCnt,X);
			assertEquals(0.0,X.distance(db.feats3D.get(featCnt)),1e-7);
		}
	}

	/**
	 * Performs various tests. Because of the overhead in configuring the scene these were all done in one test.
	 */
	@Test
	void pruneObservationsBehindCamera() {
		var db = new MockLookupSimilarImagesCircleAround().setIntrinsic(intrinsic).init(6,1);

		ProjectiveToMetricReconstruction alg = createAlg();
		SceneWorkingGraph working = db.createWorkingGraph();
		alg.initialize(db,working);

		// set metric transform up using ground truth
		db.listOriginToView.forEach((i,o)-> working.views.get(db.viewIds.get(i)).world_to_view.set(o));
		alg.workViews.forEach(o->o.pinhole.set(intrinsicZero));

		// create a very simple scene with all features at the origin and all visible
		for (int featCnt = 0; featCnt < db.numFeatures; featCnt++) {
			SceneWorkingGraph.Feature f = new SceneWorkingGraph.Feature();
			f.location.set(0,0,0); // origin should be visible by all
			for( SceneWorkingGraph.View v : working.views.values() ) {
				SceneWorkingGraph.Observation o = new SceneWorkingGraph.Observation(v,featCnt);
				f.observations.add(o);
				v.obs_to_feat.put(featCnt,f);
			}
			working.features.add(f);
		}

		//------------------------------------------------------------------------------------------------
		// nothing should change in this situation!
		alg.pruneObservationsBehindCamera();
		assertEquals(db.numFeatures, working.features.size());
		working.features.forEach(f->assertEquals(6,f.observations.size()));
		working.views.values().forEach(v->assertEquals(db.numFeatures,v.obs_to_feat.size()));

		//------------------------------------------------------------------------------------------------
		// pick an arbitrary feature and move it into the distance. It's bound ot be behind something
		SceneWorkingGraph.Feature f = working.features.get(10);
		f.location.x = -1000;
		assertEquals(6,f.observations.size());

		alg.pruneObservationsBehindCamera();
		// All the features should still be there
		assertEquals(db.numFeatures, working.features.size());

		// see if it had some of the observation removed
		assertTrue(f.observations.size()<6);

		//  see if it was removed from any of the views
		int totalWithMissing = 0;
		for (int i = 0; i < alg.workViews.size(); i++) {
			if( alg.workViews.get(i).obs_to_feat.size() != db.numFeatures )
				totalWithMissing++;
		}
		assertTrue(totalWithMissing>0 && totalWithMissing < 6);

		//------------------------------------------------------------------------------------------------
		// F will have no observations. It should be removed
		f.observations.clear();
		alg.pruneObservationsBehindCamera();
		assertEquals(db.numFeatures-1, working.features.size());

	}

	@Test
	void refineWithBundleAdjustment() {
		// Skipping bundle adjustment related function since they are tested in the over all
		// I was also lazy. They should be tested but if there was a mistake it would be really obvious
	}

	private static ProjectiveToMetricReconstruction createAlg() {
		var configTri = new ConfigTriangulation();
		configTri.type = ConfigTriangulation.Type.GEOMETRIC;

		var sba = FactoryMultiView.bundleSparseMetric(null);

		return new ProjectiveToMetricReconstruction(
				new ConfigProjectiveToMetric(),
				sba,
				FactoryMultiView.triangulateNViewCalibrated(configTri));
	}
}
