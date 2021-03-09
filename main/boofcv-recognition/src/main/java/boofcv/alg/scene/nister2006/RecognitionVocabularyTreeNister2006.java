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

import boofcv.alg.scene.nister2006.TupleMapDistanceNorm.CommonWords;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree.Node;
import gnu.trove.map.TIntFloatMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.Collections;
import java.util.List;

/**
 * Image recognition based off of [1] using inverted files. A {@link HierarchicalVocabularyTree} is assumed to hav
 * e been already trained. When an image is added to the database a TF-IDF descriptor is computed using the tree
 * and then added to the relevant tree's leaves. When an image is looked up its TF-IDF descriptor is found then
 * all images in the data base are found that share at least one leaf node. These candidate matches are then
 * compared against each other and scored using L2-Norm.
 *
 * <p>
 * [1] Nister, David, and Henrik Stewenius. "Scalable recognition with a vocabulary tree."
 * 2006 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'06). Vol. 2. Ieee, 2006.
 * </p>
 *
 * @author Peter Abeles
 */
public class RecognitionVocabularyTreeNister2006<Point> {
	/** Vocabulary Tree */
	public @Getter HierarchicalVocabularyTree<Point> tree;

	/** Nodes only contribute to the descriptor if they are at most this number of hops from a leaf */
	public int maxDistanceFromLeaf = Integer.MAX_VALUE;

	/** List of images added to the database */
	protected @Getter final DogArray<ImageInfo> imagesDB = new DogArray<>(ImageInfo::new, ImageInfo::reset);

	/** Scores for all candidate images which have been sorted */
	protected @Getter final DogArray<Match> matchScores = new DogArray<>(Match::new, Match::reset);

	/** Distance between two TF-IDF descriptors. L1 and L2 norms are provided */
	protected @Getter @Setter TupleMapDistanceNorm distanceFunction = new TupleMapDistanceNorm.L2();

	//---------------- Internal Workspace
	// Number of images in DB that pass through this node
	protected final DogArray_I32 nodeImageCount = new DogArray_I32();

	// The "frequency" that nodes in the tree appear in this image
	protected final DogArray<Frequency> frequencies = new DogArray<>(Frequency::new, Frequency::reset);
	protected final TIntObjectMap<Frequency> node_to_frequency = new TIntObjectHashMap<>();

	// Temporary storage for an image's TF description while it's being looked up
	protected final TIntFloatMap queryDescTermFreq = new TIntFloatHashMap();

	// Predeclare array for storing keys. Avoids unnecessary array creation
	protected final DogArray_I32 keysDesc = new DogArray_I32(); // ONLY use in describe()

	// For lookup
	TIntIntMap identification_to_match = new TIntIntHashMap();

	LeafHistogram leafHistogram = new LeafHistogram();

	/**
	 * Configures the tree by adding LeafData to all the leaves in the tree then saves a reference for future use
	 *
	 * @param tree Three which is to be used as the database. Saved internally.
	 */
	public void initializeTree( HierarchicalVocabularyTree<Point> tree ) {
		this.tree = tree;
		clearImages();
	}

	/**
	 * Removes all images from the database.
	 */
	public void clearImages() {
		imagesDB.reset();

		// Removes the old leaf data and replaces it with empty structures
		tree.nodeData.clear();
		for (int i = 0; i < tree.nodes.size; i++) {
			if (!tree.nodes.get(i).isLeaf())
				continue;
			tree.nodes.get(i).dataIdx = -1;
			tree.addData(tree.nodes.get(i), new InvertedFile());
		}

		// Set image count to zero since there are no images
		nodeImageCount.resize(tree.nodes.size, 0);
	}

	/**
	 * Adds a new image to the database.
	 *
	 * @param imageID The image's unique ID for later reference
	 * @param imageFeatures Feature descriptors from an image
	 * @param cookie Optional user defined data which will be attached to the image
	 */
	public void addImage( int imageID, List<Point> imageFeatures, Object cookie ) {
		if (imageFeatures.isEmpty())
			return;

		int imageIdx = imagesDB.size;
		ImageInfo info = imagesDB.grow();
		info.identification = imageID;
		info.cookie = cookie;

		// compute a descriptor for this image while adding it to the leaves
		describe(imageFeatures, info.descTermFreq, leafHistogram);

		// Add this image to all the leaves it encountered
		for (int histIdx = 0; histIdx < leafHistogram.leaves.size; histIdx++) {
			LeafCounts counts = leafHistogram.leaves.get(histIdx);
			Node node = tree.nodes.get(counts.nodeIdx);

			// This leaf has been marked as useless for information. In general it means it's too common
			if (node.weight <= 0.0f)
				continue;

			// Add the image to the list of images which observed this leaf
			((InvertedFile)tree.nodeData.get(node.dataIdx)).add(imageIdx);
		}

		// For each node which is in the descriptor, increment its image count
		keysDesc.resize(info.descTermFreq.size());
		info.descTermFreq.keys(keysDesc.data);
		for (int i = 0; i < keysDesc.size; i++) {
			nodeImageCount.data[keysDesc.get(i)]++;
		}
	}

	/**
	 * Looks up the best match from the database. The list of all potential matches can be accessed by calling
	 * {@link #getMatchScores()}.
	 *
	 * @param queryImage Set of feature descriptors from the query image
	 * @oaran limit Maximum number of matches it will return.
	 * @return The best matching image with score from the database
	 */
	public boolean query( List<Point> queryImage, int limit ) {
		identification_to_match.clear();
		matchScores.reset();

		// Can't match to anything if it's empty
		if (queryImage.isEmpty()) {
			return false;
		}

		// Create a description of this image and collect potential matches from leaves
		describe(queryImage, queryDescTermFreq, leafHistogram);

		// Create a list of images in the DB that have at least a single feature pass through a node
		for (int histIdx = 0; histIdx < leafHistogram.leaves.size; histIdx++) {
			LeafCounts count = leafHistogram.leaves.get(histIdx);
			HierarchicalVocabularyTree.Node node = tree.nodes.get(count.nodeIdx);

			// Find the database images by looking at the inverted file for this particular node/work
			InvertedFile invertedFile = (InvertedFile)tree.nodeData.get(node.dataIdx);

			// distance from leaf along the graph
			int distanceFromLeaf = 0;

			// Go up the graph until it hits the root or maximum distance away from a leaf
			while (distanceFromLeaf <= maxDistanceFromLeaf && node.index > 0) {
				if (node.weight <= 0.0f)
					break;

				// Look up the value of this word in the image descriptor for the query
				float queryWordWeight = queryDescTermFreq.get(node.index);

				// Go through each leaf and compute the error for all related nodes
				for (int i = 0; i < invertedFile.size(); i++) {
					// Get the list of images in the database which have this particular word using
					// the inverted file list
					ImageInfo dbImage = imagesDB.get(invertedFile.get(i));

					// The match stores how well this particular images matches the target as well as book keeping info
					int identification = dbImage.identification;
					Match m;
					if (!identification_to_match.containsKey(identification)) {
						identification_to_match.put(identification, matchScores.size);
						m = matchScores.grow();
						m.image = dbImage;
					} else {
						m = matchScores.get(identification_to_match.get(identification));
						// If this node has already been examined skip it
						if (m.traversed.contains(node.index))
							continue;
					}

					// Mark this node as being traversed so that it isn't double counted
					m.traversed.add(node.index);

					// Update the score computation. See TupleMapDistanceNorm for why this is done
					m.commonWords.grow().setTo(node.index, queryWordWeight, dbImage.descTermFreq.get(node.index));
				}

				// move to the parent node
				node = tree.nodes.get(node.parent);
				distanceFromLeaf++;
			}
		}

		if (matchScores.isEmpty())
			return false;

		// Compute the final scores
		for (int i = 0; i < matchScores.size(); i++) {
			Match m = matchScores.get(i);
			m.error = distanceFunction.distance(
					queryDescTermFreq, m.image.descTermFreq, m.commonWords.toList());
		}

		// NOTE: quick select then Collections.sort on the remaining entries is about 1.3x to 2x faster, but it's not
		//       a bottle neck. Sort time for 8000 elements is about 2.5 ms
		Collections.sort(matchScores.toList());
		matchScores.size = Math.min(matchScores.size,limit);

		return true;
	}

	/**
	 * Given the image features, compute a sparse descriptor for the image and pass in leaf nodes to 'op' for each
	 * image feature.
	 *
	 * @param imageFeatures (Input) All image features in the image
	 * @param descTermFreq (Output) Sparse TF-IDF descriptor for the image
	 * @param leafHistogram (Output) Which leaves were encountered and how many times
	 */
	protected void describe( List<Point> imageFeatures, TIntFloatMap descTermFreq, LeafHistogram leafHistogram ) {
		// Reset work variables
		frequencies.reset();
		node_to_frequency.clear();
		descTermFreq.clear();

		// Find all the leaves and the number of times they were observed
		computeLeafHistogram(imageFeatures, leafHistogram);

		// From each leaf the parents can be traversed to find the other nodes and the number of times
		// they were passed though
		for (int histIdx = 0; histIdx < leafHistogram.leaves.size; histIdx++) {
			LeafCounts count = leafHistogram.leaves.get(histIdx);

			HierarchicalVocabularyTree.Node node = tree.nodes.get(count.nodeIdx);
			// distance the node is from the leaf node
			int distanceFromLeaf = 0;

			// Traverse up the tree from leaf to root
			while (distanceFromLeaf <= maxDistanceFromLeaf && node.index > 0) {
				// if the weight is zero now, all the parents will have to be zero too
				if (node.weight <= 0.0f)
					break;

				Frequency f = node_to_frequency.get(node.index);
				if (f == null) {
					f = frequencies.grow();
					f.node = node;
					node_to_frequency.put(node.index, f);
				}
				f.totalAppearances += count.count;

				node = tree.nodes.get(node.parent);
				distanceFromLeaf++;
			}
		}

		// No nodes with a non-zero weight that matched was found
		if (node_to_frequency.isEmpty())
			return;

		// Create the descriptor and normalize it
		double totalUniqueWordsSeenByImage = frequencies.size;
		// NOTE: I'm not 100% sure this is the divisor used in the paper, but doesn't really matter due to the
		//       descriptor getting normalized.

		for (int i = 0; i < frequencies.size; i++) {
			Frequency f = frequencies.get(i);

			// Term frequency: n[i] = number of times word[i] appears in this image / total words in this image
			double termFrequency = f.totalAppearances/totalUniqueWordsSeenByImage;
			// TF-IDF feature: d[i] = n[i] * node_weight[i]
			double feature = termFrequency*f.node.weight;
			descTermFreq.put(f.node.index, (float)feature);
		}
		distanceFunction.normalize(descTermFreq);
	}

	/**
	 * Counts the number of times each leaf node is observed and stores the found leaves
	 * in the sparse histogram.
	 */
	protected void computeLeafHistogram( List<Point> features, LeafHistogram histogram ) {
		histogram.reset();

		for (int featsIdx = 0; featsIdx < features.size(); featsIdx++) {
			int leafNodeIdx = tree.searchPathToLeaf(features.get(featsIdx), ( a ) -> {});

			// See if this leaf has been observed before
			LeafCounts counts = histogram.observed.get(leafNodeIdx);

			// If it has not been seed before, add it to the set
			if (counts == null) {
				counts = histogram.leaves.grow();
				counts.nodeIdx = leafNodeIdx;
				histogram.observed.put(leafNodeIdx, counts);
			}

			counts.count++;
		}
	}

	/** Used to change distance function to one of the built in types */
	public void setDistanceType( DistanceTypes type ) {
		distanceFunction = switch (type) {
			case L1 -> new TupleMapDistanceNorm.L1();
			case L2 -> new TupleMapDistanceNorm.L2();
			default -> throw new IllegalArgumentException("Unknown type " + type);
		};
	}

	/** Information about an image stored in the database */
	public static class ImageInfo {
		/** TF-IDF description of the image. Default 0 for no key and no value. */
		public final TIntFloatMap descTermFreq = new TIntFloatHashMap();

		/** Use specified data associated with this image */
		public Object cookie;
		/** Unique ID for this image */
		public int identification;

		public <T> T getCookie() {
			return (T)cookie;
		}

		public void reset() {
			descTermFreq.clear();
			cookie = null;
			identification = -1;
		}
	}

	/**
	 * Storage for counting the number of times each leaf is observed by an image
	 */
	protected static class LeafHistogram {
		// leaf node-id to count data structure
		public TIntObjectMap<LeafCounts> observed = new TIntObjectHashMap<>();
		// array of all the leaves encountered for fast look up later on and for recycling memory
		public DogArray<LeafCounts> leaves = new DogArray<>(LeafCounts::new, LeafCounts::reset);

		public void reset() {
			observed.clear();
			leaves.reset();
		}
	}

	/**
	 * Number of times a specific node was observed by an image
	 */
	protected static class LeafCounts {
		// Number of times the leaf was observed
		public int count;
		// The node index of the leaf
		public int nodeIdx;

		public void reset() {
			count = 0;
			nodeIdx = -1;
		}
	}

	/**
	 * Used to sum the frequency of words (graph nodes) in the image
	 */
	protected static class Frequency {
		// Number of times this word/node appeared in this image
		int totalAppearances;
		// The node which is referenced
		Node node;

		public void reset() {
			totalAppearances = 0;
			node = null;
		}
	}

	/**
	 * Information on candidate match to a query image
	 */
	public static class Match implements Comparable<Match> {
		/** Fit error. 0.0 = perfect. */
		public float error;
		/** Reference to the image in the data base that was matched */
		public ImageInfo image;
		/** Nodes in the tree that it has already traversed */
		public final TIntSet traversed = new TIntHashSet();
		/** All words which are common between image in DB and the query image */
		public final DogArray<CommonWords> commonWords = new DogArray<>(CommonWords::new);

		public void reset() {
			error = 0;
			image = null;
			traversed.clear();
			commonWords.reset();
		}

		@Override public int compareTo( Match o ) {
			return Float.compare(error, o.error);
		}
	}

	/**
	 * The inverted file is a list of images that were observed in a particular node. Images are
	 * referenced by array index.
	 */
	public static class InvertedFile extends DogArray_I32 {
		public InvertedFile(){
			super(1);
		}
	}

	/** Different built in distance norms. */
	public enum DistanceTypes {L1, L2}
}
