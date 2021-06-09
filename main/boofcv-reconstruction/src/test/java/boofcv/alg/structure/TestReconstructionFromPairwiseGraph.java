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

import boofcv.alg.structure.ReconstructionFromPairwiseGraph.SeedInfo;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.FastArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestReconstructionFromPairwiseGraph extends BoofStandardJUnit {

	@Test void addOpenForView() {
		var alg = new Helper();
		PairwiseImageGraph graph = createLinearGraph(8, 1);

		var scene = new SceneWorkingGraph();

		// it should add these two children since they are unknown
		alg.addOpenForView(scene, graph.nodes.get(4));
		assertEquals(2, scene.open.size);
		assertTrue(scene.open.contains(graph.nodes.get(3)));
		assertTrue(scene.open.contains(graph.nodes.get(5)));

		// they should not be added a second time
		scene.open.clear();
		alg.addOpenForView(scene, graph.nodes.get(4));
		assertEquals(0, scene.open.size);
	}

	/**
	 * All three connections (a->b, a->c, b->c)  must have good scores for it to be selected
	 */
	@Test void selectNextToProcess_AllScoresGood() {
		var alg = new Helper();

		// Create a linear graph and make every node known and add it to the list
		PairwiseImageGraph graph = createLinearGraph(8, 2);
		// Ensure every view has at most 3 connections to make designing this test easier
		removeLastConnection(graph);

		var scene = new SceneWorkingGraph();
		var selection = new ReconstructionFromPairwiseGraph.Expansion();
		var cameraDummy = new SceneWorkingGraph.Camera();

		scene.open.addAll(graph.nodes);
		scene.open.forIdx(( i, o ) -> scene.addView(o, cameraDummy));
		PairwiseImageGraph.View expected = scene.open.get(4); // make #4 only have a slightly better score, and a valid set of connections
		PairwiseImageGraph.View expectedConnA = expected.connections.get(0).other(expected);
		PairwiseImageGraph.View expectedConnB = expected.connections.get(1).other(expected);
		expected.connections.get(0).score3D = 1.3;
		expected.connections.get(1).score3D = 1.3;
		expectedConnA.findMotion(expectedConnB).score3D = 1.3; // it picks the lowest scored connection

		assertTrue(alg.selectNextToProcess(scene, selection));
		assertSame(expected, scene.open.get(selection.openIdx));
	}

	/**
	 * Ensures that only solutions which are known will be considered
	 */
	@Test void selectNextToProcess_MustBeKnown() {
		var alg = new Helper();

		// Create a linear graph and make every node known and add it to the list
		PairwiseImageGraph graph = createLinearGraph(8, 2);
		// Ensure every view has at most 3 connections to make designing this test easier
		removeLastConnection(graph);

		var scene = new SceneWorkingGraph();
		var cameraDummy = new SceneWorkingGraph.Camera();
		var selection = new ReconstructionFromPairwiseGraph.Expansion();

		scene.open.addAll(graph.nodes);
		// Only #7 will have a full set of known connections
		scene.open.forIdx(5, 8, ( i, o ) -> scene.addView(o, cameraDummy));
		PairwiseImageGraph.View expected = scene.open.get(7);
		assertTrue(alg.selectNextToProcess(scene, selection));

		// if all where known then #2 would be selected since it's earlier in the order
		assertSame(expected, scene.open.get(selection.openIdx));
	}

	private void removeLastConnection( PairwiseImageGraph graph ) {
		for (int i = 0; i < graph.nodes.size - 1; i++) {
			FastArray<PairwiseImageGraph.Motion> conn = graph.nodes.get(i).connections;
			conn.remove(conn.size - 1);
		}
	}

	/**
	 * Fail if two connections have no common connection between them
	 */
	@Test void selectNextToProcess_TwoConnections_NoCommon() {
		var alg = new Helper();

		// Each view will have two connections but the connections with not be connected to each other
		var open = new FastArray<>(PairwiseImageGraph.View.class);
		PairwiseImageGraph graph = createLinearGraph(8, 1);

		var scene = new SceneWorkingGraph();
		var cameraDummy = new SceneWorkingGraph.Camera();
		var selection = new ReconstructionFromPairwiseGraph.Expansion();

		open.addAll(graph.nodes);
		open.forIdx(( i, o ) -> scene.addView(o, cameraDummy));

		assertFalse(alg.selectNextToProcess(scene, selection));
		assertEquals(-1, selection.openIdx);
	}

	@Test void selectAndSpawnSeeds() {
		var dbSimilar = new MockLookupSimilarImages(1, 1); // not actually used
		var dbCams = new MockLookUpCameraInfo(dbSimilar.intrinsic);

		var alg = new Helper();
		alg.spawnSceneReturn = true;

		// linear graph with each node/view just as good as any other
		// it will also only be able to find 1 neighbor since they won't be connected
		{
			PairwiseImageGraph graph = createLinearGraph(8, 1);
			Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph, 2);
			alg.selectAndSpawnSeeds(dbSimilar, dbCams, graph, alg.seedScores, mapScores);
			assertEquals(3, alg.selected.size()); // determined through manual inspection
			sanityCheckSeeds(alg.selected, 1);
		}

		// There are more connections now and the seed set should have 3 elements in it
		{
			alg.selected.clear();
			PairwiseImageGraph graph = createLinearGraph(8, 2);
			Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph, 2);
			alg.selectAndSpawnSeeds(dbSimilar, dbCams, graph, alg.seedScores, mapScores);
			sanityCheckSeeds(alg.selected, 2);
		}

		// Similar situation but with one more view
		{
			alg.selected.clear();
			PairwiseImageGraph graph = createLinearGraph(9, 2);
			Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph, 2);
			alg.selectAndSpawnSeeds(dbSimilar, dbCams, graph, alg.seedScores, mapScores);
			assertEquals(2, alg.selected.size()); // determined through manual inspection
			sanityCheckSeeds(alg.selected, 2);
		}

		// Let's make one node clearly very desirable and see if it's selected
		{
			// select different targets to stress the system more
			for (int targetIdx = 1; targetIdx < 4; targetIdx++) {
				PairwiseImageGraph graph = createLinearGraph(9, 2);
				PairwiseImageGraph.View target = graph.nodes.get(targetIdx);
				target.connections.get(0).score3D = 5;
				// Give the first two connections a higher motion score to make this triplet stand out
				PairwiseImageGraph.View ta = target.connections.get(0).other(target);
				PairwiseImageGraph.View tb = target.connections.get(1).other(target);
				Objects.requireNonNull(ta.findMotion(tb)).score3D = 4.0;

				Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph, 2);
				alg.selected.clear();
				alg.selectAndSpawnSeeds(dbSimilar, dbCams, graph, alg.seedScores, mapScores);
				assertTrue(3 == alg.selected.size() || 2 == alg.selected.size()); // determined through manual inspection
				sanityCheckSeeds(alg.selected, 2);
				assertSame(target, alg.selected.get(0).seed);
			}
		}
	}

	/**
	 * A few simple sanity checks on the selected seeds
	 */
	void sanityCheckSeeds( List<SeedInfo> seeds, int numSelected ) {
		// make sure non of the seeds are connected to each other
		for (int i = 0; i < seeds.size(); i++) {
			SeedInfo a = seeds.get(i);
			assertEquals(numSelected, a.motions.size);

			// make sure none of the seeds appear in any other seeds
			for (int j = i + 1; j < seeds.size(); j++) {
				SeedInfo b = seeds.get(j);
				// none of the other seeds should be neighbors of a
				assertNull(a.seed.findMotion(b.seed));
			}
		}
	}

	@Test void score_view() {
		var alg = new Helper();

		// first has the best connection but should have a worse sum score
		// score between the "other" views is set higher than the score to the root so that score
		// that matters is the connection to the root
		SeedInfo info0 = alg.scoreSeedAndSelectSet(viewByConnections(300, 90, 200, 150, 80), 2, new SeedInfo());
		SeedInfo info1 = alg.scoreSeedAndSelectSet(viewByConnections(300, 90, 190, 185, 150), 2, new SeedInfo());
		assertTrue(info1.score > info0.score);
		assertEquals(2, info0.motions.size);
		assertEquals(2, info1.motions.size);

		// make sure the highest scoring motion was selected
		assertTrue(info0.motions.contains(1));

		// the new score has a much higher single score but it's a single score
		SeedInfo info2 = alg.scoreSeedAndSelectSet(viewByConnections(300, 250), 2, new SeedInfo());
		assertTrue(info1.score > info2.score);
		assertEquals(1, info2.motions.size);

		// just a test to see if it blows up
		SeedInfo info3 = alg.scoreSeedAndSelectSet(viewByConnections(300), 2, new SeedInfo());
		assertEquals(0.0, info3.score, UtilEjml.TEST_F64);
	}

	/**
	 * Two views have a high score but are identical and have a low connecting score or are not 3D and should
	 * not be included together
	 */
	@Test void score_IdenticalViews() {
		var alg = new Helper();

		// The score between the "others" is low and will dominate
		SeedInfo info0 = alg.scoreSeedAndSelectSet(viewByConnections(20, 200, 200, 200, 200), 2, new SeedInfo());
		SeedInfo info1 = alg.scoreSeedAndSelectSet(viewByConnections(50, 90, 200, 150, 80), 2, new SeedInfo());

		assertEquals(2, info0.motions.size);
		assertEquals(2, info1.motions.size);
		assertTrue(info1.score > info0.score);

		// We will make other[0] and other[1] not have a 3D relationship. This should cause one
		// of them to be excluded
		PairwiseImageGraph.View root = viewByConnections(200, 200, 200, 50);
		root.connections.get(0).other(root).connections.get(1).is3D = false;
		SeedInfo info2 = alg.scoreSeedAndSelectSet(root, 2, new SeedInfo());
		assertFalse(info2.motions.contains(0) && info2.motions.contains(1));
	}

	private PairwiseImageGraph.View viewByConnections( int betweenCounts, int... counts ) {
		PairwiseImageGraph.View root = new PairwiseImageGraph.View();
		root.id = "id";
		for (int count : counts) {
			PairwiseImageGraph.Motion m = new PairwiseImageGraph.Motion();
			m.dst = root;
			m.src = new PairwiseImageGraph.View();
			m.score3D = count/100.0;
			m.is3D = true;
			root.connections.add(m);
			m.src.connections.add(m);
		}

		// Connections between the view are also considered
		for (int i = 0; i < root.connections.size; i++) {
			PairwiseImageGraph.View va = root.connections.get(i).other(root);

			for (int j = i + 1; j < root.connections.size; j++) {
				PairwiseImageGraph.View vb = root.connections.get(j).other(root);

				PairwiseImageGraph.Motion m = new PairwiseImageGraph.Motion();
				m.src = va;
				m.dst = vb;
				m.score3D = betweenCounts/100.0;
				m.is3D = true;
				va.connections.add(m);
				vb.connections.add(m);
			}
		}

		return root;
	}

	/**
	 * Creates a graph which has a linear set of connected views. Each view is connected to 2*numConnect neighbors.
	 */
	private PairwiseImageGraph createLinearGraph( int numViews, int numConnect ) {
		var graph = new PairwiseImageGraph();

		for (int i = 0; i < numViews; i++) {
			graph.createNode("" + i);
		}

		for (int i = 0; i < numViews; i++) {
			PairwiseImageGraph.View va = graph.nodes.get(i);
			for (int j = 1; j <= numConnect; j++) {
				int idx = i + j;
				if (idx >= numViews)
					break;
				PairwiseImageGraph.View vb = graph.nodes.get(idx);
				PairwiseImageGraph.Motion c = graph.connect(va, vb);
				c.is3D = true;
				c.score3D = 120.0/100.0;
			}
		}

		return graph;
	}

	private class Helper extends ReconstructionFromPairwiseGraph {
		public Helper() {super(new PairwiseGraphUtils());}

		List<SeedInfo> selected = new ArrayList<>();
		boolean spawnSceneReturn = true;

		@Override
		protected boolean spawnSceneFromSeed( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams,
											  PairwiseImageGraph pairwise, SeedInfo info ) {
			selected.add(info);
			return spawnSceneReturn;
		}
	}
}
