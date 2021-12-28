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
import boofcv.abst.feature.describe.DescribePoint;
import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.SceneRecognition;
import boofcv.abst.tracker.PointTrack;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.PackedArray;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
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
 * First track features sequentially, then use {@link boofcv.abst.scene.FeatureSceneRecognition} to identify
 * loops. Association results are saved and memory usage will grow approximately linearly with the number of
 * images.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SimilarImagesTrackThenMatch<Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
		extends SimilarImagesFromTracks<PointTrack> implements VerbosePrint {

	// TODO if matched frames have common tracks save those in the associations
	// TODO cache association results from recognition

	/**
	 * Minimum number of frames (by ID) away two frames need to be for loop closure logic to connect them
	 */
	public @Getter @Setter int minimumRecognizeDistance = 30;

	/**
	 * Limit on number of returned images made during a query. Sequential results are filtered and do not need
	 * to be compensated for.
	 */
	public @Getter @Setter int limitQuery = 5;

	/** Describes a point */
	@Getter @Setter DescribePoint<Image, TD> describer;

	/** Performs feature based association using 'words' from the recognizer */
	@Getter AssociateDescriptionHashSets<TD> associator;

	/** Looks up similar images and provides words for each image feature */
	@Getter FeatureSceneRecognition<TD> recognizer;

	/** If true it will learn the model used by the recognizer. If false you need to provide the model. */
	@Getter @Setter boolean learnModel = true;

	/** Logic used to decide if two images are similar to each other */
	@Getter @Setter SimilarImagesSceneRecognition.SimilarityTest similarityTest = new ImageSimilarityAssociatedRatio();

	// Storage for all the descriptions across all frames
	PackedArray<TD> descriptions;

	// List of indexes in 'description' where the descriptions for a frame start
	DogArray_I32 frameStartIndexes = new DogArray_I32();

	//========================= Internal Workspace
	// Storage for results when looking up matches
	final DogArray<SceneRecognition.Match> queryMatches = new DogArray<>(SceneRecognition.Match::new);

	// Temporary storage for a single feature description and pixel
	final TD tempDescription;

	// Descriptor with default value to use when stuff goes wrong
	final TD nullDescription;

	// Storage used for association
	final DogArray<TD> sourceDescriptions;
	final DogArray<Point2D_F64> sourcePixels;

	final DogArray<TD> destinationDescriptions;
	final DogArray<Point2D_F64> destinationPixels;

	// Information about similar images to a recent query
	final DogArray<PairInfo> pairInfo = new DogArray<>(PairInfo::new, PairInfo::reset);
	final Map<String, PairInfo> viewId_to_pairs = new HashMap<>();

	// If not null it will print verbose debugging info
	@Nullable PrintStream verbose;

	public SimilarImagesTrackThenMatch( DescribePoint<Image, TD> describer,
										AssociateDescriptionHashSets<TD> featureAssociator,
										FeatureSceneRecognition<TD> recognizer,
										BoofLambdas.Factory<PackedArray<TD>> factoryPackedDesc ) {
		super(t -> t.featureId, ( t, pixel ) -> pixel.setTo(t.pixel));
		this.describer = describer;
		this.associator = featureAssociator;
		this.recognizer = recognizer;
		descriptions = factoryPackedDesc.newInstance();

		tempDescription = describer.createDescription();
		nullDescription = describer.createDescription();

		sourceDescriptions = new DogArray<>(describer::createDescription);
		destinationDescriptions = new DogArray<>(describer::createDescription);

		sourcePixels = new DogArray<>(Point2D_F64::new);
		destinationPixels = new DogArray<>(Point2D_F64::new);

		// Source features are added first and there can only be a match if a set in the source exists
		featureAssociator.createNewSetsFromSource = true;
		featureAssociator.createNewSetsFromDestination = false;
	}

	/**
	 * Processes a frame. Updates the relationship between features using tracks and descriptors computed
	 * at the track's pixel coordinate
	 *
	 * @param image (Input) Image most recent image tin the sequence
	 * @param tracks (Input) List of active tracks visible in the current frame
	 * @param frameID (Input) Identifier for this image/frame
	 */
	public void processFrame( Image image, List<PointTrack> tracks, long frameID ) {
		super.processFrame(tracks, frameID);

		// Compute and save the feature description
		Frame frame = frames.getTail();

		describer.setImage(image);

		// save the index in 'featureDescriptions' that the first description is located at
		frameStartIndexes.add(descriptions.size());

		// Get pixel coordinates then compute the description
		Point2D_F64 pixel = new Point2D_F64();

		int numFeatures = frame.featureCount();
		for (int featIdx = 0; featIdx < numFeatures; featIdx++) {
			frame.getPixel(featIdx, pixel);
			if (!describer.process(pixel.x, pixel.y, tempDescription)) {
				// It couldn't compute the description, probably going outside the image
				// Not sure what else to do here. Skipping the feature will involve a lot of new code
				descriptions.append(nullDescription);
			} else {
				descriptions.append(tempDescription);
			}
		}

		// IDEA: If the model is known in advance, drop tracks if their "word" changes. That can be a hint that
		//       the track has drifted into a new object.
	}

	/**
	 * Call this function after it's done tracking. It will then attempt to connect disconnected frames to each other
	 */
	public void finishedTracking() {
		// If no model has been provided then learn a model
		if (learnModel) {
			recognizer.learnModel(new Iterator<>() {
				int frameIdx = 0;

				@Override public boolean hasNext() {return frameIdx < frameStartIndexes.size();}

				@Override public FeatureSceneRecognition.Features<TD> next() {
					return wrapFeatures(frameIdx++);
				}
			});
		}

		// Add images to the database
		recognizer.clearDatabase();
		for (int frameIdx = 0; frameIdx < frameStartIndexes.size; frameIdx++) {
			recognizer.addImage(frameIdx + "", wrapFeatures(frameIdx));
		}
	}

	@Override public void findSimilar( String target,
									   @Nullable BoofLambdas.Filter<String> filter,
									   List<String> similarImages ) {
		// discard old results
		viewId_to_pairs.clear();
		pairInfo.reset();

		// which frame we are doing with and sanity check it
		int frameIdx = frameToIndex(target);
		if (frameIdx < 0 || frameIdx >= frames.size)
			throw new IllegalArgumentException("Unknown target=" + target);

//		long time0 = System.nanoTime();

		// Look up results from the sequential tracker
		super.findSimilar(target, filter, similarImages);

//		long time1 = System.nanoTime();

		Frame frameTarget = frames.get(frameIdx);

		// Filter candidate results so that only frames it could possible connect to are considered
		BoofLambdas.Filter<String> queryFilter = filterQuery(filter, frameIdx, frameTarget);

		// Look up potential matches using the recognition algorithm while filtering results
		recognizer.query(wrapFeatures(frameIdx), queryFilter, limitQuery, queryMatches);

//		long time2 = System.nanoTime();

		// Set up feature association
		associator.initialize(recognizer.getTotalWords());
		loadFrameIntoSource(frameIdx);

		// Go through each potential match and see if it is really similar to the query image
		for (int queryIdx = 0; queryIdx < queryMatches.size; queryIdx++) {
			int matchedFrameIdx = Integer.parseInt(queryMatches.get(queryIdx).id);
			checkSimilarConnection(frameIdx, matchedFrameIdx, similarImages);
		}

//		long time3 = System.nanoTime();

//		System.out.printf("seq %.2f rec %.2f assoc %.2f\n",
//				(time1-time0)*1e-6, (time2-time1)*1e-6, (time3-time2)*1e-6);

		if (verbose != null)
			verbose.printf("query[%d] match.size=%d similar.size=%d\n", frameIdx, queryMatches.size, similarImages.size());
	}

	@Override public boolean lookupAssociated( String viewB, DogArray<AssociatedIndex> pairs ) {
		// Clear as required by the contract
		pairs.reset();

		// See if this view was associated using recognition
		PairInfo info = viewId_to_pairs.get(viewB);
		if (info != null) {
			pairs.copyAll(info.associated.toList(), ( original, copy ) -> copy.setTo(original));
			return true;
		}

		// See of the sequential tracker associated the two views
		return super.lookupAssociated(viewB, pairs);
	}

	/**
	 * Creates a filter which will preemptively remove invalid matches so that only distant valid matches are
	 * considered
	 */
	private BoofLambdas.Filter<String> filterQuery( @Nullable BoofLambdas.Filter<String> filter,
													int frameIdx, Frame frameTarget ) {
		return ( id ) -> {
			int matchedFrameIdx = Integer.parseInt(id);

			// if the result matches the target, skip it
			if (matchedFrameIdx == frameIdx)
				return false;

			Frame frameCandidate = frames.get(matchedFrameIdx);

			// See if the two frames are already connected. This needs to be done no matter what since
			// they could have been connected by an earlier frame in the loop
			if (frameTarget.isMatched(frameCandidate))
				return false;

			// Apply user provided filter
			if (filter != null && !filter.keep(id))
				return false;

			boolean checkConnection = false;
			if (Math.abs(matchedFrameIdx - frameIdx) < searchRadius) {
				// These were not connected by the sequential matcher, but they were within the search radius
				// that means there could have been an event that broke the sequential tracker but might not
				// break the feature matcher. E.g. abrupt lighting or short term motion blur
				checkConnection = true;
			} else if (Math.abs(matchedFrameIdx - frameIdx) >= minimumRecognizeDistance) {
				// They are far enough apart that it's advantageous to look for loop closures
				checkConnection = true;
			}

			return checkConnection;
		};
	}

	/**
	 * Checks to see if these two frames are similar and if so it will connect them.
	 *
	 * NOTE: We can't save the results here with the results from the sequential tracker since the filter
	 * could have changed since the last time similar was called.
	 */
	protected void checkSimilarConnection( int frameIdx, int matchedFrameIdx, List<String> similarImages ) {
		loadFrameIntoDestination(matchedFrameIdx);

		// Use the image descriptors to match features
		associator.associate();

		FastAccess<AssociatedIndex> matches = associator.getMatches();

		// See if the found feature matches pass a similarity test
		boolean similar = similarityTest.isSimilar(sourcePixels, destinationPixels, matches);

		if (!similar)
			return;

		if (verbose != null)
			verbose.printf("connecting %3d to %3d. matches.size=%d\n", frameIdx, matchedFrameIdx, matches.size);

		// The two frames are similar to each other
		String id = frames.get(matchedFrameIdx).frameID;
		similarImages.add(id);

		// Save associated features for later retrieval
		PairInfo info = pairInfo.grow();
		info.associated.copyAll(associator.getMatches().toList(), ( original, copy ) -> copy.setTo(original));
		viewId_to_pairs.put(id, info);
	}

	/**
	 * Loads descriptors in the specified frame into the source for association
	 */
	private void loadFrameIntoSource( int frameIdx ) {
		Frame frame = frames.get(frameIdx);

		int featureOffset = frameStartIndexes.get(frameIdx);
		int numberOfFeatures = frame.featureCount();

		// Load the target/source image features
		sourceDescriptions.reset();
		sourcePixels.reset();
		for (int i = 0; i < numberOfFeatures; i++) {
			// Copy description and pixel coordinate into their array
			TD desc = sourceDescriptions.grow();
			descriptions.getCopy(featureOffset + i, desc);
			frame.getPixel(i, sourcePixels.grow());

			// Add this feature to the associator
			associator.addSource(desc, recognizer.lookupWord(desc));
		}
	}

	/**
	 * Loads descriptors in the specified frame into the destination for association
	 */
	private void loadFrameIntoDestination( int frameIdx ) {
		Frame frame = frames.get(frameIdx);

		int featureOffset = frameStartIndexes.get(frameIdx);
		int numberOfFeatures = frame.featureCount();

		// Load the target/source image features
		destinationDescriptions.reset();
		destinationPixels.reset();
		associator.clearDestination();
		for (int i = 0; i < numberOfFeatures; i++) {
			// Copy description and pixel coordinate into their array
			TD desc = destinationDescriptions.grow();
			descriptions.getCopy(featureOffset + i, desc);
			frame.getPixel(i, destinationPixels.grow());

			// Add this feature to the associator
			associator.addDestination(desc, recognizer.lookupWord(desc));
		}
	}

	/**
	 * Wraps features in the specified frame so that the scene recognition can access them
	 */
	private FeatureSceneRecognition.Features<TD> wrapFeatures( int frameIdx ) {
		int offset = frameStartIndexes.get(frameIdx);
		return new FeatureSceneRecognition.Features<>() {
			// Storage for the feature's pixel coordinate in the image
			final Point2D_F64 pixel = new Point2D_F64();

			// Reference to the image/frame in question
			final Frame frame = frames.get(frameIdx);

			@Override public Point2D_F64 getPixel( int index ) {
				frame.getPixel(index, pixel);
				return pixel;
			}

			@Override public TD getDescription( int index ) {
				return descriptions.getTemp(offset + index);
			}

			@Override public int size() {
				return frame.featureCount();
			}
		};
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

	/** Converts the frame ID into the frame index. It's just an integer string */
	private int frameToIndex( String id ) {
		return Integer.parseInt(id);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> options ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, options, recognizer);
	}
}
