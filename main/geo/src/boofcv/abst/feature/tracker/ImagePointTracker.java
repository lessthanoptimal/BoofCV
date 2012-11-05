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

import boofcv.struct.image.ImageBase;

import java.util.List;

/**
 * <p>
 * Interface for tracking point features in a sequence of images for SFM applications.
 * </p>
 * <p>
 * Lower level/mundane track maintenance is handled by implementations of this interface. This
 * includes selecting features, dropping features, and updating features. THe ability to manually
 * add tracks has intentionally been omitted from this interface for simplicity.  If that level
 * of control is needed then a more complex tracker should be used.
 * </p>
 *
 * <p>
 * Contract:
 * <ul>
 * <li> The current location of a feature in AssociatedPair will be used by the tracker if it
 * can affect the tracking outcome. </li>
 * <li> If a track is dropped its description will not be modified.</li>
 * <li> Each time a new track is spawned it is given a new unique ID </li>
 * </ul>
 * </p>
 *
 * TODO UPDATE THIS
 *
 * @author Peter Abeles
 */
public interface ImagePointTracker <T extends ImageBase> {

	/**
	 * Discard memory of all current and past tracks.
	 */
	void reset();

	/**
	 * Process input image and perform tracking.
	 *
	 * @param image Next image in the sequence
	 */
	void process( T image );

	/**
	 * Adds a new feature to be tracked at the specified location.
	 *
	 * @param x coordinate of the new feature being tracked.
	 * @param y coordinate of the new feature being tracked.
	 * @return If a new track was added or not.
	 */
	// TODO remove this function?
	public boolean addTrack( double x , double y );

	/**
	 * Automatically selects new features in the image to track.
	 *
	 * TODO add requirement that there be no sumplicate
	 */
	public void spawnTracks();

	/**
	 * Drops all feature currently being tracked
	 */
	public void dropAllTracks();

	/**
	 * Manually forces a track to be dropped.
	 *
	 * @param track The track which is to be dropped
	 */
	// todo more efficient way to drop tracks
	// this will be slow since it need to searh through the whole list
	public void dropTrack(PointTrack track);


	/**
	 * Returns a list of all features that it is currently tracking
	 * @return
	 */
	public List<PointTrack> getAllTracks();

	/**
	 * Returns a list of active tracks. An active track is defined as a track
	 * which was found in the most recently processed image.
	 */
	public List<PointTrack> getActiveTracks();

	/**
	 * Returns a list of tracks dropped by the tracker during the most recent update.
	 * Tracks dropped by invoking {@link #dropAllTracks()} or {@link #dropTrack(PointTrack)}
	 * will not be included.
	 */
	public List<PointTrack> getDroppedTracks();

	/**
	 * Returns a list of tracks that have been added since process was called.
	 */
	public List<PointTrack> getNewTracks();
}

