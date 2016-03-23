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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;


/**
 * <p/>
 * Extracts corners at local maximums that are above a threshold.  Basic unoptimized implementation.
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxExtractorNaive {

	// size of the search area
	protected int radius;
	// the threshold which points must be above to be a feature
	protected float thresh;

	// border around the image in which pixels will not be considered
	protected int border;

	// should it use a strict rule for defining the local max?
	protected boolean useStrictRule;

	public NonMaxExtractorNaive(boolean useStrictRule) {
		this.useStrictRule = useStrictRule;
	}

	public void setSearchRadius(int radius) {
		this.radius = radius;
	}

	public float getThreshold() {
		return thresh;
	}

	public void setThreshold(float thresh) {
		this.thresh = thresh;
	}

	public void setBorder(int border) {
		this.border = border;
	}

	public int getBorder() {
		return border;
	}

	public boolean isStrict() {
		return useStrictRule;
	}

	public void process(GrayF32 intensityImage, QueueCorner peaks) {

		if (useStrictRule)
			strictRule(intensityImage, peaks);
		else
			notStrictRule(intensityImage, peaks);
	}

	private void strictRule(GrayF32 intensityImage, QueueCorner corners) {
		final int imgWidth = intensityImage.getWidth();
		final int imgHeight = intensityImage.getHeight();

		final float inten[] = intensityImage.data;

		for (int y = border; y < imgHeight - border; y++) {
			int center = intensityImage.startIndex + y * intensityImage.stride + border;
			for (int x = border; x < imgWidth - border; x++) {

				float val = inten[center++];
				if (val < thresh) continue;

				boolean max = true;

				int x0 = x - radius;
				int x1 = x + radius;
				int y0 = y - radius;
				int y1 = y + radius;

				if (x0 < 0) x0 = 0;
				if (y0 < 0) y0 = 0;
				if (x1 >= imgWidth) x1 = imgWidth - 1;
				if (y1 >= imgHeight) y1 = imgHeight - 1;

				escape:
				for (int i = y0; i <= y1; i++) {
					int index = intensityImage.startIndex + i * intensityImage.stride + x0;
					for (int j = x0; j <= x1; j++, index++) {
						// don't compare the center point against itself
						if (i == y && j == x)
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

	private void notStrictRule(GrayF32 intensityImage, QueueCorner corners) {
		final int imgWidth = intensityImage.getWidth();
		final int imgHeight = intensityImage.getHeight();

		final float inten[] = intensityImage.data;

		for (int y = border; y < imgHeight - border; y++) {
			int center = intensityImage.startIndex + y * intensityImage.stride + border;
			for (int x = border; x < imgWidth - border; x++) {

				float val = inten[center++];
				if (val < thresh) continue;

				boolean max = true;

				int x0 = x - radius;
				int x1 = x + radius;
				int y0 = y - radius;
				int y1 = y + radius;

				if (x0 < 0) x0 = 0;
				if (y0 < 0) y0 = 0;
				if (x1 >= imgWidth) x1 = imgWidth - 1;
				if (y1 >= imgHeight) y1 = imgHeight - 1;

				escape:
				for (int i = y0; i <= y1; i++) {
					int index = intensityImage.startIndex + i * intensityImage.stride + x0;
					for (int j = x0; j <= x1; j++, index++) {

						if (val < inten[index]) {
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

	public int getSearchRadius() {
		return radius;
	}
}
