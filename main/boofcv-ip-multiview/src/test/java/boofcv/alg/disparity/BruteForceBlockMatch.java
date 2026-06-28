/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity;

import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/// Brute force block matching stereo
public abstract class BruteForceBlockMatch<T extends ImageBase<T>> {

	protected int radius;
	protected int width;
	protected int minDisparity, maxDisparity;
	protected int rangeDisparity;
	ImageBorder<T> bleft, bright;

	protected double[] scores;

	public boolean minimize = true;

	protected BruteForceBlockMatch( BorderType borderType, ImageType<T> imageType ) {
		bleft = FactoryImageBorder.generic(borderType, imageType);
		bright = FactoryImageBorder.generic(borderType, imageType);
	}

	public void configure( int radius, int minDisparity, int maxDisparity ) {
		this.radius = radius;
		this.minDisparity = minDisparity;
		this.maxDisparity = maxDisparity;
		this.rangeDisparity = maxDisparity - minDisparity + 1;
		this.width = radius*2 + 1;
		this.scores = new double[rangeDisparity];
	}

	public void process( T left, T right, GrayU8 disparity ) {
		bleft.setImage(left);
		bright.setImage(right);

		// Columns outside [col0, col1) have no disparity whose match stays inside the image (either border).
		// minDisparity/maxDisparity may be negative.
		int col0 = Math.max(0, minDisparity);
		int col1 = left.width + Math.min(0, maxDisparity);

		for (int y = 0; y < left.height; y++) {
			for (int x = 0; x < col0; x++) {
				disparity.set(x, y, rangeDisparity);
			}
			for (int x = col0; x < col1; x++) {
				// disparity range at this column, clamped so the matched right column (x-d) stays in the image
				int disparityLow = Math.max(minDisparity, x - (left.width - 1));
				int disparityHigh = Math.min(maxDisparity, x);

				int firstRange = disparityLow - minDisparity; // disparity index of the first valid disparity
				int localCount = disparityHigh - disparityLow + 1;

				for (int d = 0; d < localCount; d++) {
					scores[d] = computeScore(bleft, bright, x, y, disparityLow + d);
				}

				int bestRange = firstRange;
				double bestScore = scores[0];

				for (int d = 1; d < localCount; d++) {
					double s = scores[d];
					if (minimize) {
						if (s < bestScore) {
							bestScore = s;
							bestRange = firstRange + d;
						}
					} else {
						if (s > bestScore) {
							bestScore = s;
							bestRange = firstRange + d;
						}
					}
				}

				disparity.set(x, y, bestRange);
			}
			for (int x = col1; x < left.width; x++) {
				disparity.set(x, y, rangeDisparity);
			}
		}
	}

	public abstract double computeScore( ImageBorder<T> left, ImageBorder<T> right, int cx, int cy, int disparity );
}
