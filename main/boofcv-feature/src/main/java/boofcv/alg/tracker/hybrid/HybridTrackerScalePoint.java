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

package boofcv.alg.tracker.hybrid;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.AssociateDescriptionSets2D;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.tracker.PruneCloseTracks;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * Combines a KLT tracker with Detect-Describe-Associate type trackers. Features are nominally tracked
 * using KLT, but after KLT drops a track it is deactivated and made dormant. Upon request, it will
 * attempt to reactivate a dormant track by associating it with newly detected features. After a
 * track has been reactivated it will be tracked normally.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class HybridTrackerScalePoint
		<I extends ImageGray<I>, D extends ImageGray<D>, TD extends TupleDesc<TD>> {
	// The max number of allowed unassociated tracks before it starts to drop them
	public int maxInactiveTracks = 200;
	// Used to select tracks to drop
	public Random rand = new Random(345);

	// Size of input image
	protected int imageWidth, imageHeight;

	/** The KLT tracker used to perform the nominal track update */
	protected @Getter PyramidKltForHybrid<I, D> trackerKlt;

	// tolerance for forwards-backwards validation for KLT tracks in pixels at level 0. disabled if < 0
	protected double toleranceFB = -1; // TODO implement this

	/** feature detector and describer */
	protected @Getter DetectDescribePoint<I, TD> detector;
	/** Used to associate features using their DDA description */
	protected @Getter AssociateDescriptionSets2D<TD> associate;

	/** List of all tracks (active + inactive) and stores unused track data for future re-use */
	protected @Getter DogArray<HybridTrack<TD>> tracksAll;

	/** all active tracks that have been tracked purely by KLT */
	protected @Getter FastArray<HybridTrack<TD>> tracksActive = new FastArray(HybridTrack.class);
	/** tracks not visible in the current frame and were not updated */
	protected @Getter FastArray<HybridTrack<TD>> tracksInactive = new FastArray(HybridTrack.class);
	/** recently spawned tracks */
	protected @Getter List<HybridTrack<TD>> tracksSpawned = new ArrayList<>();
	/** tracks which were dropped this frame */
	protected @Getter List<HybridTrack<TD>> tracksDropped = new ArrayList<>();

	// local storage used by association
	protected FastArray<TD> detectedDesc;
	protected FastArray<TD> knownDesc;
	protected DogArray_I32 detectedSet = new DogArray_I32();
	protected DogArray_I32 knownSet = new DogArray_I32();
	protected FastArray<Point2D_F64> detectedPixels = new FastArray<>(Point2D_F64.class);
	protected FastArray<Point2D_F64> knownPixels = new FastArray<>(Point2D_F64.class);

	// number of tracks it has created. Used to assign track IDs
	protected long totalTracks = 0;

	// number of frames which have been processed
	@Getter long frameID = -1;

	// Used to prune points close by
	PruneCloseTracks<HybridTrack<TD>> pruneClose;
	List<HybridTrack<TD>> closeDropped = new ArrayList<>();

	/**
	 * Configures tracker
	 *
	 * @param trackerKlt KLT tracker used nominally
	 * @param detector Feature detector
	 * @param associate Association algorithm
	 * @param tooCloseRadius If tracks are less than or equal to this distance to each other some will be pruned
	 */
	public HybridTrackerScalePoint( PyramidKltForHybrid<I, D> trackerKlt,
									DetectDescribePoint<I, TD> detector,
									AssociateDescription2D<TD> associate,
									int tooCloseRadius ) {
		if (!associate.uniqueDestination() || !associate.uniqueSource())
			throw new IllegalArgumentException("Associations must be unique");

		this.trackerKlt = trackerKlt;
		this.detector = detector;

		detectedDesc = new FastArray<>(detector.getDescriptionType());
		knownDesc = new FastArray<>(detector.getDescriptionType());

		this.associate = new AssociateDescriptionSets2D<>(associate);
		this.associate.initializeSets(detector.getNumberOfSets());

		this.tracksAll = new DogArray<>(this::createNewTrack);
		if (tooCloseRadius > 0) {
			this.pruneClose = new PruneCloseTracks<>(tooCloseRadius, new PruneCloseTracks.TrackInfo<>() {
				@Override public void getLocation( HybridTrack<TD> track, Point2D_F64 l ) {l.setTo(track.pixel);}

				@Override public long getID( HybridTrack<TD> track ) {return track.featureId;}
			});
		}
	}

	/**
	 * Creates a new track and sets the descriptor
	 */
	protected HybridTrack<TD> createNewTrack() {
		var track = new HybridTrack<TD>();
		track.descriptor = detector.createDescription();
		return track;
	}

	/**
	 * Sets the tracker into its initial state. Previously declared track data structures are saved
	 * for re-use.
	 */
	public void reset() {
		dropAllTracks();
		totalTracks = 0;
		frameID = -1;
	}

	/**
	 * Updates the location and description of tracks using KLT. Saves a reference
	 * to the input image for future processing. Also updates the `totalPureKlt` count.
	 *
	 * @param pyramid Image pyramid of input.
	 * @param derivX Derivative pyramid of input x-axis
	 * @param derivY Derivative pyramid of input y-axis
	 */
	public void updateTracks( PyramidDiscrete<I> pyramid,
							  D[] derivX,
							  D[] derivY ) {
		this.imageWidth = pyramid.getInputWidth();
		this.imageHeight = pyramid.getInputWidth();
		this.tracksDropped.clear();
		this.tracksSpawned.clear();
		if (frameID == -1)
			associate.initializeAssociator(imageWidth, imageHeight);

		frameID++;
//		System.out.println("frame: "+frameID+" active "+tracksActive.size+" all "+tracksAll.size);

		// Run the KLT tracker
		trackerKlt.setInputs(pyramid, derivX, derivY);

		// TODO add forwards-backwards validation
		for (int i = tracksActive.size() - 1; i >= 0; i--) {
			HybridTrack<TD> track = tracksActive.get(i);

			if (!trackerKlt.performTracking(track.trackKlt)) {
				// The track got dropped by KLT but will still be around as an inactive track
				tracksActive.removeSwap(i);
				tracksInactive.add(track);
			} else {
				track.lastSeenFrameID = frameID;
				track.pixel.setTo(track.trackKlt.x, track.trackKlt.y);
			}
		}
	}

	/**
	 * KLT tracks can drift towards each other (camera moving away) and this is often a bad situation as little new
	 * information is added by each track
	 */
	public void pruneActiveTracksWhichAreTooClose() {
		if (pruneClose == null)
			return;

		pruneClose.init(imageWidth, imageHeight);
		pruneClose.process(tracksActive.toList(), closeDropped);

		for (int dropIdx = 0; dropIdx < closeDropped.size(); dropIdx++) {
			HybridTrack<TD> track = closeDropped.get(dropIdx);
			dropTrackByAllIndex(tracksAll.indexOf(track));
			tracksDropped.add(track);
		}
	}

	/**
	 * These tracks were dropped by KLT at some point in the past. See if any of the recent detection match them
	 */
	public void associateInactiveTracks( I input ) {
		// TODO add ability to specify location of active tracks so those pixels can be skipped
		// TODO add ability to specify the maximum number of detections to return
		detector.detect(input);
		// TODO add ability to force a maximum limit on number of active tracks?

		int N = detector.getNumberOfFeatures();

		// initialize data structures
		detectedDesc.resize(N);
		detectedSet.resize(N);
		detectedPixels.resize(N);

		// create a list of detected feature descriptions
		for (int i = 0; i < N; i++) {
			detectedDesc.data[i] = detector.getDescription(i);
			detectedSet.data[i] = detector.getSet(i);
			detectedPixels.data[i] = detector.getLocation(i);
		}

		// Associate against all the tracks, including active ones. By considering active and not just inactive
		// tracks we should be able to reduce the false positive rate
		knownDesc.resize(tracksAll.size());
		knownSet.resize(tracksAll.size());
		knownPixels.resize(tracksAll.size());
		for (int i = 0; i < tracksAll.size(); i++) {
			HybridTrack<TD> t = tracksAll.get(i);
			knownDesc.data[i] = t.descriptor;
			knownSet.data[i] = t.detectorSetId;
			knownPixels.data[i] = t.pixel;
		}

		// associate features
		UtilFeature.setSource(knownDesc, knownSet, knownPixels, associate);
		UtilFeature.setDestination(detectedDesc, detectedSet, detectedPixels, associate);
		associate.associate();

		// Re-active / re-initial all non pure KLT tracks
		FastAccess<AssociatedIndex> matches = associate.getMatches();
		for (int i = 0; i < matches.size; i++) {
			// If the track is active skip it
			HybridTrack<TD> track = tracksAll.get(matches.get(i).src);
			Point2D_F64 detectedPixel = detectedPixels.data[matches.get(i).dst];

			// Re-spawning an inactive track is tricky. Let's say DDA gets it right 80% of the time, if we just do a KLT
			// track after respawning it will then be reliably wrong 20% each time this is called. That number will
			// increase with time. Doing so resulted in MUCH worse tracking performance. Until a way to ensure
			// detected tracks match the original one to a very high level of accuracy it's better to just
			// re-init all non pure KLT tracks
			boolean active = track.lastSeenFrameID == frameID;
			if (!track.respawned && active) { // skip pure KLT tracks
				continue;
			}

			// re-active the track and update it's state
			track.respawned = true;
			track.lastSeenFrameID = frameID;
			track.pixel.setTo(detectedPixel);
			trackerKlt.setDescription((float)detectedPixel.x, (float)detectedPixel.y, track.trackKlt);
			if (!active) { // don't add the same track twice to the active list
				tracksActive.add(track);
			}
		}

		// Updated inactive list by removing tracks which have become active
		for (int i = tracksInactive.size() - 1; i >= 0; i--) {
			if (tracksInactive.get(i).lastSeenFrameID == frameID) {
				tracksInactive.removeSwap(i);
			}
		}
	}

	/**
	 * Spawns new tracks from the list of unassociated detections. Must call {@link #associateInactiveTracks} first.
	 */
	public void spawnNewTracks() {
		// mark detected features with no matches as available
		DogArray_I32 unassociatedDetected = associate.getUnassociatedDestination();

		// spawn new tracks for unassociated detected features
		for (int unassociatedIdx = 0; unassociatedIdx < unassociatedDetected.size; unassociatedIdx++) {
			int detectedIdx = unassociatedDetected.get(unassociatedIdx);

			Point2D_F64 p = detectedPixels.data[detectedIdx];

			HybridTrack<TD> track = tracksAll.grow();

			// KLT track descriptor shape isn't known until after the first image has been processed
			if (track.trackKlt == null)
				track.trackKlt = trackerKlt.createNewTrack();
			// create the descriptor for KLT tracking
			trackerKlt.setDescription((float)p.x, (float)p.y, track.trackKlt);
			// set track ID and location
			track.respawned = false;
			track.spawnFrameID = track.lastSeenFrameID = frameID;
			track.featureId = totalTracks++;
			track.descriptor.setTo(detectedDesc.get(detectedIdx));
			track.detectorSetId = detectedSet.get(detectedIdx);
			track.pixel.setTo(p);

			// update list of active tracks
			tracksActive.add(track);
			tracksSpawned.add(track);
		}
	}

	/**
	 * If there are too many inactive tracks drop some of them
	 */
	public void dropExcessiveInactiveTracks() {
		if (tracksInactive.size() > maxInactiveTracks) {
			int numDrop = tracksInactive.size() - maxInactiveTracks;
			for (int i = 0; i < numDrop; i++) {
				// Randomly select tracks to drop
				int selectedIdx = rand.nextInt(tracksInactive.size());

				// Remove the selected track from the inactive list and add it tot he dropped list
				HybridTrack<TD> track = tracksInactive.removeSwap(selectedIdx);
				int indexAll = tracksAll.indexOf(track);
				tracksAll.removeSwap(indexAll);
			}
		}
	}

	/**
	 * Stops tracking the specified track and recycles its data.
	 *
	 * @param track The track being dropped
	 * @return true if the track was being tracked and data was recycled false if not.
	 */
	public boolean dropTrack( HybridTrack<TD> track ) {
		int index = tracksAll.indexOf(track);
		if (index < 0)
			return false;
		HybridTrack<TD> dropped = dropTrackByAllIndex(index);
		assert dropped == track;
		return true;
	}

	/**
	 * Drops the track using its index in all. Note that removeSwap is used so the order will change
	 *
	 * @return The dropped track
	 */
	public HybridTrack<TD> dropTrackByAllIndex( int index ) {
		HybridTrack<TD> track = tracksAll.removeSwap(index);

//		if( tracksActive.contains(track) && tracksInactive.contains(track))
//			throw new RuntimeException("BUG");

		boolean found = false;
		{
			// TODO update code when ddogleg adds a remove function
			int indexActive = tracksActive.indexOf(track);
			if (indexActive >= 0) {
				found = true;
				tracksActive.remove(indexActive);
			}
		}
		if (!found) {
			int indexInactive = tracksInactive.indexOf(track);
			if (indexInactive >= 0) {
				found = true;
				tracksInactive.remove(indexInactive);
			}
		}
		// Sanity Check: The track should have been in at least one of those lists
		assert found;

		return track;
	}

	/**
	 * Drops all tracks and recycles the data
	 */
	public void dropAllTracks() {
		tracksAll.reset();
		tracksActive.reset();
		tracksInactive.reset();
		tracksSpawned.clear();
		tracksDropped.clear();
	}
}
