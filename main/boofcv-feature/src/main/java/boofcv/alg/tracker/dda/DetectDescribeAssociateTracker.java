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

package boofcv.alg.tracker.dda;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.AssociateDescriptionSets2D;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * Base class for detect-describe-associate type trackers. Tracker works by detecting features in each image,
 * computing a descriptor for each feature, then associating the features together.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectDescribeAssociateTracker<I extends ImageGray<I>, TD extends TupleDesc<TD>> {
	// associates features between two images together
	protected AssociateDescriptionSets2D<TD> associate;

	// Detects and describes image features
	protected DetectDescribePoint<I, TD> detector;

	// all tracks. active and inactive.
	protected @Getter DogArray<PointTrack> tracksAll;
	// recently associated tracks
	protected @Getter List<PointTrack> tracksActive = new ArrayList<>();
	// tracks not matched to any recent features
	protected @Getter List<PointTrack> tracksInactive = new ArrayList<>();
	// tracks dropped by the tracker
	protected @Getter List<PointTrack> tracksDropped = new ArrayList<>();
	// tracks recently spawned
	protected @Getter List<PointTrack> tracksNew = new ArrayList<>();

	// ID of the most recently processed frame
	protected @Getter long frameID = -1;

	// number of features created. Used to assign unique IDs
	protected long featureID = 0;

	// should it update the feature description after each association?
	@Getter @Setter boolean updateDescription;

	// maximum number of tracks it will keep track of that were not associated before it starts discarding
	protected int maxInactiveTracks;

	// Random number generator
	protected Random rand;

	// destination features are ones which were detected in this frame
	protected FastArray<TD> dstDesc;
	protected DogArray_I32 dstSet = new DogArray_I32();
	protected FastArray<Point2D_F64> dstPixels = new FastArray<>(Point2D_F64.class);

	// source features are ones from active tracks
	protected FastArray<TD> srcDesc;
	protected DogArray_I32 srcSet = new DogArray_I32();
	protected FastArray<Point2D_F64> srcPixels = new FastArray<>(Point2D_F64.class);

	/**
	 * Configures tracker
	 *
	 * @param associate Association
	 * @param config Configures behavior.
	 */
	public DetectDescribeAssociateTracker( DetectDescribePoint<I, TD> detector,
										   final AssociateDescription2D<TD> associate,
										   ConfigTrackerDda config ) {
		this.detector = detector;
		this.associate = new AssociateDescriptionSets2D<>(associate);
		this.updateDescription = config.updateDescription;
		this.maxInactiveTracks = config.maxInactiveTracks;
		this.rand = new Random(config.seed);

		this.dstDesc = new FastArray<>(detector.getDescriptionType());
		this.srcDesc = new FastArray<>(detector.getDescriptionType());

		this.tracksAll = new DogArray<>(this::createNewTrack, this::resetTrack);

		this.associate.initializeSets(detector.getNumberOfSets());
	}

	protected DetectDescribeAssociateTracker() {}

	/**
	 * Creates a new track and sets the descriptor
	 */
	protected PointTrack createNewTrack() {
		var t = new PointTrack();
		t.setDescription(detector.createDescription());
		return t;
	}

	/**
	 * Resets the track but saves the descriptor reference
	 */
	protected void resetTrack( PointTrack t ) {
		TD desc = t.getDescription();
		Object cookie = t.getCookie();
		t.reset();
		t.setDescription(desc);
		t.setCookie(cookie);
	}

	/**
	 * Discards all tracking history
	 */
	public void reset() {
		dropAllTracks();
		featureID = 0;
		frameID = -1;
	}

	/**
	 * Detect features and associate with existing tracks
	 */
	public void process( I input ) {
		if (frameID == -1)
			associate.initializeAssociator(input.width, input.height);
		frameID++;
		tracksActive.clear();
		tracksInactive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		detector.detect(input);

		final int N = detector.getNumberOfFeatures();
		// initialize data structures
		dstDesc.resize(N);
		dstSet.resize(N);
		dstPixels.resize(N);

		// create a list of detected feature descriptions
		for (int i = 0; i < N; i++) {
			dstDesc.data[i] = detector.getDescription(i);
			dstSet.data[i] = detector.getSet(i);
			dstPixels.data[i] = detector.getLocation(i);
		}

		if (tracksAll.size == 0) {
			return;
		}

		performTracking();

		// add unassociated to the list
		DogArray_I32 unassociatedIdx = associate.getUnassociatedSource();
		for (int j = 0; j < unassociatedIdx.size(); j++) {
			tracksInactive.add(tracksAll.get(unassociatedIdx.get(j)));
		}

		dropExcessiveInactiveTracks(unassociatedIdx);
	}

	/**
	 * If there are too many unassociated tracks, randomly select some of those tracks and drop them
	 */
	void dropExcessiveInactiveTracks( DogArray_I32 unassociated ) {
		if (unassociated.size > maxInactiveTracks) {
			// make the first N elements the ones which will be dropped
			int numDrop = unassociated.size - maxInactiveTracks;
			for (int i = 0; i < numDrop; i++) {
				int selected = rand.nextInt(unassociated.size - i) + i;
				int a = unassociated.get(i);
				unassociated.data[i] = unassociated.data[selected];
				unassociated.data[selected] = a;
			}

			// Drop tracks, but do so in reverse order so that the faster removeSwap() can be used since it changes
			// the order of later tracks
			unassociated.size = numDrop;
			unassociated.sort();
			for (int i = unassociated.size - 1; i >= 0; i--) {
				tracksDropped.add(dropTrackIndexInAll(unassociated.get(i)));
			}
		}
	}

	/**
	 * Associate detections to tracks
	 */
	protected void performTracking() {

		// Create a list for association from all tracks
		final int numTracks = tracksAll.size();
		srcDesc.resize(numTracks);
		srcPixels.resize(numTracks);
		srcSet.resize(numTracks);
		for (int i = 0; i < numTracks; i++) {
			PointTrack t = tracksAll.get(i);
			srcDesc.data[i] = t.getDescription();
			srcPixels.data[i] = t.pixel;
			srcSet.data[i] = t.detectorSetId;
		}

		// Associate existing tracks with detections
		UtilFeature.setSource(srcDesc, srcSet, srcPixels, associate);
		UtilFeature.setDestination(dstDesc, dstSet, dstPixels, associate);
		associate.associate();

		FastAccess<AssociatedIndex> matches = associate.getMatches();

		for (int i = 0; i < matches.size; i++) {
			AssociatedIndex indexes = matches.data[i];
			PointTrack track = tracksAll.get(indexes.src);
			Point2D_F64 loc = dstPixels.data[indexes.dst];
			track.pixel.setTo(loc.x, loc.y);
			track.lastSeenFrameID = frameID;
			tracksActive.add(track);

			// update the description
			if (updateDescription) {
				((TD)track.getDescription()).setTo(dstDesc.get(indexes.dst));
			}
		}
	}

	/**
	 * Takes the current crop of detected features and makes them the keyframe
	 */
	public void spawnTracks() {
		// If there are no tracks then associate is not called. Reset() could have been called at associate is
		// in an undefined state
		if (tracksAll.size == 0) {
			for (int i = 0; i < dstDesc.size; i++) {
				Point2D_F64 loc = dstPixels.get(i);
				addNewTrack(dstSet.get(i), loc.x, loc.y, dstDesc.get(i));
			}
			return;
		}

		// create new tracks from latest unassociated detected features
		DogArray_I32 unassociated = associate.getUnassociatedDestination();
		for (int i = 0; i < unassociated.size; i++) {
			int indexDst = unassociated.get(i);
			Point2D_F64 loc = dstPixels.get(indexDst);
			addNewTrack(dstSet.get(i), loc.x, loc.y, dstDesc.get(indexDst));
		}
	}

	/**
	 * Adds a new track given its location and description
	 */
	protected void addNewTrack( int set, double x, double y, TD desc ) {
		PointTrack p = tracksAll.grow();
		p.pixel.setTo(x, y);
		((TD)p.getDescription()).setTo(desc);

		p.spawnFrameID = frameID;
		p.lastSeenFrameID = frameID;
		p.detectorSetId = set;
		p.featureId = featureID++;

//		if( tracksActive.contains(p))
//			throw new RuntimeException("Contained twice active! p.id="+p.featureId);

		tracksNew.add(p);
		tracksActive.add(p);
	}

	/**
	 * Drops all tracks
	 */
	public void dropAllTracks() {
		tracksActive.clear();
		tracksInactive.clear();
		tracksAll.reset();
		tracksNew.clear();
	}

	/**
	 * Remove from active list and mark so that it is dropped in the next cycle
	 *
	 * @param track The track which is to be dropped
	 */
	public boolean dropTrack( PointTrack track ) {

		int indexInAll = tracksAll.indexOf(track);
		if (indexInAll < 0)
			return false; // this is probably a bug...
		dropTrackIndexInAll(indexInAll);

//		if( tracksActive.contains(track))
//			throw new RuntimeException("Contained twice active! pt.id="+track.featureId);
		return true;
	}

	/**
	 * Given the index of the track in the `all` list, drop it from the tracker
	 */
	private PointTrack dropTrackIndexInAll( int indexInAll ) {
		PointTrack track = tracksAll.removeSwap(indexInAll);

		// the track may or may not be in the active list
		boolean found = tracksActive.remove(track);
		found |= tracksInactive.remove(track);

		// if a track has been drop it must be in `all` and `active` OR `inactive` lists
		assert found;

		return track;
	}

	public void dropTracks( PointTracker.Dropper dropper ) {
		for (int i = tracksAll.size() - 1; i >= 0; i--) {
			PointTrack track = tracksAll.get(i);
			if (!dropper.shouldDropTrack(track))
				continue;
			dropTrackIndexInAll(i);
		}
	}

	public ImageType<I> getImageType() {
		return this.detector.getInputType();
	}
}
