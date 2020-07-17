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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestPairwiseGraphUtils {
	Random rand = BoofTesting.createRandom(1);

	@Test
	void findCommonFeatures() {
		PairwiseImageGraph2.View seed = new PairwiseImageGraph2.View();
		seed.totalObservations = 100;

		for (int i = 0; i < 2; i++) {
			PairwiseImageGraph2.Motion motion = new PairwiseImageGraph2.Motion();
			motion.src = seed;
			motion.dst = new PairwiseImageGraph2.View();
			motion.dst.totalObservations = 110; // give it a few extra and see if that causes a problem

			for (int j = 0; j < 100; j++) {
				motion.inliers.grow().setAssociation(j,j,0);
			}
			seed.connections.add(motion);
		}

		// only connect 1/2 of them from B to C
		PairwiseImageGraph2.Motion motionBC = new PairwiseImageGraph2.Motion();
		motionBC.src = seed.connections.get(0).dst;
		motionBC.dst = seed.connections.get(1).dst;
		for (int i = 0; i < seed.totalObservations; i++) {
			motionBC.inliers.grow().setAssociation(i,i,0);
		}
		motionBC.src.connections.add(motionBC);
		motionBC.dst.connections.add(motionBC);

		// Find the common tracks
		var alg = new PairwiseGraphUtils();
		alg.seed = seed;
		alg.viewB = seed.connections.get(0).other(seed);
		alg.viewC = seed.connections.get(1).other(seed);
		alg.createThreeViewLookUpTables();
		alg.findCommonFeatures();

		assertEquals(seed.totalObservations,alg.commonIdx.size);
		for (int i = 0; i < alg.commonIdx.size; i++) {
			assertEquals(i,alg.commonIdx.get(i));
		}
	}

	@Test
	void findCommonFeatures_list() {
		PairwiseImageGraph2.View seed = new PairwiseImageGraph2.View();
		seed.totalObservations = 100;
		GrowQueue_I32 seedFeatsIdx = new GrowQueue_I32();

		for (int i = 0; i < seed.totalObservations /2; i++) {
			seedFeatsIdx.add(i);
		}

		for (int i = 0; i < 2; i++) {
			PairwiseImageGraph2.Motion motion = new PairwiseImageGraph2.Motion();
			motion.src = seed;
			motion.dst = new PairwiseImageGraph2.View();
			motion.dst.totalObservations = 110; // give it a few extra and see if that causes a problem

			for (int j = 0; j < 100; j++) {
				motion.inliers.grow().setAssociation(j,j,0);
			}
			seed.connections.add(motion);
		}

		// only connect 1/2 of them from B to C
		PairwiseImageGraph2.Motion motionBC = new PairwiseImageGraph2.Motion();
		motionBC.src = seed.connections.get(0).dst;
		motionBC.dst = seed.connections.get(1).dst;
		for (int i = 0; i < seed.totalObservations; i++) {
			motionBC.inliers.grow().setAssociation(i,i,0);
		}
		motionBC.src.connections.add(motionBC);
		motionBC.dst.connections.add(motionBC);

		// Find the common tracks
		var alg = new PairwiseGraphUtils();
		alg.seed = seed;
		alg.viewB = seed.connections.get(0).other(seed);
		alg.viewC = seed.connections.get(1).other(seed);
		alg.createThreeViewLookUpTables();
		alg.findCommonFeatures(seedFeatsIdx);

		assertEquals(seed.totalObservations /2,alg.commonIdx.size);
		for (int i = 0; i < alg.commonIdx.size; i++) {
			assertEquals(i,alg.commonIdx.get(i));
		}
	}

	@Test
	void createThreeViewLookUpTables() {
		var alg = new PairwiseGraphUtils();
		var db = new MockLookupSimilarImages(4,0xDEADBEEF);
		alg.db = db;
		alg.seed = db.graph.nodes.get(0);
		alg.viewB = db.graph.nodes.get(1);
		alg.viewC = db.graph.nodes.get(3);

		// Compute and check the results
		alg.createThreeViewLookUpTables();

		final int N = db.feats3D.size();
		assertEquals(N,alg.table_A_to_B.size);
		assertEquals(N,alg.table_A_to_C.size);
		assertEquals(N,alg.table_B_to_C.size);

		// compare to ground truth tables
		for (int i = 0; i < N; i++) {
			assertEquals(db.featToView.get(1)[i],alg.table_A_to_B.data[i]);
			assertEquals(db.featToView.get(3)[i],alg.table_A_to_C.data[i]);
			int b_to_c = db.featToView.get(3)[db.viewToFeat.get(1)[i]];
			assertEquals(b_to_c,alg.table_B_to_C.data[i]);
		}
	}

	@Test
	void createTripleFromCommon() {
		var alg = new PairwiseGraphUtils();
		var db = new MockLookupSimilarImages(4,0xDEADBEEF);
		alg.db = db;
		alg.seed = db.graph.nodes.get(0);
		alg.viewB = db.graph.nodes.get(1);
		alg.viewC = db.graph.nodes.get(3);

		// common is empty
		alg.createTripleFromCommon();
		assertEquals(0,alg.matchesTriple.size);

		// one element
		alg.table_A_to_B.add(1);
		alg.table_A_to_C.add(5);
		alg.commonIdx.add(0);
		alg.createTripleFromCommon();
		assertEquals(1,alg.matchesTriple.size);
		assertEquals(0.0,alg.matchesTriple.get(0).p1.distance(db.viewObs.get(0).get(0)));
		assertEquals(0.0,alg.matchesTriple.get(0).p2.distance(db.viewObs.get(1).get(1)));
		assertEquals(0.0,alg.matchesTriple.get(0).p3.distance(db.viewObs.get(3).get(5)));

		// Add one more
		alg.table_A_to_B.add(2);
		alg.table_A_to_C.add(2);
		alg.commonIdx.add(1);
		alg.createTripleFromCommon();
		assertEquals(2,alg.matchesTriple.size);
	}

	@Test
	void initializeSbaSceneThreeView() {
		var db = new MockLookupSimilarImages(4,0xDEADBEEF);
		var alg = new PairwiseGraphUtils() {
			boolean called = false;

			@Override
			public void triangulateFeatures() {
				called = true;
			}
		};

		alg.db = db;
		alg.seed = db.graph.nodes.get(0);
		alg.viewB = db.graph.nodes.get(1);
		alg.viewC = db.graph.nodes.get(3);
		alg.inliersThreeView = new ArrayList<>();
		alg.P1.set(db.listCameraMatrices.get(0));
		alg.P2.set(db.listCameraMatrices.get(1));
		alg.P3.set(db.listCameraMatrices.get(3));

		alg.inliersThreeView.add( new AssociatedTriple() );

		alg.initializeSbaSceneThreeView();

		assertEquals(1,alg.structure.points.size);

		// triangulate is tested elsewhere for accuracy
		assertTrue(alg.called);

		// See if the results were copied
		assertTrue(MatrixFeatures_DDRM.isIdentical(alg.P1,alg.structure.views.get(0).worldToView,1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(alg.P2,alg.structure.views.get(1).worldToView,1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(alg.P3,alg.structure.views.get(2).worldToView,1e-8));
	}

	@Test
	void estimateProjectiveCamerasRobustly() {
		var alg = new PairwiseGraphUtils(new ConfigProjectiveReconstruction());
		var db = new MockLookupSimilarImages(4,0xDEADBEEF);
		alg.db = db;
		alg.seed = db.graph.nodes.get(0);
		alg.viewB = db.graph.nodes.get(1);
		alg.viewC = db.graph.nodes.get(3);

		// Create boilerplate input for RANSAC
		alg.createThreeViewLookUpTables();
		alg.findCommonFeatures();
		alg.createTripleFromCommon();

		// Simple test where we see how many inliers there are
		assertTrue(alg.estimateProjectiveCamerasRobustly());
		assertEquals(db.feats3D.size(),alg.ransac.getMatchSet().size());

		// Change one of the edges to see if it handles src/dst swap correctly
		alg.viewB = db.graph.nodes.get(2);
		// Everything should be an inlier since there's no noise
		assertEquals(db.feats3D.size(),alg.ransac.getMatchSet().size());

		// Crude test to see if it saved some reasonable results
		assertTrue(MatrixFeatures_DDRM.isIdentity(alg.P1,1e-8));
		assertFalse(MatrixFeatures_DDRM.isIdentity(alg.P2,1e-8));
		assertFalse(MatrixFeatures_DDRM.isIdentity(alg.P3,1e-8));
	}

	@Test
	void triangulateFeatures() {
		var alg = new PairwiseGraphUtils(new ConfigProjectiveReconstruction());
		var db = new MockLookupSimilarImages(4,0xDEADBEEF);

		int offset = 5;
		alg.db = db;
		alg.P1.set(db.listCameraMatrices.get(0));
		alg.P2.set(db.listCameraMatrices.get(1));
		alg.P3.set(db.listCameraMatrices.get(3));
		alg.inliersThreeView = new ArrayList<>();

		// Create the set of inliers. Only these inliers will be considered when computing 3D features
		for (int i = offset; i < db.feats3D.size(); i++) {
			int view1_idx = db.featToView.get(0)[i];
			int view2_idx = db.featToView.get(1)[i];
			int view3_idx = db.featToView.get(3)[i];

			AssociatedTriple t = new AssociatedTriple();
			t.p1.set(db.viewObs.get(0).get(view1_idx));
			t.p2.set(db.viewObs.get(1).get(view2_idx));
			t.p3.set(db.viewObs.get(3).get(view3_idx));

			alg.inliersThreeView.add(t);
		}

		alg.structure.initialize(3,alg.inliersThreeView.size());
		alg.triangulateFeatures();

		// start with 3D triangulation
		checkTriangulatedFeatures(alg);
	}

	/**
	 * Verifies that triangulation was done correctly by comparing the distance to ground truth
	 */
	private void checkTriangulatedFeatures(PairwiseGraphUtils alg) {
		// Project the points back into the image and see if it gets the observations back
		// the input should be noise free so this should work to a high level of precision
		Point4D_F64 X = new Point4D_F64();
		Point2D_F64 expected = new Point2D_F64();
		for (int i = 0; i < alg.inliersThreeView.size(); i++) {
			alg.structure.getPoints().get(i).get(X);

			PerspectiveOps.renderPixel(alg.P1,X,expected);
			assertEquals(0.0, expected.distance(alg.inliersThreeView.get(i).p1), UtilEjml.TEST_F64);
			PerspectiveOps.renderPixel(alg.P2,X,expected);
			assertEquals(0.0, expected.distance(alg.inliersThreeView.get(i).p2), UtilEjml.TEST_F64);
			PerspectiveOps.renderPixel(alg.P3,X,expected);
			assertEquals(0.0, expected.distance(alg.inliersThreeView.get(i).p3), UtilEjml.TEST_F64);
		}
	}

	@Test
	void initializeSbaObservationsThreeView() {
		var alg = new PairwiseGraphUtils();
		var db = new MockLookupSimilarImages(4,0xDEADBEEF);

		int[] views = new int[]{0,1,3};
		alg.db = db;
		alg.P1.set(db.listCameraMatrices.get(views[0]));
		alg.P2.set(db.listCameraMatrices.get(views[1]));
		alg.P3.set(db.listCameraMatrices.get(views[2]));
		alg.inliersThreeView = new ArrayList<>();
		for (int i = 0; i < db.feats3D.size(); i++) {
			Point2D_F64 o1 = db.viewObs.get(views[0]).get(i);
			Point2D_F64 o2 = db.viewObs.get(views[1]).get(i);
			Point2D_F64 o3 = db.viewObs.get(views[2]).get(i);
			alg.inliersThreeView.add( new AssociatedTriple(o1,o2,o3));
		}

		alg.initializeSbaObservationsThreeView();

		Point2D_F64 found = new Point2D_F64();
		for (int viewIdx = 0; viewIdx < 3; viewIdx++) {
			assertEquals(db.feats3D.size(),alg.observations.views.get(viewIdx).point.size());

			for (int i = 0; i < db.feats3D.size(); i++) {
				Point2D_F64 expected = db.viewObs.get(views[viewIdx]).get(i);
				alg.observations.views.get(viewIdx).get(i,found);

				assertEquals(0.0,expected.distance(found), UtilEjml.TEST_F32);
			}
		}
	}

	@Test
	void createTableViewAtoB() {
		// Create a randomized table showing how features are matched between the two views
		GrowQueue_I32 table_src_to_dst = GrowQueue_I32.range(0,50);
		PrimitiveArrays.shuffle(table_src_to_dst.data,0,table_src_to_dst.size,rand);

		// Create the list of associated pairs
		PairwiseImageGraph2.Motion motion = new PairwiseImageGraph2.Motion();
		motion.inliers.resize(table_src_to_dst.size);
		for (int i = 0; i < table_src_to_dst.size; i++) {
			AssociatedIndex associated = motion.inliers.get(i);
			associated.src = i;
			associated.dst = table_src_to_dst.data[i];
		}
		motion.src = new PairwiseImageGraph2.View();
		motion.dst = new PairwiseImageGraph2.View();
		motion.src.totalObservations = motion.dst.totalObservations = table_src_to_dst.size;
		motion.inliers.shuffle(rand); // shuffle this to make the order interesting to test

		GrowQueue_I32 found_src_to_dst = new GrowQueue_I32(table_src_to_dst.size);
		// Reconstruct the look up table
		PairwiseGraphUtils.createTableViewAtoB(motion.src,motion,found_src_to_dst);
		for (int i = 0; i < table_src_to_dst.size; i++) {
			assertEquals(table_src_to_dst.data[i], found_src_to_dst.data[i]);
		}

		// Reverse direction now
		{
			PairwiseImageGraph2.View tmp = motion.src;
			motion.src = motion.dst;
			motion.dst = tmp;
		}
		for (int i = 0; i < motion.inliers.size; i++) {
			AssociatedIndex associated = motion.inliers.get(i);
			int tmp = associated.src;
			associated.src = associated.dst;
			associated.dst = tmp;
		}
		PairwiseGraphUtils.createTableViewAtoB(motion.dst,motion,found_src_to_dst);
		for (int i = 0; i < table_src_to_dst.size; i++) {
			assertEquals(table_src_to_dst.data[i], found_src_to_dst.data[i]);
		}
	}

	@Test
	void saveInlierIndexes() {
		fail("Implement");
	}

	@Nested
	class ChecksDefaultScoreMotion {
		@Test
		void checkScore() {
			PairwiseGraphUtils.ScoreMotion scorer = new PairwiseGraphUtils.DefaultScoreMotion();
			PairwiseImageGraph2.Motion m = new PairwiseImageGraph2.Motion();
			m.countF = 100;
			m.countH = 100;
			double score0 = scorer.score(m);

			// fewer points found to match homography so it should be more 3D
			m.countH = 50;
			double score1 = scorer.score(m);
			assertTrue(score1 > score0);

			// more points should result in a better score too
			m.countF *= 2;
			m.countH *= 2;
			double score2 = scorer.score(m);
			assertTrue(score2 > score1);
		}
	}
}