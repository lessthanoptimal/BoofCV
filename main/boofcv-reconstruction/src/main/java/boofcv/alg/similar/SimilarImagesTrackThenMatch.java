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
import boofcv.abst.tracker.PointTracker;
import boofcv.misc.BoofLambdas;
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
import java.util.Iterator;
import java.util.Set;

/**
 * First track features sequentially, then use {@link boofcv.abst.scene.FeatureSceneRecognition} to identify
 * loops.
 *
 * @author Peter Abeles
 */
public class SimilarImagesTrackThenMatch<Image extends ImageBase<Image>, TD extends TupleDesc<TD>>
		extends SimilarImagesPointTracker implements VerbosePrint {

	// TODO if matched frames have common tracks save those in the associations

	/**
	 * Minimum number of frames (by ID) away two frames need to be for loop closure logic to connect them
	 */
	public @Getter @Setter int minimumRecognizeDistance = 30;

	/**
	 * Limit how many images it will consider in the query when looking to find loops. If a search radius
	 * is specified then 2*radius+1 added to this number as a way to ensure that known sequential
	 * matches don't prevent loop closure..
	 */
	public @Getter @Setter int limitQuery = 30;

	/** Describes a point */
	@Getter @Setter DescribePoint<Image, TD> describer;

	/** Performs feature based association using 'words' from the recognizer */
	@Getter AssociateDescriptionHashSets<TD> featureAssociator;

	/** Looks up similar images and provides words for each image feature */
	@Getter FeatureSceneRecognition<TD> recognizer;

	/** If true it will learn the model used by the recognizer. If false you need to provide the model. */
	@Getter @Setter boolean learnModel = true;

	/** Logic used to decide if two images are similar to each other */
	@Getter @Setter SimilarImagesSceneRecognition.SimilarityTest similarityTest = new ImageSimilarityAssociatedRatio();

	PackedArray<TD> descriptions;
	DogArray_I32 frameStartIndexes = new DogArray_I32();

	//========================= Internal Workspace
	// Storage for results when looking up matches
	final DogArray<SceneRecognition.Match> queryMatches = new DogArray<>(SceneRecognition.Match::new);

	// Temporary storage for a single feature description and pixel
	final TD tempDescription;

	// Descriptor with default value to use when stuff goes worng
	final TD nullDescription;

	// Storage used for association
	final DogArray<TD> sourceDescriptions;
	final DogArray<Point2D_F64> sourcePixels;

	final DogArray<TD> destinationDescriptions;
	final DogArray<Point2D_F64> destinationPixels;

	// If not null it will print verbose debugging info
	PrintStream verbose;

	public SimilarImagesTrackThenMatch( DescribePoint<Image, TD> describer,
										AssociateDescriptionHashSets<TD> featureAssociator,
										FeatureSceneRecognition<TD> recognizer,
										BoofLambdas.Factory<PackedArray<TD>> factoryPackedDesc ) {
		this.describer = describer;
		this.featureAssociator = featureAssociator;
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

	public void processFrame( Image image, PointTracker<?> tracker ) {
		super.processFrame(tracker);

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

		searchForMatchingFrames();
	}

	/**
	 * Use the recognizer to search for frames that match but were not
	 */
	private void searchForMatchingFrames() {
		// save so that we know how many frames got matched
		int priorConnectedFrames = connections.size;

		// Handle the case where the searchRadius is infinite and has been set to a negative value
		int searchRadius = Math.max(0, this.searchRadius);

		// Total connections considered
		int totalConsidered = 0;

		// Search for loops at every frame
		for (int frameIdx = 0; frameIdx < frameStartIndexes.size; frameIdx++) {
			recognizer.query(wrapFeatures(frameIdx), 2*searchRadius + 1 + limitQuery, queryMatches);

			featureAssociator.initialize(recognizer.getTotalWords());
			loadFrameIntoSource(frameIdx);

			Frame frameTarget = frames.get(frameIdx);

			if (verbose != null)
				verbose.printf("query[%d] match.size=%d\n", frameIdx, queryMatches.size);

			for (int queryIdx = 0; queryIdx < queryMatches.size; queryIdx++) {
				int matchedFrameIdx = Integer.parseInt(queryMatches.get(queryIdx).id);

				// see if it matches itself
				if (matchedFrameIdx == frameIdx)
					continue;

				Frame frameCandidate = frames.get(matchedFrameIdx);

				// See if the two frames are already connected. This needs to be done no matter what since
				// they could have been connected by an earlier frame in the loop
				if (frameTarget.isMatched(frameCandidate))
					continue;

				// See if these two frames could have been paired up by the sequential algorithm
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

				if (!checkConnection)
					continue;

				totalConsidered++;
				attemptToConnectFrames(frameIdx, matchedFrameIdx);
			}
		}

		if (verbose != null)
			verbose.printf("Recognition created %d connections. Considered %d\n",
					(connections.size - priorConnectedFrames), totalConsidered);
	}

	/**
	 * Checks to see if these two frames are similar and if so it will connect them
	 */
	protected void attemptToConnectFrames( int frameIdx, int matchedFrameIdx ) {
		loadFrameIntoDestination(matchedFrameIdx);

		// Use the image descriptors to match features
		featureAssociator.associate();

		FastAccess<AssociatedIndex> matches = featureAssociator.getMatches();

		// See if the found feature matches pass a similarity test
		boolean similar = similarityTest.isSimilar(sourcePixels, destinationPixels, matches);

		if (!similar)
			return;

		if (verbose != null)
			verbose.printf("connecting %3d to %3d. matches.size=%d\n", frameIdx, matchedFrameIdx, matches.size);

		// Connect the two frames using the matched features
		connectFrames(frames.get(frameIdx), frames.get(matchedFrameIdx), matches);
	}

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
			featureAssociator.addSource(desc, recognizer.lookupWord(desc));
		}
	}

	private void loadFrameIntoDestination( int frameIdx ) {
		Frame frame = frames.get(frameIdx);

		int featureOffset = frameStartIndexes.get(frameIdx);
		int numberOfFeatures = frame.featureCount();

		// Load the target/source image features
		destinationDescriptions.reset();
		destinationPixels.reset();
		featureAssociator.clearDestination();
		for (int i = 0; i < numberOfFeatures; i++) {
			// Copy description and pixel coordinate into their array
			TD desc = destinationDescriptions.grow();
			descriptions.getCopy(featureOffset + i, desc);
			frame.getPixel(i, destinationPixels.grow());

			// Add this feature to the associator
			featureAssociator.addDestination(desc, recognizer.lookupWord(desc));
		}
	}

	private FeatureSceneRecognition.Features<TD> wrapFeatures( int frameIdx ) {
		int offset = frameStartIndexes.get(frameIdx);
		return new FeatureSceneRecognition.Features<>() {
			final Point2D_F64 pixel = new Point2D_F64();
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

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> options ) {
		this.verbose = out;
	}
}
