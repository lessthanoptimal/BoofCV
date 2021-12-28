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

package boofcv.alg.scene.ann;

import boofcv.alg.scene.bow.BowDistanceTypes;
import boofcv.alg.scene.bow.BowMatch;
import boofcv.alg.scene.bow.BowUtils;
import boofcv.alg.scene.bow.InvertedFile;
import boofcv.alg.scene.nister2006.TupleMapDistanceNorm;
import boofcv.misc.BoofLambdas;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.*;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * <p>Implementation of the "classical" Bog-Of-Words (BOW) (a.k.a. Bag-Of-Visual-Words) [1] for object/scene recognition
 * that uses an inverted file for fast image retrieval [2].</p>
 *
 * An image is described using a set of local image features (e.g. SIFT) which results in a set of n-dimensional
 * vectors. Each feature vector is converted into a word, which is then used to build a histogram of words in the
 * image. A similarity score is computed between two images using the histogram. Words are learned using k-means
 * clustering when applied to a large initial training set of image features.
 *
 * This implementation is designed to be simple and flexible. Allowing different algorithms in the same family
 * to be swapped out. For example, the nearest-neighbor (NN) search can be done using a brute force approach, kd-tree,
 * or an approximate kd-tree.
 *
 * There is no single source for this specific paper that inspired this implementation and it borrows ideas from
 * several papers. The paper below is one of the earlier works to discuss the concept for visual BOW.
 * <ol>
 * <li>Sivic, Josef, and Andrew Zisserman. "Video Google: A text retrieval approach to object matching in videos."
 * Computer Vision, IEEE International Conference on. Vol. 3. IEEE Computer Society, 2003.</li>
 * <li>Nister, David, and Henrik Stewenius. "Scalable recognition with a vocabulary tree."
 * 2006 IEEE Computer Society Conference on Computer Vision and Pattern Recognition (CVPR'06). Vol. 2. Ieee, 2006.</li>
 * </ol>
 *
 * @param <Point> Data type for the 'point'. Typically this is a Tuple.
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RecognitionNearestNeighborInvertedFile<Point> implements VerbosePrint {
	/** A nearest-neighbor search to look up the closest fit to each word */
	protected @Getter NearestNeighbor<Point> nearestNeighbor;

	/** Distance between two TF-IDF descriptors. L1 and L2 norms are provided */
	protected @Getter @Setter TupleMapDistanceNorm distanceFunction = new TupleMapDistanceNorm.L2();

	/** List of images added to the database */
	protected @Getter final BigDogArray_I32 imagesDB = new BigDogArray_I32(100, 10_000, BigDogGrowth.GROW_FIRST);

	/** List of all images the query was found to be similar/matched with */
	@Getter DogArray<BowMatch> matches = new DogArray<>(BowMatch::new, BowMatch::reset);

	/** List of images in the DB that are observed by each word. One element per word. */
	@Getter DogArray<InvertedFile> invertedFiles = new DogArray<>(InvertedFile::new, InvertedFile::reset);

	//--------------------------- Internal Work Space

	// Used to search for matching words
	public NearestNeighbor.Search<Point> search;
	public final NnData<Point> searchResult = new NnData<>();

	// Look up table from image to BowMatch. All values but be set to -1 after use
	// The size of this array will be the same as the number of DB images
	DogArray_I32 imageIdx_to_match = new DogArray_I32();

	// Histogram for the number of times each word appears. All values must be 0 initially
	// One element for each word
	DogArray_I32 wordHistogram = new DogArray_I32();
	// List of words which were observed
	public DogArray_I32 observedWords = new DogArray_I32();

	// temporary storage for an image TF-IDF descriptor
	DogArray_F32 tmpDescWeights = new DogArray_F32();

	// If not null then print verbose information here
	@Nullable PrintStream verbose;

	/**
	 * Initializes the data structures.
	 *
	 * @param nearestNeighbor Search used to find the words.
	 * @param numWords Number of words
	 */
	public void initialize( NearestNeighbor<Point> nearestNeighbor, int numWords ) {
		this.nearestNeighbor = nearestNeighbor;
		invertedFiles.resize(numWords);
		imagesDB.reset();

		wordHistogram.resetResize(numWords, 0);

		this.search = nearestNeighbor.createSearch();
	}

	/**
	 * Discards all memory of words which were added
	 */
	public void clearImages() {
		imagesDB.reset();

		// Clear the inverted files list. This will force all elements to be reset
		int numWords = invertedFiles.size;
		invertedFiles.reset();
		invertedFiles.resize(numWords);
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

		computeWordHistogram(imageFeatures);
		computeImageDescriptor(imageFeatures.size());

		// Add this image to the inverted file for each word
		for (int i = 0; i < observedWords.size; i++) {
			int word = observedWords.get(i);
			invertedFiles.get(word).addImage(imageIdx, tmpDescWeights.get(i));
		}
	}

	/**
	 * Computes the number of times each word appears in the list of features
	 */
	void computeWordHistogram( List<Point> imageFeatures ) {
		// Find and count the number of times each word appears in this set of features
		observedWords.reset();
		for (int featureIdx = 0; featureIdx < imageFeatures.size(); featureIdx++) {
			if (!search.findNearest(imageFeatures.get(featureIdx), -1, searchResult))
				continue;

			int count = wordHistogram.data[searchResult.index];
			wordHistogram.data[searchResult.index] = count + 1;
			if (count == 0) {
				observedWords.add(searchResult.index);
			}
		}
	}

	/**
	 * Given the image histogram, compute the TF-IDF descriptor
	 *
	 * @param totalUniqueWordsSeenByImage Number of features in this image
	 */
	void computeImageDescriptor( float totalUniqueWordsSeenByImage ) {
		// Compute the weight for each word in the descriptor based on its frequency
		tmpDescWeights.reset();
		for (int i = 0; i < observedWords.size; i++) {
			int word = observedWords.get(i);

			// Term frequency: n[i] = number of times word[i] appears in this image / total words in this image
			float termFrequency = wordHistogram.get(word)/totalUniqueWordsSeenByImage;
			tmpDescWeights.add(termFrequency);

			// make sure the histogram is full of zeros again
			wordHistogram.set(word, 0);
		}

		// Normalize the image descriptor
		distanceFunction.normalize(tmpDescWeights);
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

		computeWordHistogram(queryImage);
		computeImageDescriptor(queryImage.size());
		findAndScoreMatches();

		if (matches.isEmpty())
			return false;

		if (verbose != null) verbose.println("raw matches.size=" + matches.size);

		// Compute the score for each candidate and other book keeping
		for (int candidateIter = 0; candidateIter < matches.size; candidateIter++) {
			BowMatch c = matches.get(candidateIter);

			// Ensure this array is once again full of -1
			imageIdx_to_match.set(c.identification, -1);

			// convert it from image index into the user provided ID number
			c.identification = imagesDB.get(c.identification);
		}

		BowUtils.filterAndSortMatches(matches, filter, limit);

		return matches.size > 0;
	}

	/**
	 * Finds all the matches using the observed words and the inverted files.
	 */
	void findAndScoreMatches() {
		// This will always be filled with -1 initially, resize will just set new elements to -1
		imageIdx_to_match.resize(imagesDB.size, -1);

		// Create a list of all candidate images in the DB
		matches.reset();
		for (int wordIdx = 0; wordIdx < observedWords.size; wordIdx++) {
			float queryWordWeight = tmpDescWeights.get(wordIdx);
			int word = observedWords.get(wordIdx);
			InvertedFile invertedFile = invertedFiles.get(word);

			// Go through the inverted file list
			final int N = invertedFile.weights.size;
			for (int invertedIdx = 0; invertedIdx < N; invertedIdx++) {
				int imageIdx = invertedFile.get(invertedIdx);

				// See if this DB image has been seen before
				BowMatch m;
				int matchIdx = imageIdx_to_match.get(imageIdx);
				if (matchIdx == -1) {
					// It has not been seen before, create a new entry for it in the candidate list
					imageIdx_to_match.set(imageIdx, matches.size);
					m = matches.grow();
					m.identification = imageIdx; // this will be converted to ID on output
				} else {
					m = matches.get(matchIdx);
				}

				// Update the score computation. See TupleMapDistanceNorm for why this is done
				m.error += distanceFunction.distanceUpdate(queryWordWeight, invertedFile.weights.get(invertedIdx));
			}
		}
	}

	/** Used to change distance function to one of the built in types */
	public void setDistanceType( BowDistanceTypes type ) {
		distanceFunction = switch (type) {
			case L1 -> new TupleMapDistanceNorm.L1();
			case L2 -> new TupleMapDistanceNorm.L2();
			default -> throw new IllegalArgumentException("Unknown type " + type);
		};
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
