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

package boofcv.alg.fiducial.dots;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.RefineEpipolar;
import boofcv.alg.feature.describe.llah.LlahDocument;
import boofcv.alg.feature.describe.llah.LlahOperations;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>Detector and tracker for Uchiya Markers (a.k.a. Random Dot) see [1].</p>
 *
 * <p>All known targets are stored in the "global" dictionary. The documentID in global dictionary are persistent.
 * When documents are tracked between two frames they are assigned a temporary track ID. Tracking works by
 * taking the most recent observations and computing a new LLAH description from those. This allows the description
 * to change with a changing perspective.</p>
 *
 * When a document is detected a homography is computed from the canonical coordinates (global or previous track)
 * to the current image pixels. This homography is then used to recompute the predicted location of all features,
 * even ones which were not observed. The new track LLAH description is computed from these predicted landmarks.
 *
 * <p>NOTE: See in code comments about attempts to speed up this tracker.</p>
 *
 * @author Peter Abeles
 * @see boofcv.alg.feature.describe.llah.LlahOperations
 *
 * <p>[1] Uchiyama, Hideaki, and Hideo Saito. "Random dot markers." 2011 IEEE Virtual Reality Conference. IEEE, 2011.</p>
 */
public class UchiyaMarkerTracker implements VerbosePrint {
	// Optimizing the tracker appears to be more difficult than initially thought. Below are some attempts that failed.
	//
	// 1) Combining llahOps and llahTrackingOps together and making detection/tracking into a single step.
	//    The combined detection/tracking step was faster, but track update was slower because it had to undo
	//    the modification to llahOps when removing the old documents. Net result was about the same.
	// 2) Looking up documents only using the feature hashcode and not the invariants since Uchiya doesn't need
	//    invariants.
	//    This ended up being slower. This could be because a hashmap was built instead of an inexpensive linked list
	//    when going from hash code to features.
	// 3) Simplifying image processing to use crude dots instead of refined ellipses would speed things up, but the
	//    bottle neck is detection, tracking, and update steps.
	//
	// Profiling didn't show any obvious easy to fix inefficient code.

	// Stores the "global" dictionary of documents
	@Getter @Setter LlahOperations llahOps;

	/** Threshold used to filter false positives documents. At least this many landmarks need to be seen. */
	@Getter @Setter int minLandmarkDoc = 8;
	/** Minimum number of hits a dot needs to a landmark to be considered a pair */
	@Getter @Setter int minDotHits = 5;
	/** Sets if tracking is turned on or not */
	@Getter @Setter boolean tracking = true;

	/** Print tracking and debugging messages */
	private @Nullable PrintStream verbose = null;

	// Storage for documents which have been lookd up
	List<LlahOperations.FoundDocument> foundDocs = new ArrayList<>();

	// List of tracks which were visible in the most recent frame
	@Getter DogArray<Track> currentTracks = new DogArray<>(Track::new, Track::reset);
	// Look up table that goes from global ID to Track
	TIntObjectHashMap<Track> globalId_to_track = new TIntObjectHashMap<>();
	// Lookup table from track ID to global ID
	TIntIntHashMap trackId_to_globalId = new TIntIntHashMap();
	// LLAH dictionary for tracks in the previous frame
	LlahOperations llahTrackingOps;

	// Internal profiling
	/** Time to track objects */
	@Getter double timeTrack;
	/** Time to detect objects */
	@Getter double timeDetect;
	/** Time to update track descriptions */
	@Getter double timeUpdate;

	// Estimate the homography with noise
	Ransac<Homography2D_F64, AssociatedPair> ransac;
	// Estimate from a batch of observations
	Estimate1ofEpipolar estimateHomography = FactoryMultiView.homographyTLS();
	// Non-linear refinement with mixel errors
	RefineEpipolar refineHomography = FactoryMultiView.homographyRefine(0.01, 50, EpipolarError.SAMPSON);
	DMatrixRMaj foundH = new DMatrixRMaj(3, 3);
	DMatrixRMaj refinedH = new DMatrixRMaj(3, 3);
	// landmark -> dots
	DogArray<AssociatedPair> ransacPairs = new DogArray<>(AssociatedPair::new);
	List<AssociatedPair> inlierPairs = new ArrayList<>(); // for refinement
	// which dots were given as input to RANSAC
	DogArray_I32 ransacDotIdx = new DogArray_I32();

	/**
	 * Configures the tracker
	 */
	public UchiyaMarkerTracker( LlahOperations llahOps,
								Ransac<Homography2D_F64, AssociatedPair> ransac ) {
		this.llahOps = llahOps;
		this.ransac = ransac;

		llahTrackingOps = new LlahOperations(llahOps.getNumberOfNeighborsN(), llahOps.getSizeOfCombinationM(), llahOps.getHasher());
	}

	/**
	 * Resets the track into its original state
	 */
	public void resetTracking() {
		llahTrackingOps.clearDocuments();
		trackId_to_globalId.clear();
		globalId_to_track.clear();
		ransac.reset();
	}

	/**
	 * Detects and tracks dot patterns.
	 *
	 * @param detectedDots Input image. Not modified.
	 */
	public void process( List<Point2D_F64> detectedDots ) {
		// Reset the tracker
		currentTracks.reset();
		globalId_to_track.clear();

		double nano0 = System.nanoTime();
		performTracking(detectedDots);
		double nano1 = System.nanoTime();
		performDetection(detectedDots);
		double nano2 = System.nanoTime();
		setTrackDescriptionsAndID();
		double nano3 = System.nanoTime();

		this.timeTrack = (nano1 - nano0)*1e-6;
		this.timeDetect = (nano2 - nano1)*1e-6;
		this.timeUpdate = (nano3 - nano2)*1e-6;
	}

	/**
	 * Detects landmarks using their tracking definition.
	 */
	void performTracking( List<Point2D_F64> detectedDots ) {
		// See if any previously tracked markers are visible
		llahTrackingOps.lookupDocuments(detectedDots, minLandmarkDoc, foundDocs);

		// save the observations
		for (int i = 0; i < foundDocs.size(); i++) {
			LlahOperations.FoundDocument foundTrackDoc = foundDocs.get(i);
			Track track = currentTracks.grow();
			if (fitHomographAndPredict(detectedDots, foundTrackDoc, track)) {
				// convert from track doc to dictionary doc ID
				int globalID = trackId_to_globalId.get(foundTrackDoc.document.documentID);
				track.globalDoc = llahOps.getDocuments().get(globalID);
				globalId_to_track.put(globalID, track);
				if (verbose != null) verbose.println(" tracked doc " + globalID);
			} else {
				if (verbose != null) verbose.println(" failed to fit homography while tracking");
				currentTracks.removeTail();
			}
		}
	}

	/**
	 * Detects landmarks using global dictionary. If a document is already being tracked it will be ignored
	 */
	void performDetection( List<Point2D_F64> detectedDots ) {
		// Detect new markers from their definitions
		llahOps.lookupDocuments(detectedDots, minLandmarkDoc, foundDocs);

		// save the observations, but ignore previously detected markers
		for (int i = 0; i < foundDocs.size(); i++) {
			LlahOperations.FoundDocument foundDoc = foundDocs.get(i);
			if (globalId_to_track.containsKey(foundDoc.document.documentID))
				continue;

			Track track = currentTracks.grow();
			track.globalDoc = foundDoc.document;
			if (fitHomographAndPredict(detectedDots, foundDoc, track)) {
				globalId_to_track.put(track.globalDoc.documentID, track);
				if (verbose != null) verbose.println(" detected doc " + track.globalDoc.documentID);
			} else {
				currentTracks.removeTail();
			}
		}
	}

	/**
	 * Updates the track descriptions based on the most recent predicted observations
	 */
	private void setTrackDescriptionsAndID() {
		// Compute new definitions for all tracks
		llahTrackingOps.clearDocuments();
		trackId_to_globalId.clear();
		globalId_to_track.forEachEntry(( globalID, track ) -> {
			track.trackDoc = llahTrackingOps.createDocument(track.predicted.toList());
			// copy global landmarks into track so that in the next iteration the homography will be correct
			track.trackDoc.landmarks.reset();
			track.trackDoc.landmarks.copyAll(track.globalDoc.landmarks.toList(), ( src, dst ) -> dst.setTo(src));
			trackId_to_globalId.put(track.trackDoc.documentID, globalID);
			return true;
		});
	}

	/**
	 * Robustly fit a homography to the observations and then use that to predict where all the
	 * corners should have appeared.
	 *
	 * @param doc observed corners on document
	 * @param track (Output) storage for results
	 * @return true is successful
	 */
	private boolean fitHomographAndPredict( List<Point2D_F64> detectedDots,
											LlahOperations.FoundDocument doc,
											Track track ) {
		// Fit a homography to points
		if (!fitHomography(detectedDots, doc))
			return false;

		// Create a list of used landmarks from the inlier set
		inlierPairs.clear();
		int N = ransac.getMatchSet().size();
		for (int i = 0; i < N; i++) {
			int inputIdx = ransac.getInputIndex(i);
			int dotIdx = ransacDotIdx.get(inputIdx);
			int landmarkIdx = doc.landmarkToDots.indexOf(dotIdx);
			track.observed.grow().setTo(detectedDots.get(dotIdx), landmarkIdx);
			inlierPairs.add(ransacPairs.get(inputIdx));
		}

		// Estimate using all the inliers by minimizing algebraic errors
		estimateHomography.process(inlierPairs, foundH);
		// Non-linear refinement of reprojection error
		refineHomography.fitModel(inlierPairs, foundH, refinedH);

		// Use the homography to estimate where the landmarks would have appeared
		UtilHomography_F64.convert(refinedH, track.doc_to_imagePixel);
		track.predicted.resize(doc.document.landmarks.size);

		// Predict where all the observations shuld be based on the homography
		for (int landmarkIdx = 0; landmarkIdx < doc.document.landmarks.size; landmarkIdx++) {
			Point2D_F64 predictedPixel = track.predicted.get(landmarkIdx);
			HomographyPointOps_F64.transform(track.doc_to_imagePixel,
					doc.document.landmarks.get(landmarkIdx), predictedPixel);
		}

		return true;
	}

	/**
	 * Fits a homography from document coordinates to observed image pixels
	 *
	 * @param dots Dots seen in the image
	 * @param observed The matched document
	 * @return true if successful
	 */
	boolean fitHomography( List<Point2D_F64> dots, LlahOperations.FoundDocument observed ) {
		// create the ransac pairs
		ransacPairs.reset();
		ransacDotIdx.reset();
		for (int landmarkIdx = 0; landmarkIdx < observed.document.landmarks.size; landmarkIdx++) {
			final Point2D_F64 landmark = observed.document.landmarks.get(landmarkIdx);
			int dotIdx = observed.landmarkToDots.get(landmarkIdx);
			if (dotIdx < 0)
				continue;
			ransacDotIdx.add(dotIdx);
			ransacPairs.grow().setTo(landmark, dots.get(dotIdx));
		}
		if (ransacPairs.size < ransac.getMinimumSize())
			return false;

		// Ransac needs to find an inlier set and the inlier set needs to be of sufficient size
		if (ransac.process(ransacPairs.toList())) {
			return ransac.getMatchSet().size() >= minLandmarkDoc;
		}
		return false;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}

	/**
	 * Contains information on a marker that's being tracked
	 */
	@SuppressWarnings("NullAway.Init")
	public static class Track {
		/** Reference to Tracking document */
		public LlahDocument trackDoc;
		/** Reference to the global document */
		public LlahDocument globalDoc;
		/** Found homography from landmark to image pixels */
		public final Homography2D_F64 doc_to_imagePixel = new Homography2D_F64();
		/** Pixel location of each landmark predicted using the homography */
		public final DogArray<Point2D_F64> predicted = new DogArray<>(Point2D_F64::new);
		/** Observed pixels with landmarks indexes */
		public final DogArray<PointIndex2D_F64> observed = new DogArray<>(PointIndex2D_F64::new);

		/** Resets to initial state */
		@SuppressWarnings("NullAway")
		public void reset() {
			trackDoc = null;
			globalDoc = null;
			predicted.reset();
			observed.reset();
			doc_to_imagePixel.reset();
		}
	}
}
