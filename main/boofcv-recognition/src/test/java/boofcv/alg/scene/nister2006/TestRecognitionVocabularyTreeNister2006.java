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

import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.Match;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.scene.vocabtree.TestHierarchicalVocabularyTree.createTree;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ConstantConditions")
class TestRecognitionVocabularyTreeNister2006 extends BoofStandardJUnit {
	/**
	 * Simple tests with random images to see if they can be found without issue
	 */
	@Test void allTogether() {
		HierarchicalVocabularyTree<Point2D_F64> tree = create2x2Tree();
		var alg = new RecognitionVocabularyTreeNister2006<Point2D_F64>();
		alg.initializeTree(tree);

		// Crate a list of "images" and add them to the image DB
		List<List<Point2D_F64>> images = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			images.add(createRandomImage());
			alg.addImage(i, images.get(i));
		}

		for (int i = 0; i < 5; i++) {
			// The best match should be the input image since the exact same image is being passed in
			assertTrue(alg.query(images.get(i), ( a ) -> true, Integer.MAX_VALUE));
			Match best = alg.getMatches().get(0);

			assertEquals(0.0, best.error, UtilEjml.TEST_F32);
			assertEquals(best.identification, i);
		}
	}

	/**
	 * Creates a set of random features that are close to the means in the generated tree
	 */
	List<Point2D_F64> createRandomImage() {
		var ret = new ArrayList<Point2D_F64>();
		for (int i = 0; i < 10; i++) {
			Point2D_F64 p = new Point2D_F64();
			p.x = rand.nextDouble()*12 - 6;
			p.y = rand.nextDouble()*3 - 1.5;
			ret.add(p);
		}
		return ret;
	}

	@Test void describe() {
		HierarchicalVocabularyTree<Point2D_F64> tree = create2x2Tree();

		// Add a few features at the leaves
		List<Point2D_F64> imageFeatures = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			imageFeatures.add(new Point2D_F64(-5, -1));
			imageFeatures.add(new Point2D_F64(5, -1));
		}

		// TF-IDF descriptor
		var descWeights = new DogArray_F32();
		var descWords = new DogArray_I32();

		var alg = new RecognitionVocabularyTreeNister2006<Point2D_F64>();
		alg.initializeTree(tree);
		alg.describe(imageFeatures, descWeights, descWords);

		// See if the description is as expected
		assertEquals(4, descWeights.size());
		assertEquals(4, descWords.size());

		// All the expected nodes should have values
		float f1 = descWeights.get(descWords.indexOf(1));
		float f2 = descWeights.get(descWords.indexOf(2));
		float f3 = descWeights.get(descWords.indexOf(3));
		float f5 = descWeights.get(descWords.indexOf(5));
		assertTrue(f1 > 0 && f2 > 0 && f3 > 0 && f5 > 0);
		assertTrue(f1 < f2);
		assertTrue(f3 < f5);

		// L2-norm should be 1.0
		float norm = (float)Math.sqrt(f1*f1 + f2*f2 + f3*f3 + f5*f5);
		assertEquals(1.0f, norm, UtilEjml.TEST_F32);
	}

	/**
	 * The filter accepts everything
	 */
	@Test void filterAndSortMatches_true() {
		var alg = new RecognitionVocabularyTreeNister2006<Point2D_F64>();

		alg.matches.resize(10);
		alg.matches.forIdx(( idx, m ) -> m.identification = idx);
		alg.matches.forIdx(( idx, m ) -> m.error = 10 - idx);

		// Limit is greater than the number of matches
		// All matches should be left, but the order changed
		alg.filterAndSortMatches(( id ) -> true, 20);
		assertEquals(10, alg.matches.size);
		alg.matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));

		// Limit is less than the number of matches
		alg.matches.forIdx(( idx, m ) -> m.identification = idx);
		alg.matches.forIdx(( idx, m ) -> m.error = 10 - idx);
		alg.filterAndSortMatches(( id ) -> true, 4);
		assertEquals(4, alg.matches.size);
		alg.matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));
	}

	/**
	 * The filter will reject even numbers
	 */
	@Test void filterAndSortMatches_Filtered() {
		var alg = new RecognitionVocabularyTreeNister2006<Point2D_F64>();

		// Limit is greater than the number of matches, before filtering
		// The filter will remove all odd ID and return half
		alg.matches.resize(50);
		alg.matches.forIdx(( idx, m ) -> m.identification = idx);
		alg.matches.forIdx(( idx, m ) -> m.error = 50 - idx);
		alg.filterAndSortMatches(( id ) -> id%2==0, 100);
		assertEquals(25, alg.matches.size);
		alg.matches.forIdx(( idx, m ) -> assertEquals(48 - idx*2, m.identification));

		// Limit is greater than the number of matches, after filtering
		alg.matches.resize(50);
		alg.matches.forIdx(( idx, m ) -> m.identification = idx);
		alg.matches.forIdx(( idx, m ) -> m.error = 50 - idx);
		alg.filterAndSortMatches(( id ) -> id%2==0, 27);
		assertEquals(25, alg.matches.size);
		alg.matches.forIdx(( idx, m ) -> assertEquals(48 - idx*2, m.identification));

		// Limit is less than the number of matches, after filtering
		for (int limit = 5; limit < 20; limit++) {
			alg.matches.resize(50);
			alg.matches.forIdx(( idx, m ) -> m.identification = idx);
			alg.matches.forIdx(( idx, m ) -> m.error = 50 - idx);
			alg.filterAndSortMatches(( id ) -> id%2==0, limit);
			assertEquals(limit, alg.matches.size);
			alg.matches.forIdx(( idx, m ) -> assertEquals(48 - idx*2, m.identification));
		}
	}

	/**
	 * Null is passed in as the filter
	 */
	@Test void filterAndSortMatches_noFilter() {
		var alg = new RecognitionVocabularyTreeNister2006<Point2D_F64>();

		alg.matches.resize(10);
		alg.matches.forIdx(( idx, m ) -> m.identification = idx);
		alg.matches.forIdx(( idx, m ) -> m.error = 10 - idx);

		// Limit is greater than the number of matches
		// All matches should be left, but the order changed
		alg.filterAndSortMatches(null, 20);
		assertEquals(10, alg.matches.size);
		alg.matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));

		// Limit is less than the number of matches
		alg.matches.forIdx(( idx, m ) -> m.identification = idx);
		alg.matches.forIdx(( idx, m ) -> m.error = 10 - idx);
		alg.filterAndSortMatches(null, 4);
		assertEquals(4, alg.matches.size);
		alg.matches.forIdx(( idx, m ) -> assertEquals(9 - idx, m.identification));
	}

	public static HierarchicalVocabularyTree<Point2D_F64> create2x2Tree() {
		HierarchicalVocabularyTree<Point2D_F64> tree = createTree();
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
}
