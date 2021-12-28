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

package boofcv.alg.similar;

import boofcv.abst.feature.associate.AssociateDescriptionHashSets;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.PackedArray;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.packed.PackedArrayPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Identifies similar images using {@link boofcv.abst.scene.FeatureSceneRecognition}. For each image which is added
 * a set of image features (descriptions and pixel coordinate) is found using the {@link #detector}. After all
 * the images have been added {@link #fixate()} is called and after sometime a fast but not 100% reliable
 * model which be required for looking up which images are similar. In this case similar means that they are of
 * the same physical scene but from a "similar" perspective. Then for each image all the "similar" images are found
 * and the image features are associated. Theses associated image features are tested to see if the two images
 * really are related using the logic specified by {@link #similarityTest}. If they are related their
 * pairwise information is saved for later fast retrieval.
 *
 * While building there is significant cost in building the retrieval system, this is in general much faster than
 * considering every possible image pair and trying to match them. The main down side is that it will not be
 * 100% reliable.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SimilarImagesSceneRecognition<Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
		implements LookUpSimilarImages, VerbosePrint {
	/** Detects image features */
	@Getter @Setter DetectDescribePoint<Image, TD> detector;

	/** Performs feature based association using 'words' from the recognizer */
	@Getter AssociateDescriptionHashSets<TD> asscociator;

	/** Looks up similar images and provides words for each image feature */
	@Getter FeatureSceneRecognition<TD> recognizer;

	/** If true it will relearn the model used by the recognizer. If false you need to provide the model. */
	@Getter @Setter boolean relearnModel = true;

	/** Number of images which will be considered as matches when using the recognizer */
	@Getter @Setter int limitMatchesConsider = 30;

	/** Logic used to decide if two images are similar to each other */
	@Getter @Setter SimilarityTest similarityTest = new ImageSimilarityAssociatedRatio();

	//========================== Image Information and Relationships
	// List of ID strings for each image
	final List<String> imageIDs = new ArrayList<>();
	// Mapping from image ID to image array index. -1 means no mapping
	final TObjectIntMap<String> imageToIndex = new TObjectIntHashMap<>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1);
	// Information about similar images to a recent query
	final DogArray<PairInfo> pairInfo = new DogArray<>(PairInfo::new, PairInfo::reset);
	final Map<String, PairInfo> viewId_to_info = new HashMap<>();

	//========================== Image Feature Storage
	// A single large array for all image feature descriptions.
	final PackedArray<TD> descriptions;
	// A single large array for all image feature pixel coordinates
	final PackedArray<Point2D_F64> pixels = new PackedArrayPoint2D_F64();
	// Stores the location of image features in the packed array. interleaved (first index, number of features)
	final DogArray_I32 imageFeatureStartIndexes = new DogArray_I32();

	//========================= Internal Profiling
	/** Time {@link #fixate()} took to learn the model. Milliseconds. */
	public @Getter double timeFixateLearnMS;
	/** Time {@link #fixate()} took to add images to the database. Milliseconds. */
	public @Getter double timeFixateAddMS;

	//========================= Internal Workspace
	// Storage for results when looking up matches
	final DogArray<SceneRecognition.Match> sceneMatches = new DogArray<>(SceneRecognition.Match::new);

	// Temporary storage for a single feature description and pixel
	final TD tempDescription;
	final Point2D_F64 tempPixel = new Point2D_F64();

	// Storage used for association
	final DogArray<TD> sourceDescriptions;
	final DogArray<Point2D_F64> sourcePixels;

	final DogArray<TD> destinationDescriptions;
	final DogArray<Point2D_F64> destinationPixels;

	// If not null it will print verbose debugging info
	@Nullable PrintStream verbose;

	public SimilarImagesSceneRecognition( DetectDescribePoint<Image, TD> detector,
										  AssociateDescriptionHashSets<TD> asscociator,
										  FeatureSceneRecognition<TD> recognizer,
										  BoofLambdas.Factory<PackedArray<TD>> factoryPackedDesc ) {
		this.detector = detector;
		this.asscociator = asscociator;
		this.recognizer = recognizer;
		descriptions = factoryPackedDesc.newInstance();

		tempDescription = detector.createDescription();

		sourceDescriptions = new DogArray<>(detector::createDescription);
		destinationDescriptions = new DogArray<>(detector::createDescription);

		sourcePixels = new DogArray<>(Point2D_F64::new);
		destinationPixels = new DogArray<>(Point2D_F64::new);

		// Source features are added first and there can only be a match if a set in the source exists
		asscociator.createNewSetsFromSource = true;
		asscociator.createNewSetsFromDestination = false;
	}

	/**
	 * Adds a new image. Must call {@link #fixate()} inorder for it to be retrieved later on
	 *
	 * @param id Unique ID for this image
	 * @param image The image
	 */
	public void addImage( String id, Image image ) {
		imageToIndex.put(id, imageIDs.size());
		imageIDs.add(id);

		// Detect the point features
		detector.detect(image);

		int N = detector.getNumberOfFeatures();

		// Record the first index and the number of features
		imageFeatureStartIndexes.add(descriptions.size());
		imageFeatureStartIndexes.add(N);

		// NOTE: Intentionally not pre-allocating and letting the data structure manage its memory
		//       This is to avoid needing to constantly increase the array size
		for (int i = 0; i < N; i++) {
			descriptions.append(detector.getDescription(i));
			pixels.append(detector.getLocation(i));
		}
	}

	/**
	 * After all the images you wish to look up have been added call this function. If you add more images after
	 * calling fixate you will need to call it again.
	 */
	public void fixate() {

		// Learn the model
		long time0 = System.nanoTime();
		if (relearnModel)
			learnModel();
		long time1 = System.nanoTime();
		timeFixateLearnMS = (time1 - time0)*1e-6;
		if (verbose != null) verbose.printf("fixate learning time: %.1f (ms)\n", timeFixateLearnMS);

		// Add images to the data base
		for (int imageIndex = 0; imageIndex < imageIDs.size(); imageIndex++) {
			recognizer.addImage(imageIDs.get(imageIndex), createFeaturesLambda(imageIndex));
		}
		long time2 = System.nanoTime();
		timeFixateAddMS = (time2 - time1)*1e-6;
		if (verbose != null) verbose.printf("fixate learning add: %.1f (ms)\n", timeFixateAddMS);
	}

	/**
	 * Creates an iterator from saved image features. Loads the pixels and descriptors as needed.
	 */
	private void learnModel() {
		recognizer.learnModel(new Iterator<>() {
			int index = 0;

			@Override public boolean hasNext() {
				return index*2 < imageFeatureStartIndexes.size;
			}

			@Override public FeatureSceneRecognition.Features<TD> next() {
				return createFeaturesLambda(index++);
			}
		});
	}

	@Override public List<String> getImageIDs() {
		return imageIDs;
	}

	@Override public void findSimilar( String target,
									   @Nullable BoofLambdas.Filter<String> filter,
									   List<String> similarImages ) {
		similarImages.clear();
		int imageIndex = imageToIndex.get(target);

		// clear paired info storage
		viewId_to_info.clear();
		pairInfo.reset();

		// Get the location of this image's features
		int targetFeatureOffset = imageFeatureStartIndexes.get(imageIndex*2);
		int targetFeatureSize = imageFeatureStartIndexes.get(imageIndex*2 + 1);

		// Look up similar images
		if (!recognizer.query(createFeaturesLambda(imageIndex), filter, limitMatchesConsider, sceneMatches)) {
			if (verbose != null) verbose.printf("image[%d] cbir found no matches\n", imageIndex);
			return;
		}

		if (verbose != null) verbose.printf("image[%d].cbir_matches.size=%d\n", imageIndex, sceneMatches.size);

		// Initialize association
		asscociator.initialize(recognizer.getTotalWords());

		// Load the target/source image features
		sourceDescriptions.reset();
		sourcePixels.reset();
		for (int i = 0; i < targetFeatureSize; i++) {
			TD desc = sourceDescriptions.grow();
			descriptions.getCopy(targetFeatureOffset + i, desc);
			pixels.getCopy(targetFeatureOffset + i, sourcePixels.grow());
			asscociator.addSource(desc, recognizer.getQueryWord(i));
		}

		// inspect all matches that the recognition algorithm found and see if anything of them look good
		for (int matchIndex = 0; matchIndex < sceneMatches.size; matchIndex++) {
			SceneRecognition.Match match = sceneMatches.get(matchIndex);

			int imageIndexMatch = imageToIndex.get(match.id);
			if (imageIndex == imageIndexMatch)
				continue;

			addDestFeaturesThenAssociate(imageIndexMatch);

			if (verbose != null) {
				verbose.printf("_ dst.size=%d associated[%d].size=%d",
						destinationPixels.size, imageIndexMatch, asscociator.getMatches().size);
			}

			if (!similarityTest.isSimilar(sourcePixels, destinationPixels, asscociator.getMatches())) {
				if (verbose != null) verbose.println();
				// Idea: Save PairInfo even if not matched to avoid checking again
				continue;
			}

			similarImages.add(match.id);

			// Copy results for later retrieval
			PairInfo info = pairInfo.grow();
			info.associated.copyAll(asscociator.getMatches().toList(), ( original, copy ) -> copy.setTo(original));
			viewId_to_info.put(match.id, info);

			if (verbose != null) verbose.println(" accepted");
		}
	}

	/**
	 * Adds features from the destination image then associate
	 */
	private void addDestFeaturesThenAssociate( int imageIndex ) {
		int destFeatureOffset = imageFeatureStartIndexes.get(imageIndex*2);
		int destFeatureSize = imageFeatureStartIndexes.get(imageIndex*2 + 1);

		// Purge features from previous matches
		asscociator.clearDestination();

		// Load the match/destination image features
		destinationDescriptions.reset();
		destinationPixels.reset();
		for (int featureIndex = 0; featureIndex < destFeatureSize; featureIndex++) {
			TD desc = destinationDescriptions.grow();
			descriptions.getCopy(destFeatureOffset + featureIndex, desc);
			pixels.getCopy(destFeatureOffset + featureIndex, destinationPixels.grow());
			asscociator.addDestination(desc, recognizer.lookupWord(desc));
		}

		// Associate the features together
		asscociator.associate();
	}

	@Override public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		// Look up which image is being requested
		int imageIndex = imageToIndex.get(target);
		if (imageIndex == -1)
			throw new IllegalArgumentException("Unknown view=" + target);

		// Look up where these features are stored
		int offset = imageFeatureStartIndexes.get(imageIndex*2);
		int size = imageFeatureStartIndexes.get(imageIndex*2 + 1);
		features.resize(size);

		// Copy the features in the output array
		for (int i = 0; i < size; i++) {
			pixels.getCopy(offset + i, features.get(i));
		}
	}

	@Override public boolean lookupAssociated( String viewDst, DogArray<AssociatedIndex> pairs ) {
		// clear the list so that nothing is returned if there is no match
		pairs.reset();

		// Lookup information about this match
		PairInfo info = viewId_to_info.get(viewDst);

		// The user probably made a mistake and is trying to get info from an image which wasn't similar
		if (info == null)
			throw new IllegalArgumentException("View is not similar " + viewDst);

		pairs.copyAll(info.associated.toList(), ( original, copy ) -> copy.setTo(original));

		return !pairs.isEmpty();
	}

	/**
	 * Looks up the word each feature belongs in for an image
	 */
	public void lookupImageWords( String imageID, DogArray_I32 words ) {
		words.reset();

		int imageIndex = imageToIndex.get(imageID);
		int offset = imageFeatureStartIndexes.get(imageIndex*2);
		int numFeatures = imageFeatureStartIndexes.get(imageIndex*2 + 1);

		for (int i = 0; i < numFeatures; i++) {
			words.add(recognizer.lookupWord(descriptions.getTemp(offset + i)));
		}
	}

	/**
	 * Accesses feature information directly from internal data structures.
	 */
	private FeatureSceneRecognition.Features<TD> createFeaturesLambda( int imageIndex ) {
		int offset = imageFeatureStartIndexes.get(imageIndex*2);
		int size = imageFeatureStartIndexes.get(imageIndex*2 + 1);

		return new FeatureSceneRecognition.Features<>() {
			@Override public Point2D_F64 getPixel( int index ) {
				pixels.getCopy(offset + index, tempPixel);
				return tempPixel;
			}

			@Override public TD getDescription( int index ) {
				descriptions.getCopy(offset + index, tempDescription);
				return tempDescription;
			}

			@Override public int size() {return size;}
		};
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> options ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, options, recognizer);
	}

	/**
	 * Describes the relationship between two images
	 */
	protected static class PairInfo {
		// Image feature index for features which were associated
		public DogArray<AssociatedIndex> associated = new DogArray<>(AssociatedIndex::new);

		public void reset() {
			associated.reset();
		}
	}

	/**
	 * Contains logic for deciding if two images are similar or not from associated features and their image
	 * coordinates.
	 */
	public interface SimilarityTest {
		boolean isSimilar( FastAccess<Point2D_F64> srcPixels,
						   FastAccess<Point2D_F64> dstPixels,
						   FastAccess<AssociatedIndex> matches );
	}
}
