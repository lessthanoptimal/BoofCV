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
	 * Test with the minimum number of views
	 */
	@Test
	void process_perfect_linear_3() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();
		alg.getInitProjective().utils.configConvergeSBA.maxIterations = 0; // TODO remove.  SBA causes an error. Normalization?
		var db = new MockLookupSimilarImagesCircleAround().init(3,1);

		assertTrue(alg.process(db,db.graph));

		checkCameraMatrices(alg, db);
	}

	/**
	 * Test with a bunch of views
	 */
	@Test
	void process_perfect_linear_10() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();
		alg.getInitProjective().utils.configConvergeSBA.maxIterations = 0; // TODO remove. SBA causes an error. Normalization?
		var db = new MockLookupSimilarImagesCircleAround().init(10,2);

		assertTrue(alg.process(db,db.graph));

		checkCameraMatrices(alg, db);
	}

	/**
	 * Compare found camera matrices against truth by converting them into the same projective scale
	 */
	private void checkCameraMatrices(ProjectiveReconstructionFromPairwiseGraph alg, MockLookupSimilarImagesCircleAround db) {
		List<SceneWorkingGraph.View> foundViews = new ArrayList<>(alg.workGraph.getAllViews());
		assertEquals(db.graph.nodes.size, foundViews.size());

		CompatibleProjectiveHomography compatible = new CompatibleProjectiveHomography();
		List<DMatrixRMaj> listA = new ArrayList<>();
		List<DMatrixRMaj> listB = new ArrayList<>();

		for( String id : db.viewIds ) {
//			System.out.println("id="+id+" index="+db.viewIds.indexOf(id));
			listA.add( alg.workGraph.lookupView( id ).projective);
			listB.add( db.listCameraMatrices.get( db.viewIds.indexOf(id)) );
		}

		DMatrixRMaj H = new DMatrixRMaj(4,4);
		assertTrue(compatible.fitCameras(listA,listB,H));

		DMatrixRMaj found = new DMatrixRMaj(3,4);
		for (int i = 0; i < listA.size(); i++) {
			CommonOps_DDRM.mult(listA.get(i),H,found);
			DMatrixRMaj expected = listB.get(i);
			double scale = expected.get(0,0)/found.get(0,0);
			CommonOps_DDRM.scale(scale,found);
			double tol = CommonOps_DDRM.elementMaxAbs(found)*1e-4;
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
	 * Simple tests which gives it an obvious answer
	 */
	@Test
	void selectNextToProcess() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// Create a linear graph and make every node known and add it to the list
		var open = new FastArray<>(View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 1);
		open.addAll(graph.nodes);
		open.forEach((i,o)->alg.workGraph.addView(o));
		View expected = open.get(4);
		expected.connections.forEach((i,o)->o.countH=50); // make them have better scores

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
		PairwiseImageGraph2 graph = createLinearGraph(12, 1);
		open.addAll(graph.nodes);
		// This view is not known and will be ignored
		open.get(4).connections.forEach((i,o)->o.countH=50); // ensure this view has a better score
		// this view is known but has a worse score
		View expected = open.get(8);
		expected.connections.forEach((i,o)->o.countH=70); // ensure this view has a better score
		alg.workGraph.addView(open.get(8));
		open.forEach(7,10,(i,o)->alg.workGraph.addView(o));

		View selected = alg.selectNextToProcess(open);
		assertSame(expected, selected);
	}

	/**
	 * Set up a situation where if the code relies on the score only it will select the wrong view
	 */
	@Test
	void selectNextToProcess_PreferTwoConnections() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// Create a linear graph and make every node known and add it to the list
		var open = new FastArray<>(View.class);
		PairwiseImageGraph2 graph = createLinearGraph(8, 1);
		open.addAll(graph.nodes);
		open.forEach((i,o)->alg.workGraph.addView(o));
		View expected = open.get(4); // make #4 only have a slightly better score, and two connections
		expected.connections.forEach((i,o)->o.countH=80);
		View modified = open.get(5); // will have a massive better score but only one connection
		modified.connections.remove(0);
		modified.connections.get(0).countH = 20;

		View selected = alg.selectNextToProcess(open);
		assertSame(expected, selected);
	}

	@Test
	void selectSeeds() {
		var alg = new ProjectiveReconstructionFromPairwiseGraph();

		// linear graph with each node/view just as good as any other
		{
			PairwiseImageGraph2 graph = createLinearGraph(8, 1);
			Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph);
			List<SeedInfo> seeds = alg.selectSeeds(alg.seedScores, mapScores);
			assertEquals(2, seeds.size()); // determined through manual inspection
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
			PairwiseImageGraph2 graph = createLinearGraph(9, 1);
			// select different targets to stress the system more
			for (int targetIdx = 0; targetIdx < 3; targetIdx++) {
				View target = graph.nodes.get(targetIdx);
				target.connections.forEach((i,o)->o.countH=20);

				Map<String, SeedInfo> mapScores = alg.scoreNodesAsSeeds(graph);
				List<SeedInfo> seeds = alg.selectSeeds(alg.seedScores, mapScores);
				assertEquals(3, seeds.size()); // determined through manual inspection
				sanityCheckSeeds(seeds);
				assertTrue(seeds.contains(mapScores.get(target.id)));
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
				int idx = (i+j)%numViews;
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