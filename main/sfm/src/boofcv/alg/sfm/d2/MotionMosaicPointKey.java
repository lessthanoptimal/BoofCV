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

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.numerics.fitting.modelset.ModelFitter;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link ImageMotionPointKey} specifically designed
 * for creating image mosaics.  Each new image is laid on top of the previous images based on its estimated
 * motion to the original.
 *
 * @author Peter Abeles
 */
public class MotionMosaicPointKey<I extends ImageSingleBand, T extends InvertibleTransform>
	extends ImageMotionPointKey<I,T>
{
	int absoluteMinimumTracks = 40;
	double respawnTrackFraction = 0.7;
	// if less than this fraction of the window is convered by features switch views
	double respawnCoverageFraction = 0.8;

	// used to prune feature tracks which are too close together
	protected PruneCloseTracks pruneClose = new PruneCloseTracks(3,1,1);

	// coverage right after spawning new features
	double maxCoverage;

	// if the internal key frame has changed
	boolean keyFrame;
	
	// stores list of tracks to prune
	List<PointTrack> prune = new ArrayList<PointTrack>();

	/**
	 * Specify algorithms to use internally.  Each of these classes must work with
	 * compatible data structures.
	 *
	 * @param tracker feature tracker
	 * @param modelMatcher Fits model to track data
	 * @param modelRefiner (Optional) Refines the found model using the entire inlier set. Can be null.
	 * @param model Motion model data structure
	 */
	public MotionMosaicPointKey(ImagePointTracker<I> tracker,
								ModelMatcher<T, AssociatedPair> modelMatcher,
								ModelFitter<T,AssociatedPair> modelRefiner,
								T model,
								int absoluteMinimumTracks, double respawnTrackFraction,
								double respawnCoverageFraction)
	{
		super(tracker, modelMatcher, modelRefiner, model);
		
		this.absoluteMinimumTracks = absoluteMinimumTracks;
		this.respawnTrackFraction = respawnTrackFraction;
		this.respawnCoverageFraction = respawnCoverageFraction;
	}
	
	@Override
	public boolean process( I frame ) {
		if( !super.process(frame) )
			return false;

		// todo add a check to see if it is a keyframe or not- two spawns on first frame
		keyFrame = false;

		int matchSetSize = modelMatcher.getMatchSet().size();
		if( matchSetSize < getTotalSpawned()* respawnTrackFraction  || matchSetSize < absoluteMinimumTracks ) {
			keyFrame = true;
		}

		List<AssociatedPair> pairs = modelMatcher.getMatchSet();
		
		double fractionCovered = imageCoverageFraction(frame.width,frame.height,pairs);
		if( fractionCovered < respawnCoverageFraction *maxCoverage ) {
			keyFrame = true;
		}
		
		if(keyFrame) {
			changeKeyFrame();

			int width = frame.width;
			int height = frame.height;

			maxCoverage = imageCoverageFraction(width, height,pairs);

			// for some trackers, like KLT, they keep old features and these features can get squeezed together
			// this will remove some of the really close features
			if( maxCoverage < respawnCoverageFraction) {
				pruneClose.resize(width,height);
				// prune some of the ones which are too close
				prune.clear();
				pruneClose.process(tracker.getActiveTracks(null),prune);
				for( PointTrack t : prune ) {
					tracker.dropTrack(t);
				}
				// see if it can find some more in diverse locations
				changeKeyFrame();

				maxCoverage = imageCoverageFraction(width, height,pairs);
			}
		}

		return true;
	}

	/**
	 * Finds the minimal axis aligned rectangle in the image which will contain all the features in the current frame
	 * then computes the fraction of the total image which it covers.
	 *
	 * @param width image width
	 * @param height image height
	 * @param tracks current tracks
	 * @return coverage fraction 0 to 1
	 */
	public static double imageCoverageFraction( int width , int height , List<AssociatedPair> tracks ) {
		double x0 = width;
		double x1 = 0;
		double y0 = height;
		double y1 = 0;

		for( AssociatedPair p : tracks ) {
			if( p.p2.x < x0 )
				x0 = p.p2.x;
			if( p.p2.x >= x1 )
				x1 = p.p2.x;
			if( p.p2.y < y0 )
				y0 = p.p2.y;
			if( p.p2.y >= y1 )
				y1 = p.p2.y;
		}
		return ((x1-x0)*(y1-y0))/(width*height);
	}

	public boolean isKeyFrame() {
		return keyFrame;
	}
}
