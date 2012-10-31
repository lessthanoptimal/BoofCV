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

import boofcv.struct.distort.DoNothingTransform_F64;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Point tracker which saves the location of features at the keyframe and keeps track of their current location.
 * Features are removed when the tracker looses them or when the user requests that a track be dropped.
 *
 * @author Peter Abeles
 */
// TODO is data being lost/leaked by point trackers?
// TODO purge all non-active tracks on spawn.  point/detect tracker
// todo maintain track ID after spawn/set key frame
// todo custom track data type.  create KeyFrameTrack?
public class KeyFramePointTracker<I extends ImageBase, R extends KeyFrameTrack> {
	// point feature tracker
	ImagePointTracker<I> tracker;
	
	// track type
	Class<R> trackType;

	// pairs of associated tracks
	List<R> pairs = new ArrayList<R>();

	// applies a distortion to each feature's track location
	// can be used to convert points into normalized image coordinates
	PointTransform_F64 pixelToNorm;

	public KeyFramePointTracker(ImagePointTracker<I> tracker,
								PointTransform_F64 pixelToNorm ,
								Class<R> trackType ) {
		this.tracker = tracker;
		this.trackType = trackType;
		setPixelToNorm(pixelToNorm);
	}

	public KeyFramePointTracker(ImagePointTracker<I> tracker) {
		this(tracker,null,(Class)KeyFrameTrack.class);
	}

	protected KeyFramePointTracker() {
	}


	public void setPixelToNorm(PointTransform_F64 pixelToNorm) {
		if( pixelToNorm == null )
			this.pixelToNorm = new DoNothingTransform_F64();
		else
			this.pixelToNorm = pixelToNorm;
	}

	/**
	 * Tracks features inside the image and updates feature locations.
	 *
	 * @param image
	 */
	public void process( I image ) {
		tracker.process(image);

		pairs.clear();
		List<PointTrack> tracks = tracker.getActiveTracks();
		for( PointTrack t : tracks ) {
			R p = t.getCookie();
			p.pixel.p2.set(t);
			pixelToNorm.compute(t.x, t.y, p.p2);
			pairs.add(p);
		}
	}

	/**
	 * Makes the most recently processed image the keyframe.
	 */
	public void setKeyFrame() {
		pairs.clear();

		// todo purge non-active tracks here
		List<PointTrack> tracks = tracker.getActiveTracks();
		for( PointTrack t : tracks ) {
			if( t.cookie == null )
				throw new RuntimeException("Bug, cookie should have been set");
			R p = t.getCookie();
			p.pixel.p1.set(t);
			p.pixel.p2.set(t);
			pixelToNorm.compute(t.x, t.y, p.p1);
			p.p2.set(p.p1);
			pairs.add(p);
		}
	}

	/**
	 * Requests that the tracker spawn new tracks
	 */
	public List<R> spawnTracks() {
		List<R> spawned = new ArrayList<R>();
		
		tracker.spawnTracks();
		List<PointTrack> tracks = tracker.getNewTracks();
		for( PointTrack t : tracks ) {
			if( t.cookie == null )
				try {
					t.cookie = trackType.newInstance();
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			R p = t.getCookie();
			p.reset();
			p.trackID = t.featureId;
			p.pixel.p1.set(t);
			p.pixel.p2.set(t);
			pixelToNorm.compute(t.x, t.y, p.p1);
			p.p2.set(p.p1);
			pairs.add(p);
			spawned.add(p);
		}

		return spawned;
	}

	/**
	 * Removes the track from the tracker and each keyframe
	 * 
	 * @param track The track which is to be dropped
	 */
	public void dropTrack( PointTrack track ) {
		//noinspection SuspiciousMethodCalls
		if( !pairs.remove(track.cookie) )
			throw new IllegalArgumentException("Bug: Dropped track does not exist");

		tracker.dropTrack(track);
	}

	public void dropTrack( R track ) {
		for( PointTrack t : tracker.getActiveTracks() ) {
			if( t.getCookie() == track ) {
				dropTrack(t);
				return;
			}
		}
		throw new RuntimeException("Couldn't find track to drop");
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
	public List<R> getPairs() {
		return pairs;
	}

	/**
	 * Drops all tracks and clears all associated pairs.
	 */
	public void reset() {
		pairs.clear();
		tracker.dropTracks();
	}

	public ImagePointTracker<I> getTracker() {
		return tracker;
	}
}
