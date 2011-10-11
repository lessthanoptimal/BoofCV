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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;


/**
 * <p/>
 * Only examine around candidate regions for corners
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxCandidateExtractor {

	// size of the search area
	int radius;
	// the threshold which points must be above to be a feature
	float thresh;
	// does not process pixels this close to the image border
	int ignoreBorder;
	// size of the intensity image's border which can't be touched
	private int borderIntensity;

	ImageFloat32 input;
	private boolean processBorders;

	public NonMaxCandidateExtractor(int minSeparation, float thresh, boolean processBorders) {
		setMinSeparation(minSeparation);
		this.thresh = thresh;
		this.processBorders = processBorders;
	}

	public void setMinSeparation(int minSeparation) {
		this.radius = minSeparation;
		this.ignoreBorder = borderIntensity+radius;
	}

	public float getThresh() {
		return thresh;
	}

	public void setThresh(float thresh) {
		this.thresh = thresh;
	}

	public void setBorder( int border ) {
		this.borderIntensity = border;
		this.ignoreBorder = borderIntensity+radius;

	}

	public int getBorder() {
		return borderIntensity;
	}

	/**
	 * Selects a set of features from the provided intensity image and corner candidates.  If a non-empty list of
	 * corners is passed in then those corners will not be added again and similar corners will be excluded.
	 *
	 * @param intensityImage feature intensity image. Can be modified.
	 * @param candidates	 List of candidate locations for features.
	 * @param corners		List of selected features.  If not empty, previously found features will be skipped.
	 */
	public void process(ImageFloat32 intensityImage, QueueCorner candidates, QueueCorner corners) {

		this.input = intensityImage;
		final int w = intensityImage.width-ignoreBorder;
		final int h = intensityImage.height-ignoreBorder;

		final int stride = intensityImage.stride;

		final float inten[] = intensityImage.data;

		for (int iter = 0; iter < candidates.size; iter++) {
			Point2D_I16 pt = candidates.data[iter];


			int center = intensityImage.startIndex + pt.y * stride + pt.x;

			float val = inten[center];
			if (val < thresh || val == Float.MAX_VALUE ) continue;

			// see if its too close to the image edge
			if( pt.x < ignoreBorder || pt.y < ignoreBorder || pt.x >= w || pt.y >= h ) {
				if( processBorders && checkBorder(center, val, pt.x, pt.y))
					corners.add(pt.x,pt.y);
			} else if( checkInner(center, val) )
				corners.add(pt.x, pt.y);
		}
	}

	private boolean checkBorder(int center, float val, int c_x , int c_y )
	{
		int x0 = Math.max(borderIntensity,c_x-radius);
		int y0 = Math.max(borderIntensity,c_y-radius);
		int x1 = Math.min(input.width - borderIntensity, c_x + radius + 1);
		int y1 = Math.min(input.height-borderIntensity,c_y+radius+1);

		for( int i = y0; i < y1; i++ ) {
			int index = input.startIndex + i * input.stride + x0;
			for( int j = x0; j < x1; j++ , index++ ) {
				// don't compare the center point against itself
				if ( center == index )
					continue;

				if (val <= input.data[index]) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkInner( int center, float val ) {
		for (int i = -radius; i <= radius; i++) {
			int index = center + i * input.stride - radius;
			for (int j = -radius; j <= radius; j++, index++) {
				// don't compare the center point against itself
				if ( index == center )
					continue;

				if (val <= input.data[index]) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean canProcessBorder() {
		return processBorders;
	}
}
