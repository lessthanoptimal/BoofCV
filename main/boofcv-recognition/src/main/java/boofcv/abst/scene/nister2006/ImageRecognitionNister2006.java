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
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.kmeans.*;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.clustering.FactoryClustering;
import org.ddogleg.clustering.PointDistance;
import org.ddogleg.clustering.kmeans.StandardKMeans;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
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
	@Getter RecognitionVocabularyTreeNister2006<TD> databaseN;

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

	// Internal Profiling. All times in milliseconds
	@Getter long timeLearnDescribeMS;
	@Getter long timeLearnClusterMS;
	@Getter long timeLearnWeightsMS;

	public ImageRecognitionNister2006( ConfigImageRecognitionNister2006 config, ImageType<Image> imageType ) {
		this.config = config;
		this.detector = FactoryDetectDescribe.generic(config.features, imageType.getImageClass());
		this.imageFeatures = new DogArray<>(() -> detector.createDescription());
		this.databaseN = new RecognitionVocabularyTreeNister2006<>();
		this.imageType = imageType;

		databaseN.setDistanceType(config.distanceNorm);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override public void learnDescription( Iterator<Image> images ) {
		int DOF = detector.createDescription().size();
		PackedArray<TD> packedFeatures = (PackedArray)new PackedTupleArray_F64(DOF);

		// Keep track of where features from one image begins/ends
		DogArray_I32 startIndex = new DogArray_I32();

		// Detect features in all the images and save into a single array
		long time0 = System.currentTimeMillis();
		while (images.hasNext()) {
			startIndex.add(packedFeatures.size());
			detector.detect(images.next());
			int N = detector.getNumberOfFeatures();
			packedFeatures.reserve(N);
			for (int i = 0; i < N; i++) {
				packedFeatures.addCopy(detector.getDescription(i));
			}
			if (verbose != null)
				verbose.println("described.size=" + startIndex.size + " packed.size=" + packedFeatures.size());
		}
		startIndex.add(packedFeatures.size());
		if (verbose != null) verbose.println("packedFeatures.size=" + packedFeatures.size());
		long time1 = System.currentTimeMillis();

		// Create the tree data structure
		PointDistance distance = new TuplePointDistanceEuclideanSq.F64();
		tree = new HierarchicalVocabularyTree<>(distance, new PackedTupleArray_F64(DOF),
				RecognitionVocabularyTreeNister2006.LeafData.class);
		tree.branchFactor = config.tree.branchFactor;
		tree.maximumLevel = config.tree.maximumLevel;

		// Learn the tree's structure
		if (verbose != null) verbose.println("learning the tree");

		BoofLambdas.Factory<StandardKMeans<TD>> factoryKMeans = BoofConcurrency.isUseConcurrent() ?
				() -> {
					int minimumForThreads = 500;
					var computeMean = new ComputeMeanTuple_MT_F64(() -> new TupleDesc_F64(DOF));
					computeMean.setMinimumForConcurrent(minimumForThreads);
					return FactoryClustering.kMeans_MT(config.kmeans,minimumForThreads,
							computeMean, distance, () -> new TupleDesc_F64(DOF));
				} :
				() -> FactoryClustering.kMeans(config.kmeans,
						new ComputeMeanTuple_F64(), distance, () -> new TupleDesc_F64(DOF));

		LearnHierarchicalTree<TD> learnTree = new LearnHierarchicalTree<>(
				() -> (PackedArray)new PackedTupleArray_F64(DOF), factoryKMeans, config.randSeed);
		learnTree.setVerbose(verbose, null);
		learnTree.process(packedFeatures, tree);
		long time2 = System.currentTimeMillis();

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
		long time3 = System.currentTimeMillis();

		// Initialize the database
		databaseN.initializeTree(tree);

		// Compute internal profiling
		timeLearnDescribeMS = time1-time0;
		timeLearnClusterMS = time2-time1;
		timeLearnWeightsMS = time3-time2;

		if (verbose!=null)
			verbose.printf("Time (s): describe=%.1f cluster=%.1f weights=%.1f\n",
					timeLearnDescribeMS*1e-3, timeLearnClusterMS*1e-3, timeLearnWeightsMS*1e-3);
	}

	@Override public void addImage( String id, Image image ) {
		// Copy image features into an array
		detector.detect(image);
		imageFeatures.resize(detector.getNumberOfFeatures());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(detector.getDescription(i));
		}

		// Save the ID and convert into a format the database understands
		int imageIndex = imageIds.size();
		imageIds.add(id);

		if (verbose!=null) verbose.println("detected["+imageIndex+"].size="+detector.getNumberOfFeatures()+" id="+id);

		// Add the image
		databaseN.addImage(imageIndex, imageFeatures.toList(), null);
	}

	@Override public ImageRecognition.DataBase getDataBase() {
		return null;
	}

	@Override public boolean findBestMatch( Image queryImage, DogArray<Match> matches ) {
		// Detect image features then copy features into an array
		detector.detect(queryImage);
		imageFeatures.resize(detector.getNumberOfFeatures());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(detector.getDescription(i));
		}

		// Look up the closest matches
		if (!databaseN.lookup(imageFeatures.toList()))
			return false;

		DogArray<RecognitionVocabularyTreeNister2006.Match> found = databaseN.getMatchScores();
		matches.resize(found.size);

		// Copy results into output format
		int count = config.maxMatches <= 0 ? found.size :  Math.min(config.maxMatches, found.size);
		for (int i = 0; i < count; i++) {
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
