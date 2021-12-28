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

import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.alg.scene.bow.BowMatch;
import boofcv.alg.scene.nister2006.LearnNodeWeights;
import boofcv.alg.scene.nister2006.RecognitionVocabularyTreeNister2006;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.alg.scene.vocabtree.LearnHierarchicalTree;
import boofcv.factory.struct.FactoryTupleDesc;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.PackedArray;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.kmeans.FactoryTupleCluster;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.clustering.kmeans.StandardKMeans;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.Factory;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * High level implementation of {@link RecognitionVocabularyTreeNister2006} for {@link FeatureSceneRecognition}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class FeatureSceneRecognitionNister2006<TD extends TupleDesc<TD>> implements FeatureSceneRecognition<TD> {
	/** Configuration for this class */
	@Getter ConfigRecognitionNister2006 config;

	/** Tree representation of image features */
	@Getter HierarchicalVocabularyTree<TD> tree;

	/** Manages saving and locating images */
	@Getter RecognitionVocabularyTreeNister2006<TD> database;

	/** Stores features found in one image */
	@Getter @Setter DogArray<TD> imageFeatures;

	/** List of all the images in the dataset */
	@Getter List<String> imageIds = new ArrayList<>();

	/** Performance tuning. If less than this number of features a single thread algorithm will be used */
	@Getter @Setter public int minimumForThread = 500; // This value has not been proven to be optimal

	// Describes how to store the feature descriptor
	Class<TD> tupleType;
	int tupleDOF;

	// If not null then print verbose information
	@Nullable PrintStream verbose;

	// Internal Profiling. All times in milliseconds
	@Getter long timeLearnDescribeMS;
	@Getter long timeLearnClusterMS;
	@Getter long timeLearnWeightsMS;

	public FeatureSceneRecognitionNister2006( ConfigRecognitionNister2006 config, Factory<TD> factory ) {
		this.config = config;
		this.imageFeatures = new DogArray<>(factory);
		this.database = new RecognitionVocabularyTreeNister2006<>();

		database.setDistanceType(config.distanceNorm);
		database.minimumDepthFromRoot = config.minimumDepthFromRoot;
		database.maximumQueryImagesInNode.setTo(config.queryMaximumImagesInNode);

		tupleDOF = imageFeatures.grow().size();
		tupleType = (Class)imageFeatures.get(0).getClass();
	}

	public void setDatabase( RecognitionVocabularyTreeNister2006<TD> db ) {
		database = db;
		tree = db.getTree();
	}

	@Override public void learnModel( Iterator<Features<TD>> images ) {
		PackedArray<TD> packedFeatures = FactoryTupleDesc.createPackedBig(tupleDOF, tupleType);

		// Keep track of where features from one image begins/ends
		DogArray_I32 startIndex = new DogArray_I32();

		// Detect features in all the images and save into a single array
		long time0 = System.currentTimeMillis();
		while (images.hasNext()) {
			Features<TD> image = images.next();
			startIndex.add(packedFeatures.size());
			int N = image.size();
			packedFeatures.reserve(N);
			for (int i = 0; i < N; i++) {
				packedFeatures.append(image.getDescription(i));
			}
			if (verbose != null)
				verbose.println("described.size=" + startIndex.size + " features=" + N + " packed.size=" + packedFeatures.size());
		}
		startIndex.add(packedFeatures.size());
		if (verbose != null) verbose.println("packedFeatures.size=" + packedFeatures.size());
		long time1 = System.currentTimeMillis();

		// Create the tree data structure
		PackedArray<TD> packedArray = FactoryTupleDesc.createPackedBig(tupleDOF, tupleType);

		tree = new HierarchicalVocabularyTree<>(FactoryTupleCluster.createDistance(tupleType), packedArray);
		tree.branchFactor = config.tree.branchFactor;
		tree.maximumLevel = config.tree.maximumLevel;

		// Learn the tree's structure
		if (verbose != null) verbose.println("learning the tree");

		BoofLambdas.Factory<StandardKMeans<TD>> factoryKMeans = () ->
				FactoryTupleCluster.kmeans(config.kmeans, minimumForThread, tupleDOF, tupleType);

		LearnHierarchicalTree<TD> learnTree = new LearnHierarchicalTree<>(
				() -> FactoryTupleDesc.createPackedBig(tupleDOF, tupleType), factoryKMeans, config.randSeed);
		learnTree.minimumPointsForChildren.setTo(config.learningMinimumPointsForChildren);
		if (verbose != null)
			BoofMiscOps.verboseChildren(verbose, null, learnTree);
		learnTree.process(packedFeatures, tree);
		long time2 = System.currentTimeMillis();

		if (verbose != null) {
			verbose.println("Tree {bf=" + tree.branchFactor + " ml=" + tree.maximumLevel +
					" nodes.size=" + tree.nodes.size + "}");
		}

		// Learn the weight for each node in the tree
		if (verbose != null) verbose.println("learning the weights");
		if (config.learnNodeWeights) {
			LearnNodeWeights<TD> learnWeights = new LearnNodeWeights<>();
			learnWeights.maximumNumberImagesInNode.setTo(config.learningMaximumImagesInNode);
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
		} else {
			// Set all weights to zero, except for the root which has to contain everything
			tree.nodes.forEach(n -> n.weight = 1.0);
			tree.nodes.get(0).weight = 0.0;
		}
		long time3 = System.currentTimeMillis();

		// Initialize the database
		database.initializeTree(tree);

		// Compute internal profiling
		timeLearnDescribeMS = time1 - time0;
		timeLearnClusterMS = time2 - time1;
		timeLearnWeightsMS = time3 - time2;

		if (verbose != null)
			verbose.printf("Time (s): describe=%.1f cluster=%.1f weights=%.1f\n",
					timeLearnDescribeMS*1e-3, timeLearnClusterMS*1e-3, timeLearnWeightsMS*1e-3);
	}

	@Override public void clearDatabase() {
		imageIds.clear();
		database.clearImages();
	}

	@Override public void addImage( String id, Features<TD> features ) {
		// Copy image features into an array
		imageFeatures.resize(features.size());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(features.getDescription(i));
		}

		// Save the ID and convert into a format the database understands
		int imageIndex = imageIds.size();
		imageIds.add(id);

		if (verbose != null)
			verbose.println("added[" + imageIndex + "].size=" + features.size() + " id=" + id);

		// Add the image
		database.addImage(imageIndex, imageFeatures.toList());
	}

	@Override public List<String> getImageIds( @Nullable List<String> storage ) {
		if (storage == null)
			storage = new ArrayList<>();
		else
			storage.clear();

		storage.addAll(imageIds);
		return storage;
	}

	@Override public boolean query( Features<TD> query, @Nullable BoofLambdas.Filter<String> filter,
									int limit, DogArray<SceneRecognition.Match> matches ) {
		// Default is no matches
		matches.resize(0);

		// Handle the case where the limit is unlimited
		limit = limit <= 0 ? Integer.MAX_VALUE : limit;

		// Detect image features then copy features into an array
		imageFeatures.resize(query.size());
		for (int i = 0; i < imageFeatures.size; i++) {
			imageFeatures.get(i).setTo(query.getDescription(i));
		}

		// Wrap the user provided filter by converting the int ID into a String ID
		BoofLambdas.FilterInt filterInt = filter == null ? null : ( index ) -> filter.keep(imageIds.get(index));

		// Look up the closest matches
		if (!database.query(imageFeatures.toList(), filterInt, limit))
			return false;

		DogArray<BowMatch> found = database.getMatches();

		if (verbose != null) verbose.println("matches.size=" + found.size + " best.error=" + found.get(0).error);

		// Copy results into output format
		matches.resize(found.size);
		for (int i = 0; i < matches.size; i++) {
			BowMatch f = found.get(i);
			matches.get(i).id = imageIds.get(f.identification);
			matches.get(i).error = f.error;
		}

		return !matches.isEmpty();
	}

	@Override public int getQueryWord( int featureIdx ) {
		return traverseUpGetID(database.getFeatureIdxToLeafID().get(featureIdx),
				config.featureSingleWordHops);
	}

	@Override public void getQueryWords( int featureIdx, DogArray_I32 words ) {
		int leafID = getQueryWord(featureIdx);
		lookupWordsFromLeafID(leafID, words);
	}

	@Override public int lookupWord( TD description ) {
		return traverseUpGetID(database.tree.searchPathToLeaf(description, ( idx, n ) -> {}),
				config.featureSingleWordHops);
	}

	@Override public void lookupWords( TD description, DogArray_I32 word ) {
		lookupWordsFromLeafID(lookupWord(description), word);
	}

	/**
	 * Given the leafID lookup the other words in the tree leading to the leaf. Words is reset on each call
	 */
	protected void lookupWordsFromLeafID( int leafID, DogArray_I32 word ) {
		word.reset();
		HierarchicalVocabularyTree.Node node = database.tree.nodes.get(leafID);
		while (node != null) {
			// the root node has a parent of -1 and we don't want to use that as a word since every feature has it
			if (node.parent == -1)
				break;
			word.add(node.index);
			node = database.tree.nodes.get(node.parent);
		}
	}

	/**
	 * Traverses up the tree to find a parent node X hops away
	 */
	protected int traverseUpGetID( int id, int maxHops ) {
		int hops = maxHops;
		HierarchicalVocabularyTree.Node node = database.tree.nodes.get(id);
		while (hops-- > 0) {
			node = database.tree.nodes.get(node.parent);
			// the root node has a parent of -1 and we don't want to use that as a word since every feature has it
			if (node.parent == -1)
				break;
			id = node.index;
		}
		return id;
	}

	@Override public int getTotalWords() {
		return database.tree.nodes.size;
	}

	@Override public Class<TD> getDescriptorType() {
		return tupleType;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
