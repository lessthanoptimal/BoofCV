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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.pose.CompatibleProjectiveHomography;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.GrowQueue_I32;
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
class TestProjectiveInitializeAllCommon {

	Random rand = BoofTesting.createRandom(3);

	/**
	 * Perfect scene with 2 connections. This is the simplest case
	 */
	@Test
	void perfect_connections_2() {
		var alg = new ProjectiveInitializeAllCommon();

		for (int seedIdx = 0; seedIdx < 3; seedIdx++) {
			var db = new MockLookupSimilarImages(4,0xDEADBEEF);
			View seed = db.graph.nodes.get(seedIdx);
			var seedConnIdx = GrowQueue_I32.array(0,2);

			// Give a subset as a test to see if the size is being properly pass down the chain
			int offset = 0;
			int numFeatures = db.feats3D.size()-offset;
			var seedFeatsIdx = GrowQueue_I32.range(0,numFeatures);
			PrimitiveArrays.shuffle(seedFeatsIdx.data,0,seedFeatsIdx.size,rand); // order should not matter

			// Reconstruct the projective scene
			assertTrue(alg.projectiveSceneN(db,seed,seedFeatsIdx,seedConnIdx));

			// test results
			checkReconstruction(alg, db, seedConnIdx, numFeatures, 1e-4);
			checkCameraMatrices(alg, db);
		}
	}

	/**
	 * Perfect scene with 3 to 5 connections
	 */
	@Test
	void perfect_connections_3_to_5() {
		var alg = new ProjectiveInitializeAllCommon();

		for (int numConnections = 3; numConnections <= 6; numConnections++) {
			// do a few trials since things are randomized to shake out more bugs potentially
			for (int trial = 0; trial < 3; trial++) {
				var db = new MockLookupSimilarImages(6,0xDEADBEEF);
				View seed = db.graph.nodes.get(0);

				// randomly select the connections
				var seedConnIdx = new GrowQueue_I32();
				for (int i = 0; i < seed.connections.size; i++) {
					seedConnIdx.add(i);
				}
				for (int i = 0; i < seed.connections.size-numConnections; i++) {
					seedConnIdx.remove(rand.nextInt(seedConnIdx.size));
				}
				PrimitiveArrays.shuffle(seedConnIdx.data,0,seedConnIdx.size,rand); // order should not matter

				// Give a subset as a test to see if the size is being properly pass down the chain
				var seedFeatsIdx = new GrowQueue_I32();
				int offset = 0;
				int numFeatures = db.feats3D.size()-offset;
				for (int i = offset; i < numFeatures; i++) { seedFeatsIdx.add(i);}
				PrimitiveArrays.shuffle(seedFeatsIdx.data,0,seedFeatsIdx.size,rand); // order should not matter

				// Reconstruct the projective scene
				assertTrue(alg.projectiveSceneN(db,seed,seedFeatsIdx,seedConnIdx));

				// test results
				checkReconstruction(alg, db, seedConnIdx, numFeatures, 1e-4);
			}
		}
	}

	/**
	 * Add a tiny bit of noise and see if it blows up
	 */
	@Test
	void small_noise() {
		var db = new MockLookupSimilarImages(4,0xDEADBEEF);
		var alg = new ProjectiveInitializeAllCommon();

		// These observations are no longer perfect, just a little bit of error
		db.viewObs.get(0).get(20).x += 0.1;
		db.viewObs.get(3).get(25).y += 0.1;

		checkConfiguration(alg,db,true,true,false, 0.1);
	}

	/**
	 * Test out different configurations of SBA and see if they all work
	 */
	@Test
	void configurations_sba() {
		var db = new MockLookupSimilarImages(5,0xDEADBEEF);
		var alg = new ProjectiveInitializeAllCommon();

		checkConfiguration(alg,db,true,true,false, 1e-4);
		checkConfiguration(alg,db,true,true,true, 1e-4);
		checkConfiguration(alg,db,true,false,false, 1e-4);
		checkConfiguration(alg,db,false,true,false, 1e-4);
		checkConfiguration(alg,db,false,true,true, 1e-4);
		checkConfiguration(alg,db,false,false,false, 1e-4);

	}

	private void checkConfiguration( ProjectiveInitializeAllCommon alg, MockLookupSimilarImages db,
									 boolean threeViews,
									 boolean sba, boolean scale , double reprojectionTol )
	{
		alg.utils.configConvergeSBA.maxIterations = sba ? 50 : -1;
		alg.utils.configScaleSBA = scale;

		View seed = db.graph.nodes.get(0);
		var seedFeatsIdx = new GrowQueue_I32();
		var seedConnIdx = threeViews ? GrowQueue_I32.array(0,2) : GrowQueue_I32.array(1,2,3);

		// Give a subset as a test to see if the size is being properly pass down the chain
		int offset = 0;
		int numFeatures = db.feats3D.size()-offset;
		for (int i = offset; i < numFeatures; i++) { seedFeatsIdx.add(i);}
		PrimitiveArrays.shuffle(seedFeatsIdx.data,0,seedFeatsIdx.size,rand); // order should not matter

		// Reconstruct the projective scene
		assertTrue(alg.projectiveSceneN(db,seed,seedFeatsIdx,seedConnIdx));

		// test results
		checkReconstruction(alg, db, seedConnIdx, numFeatures, reprojectionTol);
	}

	/**
	 * Check reconstruction by seeing if it's consistent with the input observations
	 */
	private void checkReconstruction(ProjectiveInitializeAllCommon alg,
									 MockLookupSimilarImages db,
									 GrowQueue_I32 seedConnIdx,
									 int numFeatures,
									 double reprojectionTol ) {

		final SceneStructureProjective structure = alg.getStructure();

		// Sanity check the number of each type of structure
		assertEquals(seedConnIdx.size+1,structure.views.size);
		assertEquals(numFeatures,structure.points.size);
		assertEquals(numFeatures,alg.inlierToSeed.size);

		int dbIndexSeed = db.viewIds.indexOf(alg.getPairwiseGraphViewByStructureIndex(0).id);

		// Check results for consistency. Can't do a direct comparision to ground truth since a different
		// but equivalent projective frame would have been estimated.
		Point4D_F64 X = new Point4D_F64();
		Point2D_F64 found = new Point2D_F64();
		for (int i = 0; i < alg.inlierToSeed.size; i++) {
			int seedFeatureIdx = alg.inlierToSeed.get(i);
			int truthFeatIdx = db.viewToFeat.get(dbIndexSeed)[seedFeatureIdx];
			int structureIdx = alg.seedToStructure.get(seedFeatureIdx);
			assertTrue(structureIdx>=0); // only features that have structure should be in this list

			// Get the estimated point in 3D
			structure.points.get(structureIdx).get(X);

			// Project the point to the camera using found projection matrices
			for (int viewIdx = 0; viewIdx < structure.views.size; viewIdx++) {
				int viewDbIdx = db.viewIds.indexOf(alg.getPairwiseGraphViewByStructureIndex(viewIdx).id);
				// Project this feature to the camera
				DMatrixRMaj P = structure.views.get(viewIdx).worldToView;
				PerspectiveOps.renderPixel(P,X,found);

				// Lookup the expected pixel location
				// The seed feature ID and the ground truth feature ID are the same
				int viewFeatIdx = db.featToView.get(viewDbIdx)[truthFeatIdx];
				Point2D_F64 expected = db.viewObs.get(viewDbIdx).get(viewFeatIdx);

				assertEquals(0.0,expected.distance(found), reprojectionTol);
			}
		}
	}

	/**
	 * Check camera matrices directly by computing a matrix which allows direct comparision of the two sets
	 */
	private void checkCameraMatrices( ProjectiveInitializeAllCommon alg,
									  MockLookupSimilarImages db )
	{
		List<DMatrixRMaj> listA = new ArrayList<>();
		List<DMatrixRMaj> listB = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			View view = alg.getPairwiseGraphViewByStructureIndex(i);
			listA.add(alg.utils.structure.views.get(i).worldToView);
			int viewDbIdx = db.viewIds.indexOf(view.id);
			listB.add(db.listCameraMatrices.get(viewDbIdx));
		}

		DMatrixRMaj H = new DMatrixRMaj(4,4);

		CompatibleProjectiveHomography compatible = new CompatibleProjectiveHomography();
		assertTrue(compatible.fitCameras(listA,listB,H));

		DMatrixRMaj found = new DMatrixRMaj(3,4);
		for (int i = 0; i < listA.size(); i++) {
			CommonOps_DDRM.mult(listA.get(i),H,found);
			DMatrixRMaj expected = listB.get(i);
			double scale = expected.get(0,0)/found.get(0,0);
			CommonOps_DDRM.scale(scale,found);
			assertTrue(MatrixFeatures_DDRM.isEquals(expected,found, 1e-4));
		}
	}

	@Test
	void selectInitialTriplet() {
		// Set up a view graph with several valid options
		var seed = new View();

		for (int i = 0; i < 6; i++) {
			View view = new View();
			Motion motion = new Motion();
			motion.src = i%2==0?seed:view; // sanity check that it handles src/dst correctly
			motion.dst = i%2==0?view:seed;
			motion.countF = 100+i;
			motion.countH = 50;
			seed.connections.add(motion);
		}
		// need to make a complete loop
		for (int i = 1; i < 6; i++) {
			Motion motion = new Motion();
			motion.src = seed.connections.get(i-1).other(seed);
			motion.dst = seed.connections.get(i  ).other(seed);
			motion.countF = 50;
			motion.countH = 30;
			motion.src.connections.add(motion);
			motion.dst.connections.add(motion);
		}

		// Select the results
		var alg = new ProjectiveInitializeAllCommon();

		GrowQueue_I32 edgeIdxs = GrowQueue_I32.array(0,2,3,4,5); // skip one to see if it uses this list
		int[] selected = new int[2];

		alg.selectInitialTriplet(seed,edgeIdxs,selected);

		assertEquals(4,selected[0]);
		assertEquals(5,selected[1]);
	}

	/**
	 * Give it a set of three views which are not mutually connected in a complete circle
	 */
	@Test
	void scoreTripleView_NotCircle() {
		var alg = new ProjectiveInitializeAllCommon();

		View seedA = new View();
		View viewB = new View();
		View viewC = new View();

		Motion motionAB = new Motion();
		motionAB.src = seedA; motionAB.dst = viewB;
		motionAB.countF = 1000; // give it an excellent 3D score
		Motion motionAC = new Motion();
		motionAC.src = viewC; motionAC.dst = seedA; // src/dst shouldn't matter. bonus check
		motionAC.countF = 1000; // give it an excellent 3D score

		seedA.connections.add(motionAB); viewB.connections.add(motionAB);
		seedA.connections.add(motionAC); viewC.connections.add(motionAC);
		// No connection B to C is made

		assertEquals(0.0, alg.scoreTripleView(seedA,viewB,viewC), UtilEjml.TEST_F64);
	}

	/**
	 * See if it scores a set of three views higher if they have stronger 3D information
	 */
	@Test
	void scoreTripleView_Prefer3D() {
		var alg = new ProjectiveInitializeAllCommon();

		View seedA = new View();
		View viewB = new View();
		View viewC = new View();

		Motion motionAB = new Motion();
		motionAB.src = seedA; motionAB.dst = viewB;
		motionAB.countF = 10; motionAB.countH = 5;
		Motion motionAC = new Motion();
		motionAC.src = viewC; motionAC.dst = seedA;
		motionAC.countF = 12; motionAB.countH = 0;
		Motion motionBC = new Motion();
		motionBC.src = viewC; motionBC.dst = viewB;
		motionBC.countF = 12; motionBC.countH = 1;

		seedA.connections.add(motionAB); viewB.connections.add(motionAB);
		seedA.connections.add(motionAC); viewC.connections.add(motionAC);
		viewB.connections.add(motionBC); viewC.connections.add(motionBC);

		double score0 = alg.scoreTripleView(seedA,viewB,viewC);

		// make it have a stronger 3D score and see if this is reflected
		motionAB.countF = 20;
		assertTrue( alg.scoreTripleView(seedA,viewB,viewC) > score0 );

		// now a worse score
		motionAB.countF = 5;
		assertTrue( alg.scoreTripleView(seedA,viewB,viewC) < score0 );
	}

	@Test
	void createStructureLookUpTables() {
		int offset = 5;

		var db = new MockLookupSimilarImages(4,0xDEADBEEF);
		var alg = new ProjectiveInitializeAllCommon();
		alg.utils.ransac = new MockRansac(offset,db.feats3D.size()-offset);
		alg.utils.db = db;
		alg.utils.P1.set(db.listCameraMatrices.get(0));
		alg.utils.P2.set(db.listCameraMatrices.get(1));
		alg.utils.P3.set(db.listCameraMatrices.get(3));
		alg.utils.commonIdx.setTo(GrowQueue_I32.range(0,db.feats3D.size()));
		// order shouldn't mattter
		PrimitiveArrays.shuffle(alg.utils.commonIdx.data,0,alg.utils.commonIdx.size,rand);

		View seed = db.graph.nodes.get(0);

		alg.createStructureLookUpTables(seed);

		// Check to see if inlier indexes are correct
		int numInliers = db.feats3D.size()-offset;
		assertEquals(numInliers,alg.inlierToSeed.size);
		for (int i = 0; i < numInliers; i++) {
			assertEquals(alg.utils.commonIdx.get(i+offset),alg.inlierToSeed.get(i));
		}
		// mapping from seed features to structure featuers
		for (int i = 0; i < db.feats3D.size(); i++) {
			assertEquals(i<offset?-1:i-offset,alg.seedToStructure.get(alg.utils.commonIdx.get(i)));
		}
	}

	@Test
	void createObservationsForBundleAdjustment() {
		var db = new MockLookupSimilarImages(5,0xDEADBEEF);
		PairwiseImageGraph2 graph = db.graph;
		View seed = graph.nodes.get(0);
		GrowQueue_I32 motionIndexes = GrowQueue_I32.array(0,2,3); // which edges in the first view should be considered
		var alg = new ProjectiveInitializeAllCommon();
		alg.utils.db = db;
		alg.utils.seed = seed;

		//------------------------------ Initialize internal data structures
		// make every other feature an inlier
		alg.seedToStructure.resize(db.feats3D.size());
		alg.seedToStructure.fill(-1);
		for (int i = 0; i < db.feats3D.size(); i += 2) {
			alg.seedToStructure.data[i] = alg.inlierToSeed.size;
			alg.inlierToSeed.add(i);
		}
		db.lookupPixelFeats(seed.id,alg.utils.featsA);

		// Call the function being tested
		alg.createObservationsForBundleAdjustment(motionIndexes);

		SceneObservations found = alg.utils.observations;
		found.checkOneObservationPerView();

		// Check to see if there's the expected number of views
		assertFalse(found.hasRigid());
		assertEquals(4,found.views.size); // seed + 3 connected views
		for (int i = 0; i < 4; i++) {
			SceneObservations.View view = found.views.get(i);
			assertEquals(alg.inlierToSeed.size, view.size());
			for (int j = 0; j < view.size(); j++) {
				// crude sanity check on the index
				assertTrue(view.getPointId(j) < alg.inlierToSeed.size);
			}
		}

		// Not doing a detailed check of the values since that will be very complex
	}

	private static class MockRansac implements ModelMatcher<TrifocalTensor, AssociatedTriple> {
		int offset;
		int numInliers;

		public MockRansac(int offset, int numInliers) {
			this.offset = offset;
			this.numInliers = numInliers;
		}

		@Override
		public int getInputIndex(int matchIndex) {
			return offset+matchIndex;
		}

		@Override public List<AssociatedTriple> getMatchSet() {
			List<AssociatedTriple> list = new ArrayList<>();
			for (int i = 0; i < numInliers; i++) {
				list.add(null);
			}
			return list;
		}

		@Override public boolean process(List<AssociatedTriple> dataSet) {return false;}
		@Override public TrifocalTensor getModelParameters() {return null;}
		@Override public double getFitQuality() {return 0;}
		@Override public int getMinimumSize() {return 0;}
		@Override public void reset() {}
		@Override public Class<AssociatedTriple> getPointType() {return null;}
		@Override public Class<TrifocalTensor> getModelType() {return null;}
	}
}