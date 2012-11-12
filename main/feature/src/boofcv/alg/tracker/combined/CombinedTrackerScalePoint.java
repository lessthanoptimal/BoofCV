/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDescQueue;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;

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
		<I extends ImageSingleBand , D extends ImageSingleBand, TD extends TupleDesc> {

	// current image in sequence
	private I input;

	// The KLT tracker used to perform the nominal track update
	private PyramidKltForCombined<I,D> trackerKlt;

	// feature detector
	private InterestPointDetector<I> detector;
	// describes features for DDA update
	private DescribeRegionPoint<I,TD> describe;
	// Used to associate features using their DDA description
	private GeneralAssociation<TD> associate;

	// all active tracks that have been tracked purely by KLT
	private List<CombinedTrack<TD>> tracksPureKlt = new ArrayList<CombinedTrack<TD>>();
	// tracks that had been dropped by KLT but have been reactivated
	private List<CombinedTrack<TD>> tracksReactivated = new ArrayList<CombinedTrack<TD>>();
	// tracks that are not actively being tracked
	private List<CombinedTrack<TD>> tracksDormant = new ArrayList<CombinedTrack<TD>>();
	// recently spawned tracks
	private List<CombinedTrack<TD>> tracksSpawned = new ArrayList<CombinedTrack<TD>>();
	// track points whose data is to be reused
	private Stack<CombinedTrack<TD>> tracksUnused = new Stack<CombinedTrack<TD>>();

	// list of descriptions that are available for reuse
	private Stack<TD> descUnused = new Stack<TD>();

	// local storage used by association
	private FastQueue<TD> detectedDesc;
	private FastQueue<TD> knownDesc;

	// number of tracks it has created
	private long totalTracks = 0;

	// Marks a known track has being associated
	private boolean associated[] = new boolean[1];

	public CombinedTrackerScalePoint(PyramidKltForCombined<I, D> trackerKlt,
									 InterestPointDetector<I> detector,
									 DescribeRegionPoint<I, TD> describe,
									 GeneralAssociation<TD> associate ) {

		if( describe != null ) {
			if( describe.requiresOrientation() && !detector.hasOrientation() )
				throw new IllegalArgumentException("Descriptor requires orientation");
			if( describe.requiresScale() && !detector.hasScale() )
				throw new IllegalArgumentException("Descriptor requires scale");
		}

		this.trackerKlt = trackerKlt;
		this.detector = detector;
		this.describe = describe;

		detectedDesc = new TupleDescQueue<TD>(describe,false);
		knownDesc = new TupleDescQueue<TD>(describe,false);

		this.associate = associate;
	}

	/**
	 * Sets the tracker into its initial state.
	 */
	public void reset() {
		tracksPureKlt.clear();
		tracksReactivated.clear();
		tracksDormant.clear();
		tracksSpawned.clear();
		tracksUnused.clear();
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
							  PyramidDiscrete<D> derivX,
							  PyramidDiscrete<D> derivY ) {
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
	 * Selects new interest points in the image.
	 */
	public void detectInterestPoints() {
		// detect new interest points
		detector.detect(input);
	}

	/**
	 * From the found interest points create new tracks.  Tracks are only created at points
	 * where there are no existing tracks.
	 */
	public void spawnTracksFromPoints() {

		identifyAvailableFeatures();

		int N = detector.getNumberOfFeatures();

		for( int i = 0; i < N; i++ ) {
			if( !associated[i])
				continue;

			Point2D_F64 p = detector.getLocation(i);

			TD d = detectedDesc.get(i);

			CombinedTrack<TD> track;

			if( tracksUnused.size() > 0 ) {
				track = tracksUnused.pop();
			} else {
				track = new CombinedTrack<TD>();
				track.desc = describe.createDescription();
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
	 * Associate active tracks to detected features.  Make a list of features which do
	 * not match any existing tracks.  These features can be used to spawn new tracks.
	 */
	protected void identifyAvailableFeatures() {

		// associate existing to detected features
		// initialize data structures
		List<CombinedTrack<TD>> active = new ArrayList<CombinedTrack<TD>>();
		active.addAll(tracksReactivated);
		active.addAll(tracksPureKlt);

		associateToDetected(active);

		// mark detected features with no matches as available
		FastQueue<AssociatedIndex> matches = associate.getMatches();

		int N = detector.getNumberOfFeatures();
		for( int i = 0; i < N; i++ )
			associated[i] = true;

		for( AssociatedIndex i : matches.toList() ) {
			associated[i.dst] = false;
		}
	}

	/**
	 * Associates pre-existing tracks to newly detected features
	 *
	 * @param known List of known tracks
	 */
	private void associateToDetected( List<CombinedTrack<TD>> known ) {
		// initialize data structures
		describe.setImage(input);

		detectedDesc.reset();
		knownDesc.reset();

		// create a list of detected feature descriptions
		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 p = detector.getLocation(i);
			double scale = detector.getScale(i);
			double yaw = detector.getOrientation(i);

			TD d = descUnused.isEmpty() ? describe.createDescription() : descUnused.pop();
			describe.process(p.x,p.y,yaw,scale,d);

			detectedDesc.add(d);
		}

		// create a list of previously created track descriptions
		for( CombinedTrack<TD> t : known ) {
			knownDesc.add(t.desc);
		}

		// associate features
		associate.associate(knownDesc, detectedDesc);

		// clean up
		descUnused.addAll(detectedDesc.toList());

		int N = Math.max(known.size(),detector.getNumberOfFeatures());
		if( associated.length < N )
			associated = new boolean[N];
	}

	/**
	 * Associate tracks which have at some point been dropped by KLT to newly detected features.  When
	 * associating, consider pure-KLT tracks to help remove false positives.
	 */
	public void associateTaintedToPoints() {

		// initialize data structures
		List<CombinedTrack<TD>> all = new ArrayList<CombinedTrack<TD>>();
		all.addAll(tracksReactivated);
		all.addAll(tracksDormant);
		all.addAll(tracksPureKlt);

		int numTainted = tracksReactivated.size() + tracksDormant.size();

		tracksReactivated.clear();
		tracksDormant.clear();

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

	public void dropTrack( CombinedTrack<TD> track ) {
		if( !tracksPureKlt.remove(track) )
			if( !tracksReactivated.remove(track) )
				if( !tracksDormant.remove(track) )
					throw new RuntimeException("Track not being tracked!");

		tracksUnused.add(track);
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

	public void dropAllTracks() {
		tracksUnused.addAll(tracksSpawned);
		tracksUnused.addAll(tracksPureKlt);
		tracksUnused.addAll(tracksReactivated);

		tracksSpawned.clear();
		tracksPureKlt.clear();
		tracksReactivated.clear();
		tracksSpawned.clear();
	}
}
