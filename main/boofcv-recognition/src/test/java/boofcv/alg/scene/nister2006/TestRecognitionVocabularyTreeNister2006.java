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

import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.LeafData;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.Match;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.scene.vocabtree.TestHierarchicalVocabularyTree.createTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRecognitionVocabularyTreeNister2006 extends BoofStandardJUnit {
	/**
	 * Simple tests with random images to see if they can be found without issue
	 */
	@Test void allTogether() {
		HierarchicalVocabularyTree<Point2D_F64, LeafData> tree = create2x2Tree();
		var alg = new RecognitionVocabularyTreeNister2006<Point2D_F64>();
		alg.initializeTree(tree);

		// Crate a list of "images" and add them to the image DB
		List<List<Point2D_F64>> images = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			images.add(createRandomImage());
			alg.addImage(i,images.get(i),i);
		}

		for (int i = 0; i < 5; i++) {
			// The best match should be the input image since the exact same image is being passed in
			Match best = alg.lookup(images.get(i));
			assertEquals(0.0, best.error, UtilEjml.TEST_F32);
			assertEquals(best.image.imageId, i);
			// check to see if the cookie was correctly assigned
			assertEquals(i, (Integer)best.image.cookie);
		}
	}

	/**
	 * Creates a set of random features that are close to the means in the generated tree
	 */
	List<Point2D_F64> createRandomImage() {
		List<Point2D_F64> ret = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			Point2D_F64 p = new Point2D_F64();
			p.x = rand.nextDouble()*12-6;
			p.y = rand.nextDouble()*3-1.5;
			ret.add(p);
		}
		return ret;
	}

	@Test void describe() {
		HierarchicalVocabularyTree<Point2D_F64, LeafData> tree = create2x2Tree();

		// Add a few features at the leaves
		List<Point2D_F64> imageFeatures = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			imageFeatures.add(new Point2D_F64(-5, -1));
			imageFeatures.add(new Point2D_F64(5, -1));
		}

		var descTermFreq = new TIntFloatHashMap();

		var alg = new RecognitionVocabularyTreeNister2006<Point2D_F64>();
		alg.initializeTree(tree);
		alg.describe(imageFeatures, descTermFreq, ( n ) -> assertTrue(n.id == 3 || n.id == 5));

		// See if the description is as expected
		assertEquals(4, descTermFreq.size());

		// All the expected nodes should have values
		float f1 = descTermFreq.get(1);
		float f2 = descTermFreq.get(2);
		float f3 = descTermFreq.get(3);
		float f5 = descTermFreq.get(5);
		assertTrue(f1 > 0 && f2 > 0 && f3 > 0 && f5 > 0);
		assertTrue(f1<f2);
		assertTrue(f3<f5);

		// L2-norm should be 1.0
		float norm = (float)Math.sqrt(f1*f1 + f2*f2 + f3*f3 + f5*f5);
		assertEquals(1.0f, norm, UtilEjml.TEST_F32);
	}

	public static HierarchicalVocabularyTree<Point2D_F64, LeafData> create2x2Tree() {
		HierarchicalVocabularyTree<Point2D_F64, LeafData> tree = createTree(LeafData.class);
		tree.branchFactor = 2;
		tree.maximumLevel = 2;

		// Create a graph
		assertEquals(1, tree.addNode(0, 0, new Point2D_F64(-5, 0)));
		assertEquals(2, tree.addNode(0, 1, new Point2D_F64(5, 0)));
		assertEquals(3, tree.addNode(1, 0, new Point2D_F64(-5, -1)));
		assertEquals(4, tree.addNode(1, 1, new Point2D_F64(-5, 1)));
		assertEquals(5, tree.addNode(2, 0, new Point2D_F64(5, -1)));
		assertEquals(6, tree.addNode(2, 1, new Point2D_F64(5, 1)));

		for (int i = 0; i < tree.nodes.size; i++) {
			tree.nodes.get(i).weight = i*0.1;
		}
		return tree;
	}

	@Test void distanceL2Norm() {
		TIntFloatMap va = sparseVector(0, 0, 2, 4, 0, 0, 0, 9, 0, 2, 3);
		TIntFloatMap vb = sparseVector(1, 0, 0, 1, 4, 0, 2, 6, 1, 3, 0);

		float expected = 0.0f;
		for (int i = 0; i < 11; i++) {
			float a = va.containsKey(i) ? va.get(i) : 0.0f;
			float b = vb.containsKey(i) ? vb.get(i) : 0.0f;
			expected += (a - b)*(a - b);
		}
		expected = (float)Math.sqrt(expected);

		var alg = new RecognitionVocabularyTreeNister2006<>();
		float found = alg.distanceL2Norm(va, vb);

		assertEquals(expected, found, UtilEjml.TEST_F32);
	}

	private TIntFloatMap sparseVector( float... values ) {
		float total = 0.0f;
		for (int i = 0; i < values.length; i++) {
			total += values[i]*values[i];
		}
		total = (float)Math.sqrt(total);

		var ret = new TIntFloatHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
		for (int i = 0; i < values.length; i++) {
			if (values[i] == 0.0f)
				continue;
			ret.put(i, values[i]/total);
		}

		return ret;
	}
}