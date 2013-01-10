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
public class NonMaxCandidateStrict {

	// size of the search area
	int radius;
	// the threshold which points must be above to be a feature
	float thresh;
	// does not process pixels this close to the image border
	int ignoreBorder;

	protected ImageFloat32 input;

	public NonMaxCandidateStrict(int searchRadius, float thresh , int ignoreBorder ) {
		setSearchRadius(searchRadius);
		this.thresh = thresh;
		this.ignoreBorder = ignoreBorder;
	}

	public NonMaxCandidateStrict() {
	}

	public void setSearchRadius(int radius) {
		this.radius = radius;
	}

	public float getThresh() {
		return thresh;
	}

	public void setThresh(float thresh) {
		this.thresh = thresh;
	}

	public void setBorder( int border ) {
		this.ignoreBorder = border;

	}

	public int getBorder() {
		return ignoreBorder;
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

		// pixels indexes larger than these should not be examined
		int endX = intensityImage.width-ignoreBorder;
		int endY = intensityImage.height-ignoreBorder;

		final int stride = intensityImage.stride;

		final float inten[] = intensityImage.data;

		for (int iter = 0; iter < candidates.size; iter++) {
			Point2D_I16 pt = candidates.data[iter];

			if( pt.x < ignoreBorder || pt.y < ignoreBorder || pt.x >= endX || pt.y >= endY )
				continue;

			int center = intensityImage.startIndex + pt.y * stride + pt.x;

			float val = inten[center];
			if (val < thresh || val == Float.MAX_VALUE ) continue;

			int x0 = Math.max(ignoreBorder,pt.x - radius);
			int y0 = Math.max(ignoreBorder,pt.y - radius);
			int x1 = Math.min(endX, pt.x + radius + 1);
			int y1 = Math.min(endY, pt.y + radius + 1);

			boolean success = true;
			failed:
			for( int i = y0; i < y1; i++ ) {
				int index = input.startIndex + i * input.stride + x0;
				for( int j = x0; j < x1; j++ , index++ ) {
					// don't compare the center point against itself
					if ( center == index )
						continue;

					if (val <= input.data[index]) {
						success = false;
						break failed;
					}
				}
			}
			if( success )
				corners.add(pt.x,pt.y);
		}
	}

	public int getSearchRadius() {
		return radius;
	}
}
