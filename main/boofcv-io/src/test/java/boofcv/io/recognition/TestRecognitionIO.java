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

import boofcv.abst.scene.ConfigFeatureToSceneRecognition;
import boofcv.abst.scene.WrapFeatureToSceneRecognition;
import boofcv.abst.scene.ann.FeatureSceneRecognitionNearestNeighbor;
import boofcv.abst.scene.nister2006.ConfigRecognitionNister2006;
import boofcv.abst.scene.nister2006.FeatureSceneRecognitionNister2006;
import boofcv.alg.scene.ann.RecognitionNearestNeighborInvertedFile;
import boofcv.alg.scene.bow.InvertedFile;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.io.UtilIO;
import boofcv.struct.feature.PackedTupleBigArray_F64;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.kmeans.TuplePointDistanceEuclideanSq;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRecognitionIO extends BoofStandardJUnit {
	/**
	 * Very basic test. Mostly just checks to see if things blow up or not
	 */
	@Test void save_load_FeatureToScene_Nister2006() {
		File dir = new File(System.getProperty("java.io.tmpdir"), "feature_to_scene");
		try {
			var config = new ConfigFeatureToSceneRecognition();
			config.typeRecognize = ConfigFeatureToSceneRecognition.Type.NISTER_2006;
			ImageType<GrayU8> imageType = ImageType.SB_U8;

			var original = FactorySceneRecognition.createFeatureToScene(config, imageType);
			((FeatureSceneRecognitionNister2006<TupleDesc_F64>)original.getRecognizer()).
					setDatabase(createDefaultNister2006());

			RecognitionIO.saveFeatureToScene(original, dir);
			WrapFeatureToSceneRecognition<GrayU8, TupleDesc_F64> found = RecognitionIO.loadFeatureToScene(dir, imageType);

			// Check a some things to make sure it actually loaded
			FeatureSceneRecognitionNister2006<TupleDesc_F64> foundRecognizer = found.getRecognizer();
			assertEquals(20, foundRecognizer.getDatabase().getImagesDB().size);
			assertEquals(5, foundRecognizer.getTree().nodes.size());
		} finally {
			// clean up
			if (dir.exists())
				UtilIO.deleteRecursive(dir);
		}
	}

	/**
	 * Very basic test. Mostly just checks to see if things blow up or not
	 */
	@Test void save_load_FeatureToScene_NearestNeighbor() {
		File dir = new File(System.getProperty("java.io.tmpdir"), "feature_to_scene");
		try {
			var config = new ConfigFeatureToSceneRecognition();
			config.typeRecognize = ConfigFeatureToSceneRecognition.Type.NEAREST_NEIGHBOR;
			ImageType<GrayU8> imageType = ImageType.SB_U8;

			var original = FactorySceneRecognition.createFeatureToScene(config, imageType);
			createNearestNeighbor(original.getRecognizer());

			RecognitionIO.saveFeatureToScene(original, dir);
			WrapFeatureToSceneRecognition<GrayU8, TupleDesc_F64> found =
					RecognitionIO.loadFeatureToScene(dir, imageType);

			// Check a some things to make sure it actually loaded
			FeatureSceneRecognitionNearestNeighbor<TupleDesc_F64> foundRecognizer = found.getRecognizer();
			assertEquals(20, foundRecognizer.getDatabase().getImagesDB().size);
			assertEquals(9, foundRecognizer.getDatabase().getInvertedFiles().size());
		} finally {
			// clean up
			if (dir.exists())
				UtilIO.deleteRecursive(dir);
		}
	}

	/**
	 * Very basic test. Mostly just checks to see if things blow up or not
	 */
	@Test void save_load_nister2006() {
		File dir = new File(System.getProperty("java.io.tmpdir"), "nister2006");
		try {
			var config = new ConfigRecognitionNister2006();

			var original = new FeatureSceneRecognitionNister2006<>(config, () -> new TupleDesc_F64(10));
			original.setDatabase(createDefaultNister2006());

			RecognitionIO.saveNister2006(original, dir);
			var found = new FeatureSceneRecognitionNister2006<>(config, () -> new TupleDesc_F64(10));
			RecognitionIO.loadNister2006(dir, found);

			// Check a some things to make sure it actually loaded
			assertEquals(20, found.getDatabase().getImagesDB().size);
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
			assertEquals(e.userIdx, f.userIdx);
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
				new TuplePointDistanceEuclideanSq.F64(), new PackedTupleBigArray_F64(DOF));

		tree.maximumLevel = 2;
		tree.branchFactor = 4;
		tree.nodes.resize(5);
		for (int i = 0; i < tree.nodes.size; i++) {
			HierarchicalVocabularyTree.Node n = tree.nodes.get(i);
			n.childrenIndexes.setTo(1, 2 + i);
			n.parent = i - 1;
			n.branch = i;
			n.weight = 0.1 + i;
			n.userIdx = i*2;

			// make sure the number of descriptions doesn't match the number of nodes. this was a bug once.
			if (i == 0)
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
		assertEquals(db.invertedFiles.size(), found.invertedFiles.size());

		for (int i = 0; i < db.invertedFiles.size(); i++) {
			InvertedFile e = db.invertedFiles.get(i);
			InvertedFile f = found.invertedFiles.get(i);

			assertEquals(e.size(), f.size());
			assertEquals(e.weights.size(), f.weights.size());
			assertEquals(e.weights.size(), f.size());
			for (int imageIdx = 0; imageIdx < e.size; imageIdx++) {
				int indexE = e.get(imageIdx);
				int indexF = f.get(imageIdx);

				assertEquals(indexE, indexF);

				float weightE = e.weights.get(imageIdx);
				float weightF = f.weights.get(imageIdx);

				assertEquals(weightE, weightF);
			}
		}
	}

	@Test void nearestNeighborBin_stream() {
		var expected = new RecognitionNearestNeighborInvertedFile<>();
		expected.getImagesDB().resize(45);

		for (int i = 0; i < 20; i++) {
			InvertedFile iv = expected.getInvertedFiles().grow();
			int N = rand.nextInt(10);
			for (int j = 0; j < N; j++) {
				iv.addImage(rand.nextInt(1_000_000), rand.nextFloat());
			}
		}

		// Encode then decode
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RecognitionIO.saveNearestNeighborBin(expected, stream);

		var found = new RecognitionNearestNeighborInvertedFile<>();
		InputStream input = new ByteArrayInputStream(stream.toByteArray());
		RecognitionIO.loadNearestNeighborBin(input, found);

		// See if they are identical
		compareInverted(expected.getInvertedFiles(), found.getInvertedFiles());
		assertEquals(expected.getImagesDB().size, found.getImagesDB().size);
	}

	private void compareInverted( FastAccess<InvertedFile> expected, FastAccess<InvertedFile> found ) {
		assertEquals(expected.size(), found.size());
		for (int invIdx = 0; invIdx < expected.size(); invIdx++) {
			assertArrayEquals(expected.get(invIdx).data, found.get(invIdx).data);
			assertArrayEquals(expected.get(invIdx).weights.data, found.get(invIdx).weights.data);
		}
	}

	@Test void dictionaryBin_stream() {
		int DOF = 11;
		List<TupleDesc_F64> expected = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			var d = new TupleDesc_F64(DOF);
			for (int j = 0; j < DOF; j++) {
				d.data[j] = rand.nextDouble();
			}
			expected.add(d);
		}

		// Encode then decode
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		RecognitionIO.saveDictionaryBin(expected, DOF, TupleDesc_F64.class, stream);

		InputStream input = new ByteArrayInputStream(stream.toByteArray());
		List<TupleDesc_F64> found = RecognitionIO.loadDictionaryBin(input);

		// See if they are identical
		assertEquals(expected.size(), found.size());
		for (int i = 0; i < expected.size(); i++) {
			assertArrayEquals(expected.get(i).data, found.get(i).data);
		}
	}

	@NotNull
	private RecognitionVocabularyTreeNister2006<TupleDesc_F64> createDefaultNister2006() {
		var db = new RecognitionVocabularyTreeNister2006<TupleDesc_F64>();
		db.tree = createTree(6);

		db.getImagesDB().resize(20);
		for (int i = 0; i < 20; i++) {
			db.getImagesDB().set(i, rand.nextInt());
		}

		for (int i = 0; i < db.tree.nodes.size; i++) {
			InvertedFile ld = db.invertedFiles.grow();
			ld.add(rand.nextInt(20));
			ld.weights.add(rand.nextFloat());
		}
		return db;
	}

	private <TD extends TupleDesc<TD>>
	void createNearestNeighbor( FeatureSceneRecognitionNearestNeighbor<TD> recNN ) {
		var db = recNN.getDatabase();
		db.getImagesDB().resize(20);
		for (int i = 0; i < 20; i++) {
			db.getImagesDB().set(i, rand.nextInt());
		}

		DogArray<InvertedFile> inverted = db.getInvertedFiles();
		for (int i = 0; i < 9; i++) {
			InvertedFile ld = inverted.grow();
			ld.add(rand.nextInt(20));
			ld.weights.add(rand.nextFloat());
		}
	}
}
