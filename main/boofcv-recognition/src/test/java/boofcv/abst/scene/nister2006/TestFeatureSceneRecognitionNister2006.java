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

package boofcv.abst.scene.nister2006;

import boofcv.abst.scene.GenericFeatureSceneRecognitionChecks;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.struct.feature.PackedTupleArray_F32;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.kmeans.TuplePointDistanceEuclideanSq;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestFeatureSceneRecognitionNister2006 extends GenericFeatureSceneRecognitionChecks<TupleDesc_F32> {

	@Test void lookupWordsFromLeafID() {
		FeatureSceneRecognitionNister2006<TupleDesc_F32> alg = createAlg();
		alg.database.tree = new HierarchicalVocabularyTree<>(
				new TuplePointDistanceEuclideanSq.F32(), new PackedTupleArray_F32(5));
		alg.database.tree.nodes.resize(10);

		for (int depth = 1; depth <= 5; depth++) {
			HierarchicalVocabularyTree.Node n = alg.database.tree.nodes.get(depth);
			n.parent = depth-1;
			n.index = depth;
		}
		alg.database.tree.nodes.get(0).parent = -1;

		var words = new DogArray_I32();
		alg.lookupWordsFromLeafID(5,words);
		assertEquals(5, words.size);
		assertTrue(words.isEquals(5,4,3,2,1));

		alg.lookupWordsFromLeafID(2,words);
		assertEquals(2, words.size);
		assertTrue(words.isEquals(2,1));

		alg.lookupWordsFromLeafID(1,words);
		assertEquals(1, words.size);
		assertTrue(words.isEquals(1));
	}

	/**
	 * Manually construct a very simple tree and see if it is travered up correctly
	 */
	@Test void traverseUpGetID() {
		FeatureSceneRecognitionNister2006<TupleDesc_F32> alg = createAlg();
		alg.database.tree = new HierarchicalVocabularyTree<>(
				new TuplePointDistanceEuclideanSq.F32(), new PackedTupleArray_F32(5));
		alg.database.tree.nodes.resize(10);

		for (int depth = 1; depth <= 5; depth++) {
			HierarchicalVocabularyTree.Node n = alg.database.tree.nodes.get(depth);
			n.parent = depth-1;
			n.index = depth;
		}
		alg.database.tree.nodes.get(0).parent = -1;

		assertEquals(1, alg.traverseUpGetID(5,100));
		assertEquals(5, alg.traverseUpGetID(5,0));
		assertEquals(3, alg.traverseUpGetID(5,2));
		assertEquals(1, alg.traverseUpGetID(1,2));
	}

	@Override public FeatureSceneRecognitionNister2006<TupleDesc_F32> createAlg() {
		return FactorySceneRecognition.createSceneNister2006(null, ()->new TupleDesc_F32(64));
	}

	@Override public TupleDesc_F32 createDescriptor( int seed ) {
		var desc = new TupleDesc_F32(64);
		for (int i = 0; i < 64; i++) {
			desc.data[i] = seed+i;
		}
		return desc;
	}
}
