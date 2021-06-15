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

import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestGeneratePairwiseImageGraph extends BoofStandardJUnit {
	/**
	 * See if it gracefully handles 0 to 1 images
	 */
	@Test void process_0_to_1() {
		GeneratePairwiseImageGraph alg = FactorySceneReconstruction.generatePairwise(null);

		for (int numViews = 0; numViews < 2; numViews++) {
			var dbSimilar = new MockLookupSimilarImages(numViews, 123123);
			var dbCams = new MockLookUpCameraInfo(400,300);
			alg.process(dbSimilar, dbCams);

			PairwiseImageGraph graph = alg.getGraph();
			assertEquals(numViews, graph.nodes.size);
			assertEquals(0, graph.edges.size);
		}
	}

	/**
	 * A fully connected scene with 3D structure
	 */
	@Test void process_connected() {
		GeneratePairwiseImageGraph alg = FactorySceneReconstruction.generatePairwise(null);

		var dbSimilar = new MockLookupSimilarImages(4, 123123);
		var dbCams = new MockLookUpCameraInfo(400,300);
		alg.process(dbSimilar, dbCams);

		PairwiseImageGraph graph = alg.getGraph();
		assertEquals(4, graph.nodes.size);
		assertEquals(4, graph.mapNodes.size());
		for (int i = 0; i < graph.nodes.size; i++) {
			PairwiseImageGraph.View v = graph.nodes.get(i);
			assertTrue(v.totalObservations > 50);
			assertEquals(3, v.connections.size);
			assertNotNull(graph.mapNodes.get(v.id));
		}
		assertEquals(6, graph.edges.size);

		for (int i = 0; i < graph.edges.size; i++) {
			PairwiseImageGraph.Motion a = alg.graph.edges.get(i);
			assertTrue(a.is3D);

			// each edge pair should be unique
			for (int j = i + 1; j < graph.edges.size; j++) {
				PairwiseImageGraph.Motion b = alg.graph.edges.get(j);

				if (a.src.id.equals(b.src.id) && a.dst.id.equals(b.dst.id))
					fail("duplicate1! " + a.src.id + " " + a.dst.id);
				if (a.dst.id.equals(b.src.id) && a.src.id.equals(b.dst.id))
					fail("duplicate2! " + a.src.id + " " + a.dst.id);
			}
		}
	}
}
