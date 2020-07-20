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

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.pose.CompatibleProjectiveHomography;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.alg.sfm.structure2.ProjectiveReconstructionFromPairwiseGraph.SeedInfo;
import org.ddogleg.struct.FastArray;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestProjectiveReconstructionFromPairwiseGraph {

	/**
	 * Random scene with fairly linear motion. Everything is randomized and physical constraints on camera are enforced
	 */
	@Test
	void process_perfect() {
		// NOTE: Accuracy degrades as the number of views increases. The tolerance is also hard coded at 1e-5 and 1e-7 or 1e-8
		//       would be more reasonable upper limit
		//       Additional work should be put into this so that accuracy with perfect data is independent of the number
		//       of views.

		var alg = new ProjectiveReconstructionFromPairwiseGraph();
		for (int numViews = 3; numViews <= 20; numViews++) {
			System.out.println("numViews = "+numViews);
			var db = new MockLookupSimilarImagesRealistic().setLoop(false).setSeed(numViews).setFeatures(450).pathLine(numViews,0.30,6.0,2);
			PairwiseImageGraph2 graph = db.createPairwise();
			assertTrue(alg.process(db,graph));
			checkCameraMatrices(alg,db);
		}
	}

	/**
	 * Compare found camera matrices against truth by converting them into the same projective scale
	 */
	private void checkCameraMatrices(ProjectiveReconstructionFromPairwiseGraph alg, MockLookupSimilarImagesRealistic db) {
		List<SceneWorkingGraph.View> foundViews = alg.workGraph.getAllViews();
		assertEquals(db.views.size(), foundViews.size());

		// Undo apply and undo the shift in pixel coordinates
		DMatrixRMaj M_inv = CommonOps_DDRM.identity(3);
		M_inv.set(0,2,db.intrinsic.width/2);
		M_inv.set(1,2,db.intrinsic.height/2);

		var tmp = new DMatrixRMaj(3,4);

		CompatibleProjectiveHomography compatible = new CompatibleProjectiveHomography();
		List<DMatrixRMaj> listA = new ArrayList<>();
		List<DMatrixRMaj> listB = new ArrayList<>();

		for( MockLookupSimilarImagesRealistic.View mv : db.views ) {
			DMatrixRMaj found = alg.workGraph.lookupView( mv.id ).projective;
//			found.print();
			CommonOps_DDRM.mult(M_inv,found, tmp);
			listA.add( tmp.copy() );
			listB.add( mv.camera );
		}

		DMatrixRMaj H = new DMatrixRMaj(4,4);
		assertTrue(compatible.fitCameras(listA,listB,H));

		DMatrixRMaj found = new DMatrixRMaj(3,4);
		for (int i = 0; i < listA.size(); i++) {
			CommonOps_DDRM.mult(listA.get(i),H,found);
			DMatrixRMaj expected = listB.get(i);
			double scale = MultiViewOps.findScale(found,expected);
			CommonOps_DDRM.scale(scale,found);
			double tol = CommonOps_DDRM.elementMaxAbs(found)*1e-6; // TODO change to 1e-7
			assertTrue(MatrixFeatures_DDRM.isIdentical(expected,found, tol));
		}
	}

	@Test
	void addOpenForView() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();
		PairwiseImageGraph2 graph = createLinearGraph(8, 1);

		var found = new FastArray<>(View.class);

		// it should add these two children since they are unknown
		alg.addOpenForView(graph.nodes.get(4),found);
		assertEquals(2,found.size);
		assertTrue(found.contains(graph.nodes.get(3)));
		assertTrue(found.contains(graph.nodes.get(5)));

		// they should not be added a second time
		found.clear();
		alg.addOpenForView(graph.nodes.get(4),found);
		assertEquals(0,found.size);
	}

	/**
	 * All three connections (a->b, a->c, b->c)  must have good scores for it to be selected
	 */
	@Test
	void selectNextToProcess_AllScoresGood() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// Create a linear graph and make every node known and add it to the list
		var open = new FastArray<>(View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 2);
		// Ensure every view has at most 3 connections to make designing this test easier
		removeLastConnection(graph);

		open.addAll(graph.nodes);
		open.forEach((i,o)->alg.workGraph.addView(o));
		View expected = open.get(4); // make #4 only have a slightly better score, and a valid set of connections
		View expectedConnA = expected.connections.get(0).other(expected);
		View expectedConnB = expected.connections.get(1).other(expected);
		expected.connections.get(0).countH = 80;
		expected.connections.get(1).countH = 80;
		expectedConnA.findMotion(expectedConnB).countH = 80; // it picks the lowest scored connection

		View selected = alg.selectNextToProcess(open);
		assertSame(expected, selected);
	}

	/**
	 * Ensures that only solutions which are known will be considered
	 */
	@Test
	void selectNextToProcess_MustBeKnown() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// Create a linear graph and make every node known and add it to the list
		var open = new FastArray<>(View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 2);
		// Ensure every view has at most 3 connections to make designing this test easier
		removeLastConnection(graph);

		open.addAll(graph.nodes);
		// Only #7 will have a full set of known connections
		open.forEach(5,8,(i,o)->alg.workGraph.addView(o));
		View expected = open.get(7);
		View selected = alg.selectNextToProcess(open);

		// if all where known then #2 would be selected since it's earlier in the order
		assertSame(expected, selected);
	}

	private void removeLastConnection(PairwiseImageGraph2 graph) {
		for (int i = 0; i < graph.nodes.size - 1; i++) {
			FastArray<Motion> conn = graph.nodes.get(i).connections;
			conn.remove(conn.size - 1);
		}
	}

	/**
	 * Fail if two connections have no common connection between them
	 */
	@Test
	void selectNextToProcess_TwoConnections_NoCommon() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// Each view will have two connections but the connections with not be connected to each other
		var open = new FastArray<>(View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 1);

		open.addAll(graph.nodes);
		open.forEach((i,o)->alg.workGraph.addView(o));

		View selected = alg.selectNextToProcess(open);
		assertSame(null, selected);
	}

	@Test
	void selectSeeds() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// linear graph with each node/view just as good as any other
		{
			PairwiseImageGraph2 graph = createLinearGraph(8, 1);
			Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph);
			List<SeedInfo> seeds = alg.selectSeeds(alg.seedScores, mapScores);
			assertEquals(3, seeds.size()); // determined through manual inspection
			sanityCheckSeeds(seeds);
		}

		// Similar situation but with one more view
		{
			PairwiseImageGraph2 graph = createLinearGraph(9, 1);
			Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph);
			List<SeedInfo> seeds = alg.selectSeeds(alg.seedScores, mapScores);
			assertEquals(3, seeds.size()); // determined through manual inspection
			sanityCheckSeeds(seeds);
		}

		// Let's make one node clearly very desirable and see if it's selected
		{
			// select different targets to stress the system more
			for (int targetIdx = 1; targetIdx < 4; targetIdx++) {
				PairwiseImageGraph2 graph = createLinearGraph(9, 1);
				View target = graph.nodes.get(targetIdx);
				target.connections.forEach((i,o)->o.countH=20);

				Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph);
				List<SeedInfo> seeds = alg.selectSeeds(alg.seedScores, mapScores);
				assertTrue(3 == seeds.size() || 2 == seeds.size()); // determined through manual inspection
				sanityCheckSeeds(seeds);
				assertSame(seeds.get(0).seed,target);
			}
		}
	}

	/**
	 * A few simple sanity checks on the selected seeds
	 */
	void sanityCheckSeeds( List<SeedInfo> seeds ) {
		// make sure non of the seeds are connected to each other
		for (int i = 0; i < seeds.size(); i++) {
			SeedInfo a = seeds.get(i);
			for (int j = i+1; j < seeds.size(); j++) {
				SeedInfo b = seeds.get(j);
				assertNull( a.seed.findMotion(b.seed) );
			}
		}
	}

	@Test
	void score_view() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// first has the best connection but should have a worse sum score
		SeedInfo info0 = alg.score(viewByConnections(90,200,150,80), new SeedInfo());
		SeedInfo info1 = alg.score(viewByConnections(90,190,185,150), new SeedInfo());
		assertTrue(info1.score > info0.score);

		// the new score has a much higher single score but it's a single score
		SeedInfo info2 = alg.score(viewByConnections(250), new SeedInfo());
		assertTrue(info1.score > info2.score);

		// just a test to see if it blows up
		SeedInfo info3 = alg.score(viewByConnections(), new SeedInfo());
		assertEquals(0.0, info3.score, UtilEjml.TEST_F64);
	}

	private View viewByConnections( int ...counts ) {
		View v = new View();
		v.id = "id";
		for( int count : counts ) {
			Motion m = new Motion();
			m.countF = count;
			m.countH = 100;
			m.is3D = true;
			v.connections.add(m);
		}
		return v;
	}

	/**
	 * Creates a graph which has a linear set of connected views. Each view is connected to 2*numConnect neighbors.
	 */
	private PairwiseImageGraph2 createLinearGraph( int numViews , int numConnect) {
		var graph = new PairwiseImageGraph2();

		for (int i = 0; i < numViews; i++) {
			graph.createNode(""+i);
		}

		for (int i = 0; i < numViews; i++) {
			View va = graph.nodes.get(i);
			for (int j = 1; j <= numConnect; j++) {
				int idx = i+j;
				if( idx >= numViews)
					break;
				View vb = graph.nodes.get(idx);
				Motion c = graph.connect(va,vb);
				c.is3D = true;
				c.countH = 100;
				c.countF = 120;
			}
		}

		return graph;
	}
}