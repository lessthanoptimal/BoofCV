/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.trifocal;

import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestResolveThreeViewScale extends BoofStandardJUnit {
	/**
	 * Feed it a scenario with no noise and see if it works
	 */
	@Test void basic() {
		basic(1.5);
		basic(-1.5);
	}

	void basic( double adjustedScale ) {
		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2),
				-1, 1, -1, 1, -0.1, 0.1, 20, rand);

		// Select arbitrary locations for each view
		Se3_F64 world_to_view2 = SpecialEuclideanOps_F64.eulerXyz(0.4, 0.04, 0.02, 0, 0.01, 0.02, null);
		Se3_F64 world_to_view3 = SpecialEuclideanOps_F64.eulerXyz(0.7, -0.1, 0.03, 0.01, 0.0, -0.01, null);

		// Render perfect observations as normalized image coordinates
		var triples = new DogArray<>(AssociatedTriple::new);
		var XX = new Point3D_F64();
		for (var X : cloud) {
			AssociatedTriple t = triples.grow();
			t.p1.x = X.x/X.z;
			t.p1.y = X.y/X.z;

			world_to_view2.transform(X, XX);
			t.p2.x = XX.x/XX.z;
			t.p2.y = XX.y/XX.z;

			world_to_view3.transform(X, XX);
			t.p3.x = XX.x/XX.z;
			t.p3.y = XX.y/XX.z;
		}

		// mess up the scale
		world_to_view3.T.scale(adjustedScale);

		var alg = new ResolveThreeViewScaleAmbiguity();
		assertTrue(alg.process(triples.toList(), world_to_view2, world_to_view3));

		// Test that largest scale is 1
		double maxScale = Math.max(world_to_view2.T.norm(), world_to_view3.T.norm());
		assertEquals(1.0, maxScale, UtilEjml.TEST_F64);

		// See if it triangulates to the same point for all
		var found2 = new Point4D_F64();
		var found3 = new Point4D_F64();

		for (int i = 0; i < cloud.size(); i++) {
			AssociatedTriple t = triples.get(i);
			alg.triangulate.triangulate(t.p1, t.p2, world_to_view2, found2);
			alg.triangulate.triangulate(t.p1, t.p3, world_to_view3, found3);

			found2.normalize();
			found3.normalize();
			if (found2.w < 0 != found3.w < 0)
				found3.scale(-1);

			assertEquals(0.0, found2.distance(found3), UtilEjml.TEST_F64);
		}
	}
}
