/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d2.stabilization;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;

import java.util.List;

/**
 * @author Peter Abeles
 */
// todo create a common alg with stabilize?
// todo create a class just for rending the point
public class MosaicImagePointKey<I extends ImageSingleBand, T extends InvertibleTransform> {

	// number of detected features
	private int totalSpawned;

	boolean firstFrame = true;
	ImagePointTracker<I> tracker;
	ModelMatcher<T,AssociatedPair> modelMatcher;
	PruneCloseTracks pruneClose = new PruneCloseTracks(3,1,1);

	// transform of image to world
	T Hinit;

	// transform from the world frame to the key frame
	T worldToKey;
	// transform from key frame to current frame
	T keyToCurr;
	// transform from world to current frame
	T worldToCurr;

	public MosaicImagePointKey(ImagePointTracker<I> tracker,
							   ModelMatcher<T, AssociatedPair> modelMatcher,
							   T model )
	{
		this.tracker = tracker;
		this.modelMatcher = modelMatcher;
		
		Hinit = (T)model.createInstance();
		worldToKey = (T)model.createInstance();
		keyToCurr = (T)model.createInstance();
		worldToCurr = (T)model.createInstance();
	}

	public void setInitialTransform( T H ) {
		Hinit.set(H);
	}

	/**
	 * Makes the current frame the first frame and discards the part history
	 */
	public void reset() {
		worldToKey.set(Hinit);
		keyToCurr.set(Hinit);
		worldToCurr.set(Hinit);
		tracker.setCurrentToKeyFrame();
		tracker.spawnTracks();
	}

	public void refocus( T oldWorldToNewWorld ) {

		T worldToKey = (T)this.worldToKey.invert(null);
		Hinit.concat(worldToKey, oldWorldToNewWorld);

		this.worldToKey.set(Hinit);
		this.worldToKey.concat(keyToCurr, worldToCurr);
	}

	public boolean process( I frame ) {
		tracker.process(frame);
		
		if( firstFrame ) {
			firstFrame = false;
			tracker.spawnTracks();
			worldToKey.set(Hinit);
			worldToCurr.set(Hinit);
			pruneClose.resize(frame.width,frame.height);
			return true;
		}

		List<AssociatedPair> pairs = tracker.getActiveTracks();
		if( !modelMatcher.process(pairs,null) ) {
			return false;
		}

		keyToCurr.set(modelMatcher.getModel());

		worldToKey.concat(keyToCurr, worldToCurr);


		return true;
	}

	public void changeKeyFrame() {
		tracker.setCurrentToKeyFrame();
		tracker.spawnTracks();
//		maxCoverage = imageCoverageFraction(width, height,tracker.getActiveTracks());
//
//		// for some trackers like KLT, they keep old features and these features can get squeezed together
//		// this will remove some of the really close features
//		if( maxCoverage < respawnCoverageFraction) {
//			// prune some of the ones which are too close
//			pruneClose.process(tracker);
//			// see if it can find some more in diverse locations
//			tracker.spawnTracks();
//			maxCoverage = imageCoverageFraction(width, height,tracker.getActiveTracks());
//		}

		totalSpawned = tracker.getActiveTracks().size();
		worldToKey.set(worldToCurr);
	}

	public T getWorldToCurr() {
		return worldToCurr;
	}

	public T getWorldToKey() {
		return worldToKey;
	}

	public T getKeyToCurr() {
		return keyToCurr;
	}

	public ImagePointTracker<I> getTracker() {
		return tracker;
	}

	public ModelMatcher<T, AssociatedPair> getModelMatcher() {
		return modelMatcher;
	}

	public boolean getHasProcessedImage() {
		return !firstFrame;
	}

	public int getTotalSpawned() {
		return totalSpawned;
	}
}
