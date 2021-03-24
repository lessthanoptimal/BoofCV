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

package boofcv.alg.scene;

import boofcv.abst.feature.associate.AssociateDescriptionHashSets;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.alg.sfm.structure.LookUpSimilarImages;
import boofcv.misc.BoofLambdas;
import boofcv.struct.PackedArray;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.PackedArrayPoint2D_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
public class SceneRecognitionSimilarImages<Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
		implements LookUpSimilarImages {
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
	@Getter @Setter SimilarityTest similarityTest;

	//========================== Image Information and Relationships
	// List of ID strings for each image
	final List<String> imageIDs = new ArrayList<>();
	// Mapping from image ID to image array index
	final TObjectIntMap<String> imageToIndex = new TObjectIntHashMap<>();
	// Dimension of each image
	final DogArray<ImageDimension> imageShapes = new DogArray<>(ImageDimension::new);
	// Mapping from image to list of image indexes its matched/paired with
	final DogArray<DogArray_I32> imageToPairIndexes = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);
	// List of PairInfo for all paired images
	final DogArray<PairInfo> pairedImages = new DogArray<>(PairInfo::new, PairInfo::reset);

	//========================== Image Feature Storage
	// A single large array for all image feature descriptions.
	final PackedArray<TD> descriptions;
	// A single large array for all image feature pixel coordinates
	final PackedArray<Point2D_F64> pixels = new PackedArrayPoint2D_F64();
	// Stores the location of image features in the packed array. interleaved (first index, number of features)
	final DogArray_I32 imageFeatureStartIndexes = new DogArray_I32();

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

	public SceneRecognitionSimilarImages( DetectDescribePoint<Image, TD> detector,
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
		if (relearnModel)
			learnModel();

		// Add images to the data base
		for (int imageIndex = 0; imageIndex < imageIDs.size(); imageIndex++) {
			recognizer.addImage(imageIDs.get(imageIndex), createFeaturesLambda(imageIndex));
		}
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

	@Override public void findSimilar( String target, List<String> similar ) {
		int imageIndex = imageToIndex.get(target);

		// Get the location of this image's features
		int targetFeatureOffset = imageFeatureStartIndexes.get(imageIndex*2);
		int targetFeatureSize = imageFeatureStartIndexes.get(imageIndex*2+1);

		// Look up similar images
		recognizer.query(createFeaturesLambda(imageIndex), limitMatchesConsider, sceneMatches);

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

		for (int matchIndex = 0; matchIndex < sceneMatches.size; matchIndex++) {
			SceneRecognition.Match match = sceneMatches.get(matchIndex);

			int imageIndexMatch = imageToIndex.get(matchIndex);
			if (imageIndex == imageIndexMatch)
				continue;

			// See if these two images have been matched already
			if (null != lookupPairInfo(imageIndex, imageIndexMatch))
				continue;

			addDestFeaturesThenAssociate(imageIndexMatch);

			if (!similarityTest.isSimilar(sourcePixels, destinationPixels, asscociator.getMatches())) {
				// Idea: Save PairInfo even if not matched to avoid checking again
				continue;
			}

			saveImagePairInfo(imageIndex, imageIndexMatch);

			// Save this result as being similar
			similar.add(match.id);
		}
	}

	/**
	 * Adds features from the destination image then associate
	 */
	private void addDestFeaturesThenAssociate( int imageIndex ) {
		int destFeatureOffset = imageFeatureStartIndexes.get(imageIndex*2);
		int destFeatureSize = imageFeatureStartIndexes.get(imageIndex*2+1);

		// Purge features from previous matches
		asscociator.clearDestination();

		// Load the match/destination image features
		sourceDescriptions.reset();
		sourcePixels.reset();
		for (int featureIndex = 0; featureIndex < destFeatureSize; featureIndex++) {
			TD desc = destinationDescriptions.grow();
			descriptions.getCopy(destFeatureOffset + featureIndex, desc);
			pixels.getCopy(destFeatureOffset + featureIndex, destinationPixels.grow());
			asscociator.addSource(desc, recognizer.lookupWord(desc));
		}

		// Associate the features together
		asscociator.associate();
	}

	/**
	 * Saves association information for these two images
	 */
	private void saveImagePairInfo( int imageIndexSrc, int imageIndexDst ) {
		// Save the reference from image index to PairInfo index
		imageToPairIndexes.get(imageIndexSrc).add(pairedImages.size);
		imageToPairIndexes.get(imageIndexDst).add(pairedImages.size);

		// Create the new pair info
		PairInfo p = pairedImages.grow();
		p.src = imageIndexSrc;
		p.dst = imageIndexDst;
		p.associated.copyAll(asscociator.getMatches().toList(),(s,d)->d.setTo(s));
	}

	@Override public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		// Look up which image is being requested
		int imageIndex = imageToIndex.get(target);

		// Look up where these features are stored
		int offset = imageFeatureStartIndexes.get(imageIndex*2);
		int size = imageFeatureStartIndexes.get(imageIndex*2 + 1);
		features.resize(size);

		// Copy the features in the output array
		for (int i = 0; i < size; i++) {
			pixels.getCopy(offset+i, features.get(i));
		}
	}

	@Override public boolean lookupMatches( String viewA, String viewB, DogArray<AssociatedIndex> pairs ) {
		// clear the list so that nothing is returned if there is no match
		pairs.reset();

		// Figure out which images we are dealing with
		int imageIndexA = imageToIndex.get(viewA);
		int imageIndexB = imageToIndex.get(viewB);

		// Look up the pair
		PairInfo info = lookupPairInfo(imageIndexA, imageIndexB);

		// There are no pair so return
		if (info==null)
			return false;

		// Copy the list of associated features into the output list
		pairs.copyAll(info.associated.toList(), (src,dst)->dst.setTo(src));
		return true;
	}

	@Override public void lookupShape( String target, ImageDimension shape ) {
		int imageIndex = imageToIndex.get(target);
		shape.setTo(imageShapes.get(imageIndex));
	}

	/**
	 * Finds the Pair info for the two images. If they are not paied then null is returned
	 */
	protected @Nullable PairInfo lookupPairInfo( int imageIndexA , int imageIndexB ) {
		DogArray_I32 matches = imageToPairIndexes.get(imageIndexA);
		for (int i = 0; i < matches.size; i++) {
			PairInfo pair = pairedImages.get(matches.get(i));
			if (pair.src != imageIndexA && pair.dst != imageIndexA)
				continue;
			if (pair.src != imageIndexB && pair.dst != imageIndexB)
				continue;

			return pair;
		}
		return null;
	}

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

	/**
	 * Describes the relationship between two images
	 */
	protected static class PairInfo {
		// Index of source image
		public int src;
		// Index of destination image
		public int dst;
		// Image feature index for features which were associated
		public DogArray<AssociatedIndex> associated = new DogArray<>(AssociatedIndex::new);

		public void reset() {
			src = -1;
			dst = -1;
			associated.reset();;
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
