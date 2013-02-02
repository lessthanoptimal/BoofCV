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
import boofcv.struct.image.ImageBase;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO Comment
 *
 * @author Peter Abeles
 */
public class ImageMotionPtkSmartRespawn<I extends ImageBase, IT extends InvertibleTransform> {
	ImageMotionPointTrackerKey<I,IT> motion;

	// used to prune feature tracks which are too close together
	protected PruneCloseTracks pruneClose = new PruneCloseTracks(3,1,1);

	// stores list of tracks to prune
	List<PointTrack> prune = new ArrayList<PointTrack>();

	int absoluteMinimumTracks;
	double respawnTrackFraction;
	double respawnCoverageFraction;

	boolean previousWasKeyFrame = false;

	// computes the fraction of the screen which contains inlier points
	private ImageRectangle_F64 contRect = new ImageRectangle_F64();
	// fraction of area covered by containment rectangle
	protected double contFraction;
	// maximum fraction of area covered by containment rectangle
	private double maxCoverage;

	IT firstToKey;

	public ImageMotionPtkSmartRespawn(ImageMotionPointTrackerKey<I, IT> motion,
									  int absoluteMinimumTracks, double respawnTrackFraction,
									  double respawnCoverageFraction) {
		this.motion = motion;

		firstToKey = (IT) motion.getKeyToCurr().createInstance();

		this.absoluteMinimumTracks = absoluteMinimumTracks;
		this.respawnTrackFraction = respawnTrackFraction;
		this.respawnCoverageFraction = respawnCoverageFraction;
	}

	public boolean process(I input) {
		if( !motion.process(input) )
			return false;

		// todo add a check to see if it is a keyframe or not- two spawns on first frame
		boolean setKeyFrame = false;

		PointTracker<I> tracker = motion.getTracker();
		List<AssociatedPair> pairs = motion.getModelMatcher().getMatchSet();

		computeContainment(input.width*input.height);

		if( previousWasKeyFrame ) {
			previousWasKeyFrame = false;

			int width = input.width;
			int height = input.height;
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
				motion.changeKeyFrame(false);
			}
		} else {
			// look at the track distribution to see if new ones should be spawned
			int matchSetSize = pairs.size();
			if( matchSetSize < motion.getTotalSpawned()* respawnTrackFraction  || matchSetSize < absoluteMinimumTracks ) {
				setKeyFrame = true;
			}

			if( contFraction < respawnCoverageFraction *maxCoverage ) {
				setKeyFrame = true;
			}
		}

		if(setKeyFrame) {
			motion.changeKeyFrame(false);
			previousWasKeyFrame = true;
		}

		if( motion.isKeyFrame() ) {
			previousWasKeyFrame = true;
		}

		return true;
	}

	/**
	 * Computes an axis-aligned rectangle that contains all the inliers.  It then computes the area contained in
	 * that rectangle to the total area of the image
	 *
	 * @param imageArea width*height
	 */
	private void computeContainment( int imageArea ) {
		// mark that the track is in the inlier set and compute the containment rectangle
		contRect.x0 = contRect.y0 = Double.MAX_VALUE;
		contRect.x1 = contRect.y1 = -Double.MAX_VALUE;
		for( AssociatedPair p : motion.getModelMatcher().getMatchSet() ) {
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
		contFraction = contRect.area()/imageArea;
	}

	public ImageMotionPointTrackerKey<I, IT> getMotion() {
		return motion;
	}
}
