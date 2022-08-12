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

package boofcv.alg.structure;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.MetricCameras;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestInitializeCommonMetric extends GenericInitializeCommon {
	/**
	 * Test scenario with 3 views and perfect data under different configurations and different views as the seed.
	 */
	@Test void perfect_connections_3() {
		// Turn off SBA to make sure initialization is done correctly. It should be nearly perfect
		var alg = new InitializeCommonMetric();
		alg.pixelToMetric3.convergeSBA.maxIterations = 0; // turns off SBA

		performPerfectConnections(alg);

		// Do it with SBA and see how well it performs
		performPerfectConnections(new InitializeCommonMetric());
	}

	private void performPerfectConnections( InitializeCommonMetric alg ) {
		for (int seedIdx = 0; seedIdx < 3; seedIdx++) {
			var dbSimilar = new MockLookupSimilarImagesRealistic().pathCircle(4, 2);
			var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

			PairwiseImageGraph graph = dbSimilar.createPairwise();

			PairwiseImageGraph.View seed = graph.nodes.get(seedIdx);
			var seedConnIdx = DogArray_I32.array(0, 2);

			// in this specific scenario all features are visible by all frames
			var seedFeatsIdx = DogArray_I32.range(0, dbSimilar.numFeatures);
			// however not all features are in the inlier set. Remove all features not in inlier set
			removeConnectionOutliers(seed, seedFeatsIdx);

			var results = new MetricCameras();

			// Reconstruct the projective scene
			assertTrue(alg.metricScene(dbSimilar, dbCams, seed, seedConnIdx, results));

			// test results
			checkReconstruction(alg, dbSimilar, seedConnIdx);
		}
	}

	/**
	 * Check reconstruction by seeing if it's consistent with the input observations
	 */
	void checkReconstruction( InitializeCommonMetric alg,
							  MockLookupSimilarImagesRealistic db,
							  DogArray_I32 seedConnIdx) {

		final SceneStructureMetric structure = alg.getStructure();

		// Sanity check the number of each type of structure
		assertEquals(seedConnIdx.size + 1, structure.views.size);

		List<String> viewIds = BoofMiscOps.collectList(db.views, v -> v.id);
		int dbIndexSeed = viewIds.indexOf(alg.getPairwiseGraphViewByStructureIndex(0).id);

		// Check results for consistency. Can't do a direct comparision to ground truth since a different
		// but equivalent projective frame would have been estimated.
		var X = new Point4D_F64();
		var Xview = new Point4D_F64();
		var found = new Point2D_F64();
		var global_to_view = new Se3_F64();
		DogArray_I32 inlierToSeed = alg.inlierIndexes.get(0);
		for (int i = 0; i < inlierToSeed.size; i++) {
			int seedFeatureIdx = inlierToSeed.get(i);
			int truthFeatIdx = db.observationToFeatureIdx(dbIndexSeed, seedFeatureIdx);
			int structureIdx = alg.seedToStructure.get(seedFeatureIdx);
			assertTrue(structureIdx >= 0); // only features that have structure should be in this list

			// Get the estimated point in 3D
			structure.points.get(structureIdx).get(X);

			// Project the point to the camera using found projection matrices
			for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
				int viewDbIdx = viewIds.indexOf(alg.getPairwiseGraphViewByStructureIndex(viewIdx).id);
				// Project this feature to the camera
				SceneStructureMetric.View view = structure.views.get(viewIdx);

				structure.getWorldToView(view, global_to_view, null);
				global_to_view.transform(X, Xview);

				BundleAdjustmentCamera cam = Objects.requireNonNull(structure.getViewCamera(view).getModel());
				cam.project(Xview.x, Xview.y, Xview.z, found);

				// undo the offset
				found.x += db.intrinsic.cx;
				found.y += db.intrinsic.cy;

				// Lookup the expected pixel location
				// The seed feature ID and the ground truth feature ID are the same
				Point2D_F64 expected = db.featureToObservation(viewDbIdx, truthFeatIdx).pixel;

				assertEquals(0.0, expected.distance(found), reprojectionTol);
			}
		}
	}
}
