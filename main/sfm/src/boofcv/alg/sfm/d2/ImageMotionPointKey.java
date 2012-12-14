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

package boofcv.alg.sfm.d2;

import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.TrackGeometryManager;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.affine.AffinePointOps;

import java.util.List;

/**
 * Computes the transform from the first image in a sequence to the current frame. Designed to be
 * useful for stabilization and mosaic creation.
 *
 * @author Peter Abeles
 * @param <I> Input image type
 * @param <IT> Motion model data type
 */
@SuppressWarnings("unchecked")
public class ImageMotionPointKey<I extends ImageSingleBand, IT extends InvertibleTransform>
		implements TrackGeometryManager<IT,AssociatedPair>
{
	// total number of frames processed
	protected int totalFramesProcessed = 0;
	// Tracker and motion estimator
	protected ModelAssistedTracker<I, IT,AssociatedPair> tracker;

	// assumed initial transform from the first image to the world
	protected IT worldToInit;

	// transform from the world frame to the key frame
	protected IT worldToKey;
	// transform from key frame to current frame
	protected IT keyToCurr;
	// transform from world to current frame
	protected IT worldToCurr;

	// tracks which are not in the inlier set for this many frames in a row are pruned
	protected int pruneThreshold;

	// if the current frame is a keyframe or not
	protected boolean keyFrame;

	// list of tracks just spawned
	List<PointTrack> spawned;
	// number of detected features
	private int totalSpawned;

	/**
	 * Specify algorithms to use internally.  Each of these classes must work with
	 * compatible data structures.
	 *
	 * @param tracker Feature tracker and motion estimator
	 * @param model Motion model data structure
	 * @param pruneThreshold Tracks not in the inlier set for this many frames in a row are pruned
	 */
	public ImageMotionPointKey(ModelAssistedTracker<I, IT,AssociatedPair> tracker,
							   IT model , int pruneThreshold )
	{
		this.tracker = tracker;
		this.pruneThreshold = pruneThreshold;

		tracker.setTrackGeometry(this);

		worldToInit = (IT)model.createInstance();
		worldToKey = (IT)model.createInstance();
		keyToCurr = (IT)model.createInstance();
		worldToCurr = (IT)model.createInstance();
	}

	/**
	 * Specifies the initially assumed transform from the world frame
	 * to the first image.
	 *
	 * @param worldToInit The transform.
	 */
	public void setInitialTransform( IT worldToInit) {
		this.worldToInit.set(worldToInit);
		this.keyToCurr.set(worldToInit);
		this.worldToCurr.set(worldToInit);
	}

	/**
	 * Makes the current frame the first frame and discards its past history
	 */
	public void reset() {
		worldToKey.set(worldToInit);
		keyToCurr.set(worldToInit);
		worldToCurr.set(worldToInit);
		totalFramesProcessed = 0;
		changeKeyFrame();
	}

	/**
	 * Transforms the world frame into another coordinate system.
	 *
	 * @param oldWorldToNewWorld Transform from the old world frame to the new world frame
	 */
	public void changeWorld(IT oldWorldToNewWorld) {

		IT worldToKey = (IT) this.worldToKey.invert(null);
		worldToInit.concat(worldToKey, oldWorldToNewWorld);

		this.worldToKey.set(worldToInit);
		this.worldToKey.concat(keyToCurr, worldToCurr);
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

		totalFramesProcessed++;

		if( !tracker.foundModel() ) {
			return false;
		}

		keyToCurr.set(tracker.getModel());

		// mark that the track is in the inlier set
		for( AssociatedPair p : tracker.getMatchSet() ) {
			((AssociatedPairTrack)p).lastUsed = totalFramesProcessed;
		}

		// Update the motion
		worldToKey.concat(keyToCurr, worldToCurr);

		// prune tracks which aren't being used
		List<PointTrack> all = tracker.getAllTracks(null);
		for( PointTrack t : all ) {
			AssociatedPairTrack p = t.getCookie();

			if( totalFramesProcessed - p.lastUsed >= pruneThreshold ) {
				tracker.dropTrack(t);
			}
		}

		return true;
	}

	@Override
	public boolean handleSpawnedTrack(PointTrack track) {
		AssociatedPairTrack p = track.getCookie();
		if( p == null ) {
			track.cookie = p = new AssociatedPairTrack();
			// little bit of trickery here.  Save the reference so that the point
			// in the current frame is updated for free as PointTrack is
			p.p2 = track;
		}
		p.p1.set(track);
		p.lastUsed = totalFramesProcessed;

		return true;
	}

	@Override
	public AssociatedPair extractGeometry(PointTrack track) {
		return (AssociatedPair)track.cookie;
	}

	@Override
	public Point2D_F64 predict(IT it, PointTrack track) {
		// TODO total hack.  need to handle model in a generic way
		Point2D_F64 ret = new Point2D_F64();

		AssociatedPair p = track.getCookie();

		AffinePointOps.transform((Affine2D_F64)it, p.p1, ret);

		return ret;
	}

	/**
	 * Make the current frame the first frame in the sequence
	 */
	public void changeKeyFrame() {
		tracker.dropAllTracks();
		tracker.spawnTracks();

		spawned = tracker.getNewTracks(null);
		for( PointTrack t : spawned )
			handleSpawnedTrack(t);

		totalSpawned = spawned.size();
		worldToKey.set(worldToCurr);
		keyFrame = true;
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

	public ModelAssistedTracker<I, IT,AssociatedPair> getTracker() {
		return tracker;
	}

	public int getTotalFramesProcessed() {
		return totalFramesProcessed;
	}

	public int getTotalSpawned() {
		return totalSpawned;
	}

	public boolean isKeyFrame() {
		return keyFrame;
	}
}
