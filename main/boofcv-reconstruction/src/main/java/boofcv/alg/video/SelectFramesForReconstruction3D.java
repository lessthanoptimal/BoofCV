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

package boofcv.alg.video;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.structure.EpipolarScore3D;
import boofcv.factory.structure.ConfigSelectFrames3D;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import gnu.trove.impl.Constants;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.*;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Processes all the frames in a video sequence and decides which frames to keep for 3D reconstruction. The idea
 * here is that file size can be reduced significantly if redundant information is removed.
 *
 * Internally it works by having a key frame that the current frame is compared against. First it checks to see
 * if the image tracker shows any significant motion with its features. If not then this image is considered redundant.
 * Next it computes a homography without doing any robustness filtering, if that describes the relationship well
 * then there's no chance if it being 3d. Then if those tests are inconclusive it will robustly fit a homography
 * and fundamental matrix and compare their fit scores to decide if there's significant 3D structure.
 *
 * Instructions:
 * <ol>
 *     <li>Provide: tracker, descriptor, associate, score3D</li>
 *     <li>Invoke {@link #initialize(int, int)}</li>
 *     <li>Pass each frame into {@link #next(ImageBase)}</li>
 *     <li>Get results from {@link #getSelectedFrames()}</li>
 * </ol>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SelectFramesForReconstruction3D<T extends ImageBase<T>> implements VerbosePrint {
	// TODO Jump over frames which are destroyed by image glare
	// TODO Score frames based on an estimate of blur. Try to select in focus frames
	// TODO If way too many tracks have been dropped go back and select a new key frame that's in the past
	// TODO Plan a path that minimizes pointless jitter. Maybe a new algorithm completely. Local window?

	/**
	 * Returns the configuration with tuning parameters used. Note that settings for inner classes, e.g.
	 * association and model matchers, are ignored. While not elegant, this config design avoids duplicate
	 * code and comments.
	 */
	public @Getter final ConfigSelectFrames3D config = new ConfigSelectFrames3D();

	/** Output: Frames which have been selected to be saved. */
	private @Getter final DogArray_I32 selectedFrames = new DogArray_I32();

	/** Tracks features frame to frame */
	@Getter @Setter PointTracker<T> tracker;

	/** Descriptors of tracks */
	final @Getter DescribePointRadiusAngle<T, TupleDesc_F64> descriptor;

	/** Associates tracks to each other using their descriptors */
	@Getter @Setter AssociateDescription2D<TupleDesc_F64> associate;

	@Getter @Setter EpipolarScore3D scorer;

	/** Indexes of pairs which are considered to be inliers for the dominant model */
	public @Getter final DogArray_I32 inlierIdx = new DogArray_I32();

	//----------------------------- Access to intermediate results

	/** Estimated number of pixels that the image moved. If not computed it will be NaN */
	private @Getter double frameMotion;
	/** If it considered that new image could be 3D relative to the keyframe */
	private @Getter boolean considered3D;
	/** True if there are too few pairs with the key frame to reliably estimate scene structure */
	private @Getter boolean sufficientFeaturePairs;
	/** Why it requested a frame be saved */
	private @Getter Cause cause = Cause.UNKNOWN;

	//----------------------------- Internally used fields

	// Guessed intrinsic parameters
	CameraPinholeBrown cameraA = new CameraPinholeBrown();
	CameraPinholeBrown cameraB = new CameraPinholeBrown();

	DMatrixRMaj fundamental = new DMatrixRMaj(3, 3);

	// Number of frames currently processed
	int frameNumber;

	// Frame number of the key frame
	int keyFrameNumber;
	// If true the next frame processed will be a key frame
	boolean forceKeyFrame;

	// Expected shape of input images
	int width, height;

	// Storage for active tracks from the tracker
	@Getter final List<PointTrack> activeTracks = new ArrayList<>();

	// Pairs of features between the current frame and key frame
	@Getter final DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);

	// Storage for key frame
	final Frame keyFrame;
	// Storage for the current frame being processed
	final Frame currentFrame;

	// Storage for distances between tracks in each frame
	final DogArray_F64 distances = new DogArray_F64();

	@Nullable PrintStream verbose = null;

	public SelectFramesForReconstruction3D( DescribePointRadiusAngle<T, TupleDesc_F64> descriptor ) {
		this.descriptor = descriptor;
		this.keyFrame = new Frame();
		this.currentFrame = new Frame();
	}

	/**
	 * Must be called first and initializes data structures
	 *
	 * @param width Image width
	 * @param height Image height
	 */
	public void initialize( int width, int height ) {
		// Make sure everything has been properly configured
		BoofMiscOps.checkTrue(tracker != null, "You must assign tracker a value");
		BoofMiscOps.checkTrue(associate != null, "You must assign associate a value");
		BoofMiscOps.checkTrue(scorer != null, "You must assign scorer a value");

		this.width = width;
		this.height = height;
		tracker.reset();
		associate.initialize(width, height);
		forceKeyFrame = true;
		frameNumber = 0;
		selectedFrames.reset();

		PerspectiveOps.createIntrinsic(width, height, 90, cameraA);
		PerspectiveOps.createIntrinsic(width, height, 90, cameraB);
	}

	/**
	 * Process the next frame in the sequence
	 *
	 * @param image Image. All images are assumed to have the same shape
	 * @return true if it decided to add a new keyframe. Might not be the current frame.
	 */
	public boolean next( T image ) {
		BoofMiscOps.checkEq(image.width, width, "Width does not match.");
		BoofMiscOps.checkEq(image.height, height, "Height does not match.");
		frameMotion = Double.NaN;
		considered3D = false;
		sufficientFeaturePairs = false;
		cause = Cause.UNKNOWN;

		performTracking(image);

		copyTrackResultsIntoCurrentFrame(image);

		boolean saveImage = false;
		escape:
		if (forceKeyFrame) {
			cause = Cause.FORCED;
			forceKeyFrame = false;
			saveImage = true;
		} else {
			createPairsWithKeyFrameTracking(keyFrame, currentFrame);

			if (pairs.size < config.minimumPairs) {
				cause = Cause.TRACKING_FAILURE;
				// Almost all tracks got dropped. Could have been caused by something like a temporary obstruction
				// or motion blur. Save the image and start tracking again
				saveImage = true;
				if (verbose != null)
					verbose.printf("Too few pairs. pairs.size=%d, key.size=%d current.size=%d\n",
							pairs.size, keyFrame.size(), currentFrame.size());
				break escape;
			}
			// Note that there were enough feature pairs
			sufficientFeaturePairs = true;

			// Compute how much the frame moved
			frameMotion = computeFrameRelativeMotion();
			double minMotionThresh = config.minTranslation.computeI(Math.max(width, height));
			double maxMotionThresh = config.maxTranslation.computeI(Math.max(width, height));

			if (verbose != null) verbose.printf("_ Motion: distance=%.1f min=%.1f max=%.1f\n",
					frameMotion, minMotionThresh, maxMotionThresh);

			if (frameMotion >= minMotionThresh) {
				if (frameMotion > maxMotionThresh) {
					cause = Cause.EXCESSIVE_MOTION;
					saveImage = true;
				} else if (!isSceneStatic()) {
					saveImage = isScene3D();
					considered3D = saveImage;
					if (saveImage) {
						cause = Cause.TRANSLATION_3D;
					} else {
						saveImage = checkSkippedBadFrame();
					}
				}
			}
		}

		if (verbose != null) verbose.println("saveImage=" + saveImage + " cause=" + cause);

		// Save the frame and make it the new keyframe
		if (saveImage) {
			keyFrameNumber = frameNumber;
			selectedFrames.add(frameNumber);
			keyFrame.setTo(currentFrame);
			requireNonNull(associate).setSource(keyFrame.locations, keyFrame.descriptions);
			if (verbose != null) verbose.printf("key_frame: total=%d / %d\n", selectedFrames.size, frameNumber);
		}

		frameNumber++;

		return saveImage;
	}

	/**
	 * Retrieves the locations of tracks in the current frame which were visible in the key frame
	 *
	 * @param tracks (Output) Storage for track locations
	 */
	public void lookupKeyFrameTracksInCurrentFrame( DogArray<Point2D_F64> tracks ) {
		tracks.reset();
		tracks.resize(pairs.size);

		for (int i = 0; i < pairs.size; i++) {
			tracks.get(i).setTo(pairs.get(i).p1);
		}
	}

	protected void performTracking( T frame ) {
		requireNonNull(tracker, "Need to specify tracker. Did you call initialize too?");
		tracker.process(frame);
		tracker.spawnTracks();
		tracker.getAllTracks(activeTracks);
	}

	/**
	 * Copies results from image-to-image tracker into the 'currentFrame'
	 */
	protected void copyTrackResultsIntoCurrentFrame( T image ) {
		descriptor.setImage(image);

		// Compute the descriptor region size based in the input image size
		int featureRadius = config.featureRadius.computeI(Math.max(width, height));

		// Extract feature and track information from the current frame
		currentFrame.reserve(activeTracks.size());
		for (int i = 0; i < activeTracks.size(); i++) {
			PointTrack t = activeTracks.get(i);
			currentFrame.trackID_to_index.put(t.featureId, i);
			currentFrame.locations.grow().setTo(t.pixel);
			descriptor.process(t.pixel.x, t.pixel.y, 0.0, featureRadius, currentFrame.descriptions.grow());
		}

		if (verbose != null) verbose.printf("current_frame: tracks=%4d frame=%d\n", activeTracks.size(), frameNumber);
	}

	/**
	 * Create a set of image pairs between the key frame and the current frame.
	 */
	protected void createPairsWithKeyFrameTracking( Frame keyFrame, Frame current ) {
		pairs.reset();
		pairs.reserve(keyFrame.size());

		keyFrame.trackID_to_index.forEachEntry(( trackID, prevIdx ) -> {
			int currIdx = current.trackID_to_index.get(trackID);
			if (currIdx < 0)
				return true;

			AssociatedPair p = pairs.grow();
			p.p1.setTo(current.locations.get(currIdx));
			p.p2.setTo(keyFrame.locations.get(prevIdx));

			return true;
		});
	}

	/**
	 * Checks to see if the scene is nearly identical by the number of features which have barely moved
	 *
	 * @return true if it's nearly static
	 */
	protected boolean isSceneStatic() {
		// count the number of features which have moved
		double tol = config.motionInlierPx*config.motionInlierPx;
		int moved = 0;
		for (int pairIdx = 0; pairIdx < pairs.size; pairIdx++) {
			AssociatedPair p = pairs.get(pairIdx);
			if (p.p1.distance2(p.p2) > tol) {
				moved++;
			}
		}

		// Compute the ratio of moved vs stationary
		double ratio = moved/(double)pairs.size;

		if (verbose != null) verbose.printf("_ Static: moved=%4d total=%d ratio=%f\n", moved, pairs.size, ratio);

		return ratio < config.thresholdQuick;
	}

	/**
	 * Checks to see if the scene is 3D by comparing the inliers from fundamental matrix vs homography.
	 *
	 * @return true if 3D
	 */
	boolean isScene3D() {
		scorer.process(cameraA, cameraB, keyFrame.size(), currentFrame.size(), pairs.toList(), fundamental, inlierIdx);
		return scorer.is3D();
	}

	/**
	 * If tracks are dropped and association does much better that can indicate there was some even (like glare)
	 * which caused the tracks to drop but is now gone. In that case save this frame and skip over the bad stuff
	 */
	protected boolean checkSkippedBadFrame() {
		if (config.skipEvidenceRatio < 1.0)
			return false;
		requireNonNull(associate);

		// the source was set as the keyframe when it became the keyframe. Sometimes this saves computations.
		associate.setDestination(currentFrame.locations, currentFrame.descriptions);
		associate.associate();

		FastAccess<AssociatedIndex> matches = associate.getMatches();
		boolean skip = matches.size > pairs.size*config.skipEvidenceRatio;

		if (verbose != null)
			verbose.printf("_ Skip: associated=%d tracking=%d skip=%s\n", matches.size, pairs.size, skip);

		return skip;
	}

	/**
	 * Has there been so much motion that we should save the image anyways? If too many frames are skipped
	 * the reconstruction can be bad or incomplete
	 */
	protected double computeFrameRelativeMotion() {
		distances.reset();
		distances.resize(pairs.size);

		for (int pairIdx = 0; pairIdx < pairs.size; pairIdx++) {
			distances.set(pairIdx, pairs.get(pairIdx).distance2());
		}

		double distance2 = QuickSelect.select(distances.data, (int)(distances.size*0.9), distances.size);
		return Math.sqrt(distance2);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, scorer);
	}

	/** Storage for feature and track information for a single image frame */
	public class Frame {
		// Descriptions of features
		public final DogArray<TupleDesc_F64> descriptions = new DogArray<>(descriptor::createDescription);
		// Locations of features
		public final DogArray<Point2D_F64> locations = new DogArray<>(Point2D_F64::new);
		// Conversion from track ID to feature index
		public final TLongIntMap trackID_to_index = new TLongIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);

		public int size() {
			return descriptions.size;
		}

		public void reserve( int size ) {
			reset();
			descriptions.reserve(size);
			locations.reserve(size);
		}

		public void reset() {
			descriptions.reset();
			locations.reset();
			trackID_to_index.clear();
		}

		public void setTo( Frame frame ) {
			reserve(frame.size());
			for (int i = 0; i < frame.size(); i++) {
				descriptions.grow().setTo(frame.descriptions.get(i));
				locations.grow().setTo(frame.locations.get(i));
			}
			frame.trackID_to_index.forEachEntry(( key, value ) -> {
				trackID_to_index.put(key, value);
				return true;
			});
		}
	}

	/**
	 * What caused to to request a frame be saved
	 */
	public enum Cause {
		FORCED,
		TRACKING_FAILURE,
		EXCESSIVE_MOTION,
		TRANSLATION_3D,
		UNKNOWN
	}
}
