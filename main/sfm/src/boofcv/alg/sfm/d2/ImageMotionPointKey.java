/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.struct.ImageRectangle_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
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
{
	// total number of frames processed
	protected int totalFramesProcessed = 0;
	// feature tracker
	protected PointTracker<I> tracker;
	// Fits a model to the tracked features
	protected ModelMatcher<IT,AssociatedPair> modelMatcher;
	// Refines the model using the complete inlier set
	protected ModelFitter<IT,AssociatedPair> modelRefiner;

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

	// number of detected features
	private int totalSpawned;


	// computes the fraction of the screen which contains inlier points
	private ImageRectangle_F64 contRect = new ImageRectangle_F64();
	protected double contFraction;

	/**
	 * Specify algorithms to use internally.  Each of these classes must work with
	 * compatible data structures.
	 *
	 * @param tracker feature tracker
	 * @param modelMatcher Fits model to track data
	 * @param modelRefiner (Optional) Refines the found model using the entire inlier set. Can be null.
	 * @param model Motion model data structure
	 * @param pruneThreshold Tracks not in the inlier set for this many frames in a row are pruned
	 */
	public ImageMotionPointKey(PointTracker<I> tracker,
							   ModelMatcher<IT, AssociatedPair> modelMatcher,
							   ModelFitter<IT,AssociatedPair> modelRefiner,
							   IT model ,
							   int pruneThreshold )
	{
		this.tracker = tracker;
		this.modelMatcher = modelMatcher;
		this.modelRefiner = modelRefiner;
		this.pruneThreshold = pruneThreshold;

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

		List<PointTrack> tracks = tracker.getActiveTracks(null);

		if( tracks.size() == 0 )
			return false;

		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for( PointTrack t : tracks ) {
			pairs.add((AssociatedPair)t.getCookie());
		}

		// fit the motion model to the feature tracks
		if( !modelMatcher.process((List)pairs) ) {
			return false;
		}

		keyToCurr.set(modelMatcher.getModel());

		// mark that the track is in the inlier set and compute the containment rectangle
		contRect.x0 = contRect.y0 = Double.MAX_VALUE;
		contRect.x1 = contRect.y1 = -Double.MAX_VALUE;
		for( AssociatedPair p : modelMatcher.getMatchSet() ) {
			((AssociatedPairTrack)p).lastUsed = totalFramesProcessed;

			Point2D_F64 t = p.p2;
			if( t.x > contRect.x1 )
				contRect.x1 = t.x;
			if( t.y > contRect.y1 )
				contRect.y1 = t.y;
			if( t.x < contRect.x0 )
				contRect.x0 = t.x;
			if( t.y < contRect.y0 )
				contRect.y0 = t.y;
		}
		contFraction = contRect.area()/(frame.width*frame.height);

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

	/**
	 * Make the current frame the first frame in the sequence
	 */
	public void changeKeyFrame() {
		tracker.dropAllTracks();
		tracker.spawnTracks();

		List<PointTrack> spawned = tracker.getNewTracks(null);

		for( PointTrack l : spawned ) {
			AssociatedPairTrack p = l.getCookie();
			if( p == null ) {
				l.cookie = p = new AssociatedPairTrack();
				// little bit of trickery here.  Save the reference so that the point
				// in the current frame is updated for free as PointTrack is
				p.p2 = l;
			}
			p.p1.set(l);
			p.lastUsed = totalFramesProcessed;
		}

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

	public PointTracker<I> getTracker() {
		return tracker;
	}

	public ModelMatcher<IT, AssociatedPair> getModelMatcher() {
		return modelMatcher;
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
