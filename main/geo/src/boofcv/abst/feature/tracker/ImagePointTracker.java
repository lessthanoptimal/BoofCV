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
 * Interface for tracking natural point features in a sequence of images.  Features are automatically
 * detected and tracked between consecutive frames in a video sequence. Access is provided
 * to the pixel location of each track.  Implementation specific track information is hidden
 * from the user.
 * </p>
 *
 * <p>
 * Each track can have the following states, active, new, and dropped.  An active track is one
 * which was recently updated in the latest image. New tracks were spawned in the most recent
 * image.  Dropped tracks are tracks which were automatically dropped in the most recent update.
 * <p>
 *
 * <p>
 * NOTE: Tracks dropped by the user will not be included in the dropped list.
 * </p>
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
	 * Returns a list of all features that are currently being tracked
	 *
	 * @param list Optional storage for the list of tracks.
	 *             If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	public List<PointTrack> getAllTracks( List<PointTrack> list );

	/**
	 * Returns a list of active tracks. An active track is defined as a track
	 * which was found in the most recently processed image.
	 *
	 * @param list Optional storage for the list of tracks.
	 *             If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	public List<PointTrack> getActiveTracks(  List<PointTrack> list );

	/**
	 * Returns a list of tracks dropped by the tracker during the most recent update.
	 * Tracks dropped by user request are not included in this list.
	 *
	 * @param list Optional storage for the list of tracks.
	 *             If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	public List<PointTrack> getDroppedTracks( List<PointTrack> list );

	/**
	 * Returns a list of tracks that have been added since process was called.
	 *
	 * @param list Optional storage for the list of tracks.
	 *             If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	public List<PointTrack> getNewTracks( List<PointTrack> list );
}

