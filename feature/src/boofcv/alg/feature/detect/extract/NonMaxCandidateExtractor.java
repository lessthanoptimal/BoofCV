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
import jgrl.struct.point.Point2D_I16;


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

	public NonMaxCandidateExtractor(int minSeparation, float thresh) {
		this.radius = minSeparation;
		this.thresh = thresh;
	}

	public void setMinSeparation(int minSeparation) {
		this.radius = minSeparation;
	}

	public float getThresh() {
		return thresh;
	}

	public void setThresh(float thresh) {
		this.thresh = thresh;
	}

	/**
	 * Selects a set of features from the provided intensity image and corner candidates.  If a non-empty list of
	 * corners is passed in then those corners will not be added again and similar corners will be excluded.
	 *
	 * @param intensityImage feature intensity image. Can be modified.
	 * @param candidates	 List of candidate locations for features.
	 * @param corners		List of selected features.  If not empty, previously found features will be skipped.
	 */
	public void process(ImageFloat32 intensityImage, QueueCorner candidates, QueueCorner excludeCorners, QueueCorner corners) {
		// mark corners which have already been found
		if( excludeCorners != null ) {
			for (int i = 0; i < excludeCorners.num; i++) {
				Point2D_I16 pt = excludeCorners.get(i);
				intensityImage.set(pt.x, pt.y, Float.MAX_VALUE);
			}
		}

		final int w = intensityImage.width-radius;
		final int h = intensityImage.height-radius;

		final int stride = intensityImage.stride;

		final float inten[] = intensityImage.data;

		for (int iter = 0; iter < candidates.num; iter++) {
			Point2D_I16 pt = candidates.points[iter];

			// see if its too close to the image edge
			if( pt.x < radius || pt.y < radius || pt.x >= w || pt.y >= h )
				continue;

			int center = intensityImage.startIndex + pt.y * stride + pt.x;

			float val = inten[center];
			if (val < thresh) continue;

			boolean max = true;

			escape:
			for (int i = -radius; i <= radius; i++) {
				int index = center + i * stride - radius;
				for (int j = -radius; j <= radius; j++, index++) {
					// don't compare the center point against itself
					if (i == 0 && j == 0)
						continue;

					if (val <= inten[index]) {
						max = false;
						break escape;
					}
				}
			}

			if (max && val != Float.MAX_VALUE) {
				corners.add(pt.x, pt.y);
			}
		}
	}
}
