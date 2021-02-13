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

import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.ImageInfo;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.LeafData;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.struct.feature.PackedTupleArray_F64;
import boofcv.struct.feature.TupleDesc_F64;
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
	@Test void hierarchicalVocabularyTree_stream() {
		// Create a dummy tree that's to be saved and reconstructed
		int DOF = 6;
		HierarchicalVocabularyTree<TupleDesc_F64, Object> tree = createTree(DOF, Object.class);

		// Encode then decode
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RecognitionIO.saveTreeBin(tree, stream);

		InputStream input = new ByteArrayInputStream(stream.toByteArray());
		HierarchicalVocabularyTree<TupleDesc_F64, Object> found = RecognitionIO.loadTreeBin(input, null, Object.class);

		// See if all the important components were copied
		compareTrees(tree, found);
	}

	private void compareTrees( HierarchicalVocabularyTree<TupleDesc_F64, ?> tree,
							   HierarchicalVocabularyTree<TupleDesc_F64, ?> found ) {
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
				assertEquals(e.data[j], f.data[j]);
			}
		}
	}

	private <T> HierarchicalVocabularyTree<TupleDesc_F64, T> createTree( int DOF, Class<T> type ) {
		var tree = new HierarchicalVocabularyTree<>(
				new TuplePointDistanceEuclideanSq.F64(), new PackedTupleArray_F64(DOF), type);

		tree.maximumLevel = 2;
		tree.branchFactor = 4;
		tree.nodes.resize(5);
		for (int i = 0; i < tree.nodes.size; i++) {
			HierarchicalVocabularyTree.Node n = tree.nodes.get(i);
			n.childrenIndexes.setTo(1, 2 + i);
			n.parent = i - 1;
			n.branch = i;
			n.weight = 0.1 + i;
			n.dataIdx = i*2;

			var desc = new TupleDesc_F64(DOF);
			for (int j = 0; j < DOF; j++) {
				desc.data[j] = rand.nextDouble();
			}
			n.descIdx = tree.descriptions.size();
			tree.descriptions.addCopy(desc);
		}
		return tree;
	}

	@Test void recognitionVocabularyTreeNister2006_stream() {
		// Create the data structure and fill it in with non default values
		var db = new RecognitionVocabularyTreeNister2006<TupleDesc_F64>();
		db.tree = createTree(6, LeafData.class);

		db.getImagesDB().resize(11);
		for (int i = 0; i < 11; i++) {
			ImageInfo info = db.getImagesDB().get(i);
			info.imageId = i + 2;
			info.descTermFreq.put(1, 1.5f);
			info.descTermFreq.put(6, i + 1.5f);
			// cookie can be ignored and isn't saved
		}

		db.tree.listData.resize(9);
		for (int i = 0; i < db.tree.listData.size; i++) {
			var ld = new LeafData();
			ld.images.put(i + 2, db.getImagesDB().get(i));
			db.tree.listData.set(i,ld);
		}

		// Encode then decode
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RecognitionIO.saveBin(db, stream);

		var found = new RecognitionVocabularyTreeNister2006<TupleDesc_F64>();
		InputStream input = new ByteArrayInputStream(stream.toByteArray());
		RecognitionIO.loadBin(input, found);

		// See if all the important components were copied
		compareTrees(db.tree, found.tree);

		assertEquals(db.getImagesDB().size, found.getImagesDB().size);
		assertEquals(db.tree.listData.size, found.tree.listData.size);

		for (int i = 0; i < 11; i++) {
			ImageInfo e = db.getImagesDB().get(i);
			ImageInfo f = found.getImagesDB().get(i);

			assertEquals(e.imageId, f.imageId);
			assertEquals(e.descTermFreq.size(), f.descTermFreq.size());
			for (int key : e.descTermFreq.keys()) {
				assertEquals(e.descTermFreq.get(key), f.descTermFreq.get(key));
			}
		}

		for (int i = 0; i < db.tree.listData.size; i++) {
			LeafData e = db.tree.listData.get(i);
			LeafData f = found.tree.listData.get(i);

			assertEquals(e.images.size(), f.images.size());
			for (int key : e.images.keys()) {
				int indexE = db.getImagesDB().indexOf(e.images.get(key));
				int indexF = found.getImagesDB().indexOf(f.images.get(key));

				assertEquals(indexE, indexF);
			}
		}
	}
}
