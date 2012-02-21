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

package boofcv.abst.feature.tracker;

import boofcv.alg.geo.AssociatedPair;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Point tracker which saves the location of features at the keyframe and keeps track of their current location.
 * Features are removed when the tracker looses them or when the user requests that a track be dropped.
 *
 * @author Peter Abeles
 */
public class KeyFramePointTracker<T extends ImageBase> {
	// point feature tracker
	ImagePointTracker<T> tracker;

	// pairs of associated tracks
	List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
	// saved associated pairs
	FastQueue<AssociatedPair> reserveA = new FastQueue<AssociatedPair>(100,AssociatedPair.class,true);

	public KeyFramePointTracker(ImagePointTracker<T> tracker) {
		this.tracker = tracker;
	}

	/**
	 * Tracks features inside the image and updates feature locations.
	 *
	 * @param image
	 */
	public void process( T image ) {
		tracker.process(image);
		
		List<PointTrack> dropped = tracker.getDroppedTracks();

		for( int i = 0; i < dropped.size(); i++ ) {
			if( !pairs.remove((AssociatedPair)dropped.get(i).cookie) )
				throw new IllegalArgumentException("Bug: Dropped track does not exist");
		}
	}

	/**
	 * Makes the most recently processed image the keyframe.
	 */
	public void setKeyFrame() {
		pairs.clear();
		reserveA.reset();
		
		List<PointTrack> tracks = tracker.getActiveTracks();
		for( PointTrack t : tracks ) {
			AssociatedPair p = reserveA.pop();
			p.keyLoc.set(t);
			p.currLoc = t;
			t.cookie = p;
			pairs.add(p);
		}
	}

	/**
	 * Requests that the tracker spawn new tracks
	 */
	public void spawnTracks() {
		tracker.spawnTracks();
	}

	/**
	 * Removes the track from the tracker and each keyframe
	 * 
	 * @param track The track which is to be dropped
	 */
	public void dropTrack( PointTrack track ) {
		if( !pairs.remove((AssociatedPair)track.cookie) )
			throw new IllegalArgumentException("Bug: Dropped track does not exist");

		tracker.dropTrack(track);
	}

	/**
	 * List of active tracks from the tracker
	 *
	 * @return active tracks
	 */
	public List<PointTrack> getActiveTracks() {
		return tracker.getActiveTracks();
	}

	/**
	 * List of all associated pairs.
	 *
	 * @return associated pairs
	 */
	public List<AssociatedPair> getPairs() {
		return pairs;
	}

	/**
	 * Drops all tracks and clears all associated pairs.
	 */
	public void reset() {
		pairs.clear();
		reserveA.reset();
		tracker.dropTracks();
	}

	public ImagePointTracker<T> getTracker() {
		return tracker;
	}
}
