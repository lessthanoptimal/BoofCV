/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.InvertibleTransform;

/**
 * Extension of {@link ImageMotionPointKey} specifically designed to stabilize image motion.  The algorithm
 * attempts to minimize the difference in pixel difference between the first image and the current image.
 * If the difference between the current frame and the first frame becomes too large then the current
 * frame becomes the new first frame.
 * 
 * @author Peter Abeles
 */
public class MotionStabilizePointKey<I extends ImageSingleBand, T extends InvertibleTransform>
	extends ImageMotionPointKey<I,T>
{
	// a new keyframe is selected when there are fewer than this number of inliers
	private int thresholdKeyFrame;
	// stabilization is reset when there are fewer than this number of inliers
	private int thresholdReset;
	// largest motion along one axis which is allowed before a reset
	private int largeMotionThreshold;

	// if the image being stabilized was changed
	boolean isReset;
	// if the internal keyframe has changed
	boolean isKeyFrame;
	
	/**
	 * Specify algorithms to use internally.  Each of these classes must work with
	 * compatible data structures.
	 *
	 * @param tracker feature tracker
	 * @param modelMatcher Fits model to track data
	 * @param model Motion model data structure
	 * @param thresholdKeyFrame  If the number of inlier is less than this number the keyframe will change.
	 * @param thresholdReset If the number of inlier is less than this number a reset will occur.
	 * @param largeMotionThreshold  If the transform from the key frame to the current frame is more than this a reset will occur.
	 */
	public MotionStabilizePointKey(ImagePointTracker<I> tracker, 
								   ModelMatcher<T, AssociatedPair> modelMatcher, 
								   T model ,
								   int thresholdKeyFrame , int thresholdReset ,
								   int largeMotionThreshold )
	{
		super(tracker, modelMatcher, model);

		if( thresholdKeyFrame < thresholdReset ) {
			throw new IllegalArgumentException("Threshold for key frame should be more than reset");
		}
		
		this.thresholdKeyFrame = thresholdKeyFrame;
		this.thresholdReset = thresholdReset;
		this.largeMotionThreshold = largeMotionThreshold;
	}
	
	@Override
	public boolean process( I frame ) {
		if( !super.process(frame) )
			return false;

		isReset = false;
		isKeyFrame = false;

		int inliers = modelMatcher.getMatchSet().size();

		// too few features to have a reliable motion estimate?
		if( inliers < thresholdReset ) {
			isReset = true;
			reset();
		} else if( inliers < thresholdKeyFrame ) {
			// too few features in common with the current key frame?
			isKeyFrame = true;
			changeKeyFrame();
		}

		PixelTransform_F32 local = UtilImageMotion.createPixelTransform(keyToCurr);

		// sudden very large movements tend to be divergence
		// check for four corners for large changes
		int w = frame.width;
		int h = frame.height;
		if( checkLargeDistortion(0,0,local) ||
				checkLargeDistortion(w,0,local) ||
				checkLargeDistortion(w,h,local) ||
				checkLargeDistortion(0,h,local))
		{
			isReset = true;
			reset();
		}

		return true;
	}

	private boolean checkLargeDistortion( int x , int y , PixelTransform_F32 tran )
	{
		tran.compute(x,y);

		if( Math.abs(tran.distX-x) > largeMotionThreshold || Math.abs(tran.distY-y) > largeMotionThreshold ) {
			return true;
		}
		return false;
	}

	public boolean isReset() {
		return isReset;
	}

	public boolean isKeyFrame() {
		return isKeyFrame;
	}
}
