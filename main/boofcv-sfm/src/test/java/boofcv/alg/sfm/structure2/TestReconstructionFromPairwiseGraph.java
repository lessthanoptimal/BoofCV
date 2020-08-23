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

import boofcv.alg.sfm.structure2.ReconstructionFromPairwiseGraph.SeedInfo;
import org.ddogleg.struct.FastArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestReconstructionFromPairwiseGraph {

	@Test
	void addOpenForView() {
		var alg = new Helper();
		PairwiseImageGraph2 graph = createLinearGraph(8, 1);

		var found = new FastArray<>(PairwiseImageGraph2.View.class);

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
		var alg = new Helper();

		// Create a linear graph and make every node known and add it to the list
		var open = new FastArray<>(PairwiseImageGraph2.View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 2);
		// Ensure every view has at most 3 connections to make designing this test easier
		removeLastConnection(graph);

		open.addAll(graph.nodes);
		open.forEach((i,o)->alg.workGraph.addView(o));
		PairwiseImageGraph2.View expected = open.get(4); // make #4 only have a slightly better score, and a valid set of connections
		PairwiseImageGraph2.View expectedConnA = expected.connections.get(0).other(expected);
		PairwiseImageGraph2.View expectedConnB = expected.connections.get(1).other(expected);
		expected.connections.get(0).countH = 80;
		expected.connections.get(1).countH = 80;
		expectedConnA.findMotion(expectedConnB).countH = 80; // it picks the lowest scored connection

		PairwiseImageGraph2.View selected = alg.selectNextToProcess(open);
		assertSame(expected, selected);
	}

	/**
	 * Ensures that only solutions which are known will be considered
	 */
	@Test
	void selectNextToProcess_MustBeKnown() {
		var alg = new Helper();

		// Create a linear graph and make every node known and add it to the list
		var open = new FastArray<>(PairwiseImageGraph2.View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 2);
		// Ensure every view has at most 3 connections to make designing this test easier
		removeLastConnection(graph);

		open.addAll(graph.nodes);
		// Only #7 will have a full set of known connections
		open.forEach(5,8,(i,o)->alg.workGraph.addView(o));
		PairwiseImageGraph2.View expected = open.get(7);
		PairwiseImageGraph2.View selected = alg.selectNextToProcess(open);

		// if all where known then #2 would be selected since it's earlier in the order
		assertSame(expected, selected);
	}

	private void removeLastConnection(PairwiseImageGraph2 graph) {
		for (int i = 0; i < graph.nodes.size - 1; i++) {
			FastArray<PairwiseImageGraph2.Motion> conn = graph.nodes.get(i).connections;
			conn.remove(conn.size - 1);
		}
	}

	/**
	 * Fail if two connections have no common connection between them
	 */
	@Test
	void selectNextToProcess_TwoConnections_NoCommon() {
		var alg = new Helper();

		// Each view will have two connections but the connections with not be connected to each other
		var open = new FastArray<>(PairwiseImageGraph2.View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 1);

		open.addAll(graph.nodes);
		open.forEach((i,o)->alg.workGraph.addView(o));

		PairwiseImageGraph2.View selected = alg.selectNextToProcess(open);
		assertSame(null, selected);
	}

	@Test
	void selectSeeds() {
		var alg = new Helper();

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
				PairwiseImageGraph2.View target = graph.nodes.get(targetIdx);
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
		var alg = new Helper();

		// first has the best connection but should have a worse sum score
		SeedInfo info0 = alg.scoreAsSeed(viewByConnections(90,200,150,80), new SeedInfo());
		SeedInfo info1 = alg.scoreAsSeed(viewByConnections(90,190,185,150), new SeedInfo());
		assertTrue(info1.score > info0.score);

		// the new score has a much higher single score but it's a single score
		SeedInfo info2 = alg.scoreAsSeed(viewByConnections(250), new SeedInfo());
		assertTrue(info1.score > info2.score);

		// just a test to see if it blows up
		SeedInfo info3 = alg.scoreAsSeed(viewByConnections(), new SeedInfo());
		assertEquals(0.0, info3.score, UtilEjml.TEST_F64);
	}

	private PairwiseImageGraph2.View viewByConnections(int ...counts ) {
		PairwiseImageGraph2.View v = new PairwiseImageGraph2.View();
		v.id = "id";
		for( int count : counts ) {
			PairwiseImageGraph2.Motion m = new PairwiseImageGraph2.Motion();
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
			PairwiseImageGraph2.View va = graph.nodes.get(i);
			for (int j = 1; j <= numConnect; j++) {
				int idx = i+j;
				if( idx >= numViews)
					break;
				PairwiseImageGraph2.View vb = graph.nodes.get(idx);
				PairwiseImageGraph2.Motion c = graph.connect(va,vb);
				c.is3D = true;
				c.countH = 100;
				c.countF = 120;
			}
		}

		return graph;
	}

	private class Helper extends ReconstructionFromPairwiseGraph {

		public Helper() {
			super(new PairwiseGraphUtils());
		}
	}
}