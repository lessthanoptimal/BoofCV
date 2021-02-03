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

package boofcv.io.recognition;

import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.kmeans.PackedTupleArray_F64;
import boofcv.struct.kmeans.TuplePointDistanceEuclideanSq;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 **/
public class TestRecognitionIO extends BoofStandardJUnit {
	@Test void hierarchicalVocabularyTree() {
		// Create a dummy tree that's to be saved and reconstructed
		int DOF = 6;
		HierarchicalVocabularyTree<TupleDesc_F64,?> tree = new HierarchicalVocabularyTree<>(
				new TuplePointDistanceEuclideanSq.F64(), new PackedTupleArray_F64(DOF), Object.class);

		tree.maximumLevel = 2;
		tree.branchFactor = 4;
		tree.nodes.resize(5);
		for (int i = 0; i < tree.nodes.size; i++) {
			HierarchicalVocabularyTree.Node n = tree.nodes.get(i);
			n.childrenIndexes.setTo(1,2+i);
			n.parent = i-1;
			n.branch = i;
			n.weight = 0.1+i;
			n.dataIdx = i*2;

			var desc = new TupleDesc_F64(DOF);
			for (int j = 0; j < DOF; j++) {
				desc.value[j] = rand.nextDouble();
			}
			n.descIdx = tree.descriptions.size();
			tree.descriptions.addCopy(desc);
		}

		// Encode then decode
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RecognitionIO.saveBin(tree, stream);

		HierarchicalVocabularyTree<TupleDesc_F64,?> found = new HierarchicalVocabularyTree<>(
				new TuplePointDistanceEuclideanSq.F64(), new PackedTupleArray_F64(DOF), Object.class);
		InputStream input = new ByteArrayInputStream(stream.toByteArray());
		RecognitionIO.loadBin(input, found);

		// See if all the important components were copied
		assertEquals(tree.maximumLevel, found.maximumLevel);
		assertEquals(tree.branchFactor, found.branchFactor);
		assertEquals(tree.nodes.size, found.nodes.size);
		assertEquals(tree.descriptions.size(), found.descriptions.size());
		for (int i = 0; i < tree.nodes.size; i++) {
			HierarchicalVocabularyTree.Node e = tree.nodes.get(i);
			HierarchicalVocabularyTree.Node f = found.nodes.get(i);

			assertEquals(e.parent, f.parent);
			assertEquals(e.branch, f.branch);
			assertEquals(e.weight, f.weight);
			assertEquals(e.descIdx, f.descIdx);
			assertEquals(e.dataIdx, f.dataIdx);
			assertEquals(e.childrenIndexes.size, f.childrenIndexes.size);
			for (int j = 0; j < e.childrenIndexes.size; j++) {
				assertEquals(e.childrenIndexes.get(j), f.childrenIndexes.get(j));
			}
		}
		for (int i = 0; i < tree.descriptions.size(); i++) {
			TupleDesc_F64 e = tree.descriptions.getTemp(i);
			TupleDesc_F64 f = found.descriptions.getTemp(i);

			assertEquals(e.size(), f.size());
			for (int j = 0; j < e.size(); j++) {
				assertEquals(e.value[j], f.value[j]);
			}
		}
	}
}
