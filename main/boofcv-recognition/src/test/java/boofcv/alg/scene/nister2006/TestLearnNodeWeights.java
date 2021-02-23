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

package boofcv.alg.scene.nister2006;

import boofcv.misc.BoofMiscOps;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static boofcv.alg.scene.vocabtree.TestHierarchicalVocabularyTree.createTree;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestLearnNodeWeights extends BoofStandardJUnit {
	// location of leaves, see create2x2tree
	Point2D_F64 leaf0 = new Point2D_F64(-5, -1); // Node 3
	Point2D_F64 leaf1 = new Point2D_F64(-5, 1);// 4
	Point2D_F64 leaf2 = new Point2D_F64(5, -1); // 5
	Point2D_F64 leaf3 = new Point2D_F64(5, 1); // 6

	/**
	 * Create a simple scenario where the correct weights can be easily computed by hand
	 */
	@Test void everythingTogether() {
		var alg = new LearnNodeWeights<Point2D_F64>();
		alg.reset(TestRecognitionVocabularyTreeNister2006.create2x2Tree());

		// Add images which land on specific leaves
		alg.addImage(BoofMiscOps.asList(leaf0, leaf2));
		alg.addImage(BoofMiscOps.asList(leaf0, leaf3));
		alg.addImage(BoofMiscOps.asList(leaf0, leaf2));
		alg.addImage(BoofMiscOps.asList(leaf0, leaf2));
		alg.addImage(BoofMiscOps.asList(leaf0, leaf1));
		alg.addImage(BoofMiscOps.asList(leaf0, leaf0, leaf0, leaf1));

		alg.fixate();

		double totalImages = 6.0;
		assertEquals(Math.log(totalImages/6.0), alg.tree.nodes.get(3).weight, UtilEjml.TEST_F32);
		assertEquals(Math.log(totalImages/2.0), alg.tree.nodes.get(4).weight, UtilEjml.TEST_F32);
		assertEquals(Math.log(totalImages/3.0), alg.tree.nodes.get(5).weight, UtilEjml.TEST_F32);
		assertEquals(Math.log(totalImages/1.0), alg.tree.nodes.get(6).weight, UtilEjml.TEST_F32);
	}

	/**
	 * Simple case where we see the expected results are found
	 */
	@Test void fixate() {
		// Create a minimalist tree for this test
		int N = 6;
		var tree = createTree();
		tree.nodes.resize(N);
		tree.nodes.forIdx(( idx, n ) -> n.index = idx);

		// Initialize data structures then run fixate
		var alg = new LearnNodeWeights<Point2D_F64>();
		alg.reset(tree);
		alg.totalImages = 120;
		alg.numberOfImagesWithNode.setTo(50, 20, 1, 25, 120, 77);
		alg.fixate();

		// root node is skipped
		assertEquals(0.0, tree.nodes.get(0).weight);
		for (int i = 1; i < N; i++) {
			int numUsed = alg.numberOfImagesWithNode.get(i);
			double expected = numUsed == 0 ? 0.0 : Math.log(120.0/numUsed);
			assertEquals(expected, tree.nodes.get(i).weight);
		}
	}
}
