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

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.scene.ImageRecognition;
import boofcv.alg.scene.nister2006.LearnNodeWeights;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006.LeafData;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.alg.scene.vocabtree.LearnHierarchicalTree;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.kmeans.ComputeMeanTuple_F64;
import boofcv.struct.kmeans.PackedArray;
import boofcv.struct.kmeans.PackedTupleArray_F64;
import boofcv.struct.kmeans.TuplePointDistanceEuclideanSq;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.clustering.FactoryClustering;
import org.ddogleg.clustering.PointDistance;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * High level implementation of {@link RecognitionVocabularyTreeNister2006} for {@link ImageRecognition}.
 *
 * @author Peter Abeles
 */
public class ImageRecognitionNister2006<Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
		implements ImageRecognition<Image> {

	/** Configuration for this class */
	@Getter ConfigImageRecognitionNister2006 config;
	/** Tree representation of image features */
	@Getter HierarchicalVocabularyTree<TD, LeafData> tree;

	/** Manages saving and locating images */
	@Getter RecognitionVocabularyTreeNister2006<TD> database;

	/** Detects image features */
	@Getter @Setter DetectDescribePoint<Image, TD> detector;
	/** Stores features found in one image */
	@Getter @Setter DogArray<TD> imageFeatures;

	/** List of all the images in the dataset */
	@Getter List<String> imageIds = new ArrayList<>();

	// Type of input image
	ImageType<Image> imageType;

	// If not null then print verbose information
	PrintStream verbose;

	public ImageRecognitionNister2006( ConfigImageRecognitionNister2006 config, ImageType<Image> imageType ) {
		this.config = config;
		this.detector = FactoryDetectDescribe.generic(config.features, imageType.getImageClass());
		this.imageFeatures = new DogArray<>(() -> detector.createDescription());
		this.database = new RecognitionVocabularyTreeNister2006<>();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override public void learnDescription( Iterator<Image> images ) {
		int DOF = detector.createDescription().size();
		PackedArray<TD> packedFeatures = (PackedArray)new PackedTupleArray_F64(DOF);

		// Keep track of where features from one image begins/ends
		DogArray_I32 startIndex = new DogArray_I32();

		// Detect features in all the images and save into a single array
		while (images.hasNext()) {
			startIndex.add(packedFeatures.size());
			detector.detect(images.next());
			int N = detector.getNumberOfFeatures();
			packedFeatures.reserve(N);
			for (int i = 0; i < N; i++) {
				packedFeatures.addCopy(detector.getDescription(i));
			}
			if (verbose != null)
				verbose.println("described.size=" + startIndex.size + " packed.size="+packedFeatures.size());
		}
		startIndex.add(packedFeatures.size());
		if (verbose != null) verbose.println("packedFeatures.size=" + packedFeatures.size());

		// Create the tree data structure
		PointDistance distance = new TuplePointDistanceEuclideanSq.F64();
		tree = new HierarchicalVocabularyTree<>(distance, new PackedTupleArray_F64(DOF),
				RecognitionVocabularyTreeNister2006.LeafData.class);
		tree.branchFactor = config.tree.branchFactor;
		tree.maximumLevel = config.tree.maximumLevel;

		// Learn the tree's structure
		if (verbose != null) verbose.println("learning the tree");
		LearnHierarchicalTree<TD> learnTree = new LearnHierarchicalTree<>(
				() -> (PackedArray)new PackedTupleArray_F64(DOF),
				() -> FactoryClustering.kMeans(config.kmeans,
						new ComputeMeanTuple_F64(), distance, () -> new TupleDesc_F64(DOF)),
				config.randSeed);
		learnTree.setVerbose(verbose, null);
		learnTree.process(packedFeatures, tree);

		if (verbose != null) {
			verbose.println(" Tree {bf=" + tree.branchFactor + " ml=" + tree.maximumLevel +
					" nodes.size=" + tree.nodes.size + "}");
		}

		// Learn the weight for each node in the tree
		if (verbose != null) verbose.println("learning the weights");
		LearnNodeWeights<TD> learnWeights = new LearnNodeWeights<>();
		learnWeights.reset(tree);
		for (int imgIdx = 1; imgIdx < startIndex.size; imgIdx++) {
			imageFeatures.reset();
			int idx0 = startIndex.get(imgIdx - 1);
			int idx1 = startIndex.get(imgIdx);
			for (int i = idx0; i < idx1; i++) {
				imageFeatures.grow().setTo(packedFeatures.getTemp(i));
			}
			learnWeights.addImage(imageFeatures.toList());
		}
		learnWeights.fixate();

		// Initialize the database
		database.initializeTree(tree);
	}

	@Override public void saveDescription( Writer writer ) {

	}

	@Override public void loadDescription( Reader reader ) {

	}

	@Override public void addDataBase( String id, Image image ) {
		// Copy image features into an array
		detector.detect(image);
		imageFeatures.resize(detector.getNumberOfFeatures());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(detector.getDescription(i));
		}

		// Save the ID and convert into a format the database understands
		int imageIndex = imageIds.size();
		imageIds.add(id);

		// Add the image
		database.addImage(imageIndex, imageFeatures.toList(), null);
	}

	@Override public void saveDataBase( Writer writer ) {

	}

	@Override public void loadDataBase( Reader reader ) {

	}

	@Override public boolean findBestMatch( Image queryImage, DogArray<Match> matches ) {
		// Detect image features then copy features into an array
		detector.detect(queryImage);
		imageFeatures.resize(detector.getNumberOfFeatures());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(detector.getDescription(i));
		}

		// Look up the closest matches
		database.lookup(imageFeatures.toList());
		DogArray<RecognitionVocabularyTreeNister2006.Match> found = database.getMatchScores();
		matches.resize(found.size);

		// Copy results into output format
		for (int i = 0; i < found.size; i++) {
			RecognitionVocabularyTreeNister2006.Match f = found.get(i);
			matches.get(i).id = imageIds.get(f.image.imageId);
			matches.get(i).error = f.error;
		}

		return !matches.isEmpty();
	}

	@Override public ImageType<Image> getImageType() {
		return imageType;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = out;
	}
}
