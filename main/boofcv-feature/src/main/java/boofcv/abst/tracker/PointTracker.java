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

package boofcv.abst.tracker;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <p>
 * Interface for tracking point features in image sequences with automatic feature selection for use in
 * Structure From Motion (SFM) application. Access is provided to the pixel location of each track.
 * Implementation specific track information is hidden from the user.
 * </p>
 *
 * <p>
 * Each track can have the following states, active, inactive, new, and dropped. An active track is one
 * which was recently updated in the latest image, while an inactive one was not. New tracks were
 * spawned in the most recent image. Dropped tracks are tracks which were automatically dropped
 * in the most recent update.
 * <p>
 *
 * <p>
 * TRACK MAINTENANCE: Implementations of this interface should not automatically drop tracks or perform other forms of
 * track maintenance unless the feature is hopelessly lost and can no longer be tracked. It is the responsibility
 * of the user to drop tracks which are inactive for an excessive amount of time. New tracks should never be spawned
 * unless specifically requested by the user.
 * </p>
 *
 * <p>
 * TRACK MEMORY: Implementations of this interface must recycle tracks. After a track has been dropped, either by
 * the user or automatically, the reference should be saved and the user provided cookie
 * left unmodified. When a new track is added the track information should be updated and the
 * cookie left unmodified again. The intended purpose of this requirement is to reduce the
 * burden of memory maintenance on the user and to encourage good memory management.
 * </p>
 *
 * <p>
 * NOTE: Tracks dropped by the user will not be included in the dropped list.
 * </p>
 *
 * @author Peter Abeles
 */
public interface PointTracker<T extends ImageBase<T>> {

	/**
	 * Process input image and perform tracking.
	 *
	 * @param image Next image in the sequence
	 */
	void process( T image );

	/**
	 * Discard memory of all current and past tracks. Growing buffered might not be reset to
	 * their initial size by this method.
	 */
	void reset();

	/**
	 * Returns the ID of the most recently processed frame. Frames start at 0 and increment by one each time
	 * process is called. After reset or before any frames have been processed -1 is returned.
	 */
	long getFrameID();

	/**
	 * Returns the total number of active tracks
	 */
	int getTotalActive();

	/**
	 * Returns total number of inactive tracks
	 */
	int getTotalInactive();

	/**
	 * Drops all feature to be dropped and will no longer be tracked.
	 *
	 * <p>NOTE: Tracks dropped using this function will not appear in the dropped list.</p>
	 */
	void dropAllTracks();

	/**
	 * Returns the maximum total number of tracks that it can spawn in a single frame. This number is reduced
	 * by the number of active tracks. So if the maximum number of tracks it can spawn is 200 and 50 tracks are
	 * active then it will spawn at most 150 tracks. If there is no limit then a number &le; 0 is returned.
	 */
	int getMaxSpawn();

	/**
	 * Manually forces a track to be dropped. New requests to all and active lists will not include the track after
	 * it has been dropped using this function.
	 *
	 * If request is made to drop a track that is not being tracked (in the internal all list), then the request
	 * is ignored.
	 *
	 * <p>NOTE: Tracks dropped using this function will not appear in the dropped list.</p>
	 *
	 * @param track The track which is to be dropped
	 * @return true if the request to drop the track was done or if it was ignored because the track wasn't being
	 * tracked
	 */
	boolean dropTrack( PointTrack track );

	/**
	 * Used to drop multiple tracks using a rule. This can be more efficient than dropping them one at a time.
	 *
	 * <p>NOTE: Tracks dropped using this function will not appear in the dropped list.</p>
	 *
	 * @param dropper Rule for dropping the tracks
	 */
	void dropTracks( Dropper dropper );

	/**
	 * Returns a list of all features that are currently being tracked
	 *
	 * @param list Optional storage for the list of tracks. List is cleared before tracks are added.
	 * If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	List<PointTrack> getAllTracks( @Nullable List<PointTrack> list );

	/**
	 * Returns a list of active tracks. An active track is defined as a track
	 * which was found in the most recently processed image.
	 *
	 * @param list Optional storage for the list of tracks. List is cleared before tracks are added.
	 * If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	List<PointTrack> getActiveTracks( @Nullable List<PointTrack> list );

	/**
	 * Returns a list of inactive tracks. A track is inactive if it is not
	 * associated with any features in the current image.
	 *
	 * @param list Optional storage for the list of tracks. List is cleared before tracks are added.
	 * If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	List<PointTrack> getInactiveTracks( @Nullable List<PointTrack> list );

	/**
	 * Returns a list of tracks dropped by the tracker during the most recent update.
	 * Tracks dropped by user request are not included in this list.
	 *
	 * @param list Optional storage for the list of tracks. List is cleared before tracks are added.
	 * If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	List<PointTrack> getDroppedTracks( @Nullable List<PointTrack> list );

	/**
	 * Returns a list of tracks that have been added since process was called.
	 *
	 * @param list Optional storage for the list of tracks. List is cleared before tracks are added.
	 * If null a new list will be declared internally.
	 * @return List of tracks.
	 */
	List<PointTrack> getNewTracks( @Nullable List<PointTrack> list );

	/**
	 * Automatically selects new features in the image to track. Returned tracks must
	 * be unique and not duplicates of any existing tracks. This includes both active
	 * and inactive tracks.
	 *
	 * NOTE: This function may or may not also modify the active and inactive lists.
	 */
	void spawnTracks();

	/**
	 * Input image type
	 */
	ImageType<T> getImageType();

	/**
	 * Provides a custom rule for dropping tracks
	 */
	interface Dropper {
		/**
		 * Called to see if a track should be dropped. DO NOT MODIFY THE TRACK HERE.
		 *
		 * @param track The track which is being examined.
		 * @return true means drop the track and false means keep it
		 */
		boolean shouldDropTrack( PointTrack track );
	}
}
