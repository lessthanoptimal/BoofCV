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
import jgrl.struct.point.Point2D_I16;

/**
 * <p/>
 * This is a faster version of non-max suppression that remember the value of the last largest
 * known value.  If that value could still be in the window it is compared to the current
 * value of the center of the window.  If it is larger it moves on.  If it is smaller
 * it does the same check as the standard algorithm.
 * <p/>
 *
 * @author Peter Abeles
 */
public class FastNonMaxCornerExtractor implements NonMaxCornerExtractor {

	// search region
	private int radius;
	// minimum intensity value
	private float thresh;
	// image border that should be skipped
	private int border;

	/**
	 * @param minSeparation How close features can be to each other.
	 * @param thresh		What the minimum intensity a feature must have to be considered a feature.
	 */
	public FastNonMaxCornerExtractor(int minSeparation, int border, float thresh) {
		radius = minSeparation;
		this.border = border;
		this.thresh = thresh;

		if (this.border < radius)
			this.border = radius;
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
	public void setMinSeparation(int minSeparation) {
		this.radius = minSeparation;
	}

	/**
	 * Detects corners in the image while excluding corners which are already contained in the corners list.
	 *
	 * @param intensityImage Feature intensity image. Can be modified.
	 * @param corners		Where found corners are stored.  Corners which are already in the list will not be added twice.
	 */
	@Override
	public void process(ImageFloat32 intensityImage, QueueCorner excludeCorners, QueueCorner corners) {
		int imgWidth = intensityImage.getWidth();
		int imgHeight = intensityImage.getHeight();

		// mark corners which have already been found
		if( excludeCorners != null ) {
			for (int i = 0; i < excludeCorners.num; i++) {
				Point2D_I16 pt = excludeCorners.get(i);
				intensityImage.set(pt.x, pt.y, Float.MAX_VALUE);
			}
		}

		final float inten[] = intensityImage.data;

		for (int y = border; y < imgHeight - border; y++) {
			int maxX = Integer.MIN_VALUE;
			float maxValue = -1;

			for (int x = border; x < imgWidth - border; x++) {
				int center = intensityImage.startIndex + y * intensityImage.stride + x;
				float val = inten[center];

				if (val < thresh) continue;

				if (maxX < x || maxValue <= val) {
					boolean isMax = true;

					// todo can this be speed up by cropping the search along the x-axis?
					escape:
					for (int i = -radius; i <= radius; i++) {
						for (int j = -radius; j <= radius; j++) {
							if (i == 0 && j == 0)
								continue;
							float v = inten[center + i * intensityImage.stride + j];

							if (val <= v) {
								isMax = false;
								maxValue = v;
								maxX = x + j + radius;
								break escape;
							}
						}
					}

					if (isMax && val != Float.MAX_VALUE) {
						maxValue = val;
						maxX = x;
						corners.add(x, y);
					}
				}
			}
		}
	}
}