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

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.mvs.StereoPairGraph;
import boofcv.alg.structure.GenerateStereoPairGraphFromScene.View;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestGenerateStereoPairGraphFromScene extends BoofStandardJUnit {
	/**
	 * Simple test where viewA and viewB only share very far away points and should have a low score. A and C
	 * share some much closer points and should have a good score.
	 */
	@Test void allTogether() {
		var viewToId = new TIntObjectHashMap<String>();
		var scene = new SceneStructureMetric(false);

		scene.initialize(3, 3, 10);
		for (int i = 0; i < 3; i++) {
			viewToId.put(i, "" + (i + 1));
			scene.setView(i, i, true, new Se3_F64());
			scene.setCamera(i, true, new CameraPinhole(200, 200, 0, 150, 150, 300, 300));
			scene.motions.get(i).motion.T.x = i;
		}

		// Create the points far away
		for (int i = 0; i < 5; i++) {
			scene.setPoint(i, 0, 0, 1e4);
			scene.points.get(i).views.setTo(0, 1);
		}

		// Add the up close points
		for (int i = 5; i < 10; i++) {
			scene.setPoint(i, 0, 0, 4);
			scene.points.get(i).views.setTo(0, 2);
		}

		var alg = new GenerateStereoPairGraphFromScene();
		alg.process(viewToId, scene);

		StereoPairGraph found = alg.getStereoGraph();
		assertEquals(3, found.vertexes.size());
		StereoPairGraph.Vertex vA = Objects.requireNonNull(found.vertexes.get("1"));
		StereoPairGraph.Vertex vB = Objects.requireNonNull(found.vertexes.get("2"));
		StereoPairGraph.Vertex vC = Objects.requireNonNull(found.vertexes.get("3"));

		assertEquals(0, vA.indexSba);
		assertEquals(1, vB.indexSba);
		assertEquals(2, vC.indexSba);

		assertEquals(2, vA.pairs.size());
		assertEquals(1, vB.pairs.size());
		assertEquals(1, vC.pairs.size());

		double quality_a_b = vA.pairs.get(0).quality3D;
		double quality_a_c = vA.pairs.get(1).quality3D;

		assertTrue(quality_a_c > quality_a_b*2);
		assertTrue(quality_a_b > 0.0 && quality_a_b <= 1.0);
		assertTrue(quality_a_c > 0.0 && quality_a_c <= 1.0);
	}

	@Test void matchPointsToViews() {
		var scene = new SceneStructureMetric(false);

		// Create a scene with 3 views and a simple formula relating visible points to views
		scene.initialize(3, 3, 10);
		for (int i = 0; i < 3; i++) {
			scene.setView(i, i, true, new Se3_F64());
		}
		for (int i = 0; i < scene.points.size; i++) {
			scene.setPoint(i, 0, 0, 2);
			scene.points.get(i).views.add(0);
			if (i%2 == 0)
				scene.points.get(i).views.add(1);
			if (i%3 == 0)
				scene.points.get(i).views.add(2);
		}

		// Call the function being tested and see if the expected number of points are matched to each view
		var alg = new GenerateStereoPairGraphFromScene();
		alg.matchPointsToViews(scene);
		assertEquals(3, alg.views.size);
		assertEquals(10, alg.views.get(0).pointing.size);
		assertEquals(5, alg.views.get(1).pointing.size);
		assertEquals(4, alg.views.get(2).pointing.size);
	}

	@Test void computePointingVector_3D() {
		var point = new Point3D_F64(3, 4, -5);
		var location = new Point3D_F64(0.5, -2, 0.1);

		var scene = new SceneStructureMetric(false);
		scene.initialize(1, 1, 1);
		scene.setView(0, 0, true, SpecialEuclideanOps_F64.axisXyz(
				-location.x, -location.y, -location.z, 0.1, -0.2, 0.3, null));
		scene.setPoint(0, point.x, point.y, point.z);

		var alg = new GenerateStereoPairGraphFromScene();
		var found = new Vector3D_F64();
		alg.computePointingVector(scene.getParentToView(0), scene.points.get(0), false, found);

		var expected = new Vector3D_F64(location, point);
		expected.normalize();

		assertEquals(0.0, expected.distance(found), UtilEjml.TEST_F64);
	}

	@Test void computePointingVector_Homogenous() {
		var point = new Point3D_F64(0.5, -2, 31.0);
		var location = new Point3D_F64(0.5, -0.1, 1.6);
		var expected = new Vector3D_F64(location, point);
		expected.normalize();

		// Negative w needs to be handled carefully when normalizing. So test negative and positive
		for (double w : new double[]{-3.2, 3.2}) {
			var scene = new SceneStructureMetric(true);
			scene.initialize(1, 1, 1);
			scene.setView(0, 0, true, SpecialEuclideanOps_F64.eulerXyz(
					-location.x, -location.y, -location.z, 0.1, -0.04, 0.02, null));
			scene.setPoint(0, point.x*w, point.y*w, point.z*w, w);

			var alg = new GenerateStereoPairGraphFromScene();
			Vector3D_F64 found = new Vector3D_F64();
			alg.computePointingVector(scene.getParentToView(0), scene.points.get(0), true, found);

			assertEquals(0.0, expected.distance(found), UtilEjml.TEST_F64);
		}
	}

	@Test void connect() {
		var alg = new GenerateStereoPairGraphFromScene();

		// create a few views to connect
		View viewA = alg.views.grow();
		View viewB = alg.views.grow();
		View viewC = alg.views.grow();

		// Connect the first two then check the results
		alg.connect(0, 1);
		assertTrue(viewA.connectedViews.isEquals(1));
		assertTrue(viewB.connectedViews.isEquals(0));

		// Nothing should happen here
		alg.connect(1, 0);
		assertTrue(viewA.connectedViews.isEquals(1));
		assertTrue(viewB.connectedViews.isEquals(0));

		// Add another connection
		alg.connect(2, 1);
		assertTrue(viewA.connectedViews.isEquals(1));
		assertTrue(viewB.connectedViews.isEquals(0, 2));
		assertTrue(viewC.connectedViews.isEquals(1));
	}

	@Test void findCommonFeatureAngles() {
		var alg = new GenerateStereoPairGraphFromScene();

		// The two views will share some points
		View viewA = alg.views.grow();
		View viewB = alg.views.grow();

		viewA.pointIndexes.setTo(2, 3, 4, 5, 6, 10, 11, 12);
		viewB.pointIndexes.setTo(0, 1, 2, 3, 5, 6, 9, 11, 12);

		// If it correctly aligns the points the acute angles will all be a very small number
		// didn't want it to be zero since that's a programming error mode
		for (int i = 0; i < 15; i++) {
			if (viewA.pointIndexes.contains(i)) {
				viewA.pointing.grow().setTo(1, 0.1*i, i*0.2 - 3);
			}
			if (viewB.pointIndexes.contains(i)) {
				viewB.pointing.grow().setTo(1.001, 0.1*i, i*0.2 - 3);
			}
		}

		// Run the function being tested
		alg.findCommonFeatureAngles(viewA, viewB);

		// acute angle should be very small but not zero
		assertEquals(6, alg.acuteAngles.size());
		alg.acuteAngles.forEach(v -> {
			assertEquals(0.0, v, 1e-3);
			assertTrue(v != 0.0);
		});
	}
}
