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

import boofcv.BoofTesting;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.pose.CompatibleProjectiveHomography;
import boofcv.alg.structure.PairwiseImageGraph.Motion;
import boofcv.alg.structure.PairwiseImageGraph.View;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.AssociatedTupleDN;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("IntegerDivisionInFloatingPointContext")
class TestInitializeAllCommonProjective extends BoofStandardJUnit {

	final static double reprojectionTol = 1e-5;
	final static double matrixTol = 1e-3;

	Random rand = BoofTesting.createRandom(3);

	/**
	 * Perfect scene with 2 connections. This is the simplest case
	 */
	@Test void perfect_connections_2() {

		// NOTES: Using regular pixels maxed out at 12
		//        With zero centering maxed out at 14

		var alg = new InitializeAllCommonProjective();
//		alg.utils.sba.setVerbose(System.out,null);

		for (int seedIdx = 0; seedIdx < 3; seedIdx++) {
			var dbSimilar = new MockLookupSimilarImagesRealistic().pathCircle(4, 2);
			var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

			PairwiseImageGraph graph = dbSimilar.createPairwise();

			View seed = graph.nodes.get(seedIdx);
			var seedConnIdx = DogArray_I32.array(0, 2);

			// in this specific scenario all features are visible by all frames
			var seedFeatsIdx = DogArray_I32.range(0, dbSimilar.numFeatures);
			// however not all features are in the inlier set. Remove all features not in inlier set
			removeConnectionOutliers(seed, seedFeatsIdx);

			// Reconstruct the projective scene
			assertTrue(alg.projectiveSceneN(dbSimilar, dbCams, seed, seedFeatsIdx, seedConnIdx));

			// test results
			checkReconstruction(alg, dbSimilar, seedConnIdx, reprojectionTol);
			checkCameraMatrices(alg, dbSimilar);
		}
	}

	/**
	 * Perfect scene with 3 to 5 connections
	 */
	@Test void perfect_connections_3_to_5() {
		var alg = new InitializeAllCommonProjective();

		for (int numConnections = 3; numConnections <= 6; numConnections++) {
			// do a few trials since things are randomized to shake out more bugs potentially
			for (int trial = 0; trial < 3; trial++) {
				var dbSimilar = new MockLookupSimilarImagesRealistic().pathCircle(6, 2);
				var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

				List<String> viewIdStr = dbSimilar.getImageIDs();

				PairwiseImageGraph graph = dbSimilar.createPairwise();
				View seed = graph.nodes.get(0);

				// randomly select the connections
				var seedConnIdx = new DogArray_I32();
				for (int i = 0; i < seed.connections.size; i++) {
					seedConnIdx.add(i);
				}
				for (int i = 0; i < seed.connections.size - numConnections; i++) {
					seedConnIdx.remove(rand.nextInt(seedConnIdx.size));
				}
				PrimitiveArrays.shuffle(seedConnIdx.data, 0, seedConnIdx.size, rand); // order should not matter

				// index of connected views in ground truth
				var connectedViewidx = new DogArray_I32();
				for (int i = 0; i < seedConnIdx.size; i++) {
					String id = seed.connections.get(seedConnIdx.get(i)).other(seed).id;
					connectedViewidx.add(viewIdStr.indexOf(id));
				}

				// in this specific scenario all features are visible by all frames
				var seedFeatsIdx = DogArray_I32.range(0, dbSimilar.numFeatures);
				// however not all features are in the inlier set. Remove all features not in inlier set
				removeConnectionOutliers(seed, seedFeatsIdx);

				PrimitiveArrays.shuffle(seedFeatsIdx.data, 0, seedFeatsIdx.size, rand); // order should not matter

				// Reconstruct the projective scene
				assertTrue(alg.projectiveSceneN(dbSimilar, dbCams, seed, seedFeatsIdx, seedConnIdx));

				// test results
				checkReconstruction(alg, dbSimilar, seedConnIdx, reprojectionTol);
			}
		}
	}

	private void removeConnectionOutliers( View seed, DogArray_I32 seedFeatsIdx ) {
		for (Motion m : seed.connections.toList()) {
			// mark all indexes which are inliers
			boolean isSrc = m.src == seed;
			boolean[] inlier = new boolean[seed.totalObservations];
			for (AssociatedIndex a : m.inliers.toList()) {
				inlier[isSrc ? a.src : a.dst] = true;
			}
			// remove the outliers
			for (int i = 0; i < inlier.length; i++) {
				if (!inlier[i]) {
					int idx = seedFeatsIdx.indexOf(i);
					if (idx >= 0)
						seedFeatsIdx.remove(idx);
				}
			}
		}
	}

	/**
	 * Add a tiny bit of noise and see if it blows up
	 */
	@Test void small_noise() {
		var dbSimilar = new MockLookupSimilarImages(4, 0xDEADBEEF);
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

		var alg = new InitializeAllCommonProjective();

		// These observations are no longer perfect, just a little bit of error
		dbSimilar.viewObs.get(0).get(20).x += 0.1;
		dbSimilar.viewObs.get(3).get(25).y += 0.1;

		checkConfiguration(alg, dbSimilar, dbCams, true, true, false, 0.1);
	}

	/**
	 * Test out different configurations of SBA and see if they all work
	 */
	@Test void configurations_sba() {
		var dbSimilar = new MockLookupSimilarImages(5, 0xDEADBEEF);
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);
		var alg = new InitializeAllCommonProjective();

		checkConfiguration(alg, dbSimilar, dbCams, true, true, false, 1e-4);
		checkConfiguration(alg, dbSimilar, dbCams, true, true, true, 1e-4);
		checkConfiguration(alg, dbSimilar, dbCams, true, false, false, 1e-4);
		checkConfiguration(alg, dbSimilar, dbCams, false, true, false, 1e-4);
		checkConfiguration(alg, dbSimilar, dbCams, false, true, true, 1e-4);
		checkConfiguration(alg, dbSimilar, dbCams, false, false, false, 1e-4);
	}

	private void checkConfiguration( InitializeAllCommonProjective alg,
									 MockLookupSimilarImages dbSimilar, MockLookUpCameraInfo dbCams,
									 boolean threeViews,
									 boolean sba, boolean scale, double reprojectionTol ) {
		alg.utils.configConvergeSBA.maxIterations = sba ? 50 : -1;
		alg.utils.configScaleSBA = scale;

		View seed = dbSimilar.graph.nodes.get(0);
		var seedFeatsIdx = new DogArray_I32();
		var seedConnIdx = threeViews ? DogArray_I32.array(0, 2) : DogArray_I32.array(1, 2, 3);

		// Give a subset as a test to see if the size is being properly pass down the chain
		int offset = 0;
		int numFeatures = dbSimilar.feats3D.size() - offset;
		for (int i = offset; i < numFeatures; i++) {
			seedFeatsIdx.add(i);
		}
		PrimitiveArrays.shuffle(seedFeatsIdx.data, 0, seedFeatsIdx.size, rand); // order should not matter

		// Reconstruct the projective scene
		assertTrue(alg.projectiveSceneN(dbSimilar, dbCams, seed, seedFeatsIdx, seedConnIdx));

		// test results
		checkReconstruction(alg, dbSimilar, seedConnIdx, numFeatures, reprojectionTol);
	}

	/**
	 * Check reconstruction by seeing if it's consistent with the input observations
	 */
	private void checkReconstruction( InitializeAllCommonProjective alg,
									  MockLookupSimilarImages db,
									  DogArray_I32 seedConnIdx,
									  int numFeatures,
									  double reprojectionTol ) {

		final SceneStructureProjective structure = alg.getStructure();

		// Sanity check the number of each type of structure
		assertEquals(seedConnIdx.size + 1, structure.views.size);
		assertEquals(numFeatures, structure.points.size);
		alg.inlierIndexes.forEach(list -> assertEquals(numFeatures, list.size));

		int dbIndexSeed = db.viewIds.indexOf(alg.getPairwiseGraphViewByStructureIndex(0).id);

		// Check results for consistency. Can't do a direct comparison to ground truth since a different
		// but equivalent projective frame would have been estimated.
		Point4D_F64 X = new Point4D_F64();
		Point2D_F64 found = new Point2D_F64();
		for (int i = 0; i < numFeatures; i++) {
			int seedFeatureIdx = alg.inlierIndexes.get(0).get(i);
			int truthFeatIdx = db.viewToFeat.get(dbIndexSeed)[seedFeatureIdx];
			int structureIdx = alg.seedToStructure.get(seedFeatureIdx);
			assertTrue(structureIdx >= 0); // only features that have structure should be in this list

			// Get the estimated point in 3D
			structure.points.get(structureIdx).get(X);

			// Project the point to the camera using found projection matrices
			for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
				int viewDbIdx = db.viewIds.indexOf(alg.getPairwiseGraphViewByStructureIndex(viewIdx).id);
				// Project this feature to the camera
				DMatrixRMaj P = structure.views.get(viewIdx).worldToView;
				PerspectiveOps.renderPixel(P, X, found);
				// undo the offset
				found.x += db.intrinsic.cx;
				found.y += db.intrinsic.cy;

				// Lookup the expected pixel location
				// The seed feature ID and the ground truth feature ID are the same
				int viewFeatIdx = db.featToView.get(viewDbIdx)[truthFeatIdx];
				Point2D_F64 expected = db.viewObs.get(viewDbIdx).get(viewFeatIdx);
				assertEquals(0.0, expected.distance(found), reprojectionTol);
			}
		}
	}

	/**
	 * Check reconstruction by seeing if it's consistent with the input observations
	 */
	private void checkReconstruction( InitializeAllCommonProjective alg,
									  MockLookupSimilarImagesRealistic db,
									  DogArray_I32 seedConnIdx,
									  double reprojectionTol ) {

		final SceneStructureProjective structure = alg.getStructure();

		// Sanity check the number of each type of structure
		assertEquals(seedConnIdx.size + 1, structure.views.size);

		List<String> viewIds = BoofMiscOps.collectList(db.views, v -> v.id);
		int dbIndexSeed = viewIds.indexOf(alg.getPairwiseGraphViewByStructureIndex(0).id);

		// Check results for consistency. Can't do a direct comparision to ground truth since a different
		// but equivalent projective frame would have been estimated.
		Point4D_F64 X = new Point4D_F64();
		Point2D_F64 found = new Point2D_F64();
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
				DMatrixRMaj P = structure.views.get(viewIdx).worldToView;
				PerspectiveOps.renderPixel(P, X, found);
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

	/**
	 * Check camera matrices directly by computing a matrix which allows direct comparision of the two sets
	 */
	private void checkCameraMatrices( InitializeAllCommonProjective alg,
									  MockLookupSimilarImagesRealistic db ) {
		List<DMatrixRMaj> listA = new ArrayList<>();
		List<DMatrixRMaj> listB = new ArrayList<>();

		// Undo the shift in pixel coordinates
		DMatrixRMaj M = CommonOps_DDRM.identity(3);
		M.set(0, 2, db.intrinsic.cx);
		M.set(1, 2, db.intrinsic.cy);

		List<String> viewIds = BoofMiscOps.collectList(db.views, v -> v.id);
		for (int i = 0; i < 3; i++) {
			View view = alg.getPairwiseGraphViewByStructureIndex(i);
			DMatrixRMaj P = new DMatrixRMaj(3, 4);
			CommonOps_DDRM.mult(M, alg.utils.structurePr.views.get(i).worldToView, P);
			listA.add(P);
			int viewDbIdx = viewIds.indexOf(view.id);
			listB.add(db.views.get(viewDbIdx).camera);
		}

		DMatrixRMaj H = new DMatrixRMaj(4, 4);

		CompatibleProjectiveHomography compatible = new CompatibleProjectiveHomography();
		assertTrue(compatible.fitCameras(listA, listB, H));

		DMatrixRMaj found = new DMatrixRMaj(3, 4);
		for (int i = 0; i < listA.size(); i++) {
			CommonOps_DDRM.mult(listA.get(i), H, found);
			DMatrixRMaj expected = listB.get(i);
			double scale = expected.get(0, 0)/found.get(0, 0);
			CommonOps_DDRM.scale(scale, found);
			assertTrue(MatrixFeatures_DDRM.isEquals(expected, found, matrixTol));
		}
	}

	@Test void selectInitialTriplet() {
		// Set up a view graph with several valid options
		var seed = new View();

		for (int i = 0; i < 6; i++) {
			View view = new View();
			Motion motion = new Motion();
			motion.src = i%2 == 0 ? seed : view; // sanity check that it handles src/dst correctly
			motion.dst = i%2 == 0 ? view : seed;
			motion.score3D = (100 + i)/50.0;
			seed.connections.add(motion);
		}
		// need to make a complete loop
		for (int i = 1; i < 6; i++) {
			Motion motion = new Motion();
			motion.src = seed.connections.get(i - 1).other(seed);
			motion.dst = seed.connections.get(i).other(seed);
			motion.score3D = 50.0/30.0;
			motion.src.connections.add(motion);
			motion.dst.connections.add(motion);
		}

		// Select the results
		var alg = new InitializeAllCommonProjective();

		DogArray_I32 edgeIdxs = DogArray_I32.array(0, 2, 3, 4, 5); // skip one to see if it uses this list
		int[] selected = new int[2];

		alg.selectInitialTriplet(seed, edgeIdxs, selected);

		assertEquals(4, selected[0]);
		assertEquals(5, selected[1]);
	}

	/**
	 * Give it a set of three views which are not mutually connected in a complete circle
	 */
	@Test void scoreTripleView_NotCircle() {
		var alg = new InitializeAllCommonProjective();

		View seedA = new View();
		View viewB = new View();
		View viewC = new View();

		Motion motionAB = new Motion();
		motionAB.src = seedA;
		motionAB.dst = viewB;
		motionAB.score3D = 1000; // give it an excellent 3D score
		Motion motionAC = new Motion();
		motionAC.src = viewC;
		motionAC.dst = seedA; // src/dst shouldn't matter. bonus check
		motionAC.score3D = 1000; // give it an excellent 3D score

		seedA.connections.add(motionAB);
		viewB.connections.add(motionAB);
		seedA.connections.add(motionAC);
		viewC.connections.add(motionAC);
		// No connection B to C is made

		assertEquals(0.0, alg.scoreTripleView(seedA, viewB, viewC), UtilEjml.TEST_F64);
	}

	/**
	 * See if it scores a set of three views higher if they have stronger 3D information
	 */
	@Test void scoreTripleView_Prefer3D() {
		var alg = new InitializeAllCommonProjective();

		View seedA = new View();
		View viewB = new View();
		View viewC = new View();

		Motion motionAB = new Motion();
		motionAB.src = seedA;
		motionAB.dst = viewB;
		motionAB.score3D = 2.0;
		Motion motionAC = new Motion();
		motionAC.src = viewC;
		motionAC.dst = seedA;
		motionAC.score3D = 1000.0;
		Motion motionBC = new Motion();
		motionBC.src = viewC;
		motionBC.dst = viewB;
		motionBC.score3D = 20.0;

		seedA.connections.add(motionAB);
		viewB.connections.add(motionAB);
		seedA.connections.add(motionAC);
		viewC.connections.add(motionAC);
		viewB.connections.add(motionBC);
		viewC.connections.add(motionBC);

		double score0 = alg.scoreTripleView(seedA, viewB, viewC);

		// make it have a stronger 3D score and see if this is reflected
		motionAB.score3D *= 2.0;
		assertTrue(alg.scoreTripleView(seedA, viewB, viewC) > score0);

		// now a worse score
		motionAB.score3D *= 0.1;
		assertTrue(alg.scoreTripleView(seedA, viewB, viewC) < score0);
	}

	@Test void createStructureLookUpTables() {
		int offset = 5;

		var db = new MockLookupSimilarImages(4, 0xDEADBEEF);
		var alg = new InitializeAllCommonProjective();
		alg.utils.ransac = new MockRansac(offset, db.feats3D.size() - offset);
		alg.utils.dbSimilar = db;
		alg.utils.P1.setTo(db.listCameraMatrices.get(0));
		alg.utils.P2.setTo(db.listCameraMatrices.get(1));
		alg.utils.P3.setTo(db.listCameraMatrices.get(3));
		alg.utils.commonIdx.setTo(DogArray_I32.range(0, db.feats3D.size()));
		// order shouldn't mattter
		PrimitiveArrays.shuffle(alg.utils.commonIdx.data, 0, alg.utils.commonIdx.size, rand);

		View seed = db.graph.nodes.get(0);

		alg.inlierIndexes.resize(3);
		alg.createStructureLookUpTables(seed);

		// Check to see if inlier indexes are correct
		int numInliers = db.feats3D.size() - offset;
		DogArray_I32 inlierToSeed = alg.inlierIndexes.get(0);
		assertEquals(numInliers, inlierToSeed.size);
		for (int i = 0; i < numInliers; i++) {
			assertEquals(alg.utils.commonIdx.get(i + offset), inlierToSeed.get(i));
		}
		// mapping from seed features to structure features
		for (int i = 0; i < db.feats3D.size(); i++) {
			assertEquals(i < offset ? -1 : i - offset, alg.seedToStructure.get(alg.utils.commonIdx.get(i)));
		}
	}

	@Test void createObservationsForBundleAdjustment() {
		var dbSimilar = new MockLookupSimilarImages(5, 0xDEADBEEF);
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

		PairwiseImageGraph graph = dbSimilar.graph;
		View seed = graph.nodes.get(0);
		DogArray_I32 motionIndexes = DogArray_I32.array(0, 2, 3); // which edges in the first view should be considered
		var alg = new InitializeAllCommonProjective();
		alg.utils.dbSimilar = dbSimilar;
		alg.utils.dbCams = dbCams;
		alg.utils.seed = seed;

		//------------------------------ Initialize internal data structures
		// make every other feature an inlier
		alg.seedToStructure.resize(dbSimilar.feats3D.size());
		alg.seedToStructure.fill(-1);
		DogArray_I32 inlierToSeed = alg.inlierIndexes.grow();
		for (int i = 0; i < dbSimilar.feats3D.size(); i += 2) {
			alg.seedToStructure.data[i] = inlierToSeed.size;
			inlierToSeed.add(i);
		}
		dbSimilar.lookupPixelFeats(seed.id, alg.utils.featsA);

		// Call the function being tested
		alg.inlierIndexes.resize(motionIndexes.size + 1); // allocate for rest of the views
		alg.createObservationsForBundleAdjustment(motionIndexes);

		SceneObservations found = alg.utils.observations;
		found.checkOneObservationPerView();

		// Check to see if there's the expected number of views
		assertFalse(found.hasRigid());
		assertEquals(4, found.views.size); // seed + 3 connected views
		for (int i = 0; i < 4; i++) {
			SceneObservations.View view = found.views.get(i);
			assertEquals(inlierToSeed.size, view.size());
			for (int j = 0; j < view.size(); j++) {
				// crude sanity check on the index
				assertTrue(view.getPointId(j) < inlierToSeed.size);
			}
		}

		// Not doing a detailed check of the values since that will be very complex
	}

	/**
	 * Looks up features then tests using triangulation and checks for perfect results.
	 */
	@Test void lookupInfoForMetricElevation() {
		int numViews = 4;
		var dbSimilar = new MockLookupSimilarImages(numViews, 0xDEADBEEF);
		var dbCams = new MockLookUpCameraInfo(800, 800);
		var alg = new InitializeAllCommonProjective();
		alg.utils.dbSimilar = dbSimilar;
		alg.utils.dbCams = dbCams;

		// sanity check that all features are visible in all views. Requirement of metric escalation
		for (int i = 0; i < dbSimilar.viewIds.size(); i++) {
			assertEquals(dbSimilar.numFeatures, dbSimilar.viewObs.get(i).size());
		}

		// dividing number of features by two because only even observations are inliers
		alg.utils.structurePr.initialize(numViews, dbSimilar.numFeatures/2);
		alg.utils.observations.initialize(numViews);

		// Transform that makes view[0] identity
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		MultiViewOps.projectiveToIdentityH(dbSimilar.listCameraMatrices.get(0), H);

		// construct projective SBA scene from metric ground truth
		for (int viewIdx = 0; viewIdx < dbSimilar.viewIds.size(); viewIdx++) {
			DMatrixRMaj P = new DMatrixRMaj(3, 4);
			CommonOps_DDRM.mult(dbSimilar.listCameraMatrices.get(viewIdx), H, P);

			alg.viewsByStructureIndex.add(dbSimilar.graph.nodes.get(viewIdx));
			alg.utils.structurePr.views.get(viewIdx).worldToView.setTo(P);
			alg.utils.structurePr.views.get(viewIdx).width = viewIdx;

			// only features with an even ID will be inliers
			DogArray_I32 inliers = alg.inlierIndexes.grow();

			int[] featureIDs = dbSimilar.viewToFeat.get(viewIdx);
			SceneObservations.View oview = alg.utils.observations.views.get(viewIdx);
			for (int obsIdx = 0; obsIdx < featureIDs.length; obsIdx++) {
				int featureID = featureIDs[obsIdx];
				if (featureID%2 == 1)
					continue;
				inliers.add(oview.size());
				Point2D_F64 pixel = dbSimilar.viewObs.get(viewIdx).get(obsIdx);
				oview.add(featureID/2, (float)pixel.x, (float)pixel.y);
			}
		}

		// Call the function we are testing
		List<String> viewIds = new ArrayList<>();
		DogArray<ElevateViewInfo> views = new DogArray<>(ElevateViewInfo::new);
		DogArray<DMatrixRMaj> cameraMatrices = new DogArray<>(() -> new DMatrixRMaj(3, 4));
		DogArray<AssociatedTupleDN> observations = new DogArray<>(AssociatedTupleDN::new);
		alg.lookupInfoForMetricElevation(viewIds, views, cameraMatrices, observations);

		// check what can be checked trivially by comparing to the db
		assertEquals(4, viewIds.size());
		assertEquals(4, views.size());
		assertEquals(3, cameraMatrices.size());
		assertEquals(dbSimilar.numFeatures/2, observations.size());

		for (int viewIdx = 0; viewIdx < 4; viewIdx++) {
			assertEquals(dbSimilar.viewIds.get(viewIdx), viewIds.get(viewIdx));
			assertEquals(viewIdx, views.get(viewIdx).shape.width);
			assertEquals(0, views.get(viewIdx).cameraID); // only one camera
		}

		// See if it unscrambled the observations
		for (int obsIdx = 0; obsIdx < dbSimilar.numFeatures/2; obsIdx++) {
			for (int viewIdx = 0; viewIdx < 4; viewIdx++) {
				Point2D_F64 expected = dbSimilar.viewObs.get(viewIdx).get(dbSimilar.featToView.get(viewIdx)[obsIdx*2]);
				Point2D_F64 found = observations.get(obsIdx).get(viewIdx);
				assertEquals(0.0, expected.distance(found), UtilEjml.TEST_F32);
			}
		}
	}

	private static class MockRansac implements ModelMatcher<TrifocalTensor, AssociatedTriple> {
		int offset;
		int numInliers;

		public MockRansac( int offset, int numInliers ) {
			this.offset = offset;
			this.numInliers = numInliers;
		}

		@Override
		public int getInputIndex( int matchIndex ) {
			return offset + matchIndex;
		}

		@Override public List<AssociatedTriple> getMatchSet() {
			List<AssociatedTriple> list = new ArrayList<>();
			for (int i = 0; i < numInliers; i++) {
				list.add(null);
			}
			return list;
		}

		@Override public boolean process( List<AssociatedTriple> dataSet ) {return false;}

		@Override public TrifocalTensor getModelParameters() {return null;}

		@Override public double getFitQuality() {return 0;}

		@Override public int getMinimumSize() {return 0;}

		@Override public void reset() {}

		@Override public Class<AssociatedTriple> getPointType() {return null;}

		@Override public Class<TrifocalTensor> getModelType() {return null;}
	}
}
