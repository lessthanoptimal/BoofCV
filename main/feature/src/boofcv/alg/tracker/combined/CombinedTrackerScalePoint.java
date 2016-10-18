/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.combined;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * <p>
 * Combines a KLT tracker with Detect-Describe-Associate type trackers.  Features are nominally tracked
 * using KLT, but after KLT drops a track it is deactivated and made dormant.  Upon request, it will
 * attempt to reactivate a dormant track by associating it with newly detected features.  After a
 * track has been reactivated it will be tracked normally.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO Two versions.  One for InterestPointDetector and one for corners
public class CombinedTrackerScalePoint
		<I extends ImageGray, D extends ImageGray, TD extends TupleDesc> {

	// current image in sequence
	private I input;

	// The KLT tracker used to perform the nominal track update
	protected PyramidKltForCombined<I,D> trackerKlt;

	// feature detector and describer
	protected DetectDescribePoint<I,TD> detector;
	// Used to associate features using their DDA description
	protected AssociateDescription<TD> associate;

	// all active tracks that have been tracked purely by KLT
	protected List<CombinedTrack<TD>> tracksPureKlt = new ArrayList<>();
	// tracks that had been dropped by KLT but have been reactivated
	protected List<CombinedTrack<TD>> tracksReactivated = new ArrayList<>();
	// tracks that are not actively being tracked
	protected List<CombinedTrack<TD>> tracksDormant = new ArrayList<>();
	// recently spawned tracks
	protected List<CombinedTrack<TD>> tracksSpawned = new ArrayList<>();
	// track points whose data is to be reused
	protected Stack<CombinedTrack<TD>> tracksUnused = new Stack<>();

	// local storage used by association
	protected FastQueue<TD> detectedDesc;
	protected FastQueue<TD> knownDesc;

	// number of tracks it has created
	protected long totalTracks = 0;

	// Marks a known track as being associated
	private boolean associated[] = new boolean[1];

	/**
	 * Configures tracker
	 *
	 * @param trackerKlt KLT tracker used nominally
	 * @param detector Feature detector
	 * @param associate Association algorithm
	 */
	public CombinedTrackerScalePoint(PyramidKltForCombined<I, D> trackerKlt,
									 DetectDescribePoint<I,TD> detector,
									 AssociateDescription<TD> associate ) {
		this.trackerKlt = trackerKlt;
		this.detector = detector;

		detectedDesc = new FastQueue<>(10, detector.getDescriptionType(), false);
		knownDesc = new FastQueue<>(10, detector.getDescriptionType(), false);

		this.associate = associate;
	}

	/**
	 * Used for unit tests
	 */
	protected CombinedTrackerScalePoint() {
	}

	/**
	 * Sets the tracker into its initial state.  Previously declared track data structures are saved
	 * for re-use.
	 */
	public void reset() {
		dropAllTracks();
		totalTracks = 0;
	}

	/**
	 * Updates the location and description of tracks using KLT.  Saves a reference
	 * to the input image for future processing.
	 *
	 * @param input Input image.
	 * @param pyramid Image pyramid of input.
	 * @param derivX Derivative pyramid of input x-axis
	 * @param derivY Derivative pyramid of input y-axis
	 */
	public void updateTracks( I input ,
							  PyramidDiscrete<I> pyramid ,
							  D[] derivX,
							  D[] derivY ) {
		// forget recently dropped or spawned tracks
		tracksSpawned.clear();

		// save references
		this.input = input;

		trackerKlt.setInputs(pyramid, derivX, derivY);

		trackUsingKlt(tracksPureKlt);
		trackUsingKlt(tracksReactivated);
	}

	/**
	 * Tracks features in the list using KLT and update their state
	 */
	private void trackUsingKlt(List<CombinedTrack<TD>> tracks) {
		for( int i = 0; i < tracks.size();  ) {
			CombinedTrack<TD> track = tracks.get(i);

			if( !trackerKlt.performTracking(track.track) ) {
				// handle the dropped track
				tracks.remove(i);
				tracksDormant.add(track);
			} else {
				track.set(track.track.x,track.track.y);
				i++;
			}
		}
	}

	/**
	 * From the found interest points create new tracks.  Tracks are only created at points
	 * where there are no existing tracks.
	 *
	 * Note: Must be called after {@link #associateAllToDetected}.
	 */
	public void spawnTracksFromDetected() {
		// mark detected features with no matches as available
		FastQueue<AssociatedIndex> matches = associate.getMatches();

		int N = detector.getNumberOfFeatures();
		for( int i = 0; i < N; i++ )
			associated[i] = false;

		for( AssociatedIndex i : matches.toList() ) {
			associated[i.dst] = true;
		}

		// spawn new tracks for unassociated detected features
		for( int i = 0; i < N; i++ ) {
			if( associated[i])
				continue;

			Point2D_F64 p = detector.getLocation(i);

			TD d = detectedDesc.get(i);

			CombinedTrack<TD> track;

			if( tracksUnused.size() > 0 ) {
				track = tracksUnused.pop();
			} else {
				track = new CombinedTrack<>();
				track.desc = detector.createDescription();
				track.track = trackerKlt.createNewTrack();
			}

			// create the descriptor for tracking
			trackerKlt.setDescription((float)p.x,(float)p.y,track.track);
			// set track ID and location
			track.featureId = totalTracks++;
			track.desc.setTo(d);
			track.set(p);

			// update list of active tracks
			tracksPureKlt.add(track);
			tracksSpawned.add(track);
		}
	}


	/**
	 * Associates pre-existing tracks to newly detected features
	 *
	 * @param known List of known tracks
	 */
	private void associateToDetected( List<CombinedTrack<TD>> known ) {
		// initialize data structures
		detectedDesc.reset();
		knownDesc.reset();

		// create a list of detected feature descriptions
		int N = detector.getNumberOfFeatures();
		for( int i = 0; i < N; i++ ) {
			detectedDesc.add(detector.getDescription(i));
		}

		// create a list of previously created track descriptions
		for( CombinedTrack<TD> t : known ) {
			knownDesc.add(t.desc);
		}

		// associate features
		associate.setSource(knownDesc);
		associate.setDestination(detectedDesc);
		associate.associate();

		N = Math.max(known.size(),detector.getNumberOfFeatures());
		if( associated.length < N )
			associated = new boolean[N];
	}

	/**
	 * Associate all tracks in any state to the latest observations.  If a dormant track is associated it
	 * will be reactivated.  If a reactivated track is associated it's state will be updated.  PureKLT
	 * tracks are left unmodified.
	 */
	public void associateAllToDetected() {
		// initialize data structures
		List<CombinedTrack<TD>> all = new ArrayList<>();
		all.addAll(tracksReactivated);
		all.addAll(tracksDormant);
		all.addAll(tracksPureKlt);

		int numTainted = tracksReactivated.size() + tracksDormant.size();

		tracksReactivated.clear();
		tracksDormant.clear();

		// detect features
		detector.detect(input);
		// associate features
		associateToDetected(all);

		FastQueue<AssociatedIndex> matches = associate.getMatches();

		// See which features got respawned and which ones are made dormant
		for( int i = 0; i < numTainted; i++ ) {
			associated[i] = false;
		}

		for( AssociatedIndex a : matches.toList() ) {
			// don't mess with pure-KLT tracks
			if( a.src >= numTainted )
				continue;

			CombinedTrack<TD> t = all.get(a.src);

			t.set(detector.getLocation(a.dst));
			trackerKlt.setDescription((float) t.x, (float) t.y, t.track);
			tracksReactivated.add(t);
			associated[a.src] = true;
		}

		for( int i = 0; i < numTainted; i++ ) {
			if( !associated[i] ) {
				tracksDormant.add(all.get(i));
			}
		}
	}

	/**
	 * Stops tracking the specified track and recycles its data.
	 *
	 * @param track The track being dropped
	 * @return true if the track was being tracked and data was recycled false if not.
	 */
	public boolean dropTrack( CombinedTrack<TD> track ) {
		if( !tracksPureKlt.remove(track) )
			if( !tracksReactivated.remove(track) )
				if( !tracksDormant.remove(track) )
					return false;

		tracksUnused.add(track);
		return true;
	}

	public List<CombinedTrack<TD>> getSpawned() {
		return tracksSpawned;
	}

	public List<CombinedTrack<TD>> getPureKlt() {
		return tracksPureKlt;
	}

	public List<CombinedTrack<TD>> getReactivated() {
		return tracksReactivated;
	}

	public List<CombinedTrack<TD>> getDormant() {
		return tracksDormant;
	}

	public PyramidKltForCombined<I, D> getTrackerKlt() {
		return trackerKlt;
	}

	public DetectDescribePoint<I, TD> getDetector() {
		return detector;
	}

	/**
	 * Drops all tracks and recycles the data
	 */
	public void dropAllTracks() {
		tracksUnused.addAll(tracksDormant);
		tracksUnused.addAll(tracksPureKlt);
		tracksUnused.addAll(tracksReactivated);

		tracksSpawned.clear();
		tracksPureKlt.clear();
		tracksReactivated.clear();
		tracksSpawned.clear();
		tracksDormant.clear();
	}
}
