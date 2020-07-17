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

import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.testing.BoofTesting;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestProjectiveExpandByOneView {
	Random rand = BoofTesting.createRandom(0);

	@Test
	void perfect() {
		var db = new MockLookupSimilarImages(5,0xDEADBEEF);
		var alg = new ProjectiveExpandByOneView();

		var found = new DMatrixRMaj(3,4);
		for (int targetIdx : new int[]{3,4} ) {
			// Catch more issues by including/excluding the first node
			// First node has zeros in it's columns (edge case that once caused issues) and mixes up the indexes
			for (int startNode = 0; startNode < 2; startNode++) {
				var working = new SceneWorkingGraph();
				for (int i = startNode; i < 3; i++) {
					working.addView(db.graph.nodes.get(i)).projective.set(db.listCameraMatrices.get(i));
				}

				View target = db.graph.nodes.get(targetIdx);

				assertTrue(alg.process(db,working,target,found));

				// they should now be the same up to a scale factor
				DMatrixRMaj expected = db.listCameraMatrices.get(targetIdx);
				double scale = expected.get(0,0)/found.get(0,0);
				CommonOps_DDRM.scale(scale,found);
				assertTrue(MatrixFeatures_DDRM.isEquals(expected,found, 1e-7));
			}
		}
	}

	@Test
	void computeConversionHomography() {
		var working = new SceneWorkingGraph();
		var graph = new PairwiseImageGraph2();
		var alg = new ProjectiveExpandByOneView();
		alg.workGraph = working;

		// Known transform between the two views
		DMatrixRMaj H = RandomMatrices_DDRM.rectangle(4,4,rand);
		RandomMatrices_DDRM.fillUniform(alg.utils.P2,rand);
		RandomMatrices_DDRM.fillUniform(alg.utils.P3,rand);

		alg.utils.viewB = graph.createNode("B");
		alg.utils.viewC = graph.createNode("C");
		SceneWorkingGraph.View workB = working.addView(alg.utils.viewB);
		SceneWorkingGraph.View workC = working.addView(alg.utils.viewC);

		CommonOps_DDRM.mult(alg.utils.P2,H,workB.projective);
		CommonOps_DDRM.mult(alg.utils.P3,H,workC.projective);

		assertTrue(alg.computeConversionHomography());
		// check results
		assertTrue(MatrixFeatures_DDRM.isEquals(H,alg.localToGlobal, UtilEjml.TEST_F64));
	}

	/**
	 * Creates a more complex scenario and sees if it selects the correct two views
	 */
	@Test
	void selectTwoConnections() {
		var working = new SceneWorkingGraph();
		var graph = new PairwiseImageGraph2();
		View seed = graph.createNode("A");

		for (int i = 0; i < 6; i++) {
			View viewI = graph.createNode("" + i);

			// make sure src/dst is handled correctly
			Motion mA = i % 2 == 0 ? graph.connect(seed, viewI) : graph.connect(viewI, seed);
			// All have the same score so that the connections between the other two is what decides the winner
			mA.countF = 100;
			mA.countH = 80;
			mA.is3D = true;

			working.addView(viewI);
		}

		View viewB = seed.connection(1);
		View viewC = seed.connection(2);
		View viewD = seed.connection(3);

		Motion connBC = graph.connect(viewB,viewC);
		Motion connBD = graph.connect(viewD,viewB);

		connBD.is3D = connBC.is3D = true;
		connBC.countH = connBD.countH = 50;
		connBC.countF = 70;
		connBD.countF = 75; // this is the more desirable connection

		var alg = new ProjectiveExpandByOneView();
		alg.workGraph = working;

		var found = new ArrayList<Motion>();
		assertTrue(alg.selectTwoConnections(seed,found));

		assertTrue(found.contains(seed.connections.get(1)));
		assertTrue(found.contains(seed.connections.get(3)));
	}

	@Test
	void createListOfValid() {
		var working = new SceneWorkingGraph();
		var graph = new PairwiseImageGraph2();
		View seed = graph.createNode("A");

		for (int i = 0; i < 10; i++) {
			View viewI = graph.createNode("" + i);
			Motion mA = graph.connect(seed, viewI);

			// is3D and being known are at different frequencies and will only intersect twice
			mA.is3D = i%2==0;
			if( i%3==0)
				working.addView(viewI);
		}

		var alg = new ProjectiveExpandByOneView();
		alg.workGraph = working;

		List<Motion> valid = new ArrayList<>();
		alg.createListOfValid(seed,valid);

		assertEquals(2,valid.size());
	}

	/**
	 * Creates a slightly complex situation where the number of elements in common and lists is not the same as sees
	 * if it selects the best connection
	 */
	@Test
	void findBestCommon() {
		var graph = new PairwiseImageGraph2();
		View viewA = graph.createNode("A");
		View viewB = graph.createNode("B");
		graph.connect(viewA, viewB);

		List<Motion> connectionsA = new ArrayList<>();

		for (int i = 0; i < 6; i++) {
			View viewI = graph.createNode(""+i);

			// make sure src/dst is handled correctly
			Motion mA = i%2==0?graph.connect(viewA,viewI):graph.connect(viewI,viewA);
			mA.countF = 100+i;
			mA.countH = 80;
			mA.is3D = true;

			connectionsA.add(mA);

			if( i >= 5 ) // not all the nodes will be connected to A and B
				continue;

			Motion mB = i%2==0?graph.connect(viewB,viewI):graph.connect(viewI,viewB);
			mB.countF = 100-i*2;
			mB.countH = 80;
			mB.is3D = true;
		}

		var alg = new ProjectiveExpandByOneView();
		Motion found = alg.findBestCommon(viewA,viewA.connections.get(0),connectionsA);

		assertSame(found, viewA.connections.get(1));
	}

	/**
	 * Makes sure it checks the is3D flag
	 */
	@Test
	void findBestCommon_is3D() {
		var graph = new PairwiseImageGraph2();
		View viewA = graph.createNode("A");
		View viewB = graph.createNode("B");
		graph.connect(viewA, viewB);

		View viewI = graph.createNode("0");
		Motion mA = graph.connect(viewA,viewI);
		Motion mB = graph.connect(viewB,viewI);

		List<Motion> connectionsA = new ArrayList<>();
		connectionsA.add(mA);

		mA.countF = mB.countF = 100;
		mA.countH = mB.countH = 70;
		mB.is3D = false;

		// Not 3D so it will fail
		var alg = new ProjectiveExpandByOneView();
		assertNull(alg.findBestCommon(viewA,viewA.connections.get(0),connectionsA));
		// Should work now
		mB.is3D = true;
		assertSame(mA,alg.findBestCommon(viewA,viewA.connections.get(0),connectionsA));
	}
}