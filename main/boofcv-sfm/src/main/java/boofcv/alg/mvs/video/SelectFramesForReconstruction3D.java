/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs.video;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.geo.robust.GenerateHomographyLinear;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import gnu.trove.impl.Constants;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
 *     <li>Provide: tracker, descriptor, associate, robust3D, robustH, compareFit</li>
 *     <li>Invoke {@link #initialize(int, int)}</li>
 *     <li>Pass each frame into {@link #next(ImageBase)}</li>
 *     <li>Get results from {@link #getSelectedFrames()}</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class SelectFramesForReconstruction3D<T extends ImageBase<T>> implements VerbosePrint {
	// TODO Jump over frames which are destroyed by image glare
	// TODO Specify maximum average feature translation. Smaller steps to improve tracking
	// TODO Score frames based on an estimate of blur. Try to select in focus frames

	/** Will attempt to skip over at most this number of bad frames. */
	public @Getter @Setter int maxFrameSkip = 5;

	/** Radius of the region used to compute the description. Might be ignored */
	public @Getter @Setter ConfigLength featureRadius = ConfigLength.fixed(10);

	/** A track is considered to not have moved if it's motion is less than this value */
	public @Getter @Setter double motionInlier = 2.0;

	/** If the difference in two values is more than or equal to this then it's significant */
	public @Getter @Setter double significantFraction = 0.1;

	/** Minimum number of features that must be available for an operation to be valid */
	public @Getter @Setter int minimumFeatures = 15;

	/** Frames which have been selected for output */
	private @Getter final DogArray_I32 selectedFrames = new DogArray_I32();

	/** Tracks features frame to frame */
	@Getter @Setter @Nullable PointTracker<T> tracker;

	/** Descriptors of tracks */
	final @Getter DescribeRegionPoint<T, TupleDesc_F64> descriptor;

	/** Associates tracks to each other using their descriptors */
	@Getter @Setter @Nullable AssociateDescription2D<TupleDesc_F64> associate;

	/** Used to see how well a 3D model fits the observations */
	@Getter @Setter @Nullable ModelMatcher<DMatrixRMaj, AssociatedPair> robust3D = null;

	/** Used to see how well a homography model fits the observations */
	@Getter @Setter @Nullable ModelMatcher<Homography2D_F64, AssociatedPair> robustH = null;

	/** Used to determine if 3D is significantly better than homography model */
	@Getter @Setter @Nullable CompareGeometricModels compareFit = null;

	//----------------------------- Internally used fields

	// Number of frames currently processed
	int frameNumber;

	// Frame number of the key frame
	int keyFrameNumber;
	// If true the next frame processed will be a key frame
	boolean forceKeyFrame;

	// Expected shape of input images
	int width, height;

	// Storage for active tracks from the tracker
	final List<PointTrack> activeTracks = new ArrayList<>();

	// Pairs of features between the current frame and key frame
	final DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);

	// used to do a quick check to see if there is no chance of it being a 3D scene
	@Getter @Setter ModelGenerator<Homography2D_F64, AssociatedPair> computeHomography =
			new GenerateHomographyLinear(true);
	// Storage for the computed homography
	final Homography2D_F64 foundHomography = new Homography2D_F64();

	// Storage for key frame
	final Frame keyFrame;
	// Storage for the current frame being processed
	final Frame currentFrame;

	@Nullable PrintStream verbose = null;

	public SelectFramesForReconstruction3D( DescribeRegionPoint<T, TupleDesc_F64> descriptor ) {
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
		BoofMiscOps.checkTrue(robust3D != null, "You must assign ransac3D a value");
		BoofMiscOps.checkTrue(robustH != null, "You must assign ransacH a value");
		BoofMiscOps.checkTrue(compareFit != null, "You must assign compareFit a value");

		this.width = width;
		this.height = height;
		tracker.reset();
		associate.initialize(width, height);
		forceKeyFrame = true;
		frameNumber = 0;
	}

	/**
	 * Process the next frame in the sequence
	 *
	 * @param image Image. All images are assumed to have the same shape
	 */
	public void next( T image ) {
		BoofMiscOps.checkEq(image.width, width, "Width does not match.");
		BoofMiscOps.checkEq(image.height, height, "Height does not match.");

		performTracking(image);

		copyTrackResultsIntoCurrentFrame(image);

		boolean saveImage = false;
		if (forceKeyFrame) {
			forceKeyFrame = false;
			saveImage = true;
		} else {
			createPairsWithKeyFrameTracking(keyFrame, currentFrame);

			if (!isSceneStatic() && !isSceneClearlyNot3D()) {
				saveImage = isScene3D();
			}

			if (!saveImage) {
				// TODO match using descriptors. If better matching with descriptors then save this frame as
				//      there were probably bad frames between
				// TODO Handle the number of skip frames hitting their limit
			}

			if (verbose != null) verbose.println("saveImage=" + saveImage);

			if (!saveImage) {
				// See if this should be forced into saving the keyframe because so many tracks have been lost
				if (pairs.size < keyFrame.size()/10)
					saveImage = true;
			}
		}

		// Save the frame and make it the new keyframe
		if (saveImage) {
			keyFrameNumber = frameNumber;
			selectedFrames.add(frameNumber);
			keyFrame.setTo(currentFrame);
			if (verbose != null) verbose.printf("key_frame: total=%d / %d\n", selectedFrames.size, frameNumber);
		}

		frameNumber++;
	}

	void performTracking( T frame ) {
		Objects.requireNonNull(tracker, "Need to specify tracker. Did you call initialize too?");
		tracker.process(frame);
		tracker.spawnTracks();
		activeTracks.clear();
		tracker.getAllTracks(activeTracks);
	}

	/**
	 * Copies results from image-to-image tracker into the 'currentFrame'
	 */
	void copyTrackResultsIntoCurrentFrame( T image ) {
		descriptor.setImage(image);

		// Compute the descriptor region size based in the input image size
		int featureRadius = this.featureRadius.computeI(Math.max(width, height));

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
	void createPairsWithKeyFrameTracking( Frame keyFrame, Frame current ) {
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
	boolean isSceneStatic() {
		// count the number of features which have moved
		double tol = motionInlier*motionInlier;
		int moved = 0;
		for (int pairIdx = 0; pairIdx < pairs.size; pairIdx++) {
			AssociatedPair p = pairs.get(pairIdx);
			if (p.p1.distance2(p.p2) > tol) {
				moved++;
			}
		}

		// Compute the ratio of moved vs stationary
		double ratio = moved/(double)pairs.size;

		if (verbose != null) verbose.printf("  Static: moved=%4d total=%d ratio=%f\n", moved, pairs.size, ratio);

		return ratio < significantFraction;
	}

	/**
	 * Quick check to see if a homography can describe the track motion. Does not attempt to remove outliers.
	 *
	 * @return true if this is clearly not a 3D change relative to the key frame.
	 */
	boolean isSceneClearlyNot3D() {
		// Not sure what to do if it fails. Returning true is that it will skip over the 3D check and associated
		// using descriptions will be run as this is likely to be a bad frame
		if (pairs.size < computeHomography.getMinimumPoints())
			return true;
		if (!computeHomography.generate(pairs.toList(), foundHomography))
			return true;

		double tol = motionInlier*motionInlier;
		int inliers = 0;

		var transformed = new Point2D_F64();
		for (int pairIdx = 0; pairIdx < pairs.size; pairIdx++) {
			AssociatedPair p = pairs.get(pairIdx);
			HomographyPointOps_F64.transform(foundHomography, p.p1, transformed);
			if (p.p2.distance2(transformed) <= tol) {
				inliers++;
			}
		}

		double ratio = 1.0 - inliers/(double)pairs.size;

		if (verbose != null)
			verbose.printf("  ClearlyNot3D: inliers=%4d total=%d ratio=%f\n", inliers, pairs.size, ratio);

		return ratio < significantFraction;
	}

	/**
	 * Checks to see if the scene is 3D by comparing the inliers from fundamental matrix vs homography.
	 *
	 * @return true if 3D
	 */
	boolean isScene3D() {
		Objects.requireNonNull(robust3D);
		Objects.requireNonNull(robustH);

		if (!robust3D.process(pairs.toList()))
			return false;
		double fit3D = robust3D.getFitQuality();
		if (robust3D.getMatchSet().size() < minimumFeatures)
			return false;

		if (!robustH.process(pairs.toList()))
			return false;
		double fitH = robustH.getFitQuality();

		if (verbose != null) verbose.printf("  Check3D: H=%4f 3D=%4f\n", Math.sqrt(fitH), Math.sqrt(fit3D));

		return Objects.requireNonNull(compareFit).isFitABetter(fit3D, fitH, significantFraction);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}

	/** Storage for feature and track information for a single image frame */
	class Frame {
		// Descriptions of features
		DogArray<TupleDesc_F64> descriptions = new DogArray<>(descriptor::createDescription);
		// Locations of features
		DogArray<Point2D_F64> locations = new DogArray<>(Point2D_F64::new);
		// Conversion from track ID to feature index
		TLongIntMap trackID_to_index = new TLongIntHashMap(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);

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

	/** Used to compare fit score from robust model matchers */
	public interface CompareGeometricModels {
		/**
		 * Determines if the first fit score is significantly better
		 *
		 * @param fitA Fit score from 3D model
		 * @param fitB Fit score from Homography model
		 * @param significantFraction How "similar" two values are to be similar. Fractional. 0.0 to 1.0.
		 * @return true if fitA is significantly better
		 */
		boolean isFitABetter( double fitA, double fitB, double significantFraction );
	}
}
