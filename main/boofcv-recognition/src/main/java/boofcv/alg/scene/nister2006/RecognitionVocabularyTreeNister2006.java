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

import boofcv.alg.scene.bow.BowDistanceTypes;
import boofcv.alg.scene.bow.BowMatch;
import boofcv.alg.scene.bow.BowUtils;
import boofcv.alg.scene.bow.InvertedFile;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree;
import boofcv.alg.scene.vocabtree.HierarchicalVocabularyTree.Node;
import boofcv.misc.BoofLambdas;
import boofcv.struct.ConfigLength;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.*;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Image recognition based off of [1] using inverted files. A {@link HierarchicalVocabularyTree} is assumed to have
 * been already trained. When an image is added to the database a TF-IDF descriptor is computed using the tree
 * and then added to the relevant tree's leaves. When an image is looked up its TF-IDF descriptor is found then
 * all images in the data base are found that share at least one leaf node. These candidate matches are then
 * compared against each other and scored using L2-Norm.
 *
 * <p>Implementation Notes:<br>
 * This implementation is intended to produce output which is faithful to the original work [1] but has
 * several modifications internally where there has been an attempt to improve runtime performance, often
 * at the cost of an increase in memory consumption. A non-exhaustive set of deviations is listed below</p>
 * <ul>
 *     <li>Taking inspiration from [2], this implementation has an explicit representation of the inverted
 *     files in non-leaf nodes. This avoid an expensive graph traversal step and replaces it with a very fast
 *     array look up.</li>
 *     <li>Histogram weights are stored in inverted files instead of word counts. Allows more efficient error
 *     computation.</li>
 * </ul>
 *
 * <p>
 * [1] Nister, David, and Henrik Stewenius. "Scalable recognition with a vocabulary tree."
 * 2006 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'06). Vol. 2. Ieee, 2006.<br>
 * [2] Esteban Uriza, Francisco Gómez Fernández, and Martín Rais, "Efficient Large-scale Image Search With a Vocabulary
 * Tree", Image Processing On Line, 8 (2018), pp. 71–98
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RecognitionVocabularyTreeNister2006<Point> implements VerbosePrint {
	/** Vocabulary Tree */
	public @Getter HierarchicalVocabularyTree<Point> tree;

	/** A node can be part of the descriptor if it's at least this far from the root node */
	public int minimumDepthFromRoot = 0;

	/**
	 * If a node has an inverted file list greater than this amount then it will be skipped when scoring. This
	 * should be viewed as a last ditch effort when the query is too slow. If there are a 1,000,000 images in the
	 * DB, then 20,000 seems to be a reasonable number.
	 */
	public ConfigLength maximumQueryImagesInNode = ConfigLength.relative(1.0, 1);

	/** User data associated with each node */
	public final GrowArray<InvertedFile> invertedFiles = new GrowArray<>(InvertedFile::new, InvertedFile::reset);

	/** List of images added to the database */
	protected @Getter final BigDogArray_I32 imagesDB = new BigDogArray_I32(100, 10000, BigDogGrowth.GROW_FIRST);

	/** Scores for all candidate images which have been sorted */
	protected @Getter final DogArray<BowMatch> matches = new DogArray<>(BowMatch::new, BowMatch::reset);

	/** Distance between two TF-IDF descriptors. L1 and L2 norms are provided */
	protected @Getter @Setter TupleMapDistanceNorm distanceFunction = new TupleMapDistanceNorm.L2();

	/** Stores a mapping from feature index to leaf ID */
	protected @Getter final DogArray_I32 featureIdxToLeafID = new DogArray_I32();

	//---------------- Internal Workspace
	// The "frequency" that nodes in the tree appear in this image
	protected final DogArray<Frequency> frequencies = new DogArray<>(Frequency::new, Frequency::reset);

	// For lookup. One element for every image in the database
	DogArray_I32 imageIdx_to_match = new DogArray_I32();
	DogArray_I32 nodeIdx_to_match = new DogArray_I32();

	// temporary storage for an image TF-IDF descriptor
	DogArray_F32 tmpDescWeights = new DogArray_F32();
	DogArray_I32 tmpDescWords = new DogArray_I32();

	// If not null then print verbose information here
	@Nullable PrintStream verbose;

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
		invertedFiles.reset();
		invertedFiles.resize(tree.nodes.size);
	}

	/**
	 * Adds a new image to the database.
	 *
	 * @param imageID The image's unique ID for later reference
	 * @param imageFeatures Feature descriptors from an image
	 */
	public void addImage( int imageID, List<Point> imageFeatures ) {
		if (imageFeatures.isEmpty())
			return;

		int imageIdx = imagesDB.size;
		imagesDB.append(imageID);

		// compute a descriptor for this image while adding it to the leaves
		describe(imageFeatures, tmpDescWeights, tmpDescWords);

		for (int wordIdx = 0; wordIdx < tmpDescWords.size; wordIdx++) {
			int word = tmpDescWords.get(wordIdx);
			invertedFiles.get(word).addImage(imageIdx, tmpDescWeights.get(wordIdx));
		}
	}

	/**
	 * Looks up the best BowMatch from the database. The list of all potential matches can be accessed by calling
	 * {@link #getMatches()}.
	 *
	 * @param queryImage Set of feature descriptors from the query image
	 * @param filter Filter which can be used to reject matches that the user doesn't want returned. False = reject.
	 * @param limit Maximum number of matches it will return.
	 * @return The best matching image with score from the database
	 */
	public boolean query( List<Point> queryImage, @Nullable BoofLambdas.FilterInt filter, int limit ) {
		matches.reset();

		// Can't BowMatch to anything if it's empty
		if (queryImage.isEmpty()) {
			return false;
		}

		findAndScoreMatches(queryImage);

		if (matches.isEmpty())
			return false;

		if (verbose != null) verbose.println("raw matches.size=" + matches.size);

		// Book keeping
		for (int i = 0; i < matches.size(); i++) {
			BowMatch m = matches.get(i);

			// Undo changes and make sure all elements are -1 again
			imageIdx_to_match.set(m.identification, -1);
			// m.identification is overloaded earlier and actually stores the index
			m.identification = imagesDB.get(m.identification);
		}

		BowUtils.filterAndSortMatches(matches, filter, limit);

		return matches.size > 0;
	}

	/**
	 * Uses the inverted file for each word to create a list of potential matches while scoring the matches
	 * efficiently
	 */
	protected void findAndScoreMatches( List<Point> queryImage ) {
		// Don't use a node if it will degrade the runtime performance too much by considering too many images
		// This will also degrade the quality of query results
		int maximumInvertedFileLength = maximumQueryImagesInNode.computeI(imagesDB.size);

		// Create a description of this image and collect potential matches from leaves
		describe(queryImage, tmpDescWeights, tmpDescWords);

		// NOTE: It's assumed imageIdx_to_match is full of -1
		imageIdx_to_match.resize(imagesDB.size, -1);

		// Find and score all the images that could possible be matched with the query
		for (int wordIdx = 0; wordIdx < tmpDescWords.size; wordIdx++) {
			float queryWordWeight = tmpDescWeights.get(wordIdx);
			HierarchicalVocabularyTree.Node node = tree.nodes.get(tmpDescWords.get(wordIdx));

			InvertedFile invertedFile = invertedFiles.get(node.index);

			// See above
			if (invertedFile.size > maximumInvertedFileLength)
				continue;

			for (int i = 0; i < invertedFile.size; i++) {
				// Get the list of images in the database which have this particular word using
				// the inverted file list
				int imageIdx = invertedFile.get(i);

				BowMatch m;
				if (imageIdx_to_match.get(imageIdx) == -1) {
					imageIdx_to_match.set(imageIdx, matches.size);
					m = matches.grow();
					m.identification = imageIdx; // this will be converted to ID on output
				} else {
					m = matches.get(imageIdx_to_match.get(imageIdx));
				}

				// Update the score computation. See TupleMapDistanceNorm for why this is done
				m.error += distanceFunction.distanceUpdate(queryWordWeight, invertedFile.weights.get(i));
				// NOTE: An earlier version created a list of common word weights. That took 5x longer
			}
		}
	}

	/**
	 * Given the image features, compute a sparse descriptor for the image and pass in leaf nodes to 'op' for each
	 * image feature.
	 *
	 * @param imageFeatures (Input) All image features in the image
	 * @param descWeights (Output) Weights for non-zero word in TD-IDF descriptor for this image
	 * @param descWords (Output) Word index for non-zero word in TD-IDF descriptor for this image
	 */
	protected void describe( List<Point> imageFeatures, DogArray_F32 descWeights, DogArray_I32 descWords ) {
		// Reset work variables
		frequencies.reset();
		descWeights.reset();
		descWords.reset();

		// NOTE: It's assumed nodeIdx_to_match is full of -1
		nodeIdx_to_match.resize(tree.nodes.size, -1);
		featureIdxToLeafID.resize(imageFeatures.size());

		for (int featureIdx = 0; featureIdx < imageFeatures.size(); featureIdx++) {
			int leafID = tree.searchPathToLeaf(imageFeatures.get(featureIdx), ( depth, node ) -> {
				if (depth < minimumDepthFromRoot || node.weight <= 0.0f)
					return;

				Frequency f;
				int frequencyIdx = nodeIdx_to_match.get(node.index);
				if (frequencyIdx == -1) {
					nodeIdx_to_match.set(node.index, frequencies.size);
					f = frequencies.grow();
					f.node = node;
				} else {
					f = frequencies.get(frequencyIdx);
				}
				f.totalAppearances++;
			});

			featureIdxToLeafID.data[featureIdx] = leafID;
		}

		// undo changes to the lookup table
		for (int i = 0; i < frequencies.size; i++) {
			nodeIdx_to_match.set(frequencies.get(i).node.index, -1);
		}

		// No nodes with a non-zero weight that matched was found
		if (frequencies.isEmpty())
			return;

		// Create the descriptor and normalize it
		double totalUniqueWordsSeenByImage = frequencies.size;
		// NOTE: I'm not 100% sure this is the divisor used in the paper, but doesn't really matter due to the
		//       descriptor getting normalized.

		descWeights.reserve(frequencies.size);
		descWords.reserve(frequencies.size);

		for (int i = 0; i < frequencies.size; i++) {
			Frequency f = frequencies.get(i);

			// Term frequency: n[i] = number of times word[i] appears in this image / total words in this image
			double termFrequency = f.totalAppearances/totalUniqueWordsSeenByImage;
			// TF-IDF feature: d[i] = n[i] * node_weight[i]
			descWeights.add((float)(termFrequency*f.node.weight));
			descWords.add(f.node.index);
		}
		distanceFunction.normalize(descWeights);
	}

	/** Used to change distance function to one of the built in types */
	public void setDistanceType( BowDistanceTypes type ) {
		distanceFunction = switch (type) {
			case L1 -> new TupleMapDistanceNorm.L1();
			case L2 -> new TupleMapDistanceNorm.L2();
			default -> throw new IllegalArgumentException("Unknown type " + type);
		};
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> settings ) {
		this.verbose = out;
	}

	/**
	 * Used to sum the frequency of words (graph nodes) in the image
	 */
	@SuppressWarnings({"NullAway.Init"})
	protected static class Frequency {
		// Number of times this word/node appeared in this image
		int totalAppearances;
		// The node which is referenced
		Node node;

		@SuppressWarnings({"NullAway"})
		public void reset() {
			totalAppearances = 0;
			node = null;
		}
	}
}
