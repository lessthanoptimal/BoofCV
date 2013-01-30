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
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

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

	// stores list of tracks to prune
	List<PointTrack> prune = new ArrayList<PointTrack>();

	private boolean previousWasKeyFrame;

	/**
	 * Specify algorithms to use internally.  Each of these classes must work with
	 * compatible data structures.
	 *
	 * @param tracker feature tracker
	 * @param modelMatcher Fits model to track data
	 * @param modelRefiner (Optional) Refines the found model using the entire inlier set. Can be null.
	 * @param model Motion model data structure
	 */
	public MotionMosaicPointKey(PointTracker<I> tracker,
								ModelMatcher<T, AssociatedPair> modelMatcher,
								ModelFitter<T,AssociatedPair> modelRefiner,
								T model,
								int absoluteMinimumTracks, double respawnTrackFraction,
								int pruneThreshold ,
								double respawnCoverageFraction)
	{
		super(tracker, modelMatcher, modelRefiner, model,pruneThreshold);

		this.absoluteMinimumTracks = absoluteMinimumTracks;
		this.respawnTrackFraction = respawnTrackFraction;
		this.respawnCoverageFraction = respawnCoverageFraction;
	}

	@Override
	public boolean process( I frame ) {
		if( !super.process(frame) && totalFramesProcessed == 0 )
			return false;

		// todo add a check to see if it is a keyframe or not- two spawns on first frame
		boolean setKeyFrame = false;

		List<AssociatedPair> pairs = modelMatcher.getMatchSet();

		if( previousWasKeyFrame ) {
			previousWasKeyFrame = false;

			int width = frame.width;
			int height = frame.height;
			maxCoverage = contFraction;

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
			}
		} else {
			// look at the track distribution to see if new ones should be spawned
			int matchSetSize = pairs.size();
			if( matchSetSize < getTotalSpawned()* respawnTrackFraction  || matchSetSize < absoluteMinimumTracks ) {
				setKeyFrame = true;
			}

			if( contFraction < respawnCoverageFraction *maxCoverage ) {
				setKeyFrame = true;
			}
		}

		if(setKeyFrame) {
			changeKeyFrame();
			previousWasKeyFrame = true;
		}

		return true;
	}

}
