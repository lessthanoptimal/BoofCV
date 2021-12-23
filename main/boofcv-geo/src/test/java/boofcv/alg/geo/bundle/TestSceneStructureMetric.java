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

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestSceneStructureMetric extends BoofStandardJUnit {
	@Test void assignIDsToRigidPoints() {
		var scene = new SceneStructureMetric(false);

		scene.initialize(1, 2, 2, 3, 2);
		scene.setRigid(0, false, new Se3_F64(), 2);
		scene.setRigid(1, true, new Se3_F64(), 3);

		scene.assignIDsToRigidPoints();

		for (int i = 0; i < 2; i++) {
			assertEquals(0, scene.lookupRigid[i]);
		}
		for (int i = 0; i < 3; i++) {
			assertEquals(1, scene.lookupRigid[i + 2]);
		}

		assertEquals(0, scene.rigids.data[0].indexFirst);
		assertEquals(2, scene.rigids.data[1].indexFirst);
	}

	@Test void rigid_Reset() {
		var scene = new SceneStructureMetric(false);
		scene.initialize(1, 2, 2, 3, 2);
		SceneStructureMetric.Rigid r = scene.rigids.get(0);
		r.indexFirst = 2;
		r.known = true;
		r.object_to_world.T.x = 10;
		r.init(4, 3);
		assertEquals(-1, r.indexFirst);
		assertFalse(r.known);
		assertEquals(0, r.object_to_world.T.x, 1e-8);
		assertEquals(4, r.points.length);
		assertEquals(3, r.points[0].coordinate.length);

		r.init(4, 4);
		assertEquals(4, r.points.length);
		assertEquals(4, r.points[0].coordinate.length);

		r.init(3, 4);
		assertEquals(3, r.points.length);
		assertEquals(4, r.points[0].coordinate.length);
	}

	@Test void getParentToView() {
		var scene = new SceneStructureMetric(false);

		scene.initialize(1, 3, 4);
		// add a motion to make the index off by one
		scene.addMotion(false, new Se3_F64());
		for (int i = 0; i < scene.views.size; i++) {
			scene.setView(i, -1, true, SpecialEuclideanOps_F64.eulerXyz(i, 0, 0, 0, 0, 0, null));
		}
		for (int i = 0; i < scene.views.size; i++) {
			assertEquals(i, scene.getParentToView(i).T.x, UtilEjml.TEST_F64);
		}
	}

	@Test void getWorldToView() {
		var scene = new SceneStructureMetric(false);

		scene.initialize(1, 4, 4);

		// Create a chained scene
		for (int i = 0; i < scene.views.size; i++) {
			scene.setView(i, -1, true, SpecialEuclideanOps_F64.eulerXyz(i + 1, 0, 0, 0, 0, 0, null), i - 1);
		}

		// See if the views properly concatenate
		int location = 1;
		for (int viewIdx = 0; viewIdx < scene.views.size; viewIdx++) {
			SceneStructureMetric.View v = scene.views.data[viewIdx];
			assertEquals(location, scene.getWorldToView(v, null, null).T.x, UtilEjml.TEST_F64);
			location += viewIdx + 2;
		}
	}

	@Test void projectToPixel_3D() {
		var scene = new SceneStructureMetric(false);
		var intrinsic = new CameraPinhole(100, 100, 0, 0, 0, 300, 300);
		var world_to_view = SpecialEuclideanOps_F64.eulerXyz(1, 0, 0, 0, 0.01, -0.04, null);

		scene.initialize(1, 1, 1);
		scene.setView(0, 0, true, world_to_view);
		scene.setCamera(0, true, intrinsic);
		scene.setPoint(0, 0.1, -0.5, 1.1);

		// Point in front of camera
		var pixel = new Point2D_F64();
		assertTrue(scene.projectToPixel(0, 0, new Se3_F64(), new Se3_F64(), new Point3D_F64(), pixel));

		var expected = new Point2D_F64();
		PerspectiveOps.renderPixel(world_to_view,intrinsic,new Point3D_F64(0.1, -0.5, 1.1), expected);

		assertEquals(expected.x, pixel.x, UtilEjml.TEST_F64);
		assertEquals(expected.y, pixel.y, UtilEjml.TEST_F64);

		// sanity check for behind
		scene.setPoint(0, 0.1, -0.5, -1.1);
		assertFalse(scene.projectToPixel(0, 0, new Se3_F64(), new Se3_F64(), new Point3D_F64(), pixel));
	}
}
