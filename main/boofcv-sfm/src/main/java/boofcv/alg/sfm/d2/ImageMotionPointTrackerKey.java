/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d2;

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import georegression.struct.InvertibleTransform;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the transform from the first image in a sequence to the current frame. Keyframe based algorithm.
 * Whenever a new keyframe is selected by the user all tracks are dropped and new ones spawned.  No logic is
 * contained for selecting key frames and relies on the user for selecting them.
 *
 * @author Peter Abeles
 * @param <I> Input image type
 * @param <IT> Motion model data type
 */
@SuppressWarnings("unchecked")
public class ImageMotionPointTrackerKey<I extends ImageBase<I>, IT extends InvertibleTransform>
{
	// feature tracker
	protected PointTracker<I> tracker;
	// Fits a model to the tracked features
	protected ModelMatcher<IT,AssociatedPair> modelMatcher;
	// Refines the model using the complete inlier set
	protected ModelFitter<IT,AssociatedPair> modelRefiner;

	// transform from the world frame to the key frame
	protected IT worldToKey;
	// transform from key frame to current frame
	protected IT keyToCurr;
	// transform from world to current frame
	protected IT worldToCurr;

	// Threshold that specified when tracks that have not been inliers for this many frames in a row are pruned
	protected int thresholdOutlierPrune;

	// if the current frame is a keyframe or not
	protected boolean keyFrame;

	/**
	 * Specify algorithms to use internally.  Each of these classes must work with
	 * compatible data structures.
	 *
	 * @param tracker feature tracker
	 * @param modelMatcher Fits model to track data
	 * @param modelRefiner (Optional) Refines the found model using the entire inlier set. Can be null.
	 * @param model Motion model data structure
	 * @param thresholdOutlierPrune If a track is an outlier for this many frames in a row they are pruned
	 */
	public ImageMotionPointTrackerKey(PointTracker<I> tracker,
									  ModelMatcher<IT, AssociatedPair> modelMatcher,
									  ModelFitter<IT, AssociatedPair> modelRefiner,
									  IT model,
									  int thresholdOutlierPrune)
	{
		this.tracker = tracker;
		this.modelMatcher = modelMatcher;
		this.modelRefiner = modelRefiner;
		this.thresholdOutlierPrune = thresholdOutlierPrune;

		worldToKey = (IT)model.createInstance();
		keyToCurr = (IT)model.createInstance();
		worldToCurr = (IT)model.createInstance();
		reset();
	}

	protected ImageMotionPointTrackerKey() {
	}

	/**
	 * Makes the current frame the first frame and discards its past history
	 */
	public void reset() {
		keyFrame = false;
		tracker.reset();
		resetTransforms();
	}

	/**
	 * Processes the next frame in the sequence.
	 *
	 * @param frame Next frame in the video sequence
	 * @return true if motion was estimated and false if no motion was estimated
	 */
	public boolean process( I frame ) {
		keyFrame = false;

		// update the feature tracker
		tracker.process(frame);

		List<PointTrack> tracks = tracker.getActiveTracks(null);

		if( tracks.size() == 0 )
			return false;

		List<AssociatedPair> pairs = new ArrayList<>();
		for( PointTrack t : tracks ) {
			pairs.add((AssociatedPair)t.getCookie());
		}

		// fit the motion model to the feature tracks
		if( !modelMatcher.process((List)pairs) ) {
			return false;
		}

		if( modelRefiner != null ) {
			if( !modelRefiner.fitModel(modelMatcher.getMatchSet(),modelMatcher.getModelParameters(),keyToCurr) )
				return false;
		} else {
			keyToCurr.set(modelMatcher.getModelParameters());
		}

		// mark that the track is in the inlier set
		final long frameID = tracker.getFrameID();
		for( AssociatedPair p : modelMatcher.getMatchSet() ) {
			((AssociatedPairTrack)p).lastUsed = frameID;
		}

		// prune tracks which aren't being used
		pruneUnusedTracks();

		// Update the motion
		worldToKey.concat(keyToCurr, worldToCurr);

		return true;
	}

	private void pruneUnusedTracks() {
		final long frameID = tracker.getFrameID();
		List<PointTrack> all = tracker.getAllTracks(null);
		for( PointTrack t : all ) {
			AssociatedPairTrack p = t.getCookie();

			if( frameID - p.lastUsed >= thresholdOutlierPrune) {
				if( !tracker.dropTrack(t) )
					throw new RuntimeException("Drop track failed. Must be a bug in the tracker");
			}
		}
	}

	/**
	 * Change the current frame into the keyframe. p1 location of existing tracks is set to
	 * their current location and new tracks are spawned.  Reference frame transformations are also updated
	 */
	public void changeKeyFrame() {
		final long frameID = tracker.getFrameID();

		// drop all inactive tracks since their location is unknown in the current frame
		List<PointTrack> inactive = tracker.getInactiveTracks(null);
		for( PointTrack l : inactive ) {
			tracker.dropTrack(l);
		}

		// set the keyframe for active tracks as their current location
		List<PointTrack> active = tracker.getActiveTracks(null);
		for( PointTrack l : active ) {
			AssociatedPairTrack p = l.getCookie();
			p.p1.set(l.pixel);
			p.lastUsed = frameID;
		}

		tracker.spawnTracks();
		List<PointTrack> spawned = tracker.getNewTracks(null);
		for( PointTrack l : spawned ) {
			AssociatedPairTrack p = l.getCookie();
			if( p == null ) {
				l.cookie = p = new AssociatedPairTrack();
				// little bit of trickery here.  Save the reference so that the point
				// in the current frame is updated for free as PointTrack is
				p.p2 = l.pixel;
			}
			p.p1.set(l.pixel);
			p.lastUsed = frameID;
		}

		worldToKey.set(worldToCurr);
		keyToCurr.reset();

		keyFrame = true;
	}

	public void resetTransforms() {
		worldToCurr.reset();
		worldToKey.reset();
		keyToCurr.reset();
	}

	public IT getWorldToCurr() {
		return worldToCurr;
	}

	public IT getWorldToKey() {
		return worldToKey;
	}

	public IT getKeyToCurr() {
		return keyToCurr;
	}

	public PointTracker<I> getTracker() {
		return tracker;
	}

	public ModelMatcher<IT, AssociatedPair> getModelMatcher() {
		return modelMatcher;
	}

	public boolean isKeyFrame() {
		return keyFrame;
	}

	public Class<IT> getModelType() {
		return (Class<IT>)keyToCurr.getClass();
	}

	public long getFrameID() {
		return tracker.getFrameID();
	}
}
