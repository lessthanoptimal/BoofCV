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


/**
 * <p/>
 * Extracts corners at local maximums that are above a threshold.  Basic unoptimized implementation.
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxExtractorNaive implements NonMaxExtractor {

	// size of the search area
	protected int radius;
	// the threshold which points must be above to be a feature
	protected float thresh;

	protected int border;

	public NonMaxExtractorNaive(int minSeparation, float thresh) {
		this.radius = minSeparation;
		this.thresh = thresh;
	}

	@Override
	public void setMinSeparation(int minSeparation) {
		this.radius = minSeparation;
	}

	@Override
	public float getThresh() {
		return thresh;
	}

	@Override
	public void setThresh(float thresh) {
		this.thresh = thresh;
	}

	@Override
	public void setInputBorder(int border) {
		this.border = border;
	}

	public int getInputBorder() {
		return border;
	}

	@Override
	public void process(ImageFloat32 intensityImage, QueueCorner corners) {

		final int imgWidth = intensityImage.getWidth();
		final int imgHeight = intensityImage.getHeight();
		final int stride = intensityImage.stride;

		final float inten[] = intensityImage.data;

		int imageBorder = Math.max(radius,border);

		for (int y = imageBorder; y < imgHeight - imageBorder; y++) {
			for (int x = imageBorder; x < imgWidth - imageBorder; x++) {
				int center = intensityImage.startIndex + y * stride + x;

				float val = inten[center];
				if (val < thresh) continue;

				boolean max = true;

				escape:
				for (int i = -imageBorder; i <= imageBorder; i++) {
					int index = center + i * stride - imageBorder;
					for (int j = -imageBorder; j <= imageBorder; j++, index++) {
						// don't compare the center point against itself
						if (i == 0 && j == 0)
							continue;

						if (val <= inten[index]) {
							max = false;
							break escape;
						}
					}
				}

				// add points which are local maximums and are not already contained in the corners list
				if (max && val != Float.MAX_VALUE) {
					corners.add(x, y);
				}
			}
		}
	}
}
