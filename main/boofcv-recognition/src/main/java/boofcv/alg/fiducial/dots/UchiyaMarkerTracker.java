/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.describe.llah.LlahDocument;
import boofcv.alg.feature.describe.llah.LlahOperations;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Detector and tracker for Uchiya Markers (a.k.a. Random Dot).
 *
 * All known targets are stored in the "global" dictionary. The documentID in global dictionary are persistent.
 * When documents are tracked between two frames they are assigned a temporary track ID. Tracking works by
 * taking the most recent observations and computing a new LLAH description from those. This allows the description
 * to change with a changing perspective.
 *
 * When a document is detected a homography is computed from the canonical coordinates (global or previous track)
 * to the current image pixels. This homography is then used to recompute the predicted location of all features,
 * even ones which were not observed. The new track LLAH description is computed from these predicted landmarks.
 *
 * @see boofcv.alg.feature.describe.llah.LlahOperations
 *
 * @author Peter Abeles
 */
public class UchiyaMarkerTracker<T extends ImageBase<T>> {

	// Storage the input image after it has been converted into a binary image
	@Getter GrayU8 binary = new GrayU8(1,1);

	// Used to find ellipses in the image
	@Getter InputToBinary<T> inputToBinary;
	@Getter BinaryEllipseDetectorPixel ellipseDetector;
	// Stores the "global" dictionary of documents
	@Getter LlahOperations llahOps;

	/** Threshold used to filter false positives. 0 to 1. higher the more strict */
	@Getter @Setter double landmarkThreshold = 0.2;
	/** Minimum number of hits a dot needs to a landmark to be considered a pair */
	@Getter @Setter int minDotHits = 5;
	/** Sets if tracking is turned on or not */
	@Getter @Setter boolean tracking = true;

	/** Print tracking and debugging messages */
	private @Getter @Setter PrintStream verbose = null;

	// Storage for the centers of observed ellipses
	List<Point2D_F64> detectedPoints = new ArrayList<>();
	// Storage for documents which have been lookd up
	List<LlahOperations.FoundDocument> foundDocs = new ArrayList<>();

	// List of tracks which were visible in the most recent frame
	@Getter FastQueue<Track> currentTracks = new FastQueue<>(Track::new);
	// Look up table that goes from global ID to Track
	TIntObjectHashMap<Track> globalId_to_track = new TIntObjectHashMap<>();
	// Lookup table from track ID to global ID
	TIntIntHashMap trackId_to_globalId = new TIntIntHashMap();
	// LLAH dictionary for tracks in the previous frame
	LlahOperations llahTrackingOps;

	// Used to compute homography
	Ransac<Homography2D_F64, AssociatedPair> ransac;
	FastQueue<AssociatedPair> ransacPairs = new FastQueue<>(AssociatedPair::new); // landmark -> dots

	/**
	 * Configures the tracker
	 */
	public UchiyaMarkerTracker(InputToBinary<T> inputToBinary,
							   BinaryEllipseDetectorPixel ellipseDetector,
							   LlahOperations llahOps,
							   Ransac<Homography2D_F64, AssociatedPair> ransac) {
		this.inputToBinary = inputToBinary;
		this.ellipseDetector = ellipseDetector;
		this.llahOps = llahOps;
		this.ransac = ransac;

		llahTrackingOps = new LlahOperations(llahOps.getNumberOfNeighborsN(),llahOps.getSizeOfCombinationM(),llahOps.getHasher());
	}

	/**
	 * Resets the track into its original state
	 */
	public void reset() {
		llahTrackingOps.clearDocuments();
		trackId_to_globalId.clear();
	}

	/**
	 * Detects and tracks dot patterns.
	 * @param input Input image. Not modifid.
	 */
	public void process( T input ) {
		inputToBinary.process(input,binary);
		ellipseDetector.process(binary);
		List<BinaryEllipseDetectorPixel.Found> foundEllipses = ellipseDetector.getFound();

		// Convert ellipses to points that LLAH understands
		detectedPoints.clear();
		for (int i = 0; i < foundEllipses.size(); i++) {
			// NOTE: These centers will not be the geometric centers. The geometric center could be found using
			//       tangent points and this would make it more accurate. Not sure it's worth the effort...
			detectedPoints.add(foundEllipses.get(i).ellipse.center);
		}

		if( verbose != null ) verbose.println("Uchiya detected dots total "+detectedPoints.size());

		performTracking(detectedPoints);
		performDetection(detectedPoints);
		setTrackDescriptionsAndID();

		// System.out.println("Found documents "+foundDocs.size());
//		for( var r : foundDocs ) {
//			System.out.println("Doc #"+r.document.documentID+" matched = "+r.countMatches()+" / "+r.document.landmarks.size);
//		}
	}

	/**
	 * Detects landmarks using their tracking definition.
	 */
	void performTracking( List<Point2D_F64> observations ) {
		// Discard the old tracks
		currentTracks.reset();
		globalId_to_track.clear();

		// See if any previously tracked markers are visible
		llahTrackingOps.lookupDocuments(observations, landmarkThreshold, foundDocs);

		// save the observations
		for( int i = 0; i < foundDocs.size(); i++ ) {
			LlahOperations.FoundDocument foundTrackDoc = foundDocs.get(i);
			Track track = currentTracks.grow();
			track.reset();
			if( fitHomographAndPredict(observations,foundTrackDoc,track) ) {
				// convert from track doc to dictionary doc ID
				int globalID = trackId_to_globalId.get(foundTrackDoc.document.documentID);
				track.globalDoc = llahOps.getDocuments().get(globalID);
				globalId_to_track.put(globalID,track);
				if( verbose != null ) verbose.println(" tracked doc "+globalID);
			} else {
				if( verbose != null ) verbose.println(" failed to fit homography while tracking");
				currentTracks.removeTail();
			}
		}
	}

	/**
	 * Detects landmarks using global dictionary. If a document is already being tracked it will be ignored
	 */
	void performDetection( List<Point2D_F64> observations ) {
		// Detect new markers from their definitions
		llahOps.lookupDocuments(observations, landmarkThreshold, foundDocs);

		// save the observations, but ignore previously detected markers
		for( int i = 0; i < foundDocs.size(); i++ ) {
			LlahOperations.FoundDocument foundDoc = foundDocs.get(i);
			if( globalId_to_track.containsKey(foundDoc.document.documentID))
				continue;

			Track track = currentTracks.grow();
			track.reset();
			if( fitHomographAndPredict(observations,foundDoc,track) ) {
				track.globalDoc = foundDoc.document;
				globalId_to_track.put(track.globalDoc.documentID,track);
				if( verbose != null ) verbose.println(" detected doc "+track.globalDoc.documentID);
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
		globalId_to_track.forEachEntry((globalID, track)-> {
			track.trackDoc = llahTrackingOps.createDocument(track.predicted.toList());
			trackId_to_globalId.put(track.trackDoc.documentID,globalID);
			return true;
		});
	}

	/**
	 * Robustly fit a homography to the observations and then use that to predict where all the
	 * corners should have appearred.
	 * @param doc observed corners on document
	 * @param track (Output) storage for results
	 * @return true is successful
	 */
	private boolean fitHomographAndPredict( List<Point2D_F64> observations, LlahOperations.FoundDocument doc, Track track ) {
		// Fit a homography to points
		if( !fitHomography(observations,doc) )
			return false;

		// Use the homography to estimate where the landmarks would have appeared
		track.doc_to_imagePixel.set(ransac.getModelParameters());
		track.predicted.resize(doc.document.landmarks.size);

		// Predict where all the observations shuld be based on the homography
		for (int landmarkIdx = 0; landmarkIdx < doc.document.landmarks.size; landmarkIdx++) {
			Point2D_F64 predictedPixel = track.predicted.get(landmarkIdx);
			HomographyPointOps_F64.transform(track.doc_to_imagePixel,
					doc.document.landmarks.get(landmarkIdx),predictedPixel);
		}

		return true;
	}

	/**
	 * Fits a homography from document coordinates to observed image pixels
	 * @param dots Dots seen in the image
	 * @param observed The matched document
	 * @return true if successful
	 */
	boolean fitHomography( List<Point2D_F64> dots , LlahOperations.FoundDocument observed ) {
		// create the ransac pairs
		ransacPairs.reset();
		for (int landmarkIdx = 0; landmarkIdx < observed.document.landmarks.size; landmarkIdx++) {
			final Point2D_F64 landmark = observed.document.landmarks.get(landmarkIdx);
			TIntObjectHashMap<LlahOperations.DotCount> dotToLandmark = observed.landmarkToDots.get(landmarkIdx);
			dotToLandmark.forEachEntry((key,dotCount)-> {
				if( dotCount.counts >= minDotHits) {
					ransacPairs.grow().set(landmark,dots.get(dotCount.dotIdx));
				}
				return true; // multiple matches to this landmark can be found
			});
		}
		if( ransacPairs.size < ransac.getMinimumSize() )
			return false;

		return ransac.process(ransacPairs.toList());
	}

	/**
	 * Contains information on a marker that's being tracked
	 */
	public static class Track
	{
		/** Reference to Tracking document */
		public LlahDocument trackDoc;
		/** Reference to the global document */
		public LlahDocument globalDoc;
		/** Found homography from landmark to image pixels */
		public final Homography2D_F64 doc_to_imagePixel = new Homography2D_F64();
		/** The location of each landmark predicted using the homography */
		public final FastQueue<Point2D_F64> predicted = new FastQueue<>(Point2D_F64::new);

		/** Resets to initial state */
		public void reset() {
			trackDoc=null;
			globalDoc =null;
			predicted.reset();
			doc_to_imagePixel.reset();
		}
	}
}
