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
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.homo.UtilHomography;

import java.util.List;

/**
 * @author Peter Abeles
 */
// todo create a common alg with stabilize?
// todo create a class just for rending the point
public class MosaicImagePointKey<I extends ImageSingleBand> {

	int absoluteMinimumTracks = 50;
	double respawnTrackFraction = 0.5;
	// number of detected features
	int totalSpawned;

	// if less than this fraction of the window is convered by features switch views
	double respawnCoverageFraction = 0.80;
	// coverage right after spawning new features
	double maxCoverage;

	boolean firstFrame = true;
	boolean keyFrameChanged = false;

	ImagePointTracker<I> tracker;
	ModelMatcher<Object,AssociatedPair> modelMatcher;
	PruneCloseTracks pruneClose = new PruneCloseTracks(3,1,1);

	// transform of image to world
	Homography2D_F32 Hinit = new Homography2D_F32(1,0,0,0,1,0,0,0,1);

	// transform from the world frame to the key frame
	Homography2D_F32 worldToKey = new Homography2D_F32();
	// transform from key frame to current frame
	Homography2D_F32 keyToCurr = new Homography2D_F32();
	// transform from world to current frame
	Homography2D_F32 worldToCurr = new Homography2D_F32();

	public MosaicImagePointKey(ImagePointTracker<I> tracker,
							   ModelMatcher<Object, AssociatedPair> modelMatcher )
	{
		this.tracker = tracker;
		this.modelMatcher = modelMatcher;
	}

	public void setInitialTransform( Homography2D_F32 H ) {
		Hinit.set(H);
	}

	public void refocus( Homography2D_F32 oldWorldToNewWorld ) {

		Homography2D_F32 worldToKey = this.worldToKey.invert(null);
		Hinit.concat(worldToKey, oldWorldToNewWorld);

		this.worldToKey.set(Hinit);
		this.worldToKey.concat(keyToCurr, worldToCurr);
	}

	public void process( I frame ) {
		keyFrameChanged = false;

		tracker.process(frame);
		
		if( firstFrame ) {
			firstFrame = false;
			tracker.spawnTracks();
			maxCoverage = imageCoverageFraction(frame.width,frame.height, tracker.getActiveTracks());
			worldToKey.set(Hinit);
			pruneClose.resize(frame.width,frame.height);
		}

		List<AssociatedPair> pairs = tracker.getActiveTracks();
		if( !modelMatcher.process(pairs,null) ) {
			System.out.println("crap");
			return;
		}

		convertModelToHomography(keyToCurr, modelMatcher.getModel());

		worldToKey.concat(keyToCurr, worldToCurr);

		double fractionCovered = imageCoverageFraction(frame.width,frame.height,pairs);
		int matchSetSize = modelMatcher.getMatchSet().size();

		if( fractionCovered < respawnCoverageFraction *maxCoverage ||
				matchSetSize <absoluteMinimumTracks || matchSetSize < totalSpawned* respawnTrackFraction) {
			System.out.println("change key frame: "+(fractionCovered/maxCoverage)+"  "+matchSetSize);
			spawnTracks(frame.width,frame.height);
			worldToKey.set(worldToCurr);
		}
	}

	private void spawnTracks( int width , int height ) {
		keyFrameChanged = true;
		tracker.setCurrentToKeyFrame();
		tracker.spawnTracks();
		maxCoverage = imageCoverageFraction(width, height,tracker.getActiveTracks());

		// for some trackers like KLT, they keep old features and these features can get squeezed together
		// this will remove some of the really close features
		if( maxCoverage < respawnCoverageFraction) {
			// prune some of the ones which are too close
			pruneClose.process(tracker);
			// see if it can find some more in diverse locations
			tracker.spawnTracks();
			maxCoverage = imageCoverageFraction(width, height,tracker.getActiveTracks());
		}

		totalSpawned = tracker.getActiveTracks().size();
	}

	public Homography2D_F32 getWorldToCurr() {
		return worldToCurr;
	}

	public Homography2D_F32 getWorldToKey() {
		return worldToKey;
	}

	public ImagePointTracker<I> getTracker() {
		return tracker;
	}

	public ModelMatcher<Object, AssociatedPair> getModelMatcher() {
		return modelMatcher;
	}

	private double imageCoverageFraction( int width , int height , List<AssociatedPair> tracks ) {
		double x0 = width;
		double x1 = 0;
		double y0 = height;
		double y1 = 0;

		for( AssociatedPair p : tracks ) {
			if( p.currLoc.x < x0 )
				x0 = p.currLoc.x;
			if( p.currLoc.x >= x1 )
				x1 = p.currLoc.x;
			if( p.currLoc.y < y0 )
				y0 = p.currLoc.y;
			if( p.currLoc.y >= y1 )
				y1 = p.currLoc.y;
		}
		return ((x1-x0)*(y1-y0))/(width*height);
	}

	private void convertModelToHomography( Homography2D_F32 currToKey , Object m ) {
		if( m instanceof Affine2D_F64) {
			Affine2D_F64 affine = (Affine2D_F64)m;

			currToKey.a11 = (float)affine.a11;
			currToKey.a12 = (float)affine.a12;
			currToKey.a21 = (float)affine.a21;
			currToKey.a22 = (float)affine.a22;
			currToKey.a13 = (float)affine.tx;
			currToKey.a23 = (float)affine.ty;
			currToKey.a31 = 0;
			currToKey.a32 = 0;
			currToKey.a33 = 1;
		} else if( m instanceof Homography2D_F64) {
			Homography2D_F64 h = (Homography2D_F64)m;

			UtilHomography.convert(h, currToKey);

		} else {
			throw new RuntimeException("Unexpected type: "+m.getClass().getSimpleName());
		}
	}

	public boolean getHasProcessedImage() {
		return !firstFrame;
	}

	public boolean isKeyFrameChanged() {
		return keyFrameChanged;
	}
}
