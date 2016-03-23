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

package boofcv.alg.feature.disparity;

import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Scores the disparity for a point using multiple rectangular regions in an effort to reduce errors at object borders,
 * based off te 5 region algorithm described in [1].  Five overlapping regions are considered and the error at each
 * disparity is the center region plus the two regions with the smallest error.  The idea is that only errors for
 * common elements in each image are considered.
 * </p>
 *
 * <p>
 * Scores for individual subregions that the score is computed from are calculated in the same manor as
 * .{@link DisparityScoreSadRect}.  Unlike those classes, a rolling window of scores for rectangular regions
 * is saved.  From this rolling window of scores for the smaller rectangles the final score is computed by
 * sampling five regions as specified in [1].  The sum of the two best outer regions are added to the center region.
 * </p>
 *
 * <p>
 * Since five regions are being sampled to compute the score the overall sample region is larger than the
 * individual sub regions.  The border radius is thus twice the radius of the sub-regions.
 * </p>
 *
 * <p>
 * [1] Heiko Hirschmuller, Peter R. Innocent, and Jon Garibaldi. "Real-Time Correlation-Based Stereo Vision with
 * Reduced Border Errors." Int. J. Comput. Vision 47, 1-3 2002
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DisparityScoreWindowFive
		<Input extends ImageGray, Disparity extends ImageGray>
		extends DisparityScoreRowFormat<Input,Disparity>
{
	public DisparityScoreWindowFive(int minDisparity, int maxDisparity,
									int regionRadiusX, int regionRadiusY) {
		super(minDisparity, maxDisparity, regionRadiusX, regionRadiusY);
	}

	@Override
	public int getBorderX() {
		return radiusX*2;
	}

	@Override
	public int getBorderY() {
		return radiusY*2;
	}
}
