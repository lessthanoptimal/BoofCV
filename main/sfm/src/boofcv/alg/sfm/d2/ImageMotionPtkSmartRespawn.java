/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
 * Examines tracks inside of {@link ImageMotionPointTrackerKey} and decides when new feature tracks should be respawned.
 * Tracks are respawned when an absolute minimum is reached, when the number of inliers has dropped past a certain
 * threshold, and when the area covered by the inliers decreases by too much.  Prunes clusters of closely packed points.
 * These tend to be non-informative and use up computational resources.
 *
 * @author Peter Abeles
 */
public class ImageMotionPtkSmartRespawn<I extends ImageBase, IT extends InvertibleTransform> {

	// estimate image motion
	private ImageMotionPointTrackerKey<I,IT> motion;

	// used to prune feature tracks which are too close together
	protected PruneCloseTracks pruneClose = new PruneCloseTracks(3,1,1);

	// stores list of tracks to prune
	private List<PointTrack> prune = new ArrayList<>();

	// change the keyframe is the number of inliers drops below this number
	private int absoluteMinimumTracks;
	// change the keyframe if the number of inliers has dropped this fraction from the original number
	private double relativeInlierFraction;
	// change the keyframe if the area covered has dropped this fraction from the original number
	private double respawnCoverageFraction;

	// if true, the previous image processed was a key-frame
	protected boolean previousWasKeyFrame = false;

	// computes the fraction of the screen which contains inlier points
	private ImageRectangle_F64 contRect = new ImageRectangle_F64();
	// fraction of area covered by containment rectangle
	protected double containment;
	// containment after a new keyframe.
	private double maxContainment;
	// number of inliers after a new keyframe.
	private int maxInliers;

	/**
	 * Specifies internal algorithms and reset parameters.
	 *
	 * @param motion Algorithm for estimating image motion
	 * @param absoluteMinimumTracks Create new key-frame if number of inliers drops below this number
	 * @param relativeInlierFraction Create new key-frame if ratio of inliers to the original count drops below this number
	 * @param respawnCoverageFraction Create new key-frame if ratio point area coverage below this number
	 */
	public ImageMotionPtkSmartRespawn(ImageMotionPointTrackerKey<I, IT> motion,
									  int absoluteMinimumTracks, double relativeInlierFraction,
									  double respawnCoverageFraction) {
		this.motion = motion;

		this.absoluteMinimumTracks = absoluteMinimumTracks;
		this.relativeInlierFraction = relativeInlierFraction;
		this.respawnCoverageFraction = respawnCoverageFraction;
	}

	public boolean process(I input) {
		if( !motion.process(input) )
			return false;

		boolean setKeyFrame = false;

		PointTracker<I> tracker = motion.getTracker();
		// extract features which fit the motion model after outliers are removed
		List<AssociatedPair> inliers = motion.getModelMatcher().getMatchSet();
		int inlierSetSize = inliers.size();

		computeContainment(input.width*input.height);

		// check an absolute threshold
		if( inlierSetSize < absoluteMinimumTracks ) {
			setKeyFrame = true;
		} else if( previousWasKeyFrame ) {
			// Make the containment threshold relative to take in account cases with large textureless regions
			// do it after processing one frame since non-informative tracks are more likely to be dropped
			previousWasKeyFrame = false;
			maxContainment = containment;
			maxInliers = inliers.size();
		} else {
			// look at relative thresholds here
			if( inlierSetSize < maxInliers * relativeInlierFraction ) {
				setKeyFrame = true;
			}

			// change the keyframe is not enough of the screen is covered by freatures
			if( containment < respawnCoverageFraction * maxContainment) {
				setKeyFrame = true;
			}
		}

		if(setKeyFrame) {
			// use the new keyframe as an opportunity to discard points that are too close.  commonly occurs
			// when zooming out and points cluster together
			pruneClosePoints(tracker, input.width, input.height);
			motion.changeKeyFrame();
			previousWasKeyFrame = true;
		}

		return true;
	}

	private void pruneClosePoints(PointTracker<I> tracker, int width, int height) {
		pruneClose.resize(width,height);
		// prune some of the ones which are too close
		prune.clear();
		pruneClose.process(tracker.getActiveTracks(null),prune);
		for( PointTrack t : prune ) {
			tracker.dropTrack(t);
		}
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
		containment = contRect.area()/imageArea;
	}

	public ImageMotionPointTrackerKey<I, IT> getMotion() {
		return motion;
	}
}
