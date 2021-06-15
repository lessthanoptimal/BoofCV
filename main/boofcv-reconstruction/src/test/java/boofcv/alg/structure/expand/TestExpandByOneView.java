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

package boofcv.alg.structure.expand;

import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.alg.structure.PairwiseImageGraph.Motion;
import boofcv.alg.structure.PairwiseImageGraph.View;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestExpandByOneView extends BoofStandardJUnit {

	/**
	 * Creates a more complex scenario and sees if it selects the correct two views
	 */
	@Test
	void selectTwoConnections() {
		var working = new SceneWorkingGraph();
		var graph = new PairwiseImageGraph();
		SceneWorkingGraph.Camera camera = working.addCamera(2);
		View seed = graph.createNode("A");

		for (int i = 0; i < 6; i++) {
			View viewI = graph.createNode("" + i);

			// make sure src/dst is handled correctly
			Motion mA = i%2 == 0 ? graph.connect(seed, viewI) : graph.connect(viewI, seed);
			// All have the same score so that the connections between the other two is what decides the winner
			mA.score3D = 2.0;
			mA.is3D = true;

			working.addView(viewI, camera);
		}

		View viewB = seed.connection(1);
		View viewC = seed.connection(2);
		View viewD = seed.connection(3);

		Motion connBC = graph.connect(viewB, viewC);
		Motion connBD = graph.connect(viewD, viewB);

		connBD.is3D = connBC.is3D = true;
		connBC.score3D = 1.2;
		connBD.score3D = 1.3; // this is the more desirable connection

		var alg = new ChildProjectiveExpandByOneView();
		alg.workGraph = working;

		var found = new ArrayList<Motion>();
		assertTrue(alg.selectTwoConnections(seed, found));

		assertTrue(found.contains(seed.connections.get(1)));
		assertTrue(found.contains(seed.connections.get(3)));
	}

	@Test
	void createListOfValid() {
		var working = new SceneWorkingGraph();
		var graph = new PairwiseImageGraph();
		SceneWorkingGraph.Camera camera = working.addCamera(2);
		View seed = graph.createNode("A");

		for (int i = 0; i < 10; i++) {
			View viewI = graph.createNode("" + i);
			Motion mA = graph.connect(seed, viewI);

			// is3D and being known are at different frequencies and will only intersect twice
			mA.is3D = i%2 == 0;
			if (i%3 == 0)
				working.addView(viewI, camera);
		}

		var alg = new ChildProjectiveExpandByOneView();
		alg.workGraph = working;

		List<Motion> valid = new ArrayList<>();
		alg.createListOfValid(seed, valid);

		assertEquals(2, valid.size());
	}

	private static class ChildProjectiveExpandByOneView extends ExpandByOneView {}
}
