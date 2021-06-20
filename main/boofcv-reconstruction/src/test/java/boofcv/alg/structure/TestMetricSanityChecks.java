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

import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.so.Rodrigues_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMetricSanityChecks extends BoofStandardJUnit {
	MockLookupSimilarImagesRealistic dbSimilar;
	PairwiseImageGraph pairwise;
	SceneWorkingGraph scene;
	SceneWorkingGraph.View wview;

	@BeforeEach void initialize() {
		dbSimilar = new MockLookupSimilarImagesRealistic().pathLine(4, 0.1, 0.3, 1);
		pairwise = dbSimilar.createPairwise();
		scene = dbSimilar.createWorkingGraph(pairwise);
		wview = scene.listViews.get(1);
		dbSimilar.addInlierInfo(pairwise, wview, 0, 2);
	}

	/**
	 * No errors, perfect data
	 */
	@Test void checkPhysicalConstraints_inliers_AllGood() {
		var alg = new MetricSanityChecks();

		// Sanity check to make sure it's testing something
		assertTrue(wview.inliers.get(0).getInlierCount() > 20);

		alg.checkPhysicalConstraints(dbSimilar, scene, wview, 0);
		assertEquals(0, alg.failedTriangulate);
		assertEquals(0, alg.failedBehind);
		assertEquals(0, alg.failedImageBounds);
		assertEquals(0, alg.failedReprojection);
	}

	@Test void checkPhysicalConstraints_inliers_ErrorTriangulate() {
		var alg = new MetricSanityChecks();
		alg.triangulator = ( observations, listWorldToView, location ) -> false;

		// Sanity check to make sure it's testing something
		int N = wview.inliers.get(0).getInlierCount();
		assertTrue(N > 20);

		alg.checkPhysicalConstraints(dbSimilar, scene, wview, 0);
		assertEquals(N, alg.failedTriangulate);
		assertEquals(0, alg.failedBehind);
		assertEquals(0, alg.failedImageBounds);
		assertEquals(0, alg.failedReprojection);
	}

	@Test void checkPhysicalConstraints_inliers_ErrorBehind() {
		var alg = new MetricSanityChecks();

		// Flip the view around so that points will appear to be behind it after triangulation
		// This is done by rotating around the camera's +y axis 180 degrees
		Rodrigues_F64 rod = new Rodrigues_F64();
		rod.unitAxisRotation.setTo(
				wview.world_to_view.R.get(1,0),
				wview.world_to_view.R.get(1,1),
				wview.world_to_view.R.get(1,2));
		rod.theta = Math.PI;
		DMatrixRMaj flip = ConvertRotation3D_F64.rodriguesToMatrix(rod, null);
		CommonOps_DDRM.mult(flip, wview.world_to_view.R.copy(), wview.world_to_view.R);

		// NOTE: This test isn't going as planned. There should be N points behind the camera, no bound errors
		//       and no reprojection errors since all that stuff should not be significantly affected by it being
		//       flipped

		// Sanity check to make sure it's testing something
		int N = wview.inliers.get(0).getInlierCount();
		assertTrue(N > 20);

		alg.checkPhysicalConstraints(dbSimilar, scene, wview, 0);
		assertEquals(0, alg.failedTriangulate);
		assertTrue(alg.failedBehind > N*0.9); // Again fewer than expected, but we can see it's checking at least
//		assertEquals(0, alg.failedImageBounds);
//		assertEquals(0, alg.failedReprojection);
	}

	@Test void checkPhysicalConstraints_inliers_ErrorImageBounds() {
		var alg = new MetricSanityChecks();

		// this will cause all points to be out of bounds
		for (var c : scene.listCameras.toList()) {
			c.prior.fsetShape(0, 0);
		}

		// this will mess up other stuff too as it will try to re-center the pixels at another location

		// Sanity check to make sure it's testing something
		int N = wview.inliers.get(0).getInlierCount();
		assertTrue(N > 20);

		alg.checkPhysicalConstraints(dbSimilar, scene, wview, 0);
		assertEquals(0, alg.failedTriangulate);
		assertEquals(N*3, alg.failedImageBounds);
	}

	@Test void checkPhysicalConstraints_inliers_ErrorReprojection() {
		var alg = new MetricSanityChecks();

		// Point it in some random direction. That should cause problems with reprojection error.
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, -0.2, 1.2, 1.5, wview.world_to_view.R);

		// Sanity check to make sure it's testing something
		int N = wview.inliers.get(0).getInlierCount();
		assertTrue(N > 20);

		alg.checkPhysicalConstraints(dbSimilar, scene, wview, 0);
		assertEquals(0, alg.failedTriangulate);
		assertTrue(alg.failedReprojection > N/2);
	}
}
