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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestPairwiseGraphUtils extends BoofStandardJUnit {

	@Test void findFullyConnectedTriple() {
		PairwiseImageGraph.View seed = new PairwiseImageGraph.View();
		seed.totalObservations = 100;

		for (int i = 0; i < 2; i++) {
			PairwiseImageGraph.Motion motion = new PairwiseImageGraph.Motion();
			motion.src = seed;
			motion.dst = new PairwiseImageGraph.View();
			motion.dst.totalObservations = 110; // give it a few extra and see if that causes a problem

			for (int j = 0; j < 100; j++) {
				motion.inliers.grow().setTo(j, j, 0);
			}
			seed.connections.add(motion);
		}

		// only connect 1/2 of them from B to C
		PairwiseImageGraph.Motion motionBC = new PairwiseImageGraph.Motion();
		motionBC.src = seed.connections.get(0).dst;
		motionBC.dst = seed.connections.get(1).dst;
		for (int i = 0; i < seed.totalObservations; i++) {
			motionBC.inliers.grow().setTo(i, i, 0);
		}
		motionBC.src.connections.add(motionBC);
		motionBC.dst.connections.add(motionBC);

		// Find the common tracks
		var alg = new PairwiseGraphUtils();
		alg.seed = seed;
		alg.viewB = seed.connections.get(0).other(seed);
		alg.viewC = seed.connections.get(1).other(seed);
		alg.createThreeViewLookUpTables();
		alg.findFullyConnectedTriple();

		assertEquals(seed.totalObservations, alg.commonIdx.size);
		for (int i = 0; i < alg.commonIdx.size; i++) {
			assertEquals(i, alg.commonIdx.get(i));
		}
	}

	@Test void findFullyConnectedTriple_list() {
		PairwiseImageGraph.View seed = new PairwiseImageGraph.View();
		seed.totalObservations = 100;
		DogArray_I32 seedFeatsIdx = new DogArray_I32();

		for (int i = 0; i < seed.totalObservations/2; i++) {
			seedFeatsIdx.add(i);
		}

		for (int i = 0; i < 2; i++) {
			PairwiseImageGraph.Motion motion = new PairwiseImageGraph.Motion();
			motion.src = seed;
			motion.dst = new PairwiseImageGraph.View();
			motion.dst.totalObservations = 110; // give it a few extra and see if that causes a problem

			for (int j = 0; j < 100; j++) {
				motion.inliers.grow().setTo(j, j, 0);
			}
			seed.connections.add(motion);
		}

		// only connect 1/2 of them from B to C
		PairwiseImageGraph.Motion motionBC = new PairwiseImageGraph.Motion();
		motionBC.src = seed.connections.get(0).dst;
		motionBC.dst = seed.connections.get(1).dst;
		for (int i = 0; i < seed.totalObservations; i++) {
			motionBC.inliers.grow().setTo(i, i, 0);
		}
		motionBC.src.connections.add(motionBC);
		motionBC.dst.connections.add(motionBC);

		// Find the common tracks
		var alg = new PairwiseGraphUtils();
		alg.seed = seed;
		alg.viewB = seed.connections.get(0).other(seed);
		alg.viewC = seed.connections.get(1).other(seed);
		alg.createThreeViewLookUpTables();
		alg.findFullyConnectedTriple(seedFeatsIdx);

		assertEquals(seed.totalObservations/2, alg.commonIdx.size);
		for (int i = 0; i < alg.commonIdx.size; i++) {
			assertEquals(i, alg.commonIdx.get(i));
		}
	}

	@Test void findAllConnectedSeed() {
		PairwiseImageGraph.View seed = new PairwiseImageGraph.View();
		seed.totalObservations = 100;
		DogArray_I32 seedFeatsIdx = new DogArray_I32();

		for (int i = 0; i < seed.totalObservations/2; i++) {
			seedFeatsIdx.add(i);
		}

		// Create the connections to the seed
		for (int i = 0; i < 6; i++) {
			PairwiseImageGraph.Motion motion = new PairwiseImageGraph.Motion();
			motion.src = seed;
			motion.dst = new PairwiseImageGraph.View();
			motion.dst.totalObservations = 110; // give it a few extra and see if that causes a problem

			boolean swapped = i%2 == 0;
			if (swapped) {
				PairwiseImageGraph.View tmp = motion.src;
				motion.src = motion.dst;
				motion.dst = tmp;
			}

			for (int j = 0; j < 50; j++) {
				// every other one in the seed will be in the inlier set
				if (swapped) {
					motion.inliers.grow().setTo(j, j*2, 0);
				} else {
					motion.inliers.grow().setTo(j*2, j, 0);
				}
			}
			// The order should not matter
			motion.inliers.shuffle(rand);
			seed.connections.add(motion);
		}

		// NOTE: Connections between the non-seed views is not specified since we don't care about them

		// Specify which connected view to the seed we are concerned about
		DogArray_I32 connectIdx = DogArray_I32.array(1, 2, 4);

		// Find the common tracks
		var commonIdx = new DogArray_I32();
		var alg = new PairwiseGraphUtils();
		alg.findAllConnectedSeed(seed, connectIdx, commonIdx);

		assertEquals(50, commonIdx.size);
		for (int i = 0; i < commonIdx.size; i++) {
			assertEquals(i*2, commonIdx.get(i));
		}
	}

	@Test void createThreeViewLookUpTables() {
		var alg = new PairwiseGraphUtils();
		var dbSimilar = new MockLookupSimilarImages(4, 0xDEADBEEF);
		alg.dbSimilar = dbSimilar;
		alg.dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);
		alg.seed = dbSimilar.graph.nodes.get(0);
		alg.viewB = dbSimilar.graph.nodes.get(1);
		alg.viewC = dbSimilar.graph.nodes.get(3);

		// Compute and check the results
		alg.createThreeViewLookUpTables();

		final int N = dbSimilar.feats3D.size();
		assertEquals(N, alg.table_A_to_B.size);
		assertEquals(N, alg.table_A_to_C.size);
		assertEquals(N, alg.table_B_to_C.size);

		// compare to ground truth tables
		for (int i = 0; i < N; i++) {
			assertEquals(dbSimilar.featToView.get(1)[i], alg.table_A_to_B.data[i]);
			assertEquals(dbSimilar.featToView.get(3)[i], alg.table_A_to_C.data[i]);
			int b_to_c = dbSimilar.featToView.get(3)[dbSimilar.viewToFeat.get(1)[i]];
			assertEquals(b_to_c, alg.table_B_to_C.data[i]);
		}
	}

	@SuppressWarnings("IntegerDivisionInFloatingPointContext")
	@Test void createTripleFromCommon() {
		var alg = new PairwiseGraphUtils();
		var dbSimilar = new MockLookupSimilarImages(4, 0xDEADBEEF);
		alg.dbSimilar = dbSimilar;
		alg.dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);
		alg.seed = dbSimilar.graph.nodes.get(0);
		alg.viewB = dbSimilar.graph.nodes.get(1);
		alg.viewC = dbSimilar.graph.nodes.get(3);

		// common is empty
		alg.createTripleFromCommon(null);
		assertEquals(0, alg.matchesTriple.size);

		// one element
		alg.table_A_to_B.add(1);
		alg.table_A_to_C.add(5);
		alg.commonIdx.add(0);
		alg.createTripleFromCommon(null);
		assertEquals(1, alg.matchesTriple.size);
		// undo the shift in pixel coordinates
		for (int i = 0; i < 3; i++) {
			alg.matchesTriple.get(0).get(i).x += dbSimilar.intrinsic.cx;
			alg.matchesTriple.get(0).get(i).y += dbSimilar.intrinsic.cy;
		}
		assertEquals(0.0, alg.matchesTriple.get(0).p1.distance(dbSimilar.viewObs.get(0).get(0)));
		assertEquals(0.0, alg.matchesTriple.get(0).p2.distance(dbSimilar.viewObs.get(1).get(1)));
		assertEquals(0.0, alg.matchesTriple.get(0).p3.distance(dbSimilar.viewObs.get(3).get(5)));

		// Add one more
		alg.table_A_to_B.add(2);
		alg.table_A_to_C.add(2);
		alg.commonIdx.add(1);
		alg.createTripleFromCommon(null);
		assertEquals(2, alg.matchesTriple.size);
	}

	@Test void initializeSbaSceneThreeView() {
		var dbSimilar = new MockLookupSimilarImages(4, 0xDEADBEEF);
		var alg = new PairwiseGraphUtils() {
			boolean called = false;

			@Override
			public void triangulateFeatures() {
				called = true;
			}
		};

		alg.dbSimilar = dbSimilar;
		alg.dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);
		alg.seed = dbSimilar.graph.nodes.get(0);
		alg.viewB = dbSimilar.graph.nodes.get(1);
		alg.viewC = dbSimilar.graph.nodes.get(3);
		alg.P1.setTo(dbSimilar.listCameraMatrices.get(0));
		alg.P2.setTo(dbSimilar.listCameraMatrices.get(1));
		alg.P3.setTo(dbSimilar.listCameraMatrices.get(3));

		alg.inliersThreeView.add(new AssociatedTriple());

		alg.initializeSbaSceneThreeView(true);

		assertEquals(1, alg.structurePr.points.size);

		// triangulate is tested elsewhere for accuracy
		assertTrue(alg.called);

		// See if the results were copied
		assertTrue(MatrixFeatures_DDRM.isIdentical(alg.P1, alg.structurePr.views.get(0).worldToView, 1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(alg.P2, alg.structurePr.views.get(1).worldToView, 1e-8));
		assertTrue(MatrixFeatures_DDRM.isIdentical(alg.P3, alg.structurePr.views.get(2).worldToView, 1e-8));
	}

	@Test void estimateProjectiveCamerasRobustly() {
		var alg = new PairwiseGraphUtils(new ConfigProjectiveReconstruction());
		var dbSimilar = new MockLookupSimilarImages(4, 0xDEADBEEF);
		alg.dbSimilar = dbSimilar;
		alg.dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);
		alg.seed = dbSimilar.graph.nodes.get(0);
		alg.viewB = dbSimilar.graph.nodes.get(1);
		alg.viewC = dbSimilar.graph.nodes.get(3);

		// Create boilerplate input for RANSAC
		alg.createThreeViewLookUpTables();
		alg.findFullyConnectedTriple();
		alg.createTripleFromCommon(null);

		// Simple test where we see how many inliers there are
		assertTrue(alg.estimateProjectiveCamerasRobustly());
		assertEquals(dbSimilar.feats3D.size(), alg.ransac.getMatchSet().size());

		// Change one of the edges to see if it handles src/dst swap correctly
		alg.viewB = dbSimilar.graph.nodes.get(2);
		// Everything should be an inlier since there's no noise
		assertEquals(dbSimilar.feats3D.size(), alg.ransac.getMatchSet().size());

		// Crude test to see if it saved some reasonable results
		assertTrue(MatrixFeatures_DDRM.isIdentity(alg.P1, 1e-8));
		assertFalse(MatrixFeatures_DDRM.isIdentity(alg.P2, 1e-8));
		assertFalse(MatrixFeatures_DDRM.isIdentity(alg.P3, 1e-8));
	}

	@Test void triangulateFeatures() {
		var alg = new PairwiseGraphUtils(new ConfigProjectiveReconstruction());
		var db = new MockLookupSimilarImages(4, 0xDEADBEEF);

		int offset = 5;
		alg.dbSimilar = db;
		alg.P1.setTo(db.listCameraMatrices.get(0));
		alg.P2.setTo(db.listCameraMatrices.get(1));
		alg.P3.setTo(db.listCameraMatrices.get(3));

		// Create the set of inliers. Only these inliers will be considered when computing 3D features
		for (int i = offset; i < db.feats3D.size(); i++) {
			int view1_idx = db.featToView.get(0)[i];
			int view2_idx = db.featToView.get(1)[i];
			int view3_idx = db.featToView.get(3)[i];

			AssociatedTriple t = new AssociatedTriple();
			t.p1.setTo(db.viewObs.get(0).get(view1_idx));
			t.p2.setTo(db.viewObs.get(1).get(view2_idx));
			t.p3.setTo(db.viewObs.get(3).get(view3_idx));

			alg.inliersThreeView.add(t);
		}

		alg.structurePr.initialize(3, alg.inliersThreeView.size());
		alg.triangulateFeatures();

		// start with 3D triangulation
		checkTriangulatedFeatures(alg);
	}

	/**
	 * Verifies that triangulation was done correctly by comparing the distance to ground truth
	 */
	private void checkTriangulatedFeatures( PairwiseGraphUtils alg ) {
		// Project the points back into the image and see if it gets the observations back
		// the input should be noise free so this should work to a high level of precision
		Point4D_F64 X = new Point4D_F64();
		Point2D_F64 expected = new Point2D_F64();
		for (int i = 0; i < alg.inliersThreeView.size(); i++) {
			alg.structurePr.getPoints().get(i).get(X);

			PerspectiveOps.renderPixel(alg.P1, X, expected);
			assertEquals(0.0, expected.distance(alg.inliersThreeView.get(i).p1), UtilEjml.TEST_F64);
			PerspectiveOps.renderPixel(alg.P2, X, expected);
			assertEquals(0.0, expected.distance(alg.inliersThreeView.get(i).p2), UtilEjml.TEST_F64);
			PerspectiveOps.renderPixel(alg.P3, X, expected);
			assertEquals(0.0, expected.distance(alg.inliersThreeView.get(i).p3), UtilEjml.TEST_F64);
		}
	}

	@Test void initializeSbaObservationsThreeView() {
		var alg = new PairwiseGraphUtils();
		var db = new MockLookupSimilarImages(4, 0xDEADBEEF);

		int[] views = new int[]{0, 1, 3};
		alg.dbSimilar = db;
		alg.P1.setTo(db.listCameraMatrices.get(views[0]));
		alg.P2.setTo(db.listCameraMatrices.get(views[1]));
		alg.P3.setTo(db.listCameraMatrices.get(views[2]));
		for (int i = 0; i < db.feats3D.size(); i++) {
			Point2D_F64 o1 = db.viewObs.get(views[0]).get(i);
			Point2D_F64 o2 = db.viewObs.get(views[1]).get(i);
			Point2D_F64 o3 = db.viewObs.get(views[2]).get(i);
			alg.inliersThreeView.add(new AssociatedTriple(o1, o2, o3));
		}

		alg.initializeSbaObservationsThreeView();

		Point2D_F64 found = new Point2D_F64();
		for (int viewIdx = 0; viewIdx < 3; viewIdx++) {
			assertEquals(db.feats3D.size(), alg.observations.views.get(viewIdx).point.size());

			for (int i = 0; i < db.feats3D.size(); i++) {
				Point2D_F64 expected = db.viewObs.get(views[viewIdx]).get(i);
				alg.observations.views.get(viewIdx).getPixel(i, found);

				assertEquals(0.0, expected.distance(found), UtilEjml.TEST_F32);
			}
		}
	}

	@Test void createTableViewAtoB() {
		// Create a randomized table showing how features are matched between the two views
		DogArray_I32 table_src_to_dst = DogArray_I32.range(0, 50);
		PrimitiveArrays.shuffle(table_src_to_dst.data, 0, table_src_to_dst.size, rand);

		// Create the list of associated pairs
		PairwiseImageGraph.Motion motion = new PairwiseImageGraph.Motion();
		motion.inliers.resize(table_src_to_dst.size);
		for (int i = 0; i < table_src_to_dst.size; i++) {
			AssociatedIndex associated = motion.inliers.get(i);
			associated.src = i;
			associated.dst = table_src_to_dst.data[i];
		}
		motion.src = new PairwiseImageGraph.View();
		motion.dst = new PairwiseImageGraph.View();
		motion.src.totalObservations = motion.dst.totalObservations = table_src_to_dst.size;
		motion.inliers.shuffle(rand); // shuffle this to make the order interesting to test

		DogArray_I32 found_src_to_dst = new DogArray_I32(table_src_to_dst.size);
		// Reconstruct the look up table
		PairwiseGraphUtils.createTableViewAtoB(motion.src, motion, found_src_to_dst);
		for (int i = 0; i < table_src_to_dst.size; i++) {
			assertEquals(table_src_to_dst.data[i], found_src_to_dst.data[i]);
		}

		// Reverse direction now
		{
			PairwiseImageGraph.View tmp = motion.src;
			motion.src = motion.dst;
			motion.dst = tmp;
		}
		for (int i = 0; i < motion.inliers.size; i++) {
			AssociatedIndex associated = motion.inliers.get(i);
			int tmp = associated.src;
			associated.src = associated.dst;
			associated.dst = tmp;
		}
		PairwiseGraphUtils.createTableViewAtoB(motion.dst, motion, found_src_to_dst);
		for (int i = 0; i < table_src_to_dst.size; i++) {
			assertEquals(table_src_to_dst.data[i], found_src_to_dst.data[i]);
		}
	}

	@Test void saveRansacInliers() {
		var alg = new PairwiseGraphUtils();

		SceneWorkingGraph.View viewA = new SceneWorkingGraph.View();
		viewA.pview = new PairwiseImageGraph.View();
		viewA.pview.totalObservations = 120;

		int numInliers = 50;
		alg.seed = viewA.pview;
		alg.viewB = new PairwiseImageGraph.View();
		alg.viewC = new PairwiseImageGraph.View();

		// tables need to cover every observation in A
		alg.table_A_to_B.setTo(DogArray_I32.range(0, viewA.pview.totalObservations));
		alg.table_A_to_C.setTo(DogArray_I32.range(0, viewA.pview.totalObservations));
		PrimitiveArrays.shuffle(alg.table_A_to_B.data, 0, alg.table_A_to_B.size, rand);
		PrimitiveArrays.shuffle(alg.table_A_to_C.data, 0, alg.table_A_to_C.size, rand);

		alg.commonIdx.setTo(DogArray_I32.range(0, numInliers*2));

		// j = i*2
		alg.ransac = new MockRansac();

		// just add elements until it hits the desired size
		for (int i = 0; i < numInliers; i++) {
			alg.inliersThreeView.add(new AssociatedTriple());
			alg.inlierIdx.add(alg.ransac.getInputIndex(i));
		}

		// Go through all 3 possible views as the first view in the inliers
		for (int firstView = 0; firstView < 3; firstView++) {
			SceneWorkingGraph.View view0 = new SceneWorkingGraph.View();
			view0.pview = switch (firstView) {
				case 0 -> alg.seed;
				case 1 -> alg.viewB;
				case 2 -> alg.viewC;
				default -> throw new RuntimeException("BUG");
			};
			//-------- Call the function being tested
			alg.saveRansacInliers(view0, alg.inlierIdx);

			// Check the results
			assertEquals(1, view0.inliers.size);
			SceneWorkingGraph.InlierInfo inlier = view0.inliers.get(0);
			assertEquals(0.0, inlier.scoreGeometric);
			assertEquals(3, inlier.views.size);
			assertSame(view0.pview, inlier.views.get(0));
			assertNotSame(view0.pview, inlier.views.get(1));
			assertNotSame(view0.pview, inlier.views.get(2));
			assertEquals(3, inlier.observations.size);
			for (int checkView = 0; checkView < 3; checkView++) {
				PairwiseImageGraph.View v = inlier.views.get(checkView);
				DogArray_I32 indexes = inlier.observations.get(checkView);
				assertEquals(numInliers, indexes.size);

				if (alg.seed == v) {
					for (int j = 0; j < numInliers; j++) {
						assertEquals(j*2, indexes.get(j));
					}
				} else if (alg.viewB == v) {
					for (int j = 0; j < numInliers; j++) {
						assertEquals(alg.table_A_to_B.get(j*2), indexes.get(j));
					}
				} else if (alg.viewC == v) {
					for (int j = 0; j < numInliers; j++) {
						assertEquals(alg.table_A_to_C.get(j*2), indexes.get(j));
					}
				} else {
					fail("BUG");
				}
			}
		}
	}

	private static class MockRansac implements ModelMatcher<TrifocalTensor, AssociatedTriple> {
		@Override public boolean process( List<AssociatedTriple> dataSet ) { return false; }

		@Override public TrifocalTensor getModelParameters() { return null; }

		@Override public List<AssociatedTriple> getMatchSet() { return null; }

		@Override public int getInputIndex( int matchIndex ) { return matchIndex*2; }

		@Override public double getFitQuality() { return 0; }

		@Override public int getMinimumSize() { return 0; }

		@Override public void reset() {}

		@Override public Class<AssociatedTriple> getPointType() { return null; }

		@Override public Class<TrifocalTensor> getModelType() { return null; }
	}
}
