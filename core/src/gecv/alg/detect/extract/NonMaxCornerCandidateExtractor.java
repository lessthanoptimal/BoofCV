/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.extract;

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;
import pja.geometry.struct.point.Point2D_I16;


/**
 * <p>
 * Only examine around candidate regions for corners
 * <p/>
 * 
 * @author Peter Abeles
 */
public class NonMaxCornerCandidateExtractor {

	// size of the search area
	int radius;
	// the threshold which points must be above to be a feature
	float thresh;

	public NonMaxCornerCandidateExtractor(int minSeparation, float thresh) {
		this.radius = minSeparation;
		this.thresh = thresh;
	}

	public void setMinSeparation(int minSeparation) {
		this.radius = minSeparation;
	}

	public void setThresh(float thresh) {
		this.thresh = thresh;
	}

	/**
	 * Selects a set of features from the provided intensity image and corner candidates.
	 *
	 * @param intensityImage feature intensity image.
	 * @param candidates List of candidate locations for features.
	 * @param corners List of selected features.
	 */
	public void process(ImageFloat32 intensityImage, QueueCorner candidates , QueueCorner corners) {
		corners.reset();

		final int stride = intensityImage.stride;

		final float inten[] = intensityImage.data;

		for( int iter = 0; iter < candidates.num; iter++ ) {
			Point2D_I16 pt = candidates.points[iter];

			int center = intensityImage.startIndex + pt.y * stride + pt.x;

			float val = inten[center];
			if (val < thresh) continue;

			boolean max = true;

			escape:
			for (int i = -radius; i <= radius; i++) {
				int index = center +i*stride-radius;
				for (int j = -radius; j <= radius; j++,index++) {
					// don't compare the center point against itself
					if (i == 0 && j == 0)
						continue;

					if (val <= inten[index]) {
						max = false;
						break escape;
					}
				}
			}

			if (max) {
				corners.add(pt.x, pt.y);
			}
		}
	}
}
