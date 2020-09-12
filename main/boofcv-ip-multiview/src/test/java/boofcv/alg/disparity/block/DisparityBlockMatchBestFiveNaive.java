/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block;

import boofcv.alg.disparity.DisparityBlockMatchBestFive;
import boofcv.factory.disparity.DisparityError;
import boofcv.struct.image.ImageGray;

import java.util.Arrays;

/**
 * Naive version of {@link DisparityBlockMatchBestFive}.
 *
 * @author Peter Abeles
 */
public class DisparityBlockMatchBestFiveNaive<I extends ImageGray<I>>
		extends CommonDisparityBlockMatch<I> {
	// SCores of the four surrounding regions
	double[] four = new double[4];

	public DisparityBlockMatchBestFiveNaive( DisparityError errorType ) {
		super(errorType);
	}

	/**
	 * Compute the score for five local regions and just use the center + the two best
	 *
	 * @param leftX X-axis center left image
	 * @param rightX X-axis center left image
	 * @param centerY Y-axis center for both images
	 * @return Fit score for both regions.
	 */
	@Override
	protected double computeScore( int leftX, int rightX, int centerY ) {
		// Ensure the rows have centers inside the image along Y axis
		int radius_D = radiusY;
		int radius_U = radiusY;

		if (centerY - radius_D < 0)
			radius_D = radiusY + centerY - radius_D;
		if (centerY + radius_U >= left.height)
			radius_U = radiusY - (centerY + radius_U - left.height) - 1;

		float sgn = errorType.isCorrelation() ? -1 : 1;

		double center = computeScoreBlock(leftX, rightX, centerY);

		four[0] = computeScoreBlock(leftX - radiusX, rightX - radiusX, centerY - radius_D);
		four[1] = computeScoreBlock(leftX + radiusX, rightX + radiusX, centerY - radius_D);
		four[2] = computeScoreBlock(leftX - radiusX, rightX - radiusX, centerY + radius_U);
		four[3] = computeScoreBlock(leftX + radiusX, rightX + radiusX, centerY + radius_U);

		// This is a compromise for the border.
		// Only consider what is inside the border. Not worth it to compute scores from completely imagined pixels
		if (rightX - radiusX < 0) {
			four[0] = sgn*Float.MAX_VALUE;
			four[2] = sgn*Float.MAX_VALUE;
		} else if (leftX + radiusX >= left.width) {
			four[1] = sgn*Float.MAX_VALUE;
			four[3] = sgn*Float.MAX_VALUE;
		}

		Arrays.sort(four);
		if (errorType.isCorrelation()) {
			return four[2] + four[3] + center;
		} else {
			return four[0] + four[1] + center;
		}
	}
}
