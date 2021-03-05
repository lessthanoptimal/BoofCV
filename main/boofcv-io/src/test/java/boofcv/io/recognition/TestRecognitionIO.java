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

import boofcv.abst.scene.nister2006.ConfigImageRecognitionNister2006;
import boofcv.abst.scene.nister2006.ImageRecognitionNister2006;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.ImageInfo;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.InvertedFile;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.io.UtilIO;
import boofcv.struct.feature.PackedTupleArray_F64;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.kmeans.TuplePointDistanceEuclideanSq;
import boofcv.testing.BoofStandardJUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 **/
public class TestRecognitionIO extends BoofStandardJUnit {
	/**
	 * Very basic test. Mostly just checks to see if things blow up or not
	 */
	@Test void save_load_nister2006() {
		File dir = new File(System.getProperty("java.io.tmpdir"),"nister2006");
		try {
			var config = new ConfigImageRecognitionNister2006();
			ImageType<GrayU8> imageType = ImageType.SB_U8;

			var original = new ImageRecognitionNister2006<GrayU8,TupleDesc_F64>(config, imageType);
			original.setDatabase(createDefaultNister2006());

			RecognitionIO.saveNister2006(original, dir);
			ImageRecognitionNister2006<GrayU8,TupleDesc_F64> found = RecognitionIO.loadNister2006(dir, imageType);

			// Check a some things to make sure it actually loaded
			assertEquals(11, found.getDatabaseN().getImagesDB().size());
			assertEquals(5, found.getTree().nodes.size());
		} finally {
			// clean up
			if (dir.exists())
				UtilIO.deleteRecursive(dir);
		}
	}

	@Test void hierarchicalVocabularyTree_stream() {
		// Create a dummy tree that's to be saved and reconstructed
		int DOF = 6;
		HierarchicalVocabularyTree<TupleDesc_F64> tree = createTree(DOF);

		// Encode then decode
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RecognitionIO.saveTreeBin(tree, stream);

		InputStream input = new ByteArrayInputStream(stream.toByteArray());
		HierarchicalVocabularyTree<TupleDesc_F64> found = RecognitionIO.loadTreeBin(input, null);

		// See if all the important components were copied
		compareTrees(tree, found);
	}

	private void compareTrees( HierarchicalVocabularyTree<TupleDesc_F64> tree,
							   HierarchicalVocabularyTree<TupleDesc_F64> found ) {
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

	private <T> HierarchicalVocabularyTree<TupleDesc_F64> createTree( int DOF ) {
		var tree = new HierarchicalVocabularyTree<>(
				new TuplePointDistanceEuclideanSq.F64(), new PackedTupleArray_F64(DOF));

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

			// make sure the number of descriptions doesn't match the number of nodes. this was a bug once.
			if (i==0)
				continue;

			var desc = new TupleDesc_F64(DOF);
			for (int j = 0; j < DOF; j++) {
				desc.data[j] = rand.nextDouble();
			}
			n.descIdx = tree.descriptions.size();
			tree.descriptions.append(desc);
		}
		return tree;
	}

	@Test void recognitionVocabularyTreeNister2006_stream() {
		// Create the data structure and fill it in with non default values
		RecognitionVocabularyTreeNister2006<TupleDesc_F64> db = createDefaultNister2006();

		// Encode then decode
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RecognitionIO.saveBin(db, stream);

		var found = new RecognitionVocabularyTreeNister2006<TupleDesc_F64>();
		InputStream input = new ByteArrayInputStream(stream.toByteArray());
		RecognitionIO.loadBin(input, found);

		// See if all the important components were copied
		compareTrees(db.tree, found.tree);

		assertEquals(db.getImagesDB().size, found.getImagesDB().size);
		assertEquals(db.tree.nodeData.size(), found.tree.nodeData.size());

		for (int i = 0; i < 11; i++) {
			ImageInfo e = db.getImagesDB().get(i);
			ImageInfo f = found.getImagesDB().get(i);

			assertEquals(e.identification, f.identification);
			assertEquals(e.descTermFreq.size(), f.descTermFreq.size());
			for (int key : e.descTermFreq.keys()) {
				assertEquals(e.descTermFreq.get(key), f.descTermFreq.get(key));
			}
		}

		for (int i = 0; i < db.tree.nodeData.size(); i++) {
			InvertedFile e = (InvertedFile)db.tree.nodeData.get(i);
			InvertedFile f = (InvertedFile)found.tree.nodeData.get(i);

			assertEquals(e.images.size(), f.images.size());
			for (int wordIdx = 0; wordIdx < e.images.size; wordIdx++) {
				int indexE = db.getImagesDB().indexOf(e.images.get(wordIdx).image);
				int indexF = found.getImagesDB().indexOf(f.images.get(wordIdx).image);

				assertEquals(indexE, indexF);
			}
		}
	}

	@NotNull
	private RecognitionVocabularyTreeNister2006<TupleDesc_F64> createDefaultNister2006() {
		var db = new RecognitionVocabularyTreeNister2006<TupleDesc_F64>();
		db.tree = createTree(6);

		db.getImagesDB().resize(11);
		for (int i = 0; i < 11; i++) {
			ImageInfo info = db.getImagesDB().get(i);
			info.identification = i + 2;
			info.descTermFreq.put(1, 1.5f);
			info.descTermFreq.put(6, i + 1.5f);
			// cookie can be ignored and isn't saved
		}

		for (int i = 0; i < 9; i++) {
			var ld = new InvertedFile();
			RecognitionVocabularyTreeNister2006.ImageWord word = ld.images.grow();
			for (int levels = 1; levels < db.tree.maximumLevel; levels++) {
				word.weights.add(i*0.01f+0.4f);
			}
			word.image = db.getImagesDB().get(i);
			db.tree.nodeData.add(ld);
		}
		return db;
	}
}
