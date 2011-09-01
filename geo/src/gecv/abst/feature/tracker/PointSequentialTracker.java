/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.feature.tracker;


import gecv.alg.geo.AssociatedPair;
import gecv.struct.image.ImageBase;

import java.util.List;


/**
 * <p>
 * Interface for tracking point features in a sequence of images for SFM applications.
 * </p>
 * <p/>
 * <p>
 * Lower level/mundane track maintenance is handled by implementations of this interface. This
 * includes selecting features, dropping features, and updating features. THe ability to manually
 * add tracks has intentionally been omitted from this interface for simplicity.  If that level
 * of control is needed then a more complex tracker should be used.
 * </p>
 * <p/>
 * <p>
 * Contract:
 * <ul>
 * <li> The current location of a feature in AssociatedPair will be used by the tracker if it
 * can affect the tracking outcome. </li>
 * <li> If a track is dropped its description will not be modified.</li>
 * <li> Each time a new track is spawned it is given a new unique ID </li>
 * <li> New tracks are only added when {@link #setCurrentToKeyFrame()} is called </li>
 * <li> The location of the current frame is only modified when {@link #setCurrentToKeyFrame()} is called </li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public interface PointSequentialTracker<T extends ImageBase> {

	/**
	 * Adds a new feature to be tracked at the specified location.
	 *
	 * @param x coordinate of the new feature being tracked.
	 * @param y coordinate of the new feature being tracked.
	 * @return If a new track was added or not.
	 */
	public boolean addTrack( float x , float y );

	/**
	 * Automatically selects new features in the image to track.
	 */
	public void spawnTracks();

	/**
	 * Drops all feature currently being tracked
	 */
	public void dropTracks();

	/**
	 * Sets the current frame to be the key frame.  If configured to spawn new features, a new
	 * feature will be spawned here.
	 */
	public void setCurrentToKeyFrame();

	/**
	 * Manually forces a track to be dropped.
	 *
	 * @param track The track which is to be dropped
	 */
	public void dropTrack(AssociatedPair track);

	/**
	 * Returns a list of active tracks.
	 */
	public List<AssociatedPair> getActiveTracks();

	/**
	 * Returns a list of tracks that were dropped the last time track features was
	 * called.
	 */
	public List<AssociatedPair> getDroppedTracks();

	/**
	 * Returns a list of tracks that were added when {@link #setCurrentToKeyFrame()} was called.
	 */
	public List<AssociatedPair> getNewTracks();
}
