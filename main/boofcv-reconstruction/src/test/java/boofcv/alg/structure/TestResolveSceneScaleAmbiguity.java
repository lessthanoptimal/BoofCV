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
import boofcv.alg.structure.ResolveSceneScaleAmbiguity.SceneInfo;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestResolveSceneScaleAmbiguity extends BoofStandardJUnit {

	// Description of the scene
	List<Se3_F64> listWorldToView = new ArrayList<>();
	List<CameraPinhole> listCameras = new ArrayList<>();
	List<Point3D_F64> listFeatures3D;

	public TestResolveSceneScaleAmbiguity() {
		listFeatures3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 4), -2, 2, -2, 2, -2, 1, 50, rand);

		for (int i = 0; i < 6; i++) {
			Se3_F64 worldToView = SpecialEuclideanOps_F64.eulerXyz(
					-1.0 + i*0.4, rand.nextGaussian()*0.1, rand.nextGaussian()*0.01,
					rand.nextDouble()*0.01, rand.nextDouble()*0.01, rand.nextDouble()*0.01, null);
			listWorldToView.add(worldToView);
			listCameras.add(new CameraPinhole(500 - i*2, 500 + i*3, 0, 600, 600, 0, 0));
		}
	}

	/**
	 * Simple scene with a known solution that exercises the entire pipeline
	 */
	@Test void simpleScene() {
		var alg = new ResolveSceneScaleAmbiguity();

		double scale_2_to_1 = 2.5;

		// Create a set of views that are not the same in each scene
		alg.initialize(listFeatures3D.size());
		alg.setScene1(createPixelObservations(2, 0, 1),
				createWorldToView(scale_2_to_1, 2, 0, 1), createPixelToNorm(2, 0, 1));
		alg.setScene2(createPixelObservations(2, 1, 5, 3),
				createWorldToView(1.0, 2, 1, 5, 3), createPixelToNorm(2, 1, 5, 3));

		var found = new ScaleSe3_F64();
		assertTrue(alg.process(found));

		assertEquals(1.0/scale_2_to_1, found.scale, 1e-4);
		// after scale has been taken in account the SE3 should be identity
		assertEquals(0.0, found.transform.T.norm(), 1e-4);
	}

	@Test void selectScaleMinimumLocalVariance() {
		var alg = new ResolveSceneScaleAmbiguity();

		double expectedScale = 4.0;

		// Create scales with a bunch of noise
		alg.scales.resize(100, ( idx ) -> rand.nextDouble()*20);
		// Create a cluster around the true value
		for (int i = 0; i < 30; i++) {
			alg.scales.set(i*2 + 10, expectedScale + rand.nextGaussian()*0.01);
		}

		assertEquals(expectedScale, alg.selectScaleMinimumLocalVariance(), 0.02);
	}

	@Test void triangulate() {
		double scale = 1.5;

		ResolveSceneScaleAmbiguity.SceneInfo scene = createInfo(scale, 1, 2, 3);
		var alg = new ResolveSceneScaleAmbiguity();
		alg.initialize(listFeatures3D.size());
		assertTrue(alg.triangulate(scene, 10));

		var expected = new Point3D_F64();
		listWorldToView.get(1).transform(listFeatures3D.get(10), expected);

		Point4D_F64 found = scene.location;

		assertEquals(scale*expected.z, found.z/found.w, UtilEjml.TEST_F64);
	}

	/**
	 * Creates scene from the specified views
	 *
	 * @param scale Scale that's applied to translation
	 * @param viewIndexes Which views to include
	 */
	private SceneInfo createInfo( double scale, int... viewIndexes ) {
		var scene = new SceneInfo();
		scene.listWorldToView = createWorldToView(scale, viewIndexes);
		scene.intrinsics = createPixelToNorm(viewIndexes);
		scene.features = createPixelObservations(viewIndexes);

		Se3_F64 common_to_world = listWorldToView.get(viewIndexes[0]).invert(null);
		for (int viewIdx : viewIndexes) {
			Se3_F64 common_to_view = scene.listZeroToView.grow();
			common_to_world.concat(listWorldToView.get(viewIdx), common_to_view);
			common_to_view.T.scale(scale);
		}

		return scene;
	}

	private List<Se3_F64> createWorldToView( double scale, int... viewIndexes ) {
		var ret = new ArrayList<Se3_F64>();

		for (int viewIdx : viewIndexes) {
			var p = listWorldToView.get(viewIdx).copy();
			p.T.scale(scale);
			ret.add(p);
		}

		return ret;
	}

	private List<Point2Transform2_F64> createPixelToNorm( int... viewIndexes ) {
		var ret = new ArrayList<Point2Transform2_F64>();

		for (int viewIdx : viewIndexes) {
			var pixel_to_norm = new LensDistortionPinhole(listCameras.get(viewIdx)).
					distort_F64(true, false);
			ret.add(pixel_to_norm);
		}

		return ret;
	}

	private ResolveSceneScaleAmbiguity.FeatureObservations createPixelObservations( int... viewIndexes ) {
		return ( viewIdx, featureIdx, pixel ) -> {
			CameraPinhole intrinsic = listCameras.get(viewIndexes[viewIdx]);
			Se3_F64 world_to_camera = listWorldToView.get(viewIndexes[viewIdx]);
			Point3D_F64 X = listFeatures3D.get(featureIdx);
			PerspectiveOps.renderPixel(world_to_camera, intrinsic, X, pixel);
		};
	}
}
